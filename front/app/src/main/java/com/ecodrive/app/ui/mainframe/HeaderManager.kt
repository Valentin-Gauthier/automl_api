package com.ecodrive.app.ui.mainframe

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ecodrive.app.R

class HeaderManager(private val activity: Activity) {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Composant tiré du layout installé.
    private val headerContainer: View = activity.findViewById(R.id.headerContainer)
    private val appName: TextView = headerContainer.findViewById(R.id.headerAppName)
    private val activityDescription: TextView = headerContainer.findViewById(R.id.headerActivityDescription)
    private val appIcon: View = headerContainer.findViewById(R.id.headerAppIcon)
    private val infoSpace: LinearLayout = headerContainer.findViewById(R.id.headerInfoContainer)
    private val buttonSpace: LinearLayout = headerContainer.findViewById(R.id.headerBtnsContainer)

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    fun addInfo(number: String): HeaderManager {
        val infoView = TextView(activity).apply {
            text = number
            setTextColor(activity.getColor(android.R.color.white))
            setPadding(8, 0, 8, 0)
        }
        infoSpace.addView(infoView)
        return this
    }

    fun addButton(button: Button): HeaderManager {
        buttonSpace.addView(button)
        return this
    }

    fun changeDescr(text: String): HeaderManager {
        activityDescription.text = text
        return this
    }

    fun unactiveName(visible: Boolean): HeaderManager {
        appName.visibility = View.GONE
        return this
    }

    fun unactiveLogo(visible: Boolean): HeaderManager {
        appIcon.visibility = View.GONE
        return this
    }
}
