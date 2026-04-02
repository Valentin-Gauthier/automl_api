package com.ecodrive.app

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ecodrive.app.database.AppDatabase
import com.ecodrive.app.ui.mainframe.FooterTab
import com.ecodrive.app.ui.mainframe.MainFrameManager
import com.ecodrive.app.ui.mainframe.gallery.CategoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Managers des sous-composants
    private lateinit var frameManager:   MainFrameManager
    private lateinit var galleryManager: CategoryManager
    private lateinit var db: AppDatabase

    // Boutons de contrôle de la gallerie
    private lateinit var btnSearch: Button
    private lateinit var btnFilter: Button

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()

        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !::db.isInitialized || !::galleryManager.isInitialized }
        //enableEdgeToEdge() Envoi l'application sous les barres système (en haut et en bas).

        setContentView(R.layout.activity_main)

        // Initialisation de la base de données
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("EcoDrive", "Connexion à la base de données locale.")
            db = AppDatabase.getDatabase(this@MainActivity)
            Log.d("EcoDrive", "Base de données prête.")

            withContext(Dispatchers.Main) {
                galleryManager = CategoryManager(this@MainActivity, lifecycleScope, db)
                startObserving()
            }
        }

        frameManager = MainFrameManager(this, FooterTab.HOME)
        setupHeaderButtons()
    }

    // -------------------------------------------------------------------------
    // Header — boutons
    // -------------------------------------------------------------------------

    private fun setupHeaderButtons() {
        btnSearch = Button(this).apply {
            // Icône loupe (ressource système Android)
            setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_search, 0, 0, 0
            )
            contentDescription = getString(R.string.header_btn_search_desc)
            setOnClickListener { onSearchClick() }
        }

        btnFilter = Button(this).apply {
            // Icône filtre/tri (ressource système Android)
            setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_sort_by_size, 0, 0, 0
            )
            contentDescription = getString(R.string.header_btn_filter_desc)
            setOnClickListener { onFilterClick() }
        }

        // Ajout dans le header via la chaîne de MainFrameManager
        frameManager
            .addButton(btnSearch)
            .addButton(btnFilter)
    }

    // -------------------------------------------------------------------------
    // Actions des boutons header
    // -------------------------------------------------------------------------

    private fun onSearchClick() {
        // TODO: ouvrir la barre de recherche / filtrer la galerie par texte
        Toast.makeText(this, "Recherche", Toast.LENGTH_SHORT).show()
    }

    private fun onFilterClick() {
        // TODO: ouvrir le panneau de filtres (marque, catégorie, date…)
        Toast.makeText(this, "Filtres", Toast.LENGTH_SHORT).show()
    }

    // -------------------------------------------------------------------------
    // Observation des données
    // -------------------------------------------------------------------------

    private fun startObserving() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                galleryManager.observeCategories()
            }
        }
    }
}
