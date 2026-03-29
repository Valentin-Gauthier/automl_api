package com.ecodrive.app.camera

import android.app.Activity
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ecodrive.app.R

class PreviewManager(private val activity: Activity) {
    private var instruction: TextView = activity.findViewById(R.id.cameraInstruction)
    private var cheese: Button = activity.findViewById(R.id.cameraCheese)
    private var preview: TextureView = activity.findViewById(R.id.cameraPreview)

    // =========================================================================
    // Accesseurs
    // =========================================================================

    fun write(msg: String): CharSequence? {
        val old = instruction.text
        instruction.text = msg
        return old
    }

    fun setOnClickListener(exec: ((View) -> Unit)) {
        cheese.setOnClickListener(exec)
    }

    fun getPreview(): TextureView {
        return preview
    }

    fun inactivatedTrigger() {
        cheese.isActivated = false
    }

    fun activatedTrigger() {
        cheese.isActivated = true
    }
    val isActivated: Boolean
        get() = cheese.isActivated

    // =========================================================================
    // Initialisation
    // =========================================================================

    init { inactivatedTrigger() }

    // =========================================================================
    // Mode immersif
    // =========================================================================

    /**
     * Active le mode plein écran immersif :
     *   + Barre de statut et barre de navigation masquées
     *   + Réapparaissent temporairement sur swipe depuis le bord
     *
     * Compatible Android 6+ via WindowInsetsControllerCompat.
     */
    fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val controller = WindowInsetsControllerCompat(
            activity.window,
            activity.window.decorView
        )
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * Restaure les barres système (utile si l'Activity passe en arrière-plan).
     */
    fun exitImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)

        val controller = WindowInsetsControllerCompat(
            activity.window,
            activity.window.decorView
        )
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}