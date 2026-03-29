package com.ecodrive.app.mainframe

import android.app.Activity
import android.widget.Button

class MainFrameManager(activity: Activity, currentActivityId: FooterTab) {

    // -------------------------------------------------------------------------
    // Sub - Manager
    // -------------------------------------------------------------------------

    private val headerManager: HeaderManager = HeaderManager(activity)

    init {
        FooterManager(activity, currentActivityId)
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    fun addInfo(number: String): HeaderManager {
        return headerManager.addInfo(number)
    }

    fun addButton(button: Button): HeaderManager {
        return headerManager.addButton(button)
    }

    fun changeDescr(text: String): HeaderManager {
        return headerManager.changeDescr(text)
    }

    fun unactiveName(visible: Boolean): HeaderManager {
        return unactiveName(visible)
    }

    fun unactiveLogo(visible: Boolean): HeaderManager {
        return headerManager.unactiveLogo(visible)
    }
}