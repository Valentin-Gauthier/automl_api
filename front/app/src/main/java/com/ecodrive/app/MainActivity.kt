package com.ecodrive.app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ecodrive.app.database.AppDatabase
import com.ecodrive.app.mainframe.gallery.CategoryManager
import com.ecodrive.app.mainframe.FooterTab
import com.ecodrive.app.mainframe.MainFrameManager

class MainActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Managers des sous-composants
    private lateinit var frameManager: MainFrameManager
    private lateinit var galleryManager: CategoryManager

    // Configuration de l'activité.
    private var gallerySize: Int = 2
    private var isAppReady = false

    private var db: AppDatabase? = null

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()

        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !isAppReady }
        initAppComponents()
        //enableEdgeToEdge() Envoi l'application sous les barres système (en haut et en bas).

        setContentView(R.layout.activity_main)
        frameManager = MainFrameManager(this, FooterTab.HOME)
        galleryManager = CategoryManager(this)

        galleryManager.refresh(emptyList(), 0, gallerySize)
    }

    private fun initAppComponents() {
        Thread {
            Log.d("EcoDrive", "Connect to local bdd.")
            db = AppDatabase.getDatabase(this)
            Log.d("EcoDrive", "Ready for used.")
            isAppReady = true
        }.start()
    }
}
