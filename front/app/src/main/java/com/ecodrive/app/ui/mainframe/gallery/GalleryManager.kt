package com.ecodrive.app.ui.mainframe.gallery

import android.app.Activity
import android.content.Context.MODE_PRIVATE
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
import com.ecodrive.app.SettingsActivity
import com.ecodrive.app.database.AppDatabase
import com.ecodrive.app.database.dto.VehicleGalleryItem
import com.ecodrive.app.ui.dialog.AddToListDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GalleryManager(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val db: AppDatabase
) {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Composant tiré du layout installé.
    private val galleryContainer: ScrollView = activity.findViewById(R.id.contentContainer)

    private val voidGallery: View = LayoutInflater.from(activity)
        .inflate(R.layout.gallery_void, galleryContainer, false)
    private val addVehicleButton: Button = voidGallery.findViewById(R.id.btnAddFirstVehicle)
    private val galleryName: TextView = voidGallery.findViewById(R.id.galleryNameIfVoid)
    private val defaultName: String = galleryName.text.toString()

    // Statut
    private var currentJob: Job? = null

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    init {
        addVehicleButton.setOnClickListener {
            val intent = Intent(activity, ScanActivity::class.java)
            activity.startActivity(intent)
        }
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    fun showEmptyState(categoryName: String? = null) {
        galleryContainer.removeAllViews()
        galleryName.text = categoryName ?: defaultName
        galleryContainer.addView(voidGallery)
    }

    fun loadAllVehicles(categoryName: String) =
        load(db.vehicle().getAllGalleryItems(), categoryName)
    fun loadAllVehiclesWithinCategory(categoryName: String) =
        load(db.vehicle().getGalleryItemsWithoutList(), categoryName)
    fun loadVehiclesForCategory(categoryId: Int, categoryName: String) =
        load(db.vehicle().getGalleryItemsForList(listId), categoryName)

    // -------------------------------------------------------------------------
    // Logique interne
    // -------------------------------------------------------------------------

    private fun loadVehicles(flow: Flow<List<VehicleGalleryItem>>, categoryName: String) {
        // Annule la surveillance de la dernière catégorie sélectionnée.
        currentJob?.cancel()

        // Commence la surveillance de la nouvelle catégorie.
        currentJob = scope.launch {
            flow.collect { vehicles ->
                if (vehicles.isEmpty()) {
                    Log.d(TAG, "The '$categoryName' is void.")
                    showEmptyState(categoryName)
                } else {
                    displayVehicles(categoryName, vehicles)
                }
            }
        }
    }

    private fun displayVehicles(categoryName: String, vehicles: List<VehicleGalleryItem>) {
        Log.d(TAG, "Load content of the '$categoryName' category.")
        val gallerySize = activity
            .getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
            .getInt(SettingsActivity.KEY_GALLERY_SIZE, SettingsActivity.DEFAULT_GALLERY_SIZE)

        val gallery = TableLayout(activity)
        var row = TableRow(activity)
        var rowSize = 0

        vehicles.forEach { vehicle ->
            if (rowSize >= gallerySize) {
                gallery.addView(row)
                row = TableRow(activity)
                rowSize = 0
            }
            val vehicleItem = Button(activity).apply {
                text = vehicle.immatriculation
                setOnClickListener { onVehicleClick(vehicle) }
                setOnLongClickListener { onVehicleLongClick(vehicle); true }
            }
            row.addView(vehicleItem)
            rowSize++
        }

        if (rowSize > 0) gallery.addView(row)

        galleryContainer.removeAllViews()
        galleryContainer.addView(gallery)
    }

    private fun onVehicleClick(vehicle: VehicleGalleryItem) {
        // TODO: Afficher les détails du véhicule
    }

    private fun onVehicleLongClick(vehicle: Vehicle) {
        scope.launch {
            val vehicle = db.vehicle().getWithDetails(item.id)?.vehicle ?: return@launch
            activity.runOnUiThread {
                AddToListDialog(activity, vehicle).show()
            }
        }
    }

    companion object {
        private const val TAG = "GalleryManager"
    }
}
