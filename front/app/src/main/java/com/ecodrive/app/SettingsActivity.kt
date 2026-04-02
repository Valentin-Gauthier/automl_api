package com.ecodrive.app

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.ecodrive.app.ui.mainframe.FooterTab
import com.ecodrive.app.ui.mainframe.MainFrameManager

class SettingsActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    private lateinit var frameManager: MainFrameManager
    private lateinit var prefs: SharedPreferences

    // Galerie — taille
    private lateinit var gallerySizeSeekBar: SeekBar
    private lateinit var gallerySizeLabel: TextView

    // Galerie — onglets automatiques
    private lateinit var chipShowAll: ToggleButton
    private lateinit var editShowAllName: EditText
    private lateinit var chipShowNotCategorized: ToggleButton
    private lateinit var editShowNotCategorizedName: EditText

    // API
    private lateinit var apiTokenInput: EditText
    private lateinit var btnClearToken: Button

    // Sauvegarde
    private lateinit var btnSave: Button

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        frameManager = MainFrameManager(this, FooterTab.CONFIG)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        bindViews()
        loadSettings()
        setupListeners()
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    private fun bindViews() {
        // Galerie — taille
        gallerySizeSeekBar = findViewById(R.id.gallerySizeSeekBar)
        gallerySizeLabel   = findViewById(R.id.gallerySizeLabel)

        // Galerie — onglets automatiques
        chipShowAll                = findViewById(R.id.ShowAll)
        editShowAllName            = findViewById(R.id.ShowAllName)
        chipShowNotCategorized     = findViewById(R.id.ShowNotCategorized)
        editShowNotCategorizedName = findViewById(R.id.ShowNotCategorizedName)

        // API
        apiTokenInput = findViewById(R.id.apiTokenInput)
        btnClearToken = findViewById(R.id.btnClearToken)

        // Sauvegarde
        btnSave = findViewById(R.id.btnSaveSettings)
    }

    private fun loadSettings() {
        // --- Galerie : taille ---
        val size = prefs.getInt(KEY_GALLERY_SIZE, DEFAULT_GALLERY_SIZE)
        gallerySizeSeekBar.max      = MAX_GALLERY_SIZE - 1
        gallerySizeSeekBar.progress = size - 1
        gallerySizeLabel.text       = getString(R.string.settings_gallery_size_value, size)

        // --- Galerie : onglet "Tout afficher" ---
        val showAll = prefs.getBoolean(KEY_SHOW_ALL, DEFAULT_SHOW_ALL)
        chipShowAll.isChecked = showAll
        editShowAllName.isEnabled = showAll
        editShowAllName.setText(
            prefs.getString(KEY_SHOW_ALL_NAME, DEFAULT_SHOW_ALL_NAME) ?: DEFAULT_SHOW_ALL_NAME
        )

        // --- Galerie : onglet "Non catégorisé" ---
        val showNotCategorized = prefs.getBoolean(KEY_SHOW_NOT_CATEGORIZED, DEFAULT_SHOW_NOT_CATEGORIZED)
        chipShowNotCategorized.isChecked = showNotCategorized
        editShowNotCategorizedName.isEnabled = showNotCategorized
        editShowNotCategorizedName.setText(
            prefs.getString(KEY_SHOW_NOT_CATEGORIZED_NAME, DEFAULT_SHOW_NOT_CATEGORIZED_NAME)
                ?: DEFAULT_SHOW_NOT_CATEGORIZED_NAME
        )

        // --- API : token ---
        val token = prefs.getString(KEY_API_TOKEN, "") ?: ""
        apiTokenInput.setText(token)
    }

    private fun setupListeners() {
        // --- Galerie : taille ---
        gallerySizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                gallerySizeLabel.text = getString(R.string.settings_gallery_size_value, progress + 1)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // --- Galerie : onglet "Tout afficher" ---
        chipShowAll.setOnCheckedChangeListener { _, isChecked ->
            editShowAllName.isEnabled = isChecked
        }

        // --- Galerie : onglet "Non catégorisé" ---
        chipShowNotCategorized.setOnCheckedChangeListener { _, isChecked ->
            editShowNotCategorizedName.isEnabled = isChecked
        }

        // --- API : token ---
        btnClearToken.setOnClickListener {
            apiTokenInput.setText("")
        }

        // --- Sauvegarde ---
        btnSave.setOnClickListener { saveSettings() }
    }

    private fun saveSettings() {
        val size  = gallerySizeSeekBar.progress + 1
        val token = apiTokenInput.text.toString().trim()

        val showAll = chipShowAll.isChecked
        val showAllName = editShowAllName.text.toString().trim()
            .ifBlank { DEFAULT_SHOW_ALL_NAME }

        val showNotCategorized = chipShowNotCategorized.isChecked
        val showNotCategorizedName = editShowNotCategorizedName.text.toString().trim()
            .ifBlank { DEFAULT_SHOW_NOT_CATEGORIZED_NAME }

        prefs.edit {
            putInt(KEY_GALLERY_SIZE, size)
            putString(KEY_API_TOKEN, token)
            putBoolean(KEY_SHOW_ALL, showAll)
            putString(KEY_SHOW_ALL_NAME, showAllName)
            putBoolean(KEY_SHOW_NOT_CATEGORIZED, showNotCategorized)
            putString(KEY_SHOW_NOT_CATEGORIZED_NAME, showNotCategorizedName)
        }

        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }

    // -------------------------------------------------------------------------
    // Constantes publiques (accès depuis les autres activités)
    // -------------------------------------------------------------------------

    companion object {
        // Settings
        const val PREFS_NAME = "eco-drive_prefs"

        // API
        const val KEY_API_TOKEN = "api_token"

        // Galerie — onglets automatiques
        const val KEY_SHOW_NOT_CATEGORIZED          = "show_not_categorized"
        const val DEFAULT_SHOW_NOT_CATEGORIZED       = false
        const val KEY_SHOW_NOT_CATEGORIZED_NAME     = "show_not_categorized_name"
        const val DEFAULT_SHOW_NOT_CATEGORIZED_NAME  = "DEFAULT"

        const val KEY_SHOW_ALL           = "show_all"
        const val DEFAULT_SHOW_ALL       = false
        const val KEY_SHOW_ALL_NAME      = "show_all_name"
        const val DEFAULT_SHOW_ALL_NAME  = "ALL"

        // Galerie — taille
        const val KEY_GALLERY_SIZE     = "gallery_size"
        const val DEFAULT_GALLERY_SIZE  = 2
        const val MAX_GALLERY_SIZE      = 5
    }
}
