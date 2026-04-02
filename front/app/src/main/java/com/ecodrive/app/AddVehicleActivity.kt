package com.ecodrive.app

import android.app.DatePickerDialog
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecodrive.app.ui.mainframe.FooterTab
import com.ecodrive.app.ui.mainframe.MainFrameManager
import com.ecodrive.app.vehicle.CarBody
import com.ecodrive.app.vehicle.GearboxEnergy
import com.ecodrive.app.vehicle.GearboxType
import com.ecodrive.app.vehicle.GenreVehicle
import com.ecodrive.app.vehicle.SpinnerItem
import com.ecodrive.app.vehicle.VehicleInformation
import com.ecodrive.app.vehicle.plate.PlateAPI
import com.ecodrive.app.vehicle.plate.data.PlateInformation
import java.util.Calendar

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
    private lateinit var vehicleBrand:   EditText
    private lateinit var vehicleModel:   EditText
    private lateinit var vehicleVersion: EditText
    private lateinit var vehicleDate: EditText
    private lateinit var vehicleColor:   EditText
    private lateinit var vehicleVin:     EditText
    private lateinit var vehicleCountry: EditText

    // Motorisation
    private lateinit var vehicleEnergyType:   Spinner
    private lateinit var vehicleGearboxType:  Spinner
    private lateinit var vehicleGearboxSpeed: EditText
    private lateinit var vehiclePowerKw:      EditText
    private lateinit var vehiclePowerHp:      EditText

    // CarBody
    private lateinit var vehicleBodyType:   Spinner
    private lateinit var vehicleGenreType:  Spinner
    private lateinit var vehicleDoors:      EditText
    private lateinit var vehiclePassengers: EditText

    // Dimensions ET poids
    private lateinit var vehicleWeightKg:      EditText
    private lateinit var vehicleGrossWeightKg: EditText
    private lateinit var vehicleFuelTank:      EditText

    // Kilométrage
    private lateinit var mileageSlider:    SeekBar
    private lateinit var mileageValueText: TextView

    // Validation
    private lateinit var btnEstimatePrice: Button

    //
    // État interne
    //

    private var baseVehicle: VehicleInformation = VehicleInformation.new()
    private var plate: PlateInformation? = null

    /** Adapters pour les spinners */
    private lateinit var energyAdapter:  ArrayAdapter<String>
    private lateinit var gearboxAdapter: ArrayAdapter<String>
    private lateinit var bodyAdapter:    ArrayAdapter<String>
    private lateinit var genreAdapter:   ArrayAdapter<String>

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
        setupDatePicker()
        setupValidation()

        // Pré-remplissage si on vient du scan
        plate = intentParcelable(EXTRA_PLATE, PlateInformation::class.java)
        val prefilled = intentParcelable(EXTRA_VEHICLE, VehicleInformation::class.java)

        plate?.let { p ->
            val apiToken = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
                .getString(SettingsActivity.KEY_API_TOKEN, "")?.ifBlank { null }
                ?: PlateAPI.TOKEN_DEMO
            PlateAPI(apiToken).fetchVehicle(
                plate     = p,
                onSuccess = {
                    runOnUiThread {
                        baseVehicle = it
                        prefill(it)
                        Toast.makeText(
                            this,
                            "Véhicule détecté : ${it.displayName}",
                            Toast.LENGTH_LONG
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
        else @Suppress("DEPRECATION") intent.getParcelableExtra(key)

    // -------------------------------------------------------------------------
    // Initialisation des vues
    // -------------------------------------------------------------------------

    private fun bindViews() {
        vehicleBrand        = findViewById(R.id.vehicleBrand)
        vehicleModel        = findViewById(R.id.vehicleModel)
        vehicleVersion      = findViewById(R.id.vehicleVersion)
        vehicleDate         = findViewById(R.id.vehicleDate)
        vehicleColor        = findViewById(R.id.vehicleColor)
        vehicleVin          = findViewById(R.id.vehicleVin)
        vehicleCountry      = findViewById(R.id.vehicleCountry)
        vehicleEnergyType   = findViewById(R.id.vehicleEnergyType)
        vehicleGearboxType  = findViewById(R.id.vehicleGearboxType)
        vehicleGearboxSpeed = findViewById(R.id.vehicleGearboxSpeed)
        vehiclePowerKw      = findViewById(R.id.vehiclePowerKw)
        vehiclePowerHp      = findViewById(R.id.vehiclePowerHp)
        vehicleBodyType     = findViewById(R.id.vehicleBodyType)
        vehicleGenreType    = findViewById(R.id.vehicleGenreType)
        vehicleDoors        = findViewById(R.id.vehicleDoors)
        vehiclePassengers   = findViewById(R.id.vehiclePassengers)
        vehicleWeightKg     = findViewById(R.id.vehicleWeightKg)
        vehicleGrossWeightKg= findViewById(R.id.vehicleGrossWeightKg)
        vehicleFuelTank     = findViewById(R.id.vehicleFuelTank)
        mileageSlider       = findViewById(R.id.mileageSlider)
        mileageValueText    = findViewById(R.id.mileageValueText)
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
        bodyAdapter    = buildSpinnerAdapter(CarBody.entries.toTypedArray())
        genreAdapter   = buildSpinnerAdapter(GenreVehicle.entries.toTypedArray())

        vehicleEnergyType.adapter  = energyAdapter
        vehicleGearboxType.adapter = gearboxAdapter
        vehicleBodyType.adapter    = bodyAdapter
        vehicleGenreType.adapter   = genreAdapter

        val manualTypes = setOf(
            GearboxType.MANUELLE.label,
            GearboxType.SEQUENTIELLE.label
        )

        vehicleGearboxType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
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

    private fun setupDatePicker() {
        vehicleDate.setOnClickListener {
            val selectedDate = vehicleDate.text
            val cal = Calendar.getInstance()
            if (selectedDate.isNotBlank()) {
                runCatching {
                    val p = selectedDate.split("-")
                    cal.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
                }
            }
            DatePickerDialog(this, { _, year, month, day ->
                vehicleDate.text = getString(R.string.date_format).format(year, month + 1, day)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .also { it.datePicker.maxDate = System.currentTimeMillis() }
                .show()
        }
    }

    private fun setupSlider() {
        mileageSlider.max      = MILEAGE_MAX_STEPS
        mileageSlider.progress = MILEAGE_FR_MEAN / MILEAGE_STEP_KM
        updateMileageLabel(mileageSlider.progress)
        mileageSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { updateMileageLabel(progress) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    // -------------------------------------------------------------------------
    // Validations
    // -------------------------------------------------------------------------

    private fun updateMileageLabel(progress: Int) {
        mileageValueText.text = formatMileage(progress * MILEAGE_STEP_KM)
    }

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
        vehicleVersion.setText(info.modelVersion)
        vehicleColor.setText(info.color)
        vehicleVin.setText(info.vin)
        vehicleCountry.setText(info.country)
        if (info.powerKw > 0) vehiclePowerKw.setText(info.powerKw.toString())
        if (info.powerHp > 0) vehiclePowerHp.setText(info.powerHp.toString())
        if (info.doorCount > 0) vehicleDoors.setText(info.doorCount.toString())
        if (info.passengerCount > 0) vehiclePassengers.setText(info.passengerCount.toString())
        if (info.weightKg > 0) vehicleWeightKg.setText(info.weightKg.toString())
        if (info.grossWeightKg > 0) vehicleGrossWeightKg.setText(info.grossWeightKg.toString())
        if (info.fuelTankCapacityLiters > 0) vehicleFuelTank.setText(info.fuelTankCapacityLiters.toString())

        if (info.firstRegistrationDate.isNotBlank()) {
            val parts = info.firstRegistrationDate.split("-")
            if (parts.size == 3)
                vehicleDate.text = getString(R.string.date_format).format(parts[2], parts[1], parts[0])
        }

        selectSpinnerByLabel(vehicleEnergyType,  energyAdapter,  info.energy.label)
        selectSpinnerByLabel(vehicleGearboxType, gearboxAdapter, info.gearboxType.label)
        selectSpinnerByLabel(vehicleBodyType,    bodyAdapter,    info.body.label)
        selectSpinnerByLabel(vehicleGenreType,   genreAdapter,   info.genre.label)

        if (info.mileage >= 0) {
            val progress = minOf(info.mileage / MILEAGE_STEP_KM, MILEAGE_MAX_STEPS)
            mileageSlider.progress = progress
            updateMileageLabel(progress)
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
            Toast.makeText(this, errors.first(), Toast.LENGTH_SHORT).show()
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
        val modelVersion = vehicleVersion.text.toString().trim()
        val color        = vehicleColor.text.toString().trim()
        val vin          = vehicleVin.text.toString().trim()
        val country      = vehicleCountry.text.toString().trim().ifBlank { "FR" }
        val energyLabel  = energyAdapter.getItem(vehicleEnergyType.selectedItemPosition) ?: ""
        val gearboxLabel = gearboxAdapter.getItem(vehicleGearboxType.selectedItemPosition) ?: ""
        val bodyLabel    = bodyAdapter.getItem(vehicleBodyType.selectedItemPosition) ?: ""
        val genreLabel   = genreAdapter.getItem(vehicleGenreType.selectedItemPosition) ?: ""
        val gearboxSpeed = vehicleGearboxSpeed.text.toString().toIntOrNull() ?: 0
        val powerKw      = vehiclePowerKw.text.toString().toIntOrNull() ?: 0
        val powerHp      = vehiclePowerHp.text.toString().toIntOrNull() ?: 0
        val doors        = vehicleDoors.text.toString().toIntOrNull() ?: 0
        val passengers   = vehiclePassengers.text.toString().toIntOrNull() ?: 0
        val weightKg     = vehicleWeightKg.text.toString().toIntOrNull() ?: 0
        val grossWeightKg = vehicleGrossWeightKg.text.toString().toIntOrNull() ?: 0
        val fuelTank     = vehicleFuelTank.text.toString().toIntOrNull() ?: 0
        val mileageKm    = mileageSlider.progress * MILEAGE_STEP_KM
        val selectedDate = vehicleDate.text.toString()

        // Analyser les informations obtenues
        val brandChanged = brandName != baseVehicle.brandName
        val modelChanged = modelName != baseVehicle.modelName

        // Rassembler les informations
        return baseVehicle.copy(
            vin            = vin,
            country        = country,
            brandName      = brandName,
            brandLogoUrl   = if (brandChanged) "" else baseVehicle.brandLogoUrl,
            modelName      = modelName,
            modelVersion   = modelVersion,
            modelStartDate = if (modelChanged) "" else baseVehicle.modelStartDate,
            modelEndDate   = if (modelChanged) "" else baseVehicle.modelEndDate,
            genreCode      = GenreVehicle.codeFromLabel(genreLabel),
            bodyCode       = CarBody.codeFromLabel(bodyLabel),
            gearboxTypeCode   = GearboxType.codeFromLabel(gearboxLabel),
            gearboxPowerLevel = gearboxSpeed,
            energyCode     = GearboxEnergy.codeFromLabel(energyLabel),
            powerKw        = if (powerKw > 0) powerKw else 0,
            powerHp        = if (powerHp > 0) powerHp else 0,
            passengerCount = if (passengers > 0) passengers else 0,
            doorCount      = if (doors > 0) doors else 0,
            color          = color,
            weightKg       = if (weightKg > 0) weightKg else 0,
            grossWeightKg  = if (grossWeightKg > 0) grossWeightKg else 0,
            lengthMm       = if (modelChanged) 0 else baseVehicle.lengthMm,
            widthMm        = if (modelChanged) 0 else baseVehicle.widthMm,
            heightMm       = if (modelChanged) 0 else baseVehicle.heightMm,
            wheelbaseMm    = if (modelChanged) 0 else baseVehicle.wheelbaseMm,
            firstRegistrationDate = selectedDate,
            fuelTankCapacityLiters = if (fuelTank > 0) fuelTank else 0,
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
        // Extra information to transmitted to this activity
        const val EXTRA_VEHICLE = "extra_vehicle_information"
        const val EXTRA_PLATE   = "extra_plate_information"

        // Slider setup
        private const val MILEAGE_MAX_STEPS = 500    // 500 × 1 000 = 500 000 km
        private const val MILEAGE_STEP_KM   = 1_000
        private const val MILEAGE_FR_MEAN   = 75_000
    }
}
