package com.ecodrive.app.ui.mainframe.gallery

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ecodrive.app.R


class CategoryManager(private val activity: Activity) {
    private val TAG: String = "CategoryManager"

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Composant tiré du layout installé.
    private val tabContent: LinearLayout = activity.findViewById(R.id.tabContent)

    // Manager de sous-composants.
    private val galleryManager: GalleryManager = GalleryManager(activity)

    // Suivi du status
    private var oldSelectedTabId: Int? = -1

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    fun getGallery(): GalleryManager {
        return galleryManager
    }

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    fun refresh(categories: List<Pair<String, List<String>>>, selectedCategory: Int = 0, nbVehiclePerLine: Int) {
        tabContent.removeAllViews()
        val nbCategories = categories.size
        if (nbCategories < 1) {
            Log.d(TAG, "User don't have category registered.")
            tabContent.visibility = View.GONE
            galleryManager.changeName()
            galleryManager.refresh(emptyList(), nbVehiclePerLine)
        } else {
            var printNameDeb: String
            var printNameEnd: String
            var iCategory: Int

            if (nbCategories == 1) {
                tabContent.visibility = View.GONE

                iCategory = 0
                printNameDeb = "only category("
                printNameEnd = ") of the user "
            } else {
                for (category in categories) {
                    val categoryView = TextView(activity)
                    categoryView.text = category.first
                    tabContent.addView(categoryView)
                }
                tabContent.visibility = View.VISIBLE

                iCategory = if (selectedCategory <= 0)
                    0
                else if (selectedCategory > categories.size)
                    categories.size - 1
                else
                    selectedCategory
                printNameDeb = "'"
                printNameEnd = "' category"
            }

            val (name, content) = categories[iCategory]
            Log.d(TAG, "The $printNameDeb$name$printNameEnd is in loading state.")
            galleryManager.changeName(name)
            galleryManager.refresh(content, nbVehiclePerLine)
        }
    }
}
