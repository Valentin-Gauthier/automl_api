package com.ecodrive.app.ui.mainframe.gallery

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.ecodrive.app.R
import com.ecodrive.app.ScanActivity

class GalleryManager(private val activity: Activity) {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Composant tiré du layout installé.
    private val galleryContainer: ScrollView = activity.findViewById(R.id.contentContainer)

    // Composant construit pour l'activité.
    private val voidGallery: View
    private val galleryName: TextView
    private val addVehicleButton: Button

    // Suivi du status
    private val defaultName: String

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    init {
        // Inflater le layout void_gallery
        val inflater = LayoutInflater.from(activity)
        voidGallery = inflater.inflate(R.layout.gallery_void, galleryContainer, false)
        galleryName = voidGallery.findViewById(R.id.galleryNameIfVoid)
        addVehicleButton = voidGallery.findViewById(R.id.btnAddFirstVehicle)

        defaultName = galleryName.text.toString()

        addVehicleButton.setOnClickListener {
            val intent = Intent(activity, ScanActivity::class.java)
            activity.startActivity(intent)
        }
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    fun changeName(name: String? = null) {
        if (name != null) {
            galleryName.text = name
        } else {
            galleryName.text = defaultName
        }
    }

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    fun refresh(vehicles: List<String>, nbVehiclePerLine: Int) {
        galleryContainer.removeAllViews()
        val child = if (vehicles.isEmpty()) {
            Log.d(TAG, "The '" + galleryName.text + "' is void.")
            voidGallery
        } else {
            Log.d(TAG, "Load content of the '" + galleryName.text + "' category.")
            val gallery = TableLayout(activity)
            var nbVehicleOnLine = 0
            var row = TableRow(activity)
            for (vehicle in vehicles) {
                if (nbVehicleOnLine >= nbVehiclePerLine) {
                    nbVehicleOnLine = 0
                    gallery.addView(row)
                    row = TableRow(activity)
                }
                val vehicleItem = Button(activity)
                vehicleItem.text = vehicle
                row.addView(vehicleItem)
                nbVehicleOnLine++
            }
            if (nbVehicleOnLine > 0)
                gallery.addView(row)
            gallery
        }
        galleryContainer.addView(child)
    }

    companion object {
        private const val TAG: String = "GalleryManager"
    }
}
