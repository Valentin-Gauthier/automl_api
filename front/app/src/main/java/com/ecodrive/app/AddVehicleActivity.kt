package com.ecodrive.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecodrive.app.mainframe.FooterTab
import com.ecodrive.app.mainframe.MainFrameManager
import com.ecodrive.app.vehicle.GearboxEnergy
import com.ecodrive.app.vehicle.GearboxType
import com.ecodrive.app.vehicle.SpinnerItem
import com.ecodrive.app.vehicle.VehicleInformation
import com.ecodrive.app.vehicle.plate.PlateAPI
import com.ecodrive.app.vehicle.plate.data.PlateInformation

class AddVehicleActivity : AppCompatActivity() {

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

    // Identité
    private lateinit var vehicleBrand: EditText
    private lateinit var vehicleModel: EditText
    private lateinit var vehicleDate: EditText
    private lateinit var vehicleColor: EditText

    // Motorisation
    private lateinit var vehicleEnergyType: Spinner
    private lateinit var vehicleGearboxType: Spinner
    private lateinit var vehicleGearboxSpeed: EditText

    // Kilométrage
    private lateinit var mileageSlider: SeekBar

    // Validation
    private lateinit var btnEstimatePrice: Button

    //
    // État interne
    //

    private var baseVehicle: VehicleInformation = VehicleInformation.new()
    private var plate: PlateInformation? = null

