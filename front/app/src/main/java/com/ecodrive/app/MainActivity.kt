package com.ecodrive.app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ecodrive.app.database.AppDatabase
import com.ecodrive.app.ui.mainframe.gallery.CategoryManager
import com.ecodrive.app.ui.mainframe.FooterTab
import com.ecodrive.app.ui.mainframe.MainFrameManager

class MainActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Managers des sous-composants
    private lateinit var frameManager:   MainFrameManager
    private lateinit var galleryManager: CategoryManager

    // Configuration de l'activité.
    private var isAppReady = false
    private var db: AppDatabase? = null

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()

        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !isAppReady }
        //enableEdgeToEdge() Envoi l'application sous les barres système (en haut et en bas).

        setContentView(R.layout.activity_main)
        frameManager   = MainFrameManager(this, FooterTab.HOME)
        galleryManager = CategoryManager(this)

        val gallerySize = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
            .getInt(SettingsActivity.KEY_GALLERY_SIZE, SettingsActivity.DEFAULT_GALLERY_SIZE)
        initAppComponents(gallerySize)
    }

    private fun initAppComponents(gallerySize: Int) {
        Thread {
            Log.d("EcoDrive", "Connexion à la base de données locale.")
            db = AppDatabase.getDatabase(this)
            Log.d("EcoDrive", "Base de données prête.")

            Log.d("EcoDrive", "Chargement de la première catégorie de la gallery.")
            runOnUiThread {
                galleryManager.refresh(emptyList(), 0, gallerySize)
            }
            isAppReady = true
        }.start()
    }
}
