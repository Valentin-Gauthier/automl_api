package com.ecodrive.app

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ecodrive.app.mainframe.FooterTab
import com.ecodrive.app.mainframe.MainFrameManager
import com.ecodrive.app.vehicle.VehicleInformation
import com.ecodrive.app.vehicle.plate.data.PlateInformation


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
    private lateinit var newPriceMin: TextView
    private lateinit var newPriceMax: TextView

    private lateinit var btnFindNearestGarage: Button
    private lateinit var btnPrepareAd: Button
    private lateinit var btnAddToList: Button

    //
    // État interne
    //

    private var vehicle: VehicleInformation? = null
    private var plate: PlateInformation? = null

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_predict)
        frameManager = MainFrameManager(this, FooterTab.PREDICT)
//        listManager = ListFrameManager(this)

        usedPriceMin = findViewById(R.id.usedPriceMin)
        usedPriceMax = findViewById(R.id.usedPriceMax)
        newPriceMin  = findViewById(R.id.newPriceMin)
        newPriceMax  = findViewById(R.id.newPriceMax)

        btnFindNearestGarage = findViewById(R.id.btnFindNearestGarage)
        btnPrepareAd = findViewById(R.id.btnPrepareAd)
        btnAddToList = findViewById(R.id.btnAddToList)

        // Obtenir les informations sur le véhicule
        plate = intentParcelable(EXTRA_PLATE, PlateInformation::class.java)
        vehicle = intentParcelable(EXTRA_VEHICLE, VehicleInformation::class.java)

        vehicle?.let { v ->
            plate?.let { p ->
                if (v.brandName != "" && v.modelName != "") {
                    //TODO: Automl prediction with this informations
                } else {
                    usedPriceMin.visibility = View.GONE
                    usedPriceMax.visibility = View.GONE
                    newPriceMin.visibility  = View.GONE
                    newPriceMax .visibility = View.GONE
                }
            }
        }

        btnFindNearestGarage.setOnClickListener {
            //TODO: Launch default map application of the user OR OSM in web
        }
        btnPrepareAd.setOnClickListener {
            //TODO: Launch AdMakerActivity
        }
        btnAddToList.setOnClickListener {
            //TODO: Launch dialog for select many lists or create new list
        }
    }

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
        const val EXTRA_VEHICLE        = "extra_vehicle_information"
        const val EXTRA_PLATE          = "extra_plate_information"
    }
}