    /** Adapters pour les spinners */
    private lateinit var energyAdapter: ArrayAdapter<String>
    private lateinit var gearboxAdapter: ArrayAdapter<String>

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_vehicle)
        frameManager = MainFrameManager(this, FooterTab.ADD)

        bindViews()
        setupSpinners()
        setupSlider()
        setupValidation()

        // Pré-remplissage si on vient du scan
        plate = intentParcelable(EXTRA_PLATE, PlateInformation::class.java)
        val prefilled = intentParcelable(EXTRA_VEHICLE, VehicleInformation::class.java)

        plate?.let { plate ->
            PlateAPI().fetchVehicle(
                plate     = plate,
                onSuccess = {
                    runOnUiThread {
                        baseVehicle = it
                        prefill(it)
                        Toast.makeText(
                            this,
                            "Véhicule détecté : ${it.displayName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError   = {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            it,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        if (prefilled != null) {
            baseVehicle = prefilled
            prefill(prefilled)
        }

        btnEstimatePrice.setOnClickListener { goToPredictActivity() }
    }

    private fun <T : android.os.Parcelable> intentParcelable(key: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(key, clazz)
        else
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(key)

    // -------------------------------------------------------------------------
    // Initialisation des vues
    // -------------------------------------------------------------------------

    private fun bindViews() {
        vehicleBrand        = findViewById(R.id.vehicleBrand)
        vehicleModel        = findViewById(R.id.vehicleModel)
        vehicleDate         = findViewById(R.id.vehicleDate)
        vehicleColor        = findViewById(R.id.vehicleColor)
        vehicleEnergyType   = findViewById(R.id.vehicleEnergyType)
        vehicleGearboxType  = findViewById(R.id.vehicleGearboxType)
        vehicleGearboxSpeed = findViewById(R.id.vehicleGearboxSpeed)
        mileageSlider       = findViewById(R.id.mileageSlider)
        btnEstimatePrice    = findViewById(R.id.btnEstimatePrice)
    }

    // -------------------------------------------------------------------------
    // Spinners
    // -------------------------------------------------------------------------

    /**
     * Construit un ArrayAdapter<String> depuis un tableau d'enum [SpinnerItem],
     * en excluant l'entrée UNKNOW.
     */
    private fun <T> buildSpinnerAdapter(items: Array<T>): ArrayAdapter<String>
            where T : Enum<T>, T : SpinnerItem<*> {
        val labels = items
            .filter { it.name != "UNKNOW" }
            .map { it.label }
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
            .also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
    }

    private fun setupSpinners() {
        energyAdapter  = buildSpinnerAdapter(GearboxEnergy.entries.toTypedArray())
        gearboxAdapter = buildSpinnerAdapter(GearboxType.entries.toTypedArray())

        vehicleEnergyType.adapter  = energyAdapter
        vehicleGearboxType.adapter = gearboxAdapter

        val manualTypes = setOf(
            GearboxType.MANUELLE.label,
            GearboxType.SEQUENTIELLE.label
        )

        vehicleGearboxType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selected = gearboxAdapter.getItem(position) ?: ""
                vehicleGearboxSpeed.visibility =
                    if (selected in manualTypes) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                vehicleGearboxSpeed.visibility = View.GONE
            }
        }
    }

    // -------------------------------------------------------------------------
    // Slider kilométrage
    // -------------------------------------------------------------------------

    private fun setupSlider() {
        mileageSlider.max = MILEAGE_MAX_STEPS
        mileageSlider.progress = MILEAGE_FR_MEAN / MILEAGE_STEP_KM
    }

    // -------------------------------------------------------------------------
    // Validations
    // -------------------------------------------------------------------------

    private fun setupValidation() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateEstimateButton() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        vehicleBrand.addTextChangedListener(watcher)
        vehicleModel.addTextChangedListener(watcher)
        updateEstimateButton()
    }

    private fun updateEstimateButton() {
        val ok = vehicleBrand.text.isNotBlank() && vehicleModel.text.isNotBlank()
        btnEstimatePrice.isEnabled = ok
        btnEstimatePrice.alpha     = if (ok) 1f else 0.4f
    }

    private fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (vehicleBrand.text.isBlank())
            errors += getString(R.string.error_brand_required)

        if (vehicleModel.text.isBlank())
            errors += getString(R.string.error_model_required)

        val gearboxLabel = gearboxAdapter.getItem(vehicleGearboxType.selectedItemPosition) ?: ""
        val isManual = gearboxLabel in setOf(GearboxType.MANUELLE.label, GearboxType.SEQUENTIELLE.label)
        if (isManual) {
            val speeds = vehicleGearboxSpeed.text.toString().toIntOrNull()
            if (speeds == null || speeds < 4 || speeds > 8)
                errors += getString(R.string.error_gearbox_speed_invalid)
        }

        return errors
    }

    // -------------------------------------------------------------------------
    // Pré-remplissage
    // -------------------------------------------------------------------------

    private fun prefill(info: VehicleInformation) {
        vehicleBrand.setText(info.brandName)
        vehicleModel.setText(info.modelName)
        vehicleDate.setText(info.firstRegistrationDate)
        vehicleColor.setText(info.color)
        selectSpinnerByLabel(vehicleEnergyType,  energyAdapter,  info.energy.label)
        selectSpinnerByLabel(vehicleGearboxType, gearboxAdapter, info.gearboxType.label)
        if (info.mileage >= 0) {
            mileageSlider.progress = minOf(info.mileage / MILEAGE_STEP_KM, MILEAGE_MAX_STEPS)
        }
    }

    /** Sélectionne l'item du spinner dont le label correspond. */
    private fun selectSpinnerByLabel(
        spinner: Spinner,
        adapter: ArrayAdapter<String>,
        label: String
    ) {
        val pos = (0 until adapter.count).firstOrNull { adapter.getItem(it) == label } ?: return
        spinner.setSelection(pos)
    }

    // -------------------------------------------------------------------------
    // Action : lancer PredictActivity
    // -------------------------------------------------------------------------

    private fun goToPredictActivity() {
        val errors = validate()
        if (errors.isNotEmpty()) {
            return
        }

        val vehicle = buildVehicleInformation()

        val intent = Intent(this, PredictActivity::class.java)
            .putExtra(EXTRA_PLATE, plate)
            .putExtra(EXTRA_VEHICLE, vehicle)
        startActivity(intent)

        // Temporaire : toast de confirmation
        Toast.makeText(
            this,
            "→ ${vehicle.displayName} · ${formatMileage(vehicle.mileage)}",
            Toast.LENGTH_LONG
        ).show()
    }

    // -------------------------------------------------------------------------
    // Construction de VehicleInformation depuis le formulaire
    // -------------------------------------------------------------------------

    /**
     * Construit un [VehicleInformation] à partir des champs du formulaire.
     */
    private fun buildVehicleInformation(): VehicleInformation {
        // Extraire les informations du formulaire
        val brandName    = vehicleBrand.text.toString().trim()
        val modelName    = vehicleModel.text.toString().trim()
        val firstRegistrationDate = vehicleDate.text.toString().trim()
        val color        = vehicleColor.text.toString().trim()
        val energyLabel  = energyAdapter.getItem(vehicleEnergyType.selectedItemPosition)   ?: ""
        val gearboxLabel = gearboxAdapter.getItem(vehicleGearboxType.selectedItemPosition) ?: ""
        val gearboxSpeed = vehicleGearboxSpeed.text.toString().toIntOrNull() ?: 0
        val mileageKm    = mileageSlider.progress * MILEAGE_STEP_KM

        // Analyser les informations obtenues
        val brandChanged = brandName != baseVehicle.brandName
        val modelChanged = modelName != baseVehicle.modelName

        // Rassembler les informations
        return baseVehicle.copy(
            vin            = if (brandChanged) "" else baseVehicle.vin,
            country        = if (brandChanged) "FR" else baseVehicle.country,
            brandName      = brandName,
            brandLogoUrl   = if (brandChanged) "" else baseVehicle.brandLogoUrl,
            modelName      = modelName,
            modelVersion   = if (modelChanged) "" else baseVehicle.modelVersion,
            modelStartDate = if (modelChanged) "" else baseVehicle.modelStartDate,
            modelEndDate   = if (modelChanged) "" else baseVehicle.modelEndDate,
            genreCode      = if (modelChanged) null else baseVehicle.genreCode,
            bodyCode       = if (modelChanged) null else baseVehicle.bodyCode,
            gearboxTypeCode = GearboxType.codeFromLabel(gearboxLabel),
            gearboxPowerLevel = gearboxSpeed,
            energyCode     = GearboxEnergy.codeFromLabel(energyLabel),
            powerKw        = if (modelChanged) 0 else baseVehicle.powerKw,
            powerHp        = if (modelChanged) 0 else baseVehicle.powerHp,
            passengerCount = if (modelChanged) 0 else baseVehicle.passengerCount,
            doorCount      = if (modelChanged) 0 else baseVehicle.doorCount,
            color          = color,
            weightKg       = if (modelChanged) 0 else baseVehicle.weightKg,
            grossWeightKg  = if (modelChanged) 0 else baseVehicle.grossWeightKg,
            lengthMm       = if (modelChanged) 0 else baseVehicle.lengthMm,
            widthMm        = if (modelChanged) 0 else baseVehicle.widthMm,
            heightMm       = if (modelChanged) 0 else baseVehicle.heightMm,
            wheelbaseMm    = if (modelChanged) 0 else baseVehicle.wheelbaseMm,
            firstRegistrationDate = firstRegistrationDate,
            fuelTankCapacityLiters = if (modelChanged) 0 else baseVehicle.fuelTankCapacityLiters,
            collection     = if (modelChanged) "" else baseVehicle.collection,
            mileage        = mileageKm
        )
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    private fun formatMileage(km: Int): String =
        "%,d km".format(km).replace(',', '\u00A0')

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    companion object {
        const val EXTRA_VEHICLE        = "extra_vehicle_information"
        const val EXTRA_PLATE          = "extra_plate_information"
        private const val MILEAGE_MAX_STEPS = 500    // 500 × 1 000 = 500 000 km
        private const val MILEAGE_STEP_KM   = 1_000
        private const val MILEAGE_FR_MEAN   = 75_000
    }
}
