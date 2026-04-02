package com.ecodrive.app.ui.mainframe

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Button
import com.ecodrive.app.MainActivity
import com.ecodrive.app.R
import com.ecodrive.app.ScanActivity
import com.ecodrive.app.SettingsActivity

class FooterManager(
    private val activity: Activity,
    private val currentActivityId: FooterTab = FooterTab.NONE,
    private val onCurrentButtonClick: (view: View) -> Unit = {}
) {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Composant tiré du layout installé.
    private val btnHome: Button = activity.findViewById(R.id.FooterBtnHome)
    private val btnAddVehicle: Button = activity.findViewById(R.id.FooterBtnAddVehicle)
    private val btnConfig: Button = activity.findViewById(R.id.FooterBtnConfig)

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    init {
        setupButton(btnHome, currentActivityId == FooterTab.HOME, MainActivity::class.java)
        setupButton(btnAddVehicle, currentActivityId == FooterTab.ADD, ScanActivity::class.java)
        setupButton(btnConfig, currentActivityId == FooterTab.CONFIG, SettingsActivity::class.java)
    }

    private fun setupButton(
        button: Button,
        enable: Boolean,
        newActivity: Class<out Activity>
    ) {
        if (enable)
            button.setOnClickListener(onCurrentButtonClick)
        else {
            button.setOnClickListener {
                Log.d(TAG, "Go to '$newActivity' activity.")
                activity.startActivity(Intent(activity, newActivity))
                if (currentActivityId != FooterTab.HOME)
                    activity.finishAffinity() // Termine toutes les activités de la pile
            }
            button.isEnabled = true
        }
    }

    companion object {
        private const val TAG: String = "FooterManager"
    }
}
