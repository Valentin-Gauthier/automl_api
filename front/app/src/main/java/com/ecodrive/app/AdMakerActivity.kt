package com.ecodrive.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecodrive.app.ui.mainframe.FooterTab
import com.ecodrive.app.ui.mainframe.MainFrameManager
import com.ecodrive.app.vehicle.VehicleInformation
import com.ecodrive.app.vehicle.plate.data.PlateInformation

class AdMakerActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    private lateinit var frameManager: MainFrameManager

    private lateinit var tvAdTitle: TextView
    private lateinit var etAdTitle: EditText
    private lateinit var etAdDescription: EditText
    private lateinit var etAdPrice: EditText
    private lateinit var btnGenerateDesc: Button
    private lateinit var btnPhotoAid: Button
    private lateinit var btnShareLbc: Button
    private lateinit var btnShareGeneric: Button

    private var vehicle: VehicleInformation? = null
    private var plate: PlateInformation? = null
    private var step: Int? = 0

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad_maker)
        frameManager = MainFrameManager(this, FooterTab.NONE)

        bindViews()

        vehicle = intentParcelable(EXTRA_VEHICLE, VehicleInformation::class.java)
        plate   = intentParcelable(EXTRA_PLATE,   PlateInformation::class.java)

        vehicle?.let { prefillAd(it) }

        btnGenerateDesc.setOnClickListener { generateDescription() }
        btnPhotoAid.setOnClickListener    { goToPhotoActivity() }
        btnShareLbc.setOnClickListener    { shareToLbc() }
        btnShareGeneric.setOnClickListener{ shareGeneric() }
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    private fun bindViews() {
        tvAdTitle         = findViewById(R.id.tvAdMakerTitle)
        etAdTitle         = findViewById(R.id.etAdTitle)
        etAdDescription   = findViewById(R.id.etAdDescription)
        etAdPrice         = findViewById(R.id.etAdPrice)
        btnGenerateDesc   = findViewById(R.id.btnGenerateDescription)
        btnPhotoAid       = findViewById(R.id.btnPhotoAid)
        btnShareLbc       = findViewById(R.id.btnShareLbc)
        btnShareGeneric   = findViewById(R.id.btnShareGeneric)
    }

    // -------------------------------------------------------------------------
    // Pré-remplissage
    // -------------------------------------------------------------------------

    private fun prefillAd(v: VehicleInformation) {
        tvAdTitle.text = getString(R.string.ad_maker_title_vehicle, v.displayName)
        etAdTitle.setText(buildTitle(v))
        etAdDescription.setText(buildDescription(v))
    }

    private fun buildTitle(v: VehicleInformation): String {
        val year = v.year?.toString() ?: ""
        val km   = if (v.mileage > 0) "${v.mileage / 1000}k km" else ""
        return listOf(v.brandName, v.modelName, v.modelVersion, year, km)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun buildDescription(v: VehicleInformation): String {
        val sb = StringBuilder()
        sb.appendLine("${v.brandName} ${v.modelName}".trim())
        if (v.modelVersion.isNotBlank())  sb.appendLine("Version : ${v.modelVersion}")
        if (v.year != null)               sb.appendLine("Année : ${v.year}")
        if (v.mileage > 0)                sb.appendLine("Kilométrage : ${formatKm(v.mileage)}")
        if (v.energy.label != "Inconnu")  sb.appendLine("Énergie : ${v.energy.label}")
        if (v.gearboxType.label != "Inconnu") sb.appendLine("Boîte de vitesse : ${v.gearboxType.label}")
        if (v.powerHp > 0)                sb.appendLine("Puissance : ${v.powerHp} ch")
        if (v.color.isNotBlank())         sb.appendLine("Couleur : ${v.color}")
        if (v.doorCount > 0)              sb.appendLine("Nombre de portes : ${v.doorCount}")
        if (v.passengerCount > 0)         sb.appendLine("Places : ${v.passengerCount}")
        sb.appendLine()
        sb.appendLine("Véhicule en bon état général.")
        sb.appendLine("Prix à débattre. Sérieux s'abstenir.")
        return sb.toString().trim()
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private fun generateDescription() {
        val v = vehicle ?: return
        etAdDescription.setText(buildDescription(v))
        Toast.makeText(this, getString(R.string.ad_description_generated), Toast.LENGTH_SHORT).show()
    }

    private fun goToPhotoActivity(): Int? {
        if (step == null || step!! < 0 || step!! >= PhotoAdActivity.size)
            return null
        val intent = Intent(this, PhotoAdActivity::class.java).apply {
            vehicle?.let { putExtra(PhotoAdActivity.EXTRA_VEHICLE_PATH, it.vin) }
            plate?.let   { putExtra(PhotoAdActivity.EXTRA_INDEX, step) }
        }
        startActivity(intent)
        return step!! + 1
    }

    private fun shareToLbc() {
        val title = etAdTitle.text.toString().trim()
        val desc  = etAdDescription.text.toString().trim()
        val price = etAdPrice.text.toString().trim()
        if (title.isBlank() || desc.isBlank()) {
            Toast.makeText(this, getString(R.string.ad_fill_required_fields), Toast.LENGTH_SHORT).show()
            return
        }
        // Ouvre Le Bon Coin via le navigateur
        val url = "https://www.leboncoin.fr/deposer-une-annonce"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            shareGeneric() // Fallback
        }
    }

    private fun shareGeneric() {
        val title = etAdTitle.text.toString().trim()
        val desc  = etAdDescription.text.toString().trim()
        val price = etAdPrice.text.toString().trim()
        val text  = buildString {
            append(title)
            if (price.isNotBlank()) append("\nPrix : $price €")
            append("\n\n")
            append(desc)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.ad_share_via)))
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    private fun formatKm(km: Int): String =
        "%,d km".format(km).replace(',', '\u00A0')

    private fun <T : android.os.Parcelable> intentParcelable(key: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(key, clazz)
        else
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(key)

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    companion object {
        const val EXTRA_VEHICLE = "extra_vehicle_information"
        const val EXTRA_PLATE   = "extra_plate_information"
    }
}
