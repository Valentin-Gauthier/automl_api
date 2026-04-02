package com.ecodrive.app.ui.mainframe.gallery

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ecodrive.app.R
import com.ecodrive.app.SettingsActivity
import com.ecodrive.app.database.AppDatabase
import com.ecodrive.app.database.entities.VehicleList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryManager(
    private val activity: Activity,
    scope: CoroutineScope,
    private val db: AppDatabase
) {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    private val galleryManager = GalleryManager(activity, scope, db)
    private val tabContent: LinearLayout = activity.findViewById(R.id.tabContent)
    private var selectedCategoryId: Int? = null

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    suspend fun observeCategories() {
        db.vehicleList().getAll().collect { categories ->
            withContext(Dispatchers.Main) {
                updateCategoryTabs(normalizedCategorised(categories))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Logique interne
    // -------------------------------------------------------------------------

    private fun normalizedCategorised(categories: List<VehicleList>): List<VehicleList> {
        // Obtenir la configuration
        val prefs = activity.getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val tabForVehicleWithinCategory = prefs
            .getBoolean(SettingsActivity.KEY_SHOW_NOT_CATEGORIZED, SettingsActivity.DEFAULT_SHOW_NOT_CATEGORIZED)
        val tabForAllVehicle = prefs
            .getBoolean(SettingsActivity.KEY_SHOW_ALL, SettingsActivity.DEFAULT_SHOW_ALL)

        // Ajouter les onglets automatiques demandés
        if (tabForVehicleWithinCategory || tabForAllVehicle) {
            val mutableCategories = categories.toMutableList()
            if (tabForVehicleWithinCategory) {
                val name = prefs
                    .getString(SettingsActivity.KEY_SHOW_NOT_CATEGORIZED_NAME, SettingsActivity.DEFAULT_SHOW_NOT_CATEGORIZED_NAME)
                    ?.trim()
                    ?: SettingsActivity.DEFAULT_SHOW_NOT_CATEGORIZED_NAME
                mutableCategories.add(0, VehicleList(-1, name))
            }
            if (tabForAllVehicle) {
                val name = prefs
                    .getString(SettingsActivity.KEY_SHOW_ALL_NAME, SettingsActivity.DEFAULT_SHOW_ALL_NAME)
                    ?.trim()
                    ?: SettingsActivity.DEFAULT_SHOW_ALL_NAME
                mutableCategories.add(0, VehicleList(-2, name))
            }
            return mutableCategories
        }
        return categories
    }

    private fun updateCategoryTabs(categories: List<VehicleList>) {
        // Afficher le contenu de la liste
        tabContent.visibility = if (categories.size > 1) View.VISIBLE else View.GONE
        tabContent.removeAllViews()
        if (categories.isEmpty()) {
            Log.d(TAG, "User don't have category registered.")
            selectedCategoryId = null
            galleryManager.showEmptyState()
        } else {
            categories.forEach { category ->
                val tab = TextView(activity).apply {
                    text = category.name
                    setOnClickListener { onCategorySelected(category) }
                }
                tabContent.addView(tab)
            }

            // Sélectionner la première catégorie par défaut
            if (selectedCategoryId == null) {
                onCategorySelected(categories.first())
            }
        }
    }

    private fun onCategorySelected(category: VehicleList) {
        //TODO: Changer l'apparance de l'onglet actuellement sélectionné et du nouveau

        // Actualiser le contenu de la gallery
        val categoryId = category.id
        val categoryName = category.name
        when (categoryId) {
            -2 -> galleryManager.loadAllVehicles(categoryName)
            -1 -> galleryManager.loadAllVehiclesWithinCategory(categoryName)
            else -> galleryManager.loadVehiclesForCategory(categoryId, categoryName)
        }

        // Changer le pointeur sur la catégorie sélectionné
        selectedCategoryId = categoryId
    }

    companion object {
        private const val TAG = "CategoryManager"
    }
}
