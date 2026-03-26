package com.example.ecodrive

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import kotlin.jvm.java


class AddVehicleActivity : AppCompatActivity() {
    // UI Components
    private lateinit var loadingScreen: LinearLayout
    private lateinit var formContainer: ScrollView
    private lateinit var vehicleBrand: EditText
    private lateinit var vehicleModel: EditText
    private lateinit var vehicleDate: EditText
    private lateinit var vehicleColor: EditText
    private lateinit var vehicleEnergyType: Spinner
    private lateinit var vehicleGearboxType: Spinner
    private lateinit var vehicleGearboxSpeed: EditText
    private lateinit var mileageSlider: SeekBar
    private lateinit var btnEstimatePrice: Button
    private lateinit var homeBtn: ImageButton

    // API Client
    //private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_vehicle_activity)

        // Initialiser les composants UI
        initViews()

        // Récupérer les informations de la plaque depuis l'intent
        val plateInfo = intent.extras?.let {
            PlateInfo(
                plateNumber = it.getString("immatriculation") ?: "",
                territorialId = it.getString("territorialId"),
                endValidity = it.getString("endValidity"),
                plateType = it.getInt("plateType")
            )
        }

        // Afficher l'écran de chargement et appeler l'API
        showLoading(true)
        plateInfo?.let { fetchVehicleData(it) }
    }

    private fun initViews() {
        // Récupérer les vues depuis le layout
        loadingScreen = findViewById(R.id.loadingScreen)
        formContainer = findViewById(R.id.formContainer)
        vehicleBrand = findViewById(R.id.vehicleBrand)
        vehicleModel = findViewById(R.id.vehicleModel)
        vehicleDate = findViewById(R.id.vehicleDate)
        vehicleColor = findViewById(R.id.vehicleColor)
        vehicleEnergyType = findViewById(R.id.vehicleEnergyType)
        vehicleGearboxType = findViewById(R.id.vehicleGearboxType)
        vehicleGearboxSpeed = findViewById(R.id.vehicleGearboxSpeed)
        mileageSlider = findViewById(R.id.mileageSlider)
        btnEstimatePrice = findViewById(R.id.btnEstimatePrice)
        homeBtn = findViewById(R.id.btnHome)

        // Configurer les Spinners
        setupSpinners()

        // Configurer le bouton de validation
        btnEstimatePrice.setOnClickListener { goToPredictActivity() }

        // Configurer le bouton Home
        homeBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finishAffinity() // Termine toutes les activités de la pile
        }
    }

    private fun setupSpinners() {
        // Données pour les Spinners
        val energyTypes = arrayOf(
            getString(R.string.energyTypeFuel),
            getString(R.string.energyTypeDiesel),
            getString(R.string.energyTypeElec),
        )
        val gearboxTypes = arrayOf(
            getString(R.string.gearboxTypesHand),
            getString(R.string.gearboxTypesAuto)
        )

        // Adapter pour les Spinners
        val energyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, energyTypes)
        energyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vehicleEnergyType.adapter = energyAdapter

        val gearboxAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gearboxTypes)
        gearboxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vehicleGearboxType.adapter = gearboxAdapter
    }

    private fun showLoading(isLoading: Boolean) {
        // Affiche ou cache l'écran de chargement
        loadingScreen.visibility = if (isLoading) View.VISIBLE else View.GONE
        formContainer.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun fetchVehicleData(plateInfo: PlateInfo) {
        lifecycleScope.launch {
            try {
                // Appeler l'API pour obtenir les données du véhicule
                val vehicleData = withContext(Dispatchers.IO) { callVehicleApi(plateInfo.plateNumber) }

                // Remplir le formulaire avec les données de l'API
                withContext(Dispatchers.Main) {
                    fillForm(vehicleData)
                    showLoading(false)
                }
            } catch (e: IOException) {
                // Gérer les erreurs de connexion
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddVehicleActivity, "Erreur de connexion", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            }
        }
    }

    private fun callVehicleApi(plateNumber: String): JSONObject {
        // Exemple d'appel API (à adapter selon ton endpoint réel)
        val urlRequest = ("https://api.example.com/vehicle?plate=$plateNumber")

        execute(urlRequest) { response ->
            if (!response.isSuccessful) throw IOException("Réponse API invalide : ${response.code}")
            val responseBody = response.body?.string() ?: throw IOException("Réponse vide")
            return JSONObject(responseBody)
        }
    }

    private fun fillForm(vehicleData: JSONObject) {
        // Remplir les champs avec les données de l'API
        vehicleBrand.setText(vehicleData.optString("brand", ""))
        vehicleModel.setText(vehicleData.optString("model", ""))
        vehicleDate.setText(vehicleData.optString("registrationDate", ""))
        vehicleColor.setText(vehicleData.optString("color", ""))

        // Sélectionner les valeurs des Spinners si disponibles
        val energyType = vehicleData.optString("energyType", "")
        if (energyType.isNotEmpty()) {
            val energyAdapter = vehicleEnergyType.adapter as ArrayAdapter<String>
            val energyPosition = energyAdapter.getPosition(energyType)
            if (energyPosition >= 0) vehicleEnergyType.setSelection(energyPosition)
        }

        val gearboxType = vehicleData.optString("gearboxType", "")
        if (gearboxType.isNotEmpty()) {
            val gearboxAdapter = vehicleGearboxType.adapter as ArrayAdapter<String>
            val gearboxPosition = gearboxAdapter.getPosition(gearboxType)
            if (gearboxPosition >= 0) vehicleGearboxType.setSelection(gearboxPosition)
        }

        vehicleGearboxSpeed.setText(vehicleData.optString("gearboxSpeed", ""))

        // Configurer le SeekBar pour le kilométrage
        val mileage = vehicleData.optInt("mileage", 150000)
        mileageSlider.progress = mileage
    }

    private fun goToPredictActivity() {
        // Récupérer les données du formulaire
        val formData = mapOf(
            "brand" to vehicleBrand.text.toString(),
            "model" to vehicleModel.text.toString(),
            "date" to vehicleDate.text.toString(),
            "color" to vehicleColor.text.toString(),
            "energyType" to vehicleEnergyType.selectedItem.toString(),
            "gearboxType" to vehicleGearboxType.selectedItem.toString(),
            "gearboxSpeed" to vehicleGearboxSpeed.text.toString(),
            "mileage" to mileageSlider.progress.toString()
        )

        // Passer les données à PredictActivity
        val intent = Intent(this, PredictActivity::class.java).apply {
            putExtra("vehicleData", formData as java.io.Serializable)
        }
        startActivity(intent)
    }
}