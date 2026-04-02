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
import com.ecodrive.app.database.entities.Brand
import com.ecodrive.app.database.entities.Vehicle
import com.ecodrive.app.database.entities.VehiclePrototype
import com.ecodrive.app.ui.dialog.AddToListDialog
import com.ecodrive.app.ui.mainframe.FooterTab
import com.ecodrive.app.ui.mainframe.MainFrameManager
import com.ecodrive.app.vehicle.VehicleInformation
import com.ecodrive.app.vehicle.plate.data.PlateInformation
import com.ecodrive.app.vehicle.plate.data.PlateType
import com.ecodrive.app.vehicle.plate.data.RegistrationSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

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

        val predictible = vehicle?.let {
            it.brandName.isNotBlank() && it.modelName.isNotBlank()
        } ?: false
        if (predictible) predict()
        else             abort("Aucun véhicule à évaluer.")

        btnFindNearestGarage.setOnClickListener { openNearestGarage() }
        btnPrepareAd.setOnClickListener         { openAdMaker() }
        btnAddToList.setOnClickListener         { openAddToList() }
    }

    @SuppressLint("SetTextI18n")
    private fun predict() {
        // Si vehicle est null, on annule tout
        val vehicleData = vehicle ?: return

        val url = URL("http://10.0.2.2:8000/predict/")

        executor.execute {
            try {
                val jsonBody = JSONObject().apply {
                    put("brand",        vehicleData.brandName)
                    put("model",        vehicleData.modelName)
                    put("age",          vehicleData.year ?: 2024)
                    put("kms",          vehicleData.mileage)
                    put("fuel_type",    vehicleData.energy.label)
                    put("transmission", vehicleData.gearboxType.label)
                    put("ext_col",      vehicleData.color)
                    put("int_col",      vehicleData.color)
                    put("accident",     "None")
                    put("clean_title",  "Yes")
                    put("engine_hp",    vehicleData.powerHp)
                    put("engine_liters",vehicleData.fuelTankCapacityLiters)
                    put("engine_cyl",   0.0)
                }

                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                }

                conn.outputStream.use { os ->
                    val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val code = conn.responseCode
                if (code != 200) {
                    val errorMsg = conn.errorStream?.bufferedReader()?.readText()
                    throw Exception("HTTP $code : $errorMsg")
                }

                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(body)

                if (json.optString("status") != "success") {
                    abort("Échec de l'évaluation du véhicule")
                    return@execute
                }

                val estimatedPrice     = json.getDouble("estimated_price")
                val errorEstimation    = 0.1 * estimatedPrice
                val estimatedPriceNew  = 1.5 * estimatedPrice
                val errorEstimationNew = 0.1 * estimatedPriceNew

                runOnUiThread {
                    usedPriceMin.text = (estimatedPrice - errorEstimation).toInt().toString()
                    usedPriceMax.text = (estimatedPrice + errorEstimation).toInt().toString()
                    newPriceMin.text  = (estimatedPriceNew - errorEstimationNew).toInt().toString()
                    newPriceMax.text  = (estimatedPriceNew + errorEstimationNew).toInt().toString()

                    usedPriceMin.visibility = View.VISIBLE
                    usedPriceMax.visibility = View.VISIBLE
                    newPriceMin.visibility  = View.VISIBLE
                    newPriceMax.visibility  = View.VISIBLE
                }
            } catch (e: Exception) {
                runOnUiThread { abort("Erreur réseau : ${e.message}") }
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

    private fun openNearestGarage() {
        val brand = vehicle?.brandName?.trim()
        if (brand.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.error_no_brand_for_garage), Toast.LENGTH_SHORT).show()
            return
        }
        val query = Uri.encode("Garage $brand")
        val uri = "geo:0,0?q=$query".toUri() // Lance l'appli de carte par défaut
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val webUri = "https://www.openstreetmap.org/search?query=${Uri.encode("Garage $brand")}".toUri()
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    private fun openAdMaker() {
        val intent = Intent(this, AdMakerActivity::class.java).apply {
            vehicle?.let { putExtra(AdMakerActivity.EXTRA_VEHICLE, it) }
            plate?.let   { putExtra(AdMakerActivity.EXTRA_PLATE, it) }
        }
        startActivity(intent)
    }

    private fun openAddToList() {
        val v = vehicle
        val p = plate
        if (v == null) {
            Toast.makeText(this, getString(R.string.error_no_vehicle_to_save), Toast.LENGTH_SHORT).show()
            return
        }

        val immat = p?.number?.trim()?.ifBlank { null } ?: v.vin.ifBlank { null }
        if (immat == null) {
            Toast.makeText(this, getString(R.string.error_no_vehicle_to_save), Toast.LENGTH_SHORT).show()
            return
        }

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            try {
                val dbVehicle: Vehicle = withContext(Dispatchers.IO) {
                    // 1. Insérer ou récupérer la marque
                    // insert() retourne -1 si la marque existe déjà (IGNORE), on la récupère alors par nom
                    val insertedBrandId = db.brand().insert(
                        Brand(name = v.brandName, logoUrl = v.brandLogoUrl)
                    )
                    val brandId = if (insertedBrandId != -1L) {
                        insertedBrandId.toInt()
                    } else {
                        db.brand().getByName(v.brandName)?.id
                            ?: throw IllegalStateException("Impossible de trouver ou créer la marque '${v.brandName}'")
                    }

                    // 2. Insérer ou récupérer le prototype
                    val prototype = VehiclePrototype(
                        name           = v.modelName,
                        version        = v.modelVersion,
                        collection     = v.collection,
                        modelStartDate = v.modelStartDate,
                        modelEndDate   = v.modelEndDate,
                        carBody        = v.body,
                        vehicleType    = v.genre,
                        energy         = v.energy,
                        gearbox        = v.gearboxType,
                        gearboxSpeeds  = v.gearboxPowerLevel,
                        powerKw        = v.powerKw,
                        powerHp        = v.powerHp,
                        fuelTankLiters = v.fuelTankCapacityLiters,
                        doorCount      = v.doorCount,
                        passengerCount = v.passengerCount,
                        lengthMm       = v.lengthMm,
                        widthMm        = v.widthMm,
                        heightMm       = v.heightMm,
                        wheelbaseMm    = v.wheelbaseMm,
                        marqueId       = brandId
                    )
                    val prototypeId = db.vehiclePrototype().insert(prototype)

                    // 3. Construire et insérer le véhicule
                    val vehicle = Vehicle(
                        id                    = immat,
                        immatriculation       = immat,
                        vin                   = v.vin,
                        country               = v.country,
                        areaCode              = p?.departmentCode ?: "",
                        firstRegistrationDate = v.firstRegistrationDate,
                        plateType             = p?.type ?: PlateType.UNKNOW,
                        regSystem             = p?.registrationSystem ?: RegistrationSystem.UNKNOW,
                        validityDate          = p?.validityDate?.toString(),
                        prototypeId           = prototypeId,
                        color                 = v.color,
                        mileage               = v.mileage
                    )
                    db.vehicle().insert(vehicle)
                    vehicle
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
