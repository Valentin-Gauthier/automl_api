package com.ecodrive.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ecodrive.app.database.AppDatabase
import com.ecodrive.app.database.entities.Vehicle
import com.ecodrive.app.ui.dialog.AddToListDialog
import com.ecodrive.app.ui.mainframe.FooterTab
import com.ecodrive.app.ui.mainframe.MainFrameManager
import com.ecodrive.app.vehicle.VehicleInformation
import com.ecodrive.app.vehicle.plate.data.PlateInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class PredictActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    //
    // Managers des sous-composants
    //

    private lateinit var frameManager: MainFrameManager

    //
    // Composant construit pour l'activité.
    //

    private lateinit var usedPriceMin: TextView
    private lateinit var usedPriceMax: TextView
    private lateinit var newPriceMin:  TextView
    private lateinit var newPriceMax:  TextView

    private var estimatedPrice: Double = 0.0

    private lateinit var btnFindNearestGarage: Button
    private lateinit var btnPrepareAd: Button
    private lateinit var btnAddToList: Button

    //
    // État interne
    //

    private var vehicle: VehicleInformation? = null
    private var plate:   PlateInformation?   = null
    private val executor = Executors.newSingleThreadExecutor()

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_predict)
        frameManager = MainFrameManager(this, FooterTab.PREDICT)

        usedPriceMin = findViewById(R.id.usedPriceMin)
        usedPriceMax = findViewById(R.id.usedPriceMax)
        newPriceMin  = findViewById(R.id.newPriceMin)
        newPriceMax  = findViewById(R.id.newPriceMax)

        btnFindNearestGarage = findViewById(R.id.btnFindNearestGarage)
        btnPrepareAd = findViewById(R.id.btnPrepareAd)
        btnAddToList = findViewById(R.id.btnAddToList)

        // Obtenir les informations sur le véhicule
        plate   = intentParcelable(EXTRA_PLATE, PlateInformation::class.java)
        vehicle = intentParcelable(EXTRA_VEHICLE, VehicleInformation::class.java)

        usedPriceMin.visibility = View.GONE
        usedPriceMax.visibility = View.GONE
        newPriceMin.visibility  = View.GONE
        newPriceMax.visibility  = View.GONE
        var predictible = false
        vehicle?.let { v ->
            if (v.brandName.isNotBlank() && v.modelName.isNotBlank()) {
                predictible = true
            }
        }
        if (predictible)
            predict()
        else
            abort("Aucun véhicule à évaluer.")

        btnFindNearestGarage.setOnClickListener { openNearestGarage() }
        btnPrepareAd.setOnClickListener         { openAdMaker() }
        btnAddToList.setOnClickListener         { openAddToList() }
    }

    @SuppressLint("SetTextI18n")
    private fun predict() {
        // Si vehicle est null, on annule tout
        val vehicleData = vehicle ?: return

        // 1. L'URL propre (Vérifie si c'est / ou /predict dans ton main.py)
        val url = URL("http://10.0.2.2:8000/predict/")

        executor.execute {
            try {
                // 2. Création du corps JSON (Le fameux Body)
                val jsonBody = JSONObject().apply {
                    put("brand", vehicleData.brandName)
                    put("model", vehicleData.modelName)
                    put("age", vehicleData.year ?: 2024)
                    put("kms", vehicleData.mileage)
                    put("fuel_type", vehicleData.energy.label)
                    put("transmission", vehicleData.gearboxType.label)
                    put("ext_col", vehicleData.color)
                    put("int_col", vehicleData.color)
                    put("accident", "None")
                    put("clean_title", "Yes")
                    put("engine_hp", vehicleData.powerHp)
                    put("engine_liters", vehicleData.fuelTankCapacityLiters)
                    put("engine_cyl", 0.0)
                }

                // 3. Configuration de la requête
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8") // On prévient l'API que c'est du JSON
                    setRequestProperty("Accept", "application/json")
                    doOutput = true // On autorise l'envoi de données dans le Body
                }

                // 4. Envoi du JSON à ton API FastAPI
                conn.outputStream.use { os ->
                    val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                // 5. Vérification de la réponse
                val code = conn.responseCode
                if (code != 200) {
                    // Astuce : On lit le message d'erreur de FastAPI (le code 422 par exemple)
                    val errorMsg = conn.errorStream?.bufferedReader()?.readText()
                    throw Exception("HTTP $code : $errorMsg")
                }

                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(body)

                // 6. On s'adapte à TA réponse Python : {"status" : "sucess"}
                if (json.optString("status") != "sucess") {
                    abort("Échec de l'évaluation du véhicule")
                    return@execute
                }

                // On récupère le prix (ton API renvoie un float, donc on lit un Double en Kotlin)
                estimatedPrice = json.getDouble("estimated_price")
                val error_estimation = 0.1 * estimatedPrice

                // 7. 🚨 MODIFICATION UI : Doit se faire sur le thread principal !
                runOnUiThread {
                    // On convertit en Int pour l'affichage (pas de centimes)
                    usedPriceMin.text = (estimatedPrice - error_estimation).toInt().toString()
                    usedPriceMax.text = (estimatedPrice + error_estimation).toInt().toString()
                    newPriceMin.text  = ((estimatedPrice - error_estimation).toInt() * 1.86).roundToInt().toString()
                    newPriceMax.text  = ((estimatedPrice + error_estimation).toInt() * 1.86).roundToInt().toString()

                    usedPriceMin.visibility = View.VISIBLE
                    usedPriceMax.visibility = View.VISIBLE
                    newPriceMin.visibility  = View.VISIBLE
                    newPriceMax.visibility  = View.VISIBLE
                }

            } catch (e: Exception) {
                runOnUiThread {
                    abort("Erreur réseau : ${e.message}")
                }
            }
        }
    }

    private fun abort(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        window.decorView.postDelayed({ finish() }, 500)
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /** Ouvre l'application de carte par défaut pour chercher un garage de la marque. */
    private fun openNearestGarage() {
        val brand = vehicle?.brandName?.trim()
        if (brand.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.error_no_brand_for_garage), Toast.LENGTH_SHORT).show()
            return
        }
        val query = Uri.encode("Garage $brand")
        // geo:0,0?q=query lance l'appli de carte par défaut (Google Maps, OsmAnd, Waze…)
        val uri = "geo:0,0?q=$query".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback : ouvrir OpenStreetMap dans le navigateur
            val webUri =
                "https://www.openstreetmap.org/search?query=${Uri.encode("Garage $brand")}".toUri()
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    /** Lance AdMakerActivity pour préparer une annonce. */
    private fun openAdMaker() {
        val intent = Intent(this, AdMakerActivity::class.java).apply {
            vehicle?.let { putExtra(AdMakerActivity.EXTRA_VEHICLE, it) }
            plate?.let   { putExtra(AdMakerActivity.EXTRA_PLATE, it) }
            estimatedPrice?.let   { putExtra(AdMakerActivity.EXTRA_ESTIMATE_PRICE, it) }
        }
        startActivity(intent)
    }

    /** Ouvre le dialog d'ajout à une liste. Sauvegarde d'abord le véhicule en BDD. */
    private fun openAddToList() {
        val v = vehicle
        val p = plate
        if (v == null) {
            Toast.makeText(this, getString(R.string.error_no_vehicle_to_save), Toast.LENGTH_SHORT).show()
            return
        }

        p?.number?.trim()?.ifBlank { null }?.let {
            val dbVehicle = Vehicle(
                immatriculation = it,
                date = v.firstRegistrationDate,
                couleur = v.color,
                mileage = v.mileage,
                priceNewMin = 0,
                priceNewMax = 0,
                priceUsedMin = 0,
                priceUsedMax = 0,
                pp = v.brandLogoUrl,
                prototypeId = 0
            )

            val db = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        db.vehicle().insert(dbVehicle)
                    }
                    AddToListDialog(this@PredictActivity, dbVehicle) {
                        Toast.makeText(
                            this@PredictActivity,
                            getString(R.string.vehicle_saved_to_list),
                            Toast.LENGTH_LONG
                        ).show()
                    }.show()
                } catch (_: Exception) {
                    Toast.makeText(
                        this@PredictActivity,
                        getString(R.string.error_saving_vehicle),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun <T : android.os.Parcelable> intentParcelable(key: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(key, clazz)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(key)

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    companion object {
        const val EXTRA_VEHICLE = "extra_vehicle_information"
        const val EXTRA_PLATE   = "extra_plate_information"
    }
}
