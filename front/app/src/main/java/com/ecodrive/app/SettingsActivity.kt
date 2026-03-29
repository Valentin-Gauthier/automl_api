package com.ecodrive.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecodrive.app.ui.mainframe.FooterTab
import com.ecodrive.app.ui.mainframe.MainFrameManager

class SettingsActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    private lateinit var frameManager: MainFrameManager
    private lateinit var prefs: SharedPreferences

    private lateinit var gallerySizeSeekBar: SeekBar
    private lateinit var gallerySizeLabel: TextView
    private lateinit var apiTokenInput: EditText
    private lateinit var btnSave: Button
    private lateinit var btnClearToken: Button

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        frameManager = MainFrameManager(this, FooterTab.CONFIG)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        bindViews()
        loadSettings()
        setupListeners()
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    private fun bindViews() {
        gallerySizeSeekBar = findViewById(R.id.gallerySizeSeekBar)
        gallerySizeLabel   = findViewById(R.id.gallerySizeLabel)
        apiTokenInput      = findViewById(R.id.apiTokenInput)
        btnSave            = findViewById(R.id.btnSaveSettings)
        btnClearToken      = findViewById(R.id.btnClearToken)
    }

    private fun loadSettings() {
        val size = prefs.getInt(KEY_GALLERY_SIZE, DEFAULT_GALLERY_SIZE)
        gallerySizeSeekBar.max      = MAX_GALLERY_SIZE - 1
        gallerySizeSeekBar.progress = size - 1
        gallerySizeLabel.text       = getString(R.string.settings_gallery_size_value, size)

        val token = prefs.getString(KEY_API_TOKEN, "") ?: ""
        apiTokenInput.setText(token)
    }

    private fun setupListeners() {
        gallerySizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 1
                gallerySizeLabel.text = getString(R.string.settings_gallery_size_value, size)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnClearToken.setOnClickListener {
            apiTokenInput.setText("")
        }

        btnSave.setOnClickListener { saveSettings() }
    }

    private fun saveSettings() {
        val size  = gallerySizeSeekBar.progress + 1
        val token = apiTokenInput.text.toString().trim()

        prefs.edit()
            .putInt(KEY_GALLERY_SIZE, size)
            .putString(KEY_API_TOKEN, token)
            .apply()

        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }

    // -------------------------------------------------------------------------
    // Constantes publiques (accès depuis les autres activités)
    // -------------------------------------------------------------------------

    companion object {
        const val PREFS_NAME          = "ecodrive_prefs"
        const val KEY_GALLERY_SIZE    = "gallery_size"
        const val KEY_API_TOKEN       = "api_token"
        const val DEFAULT_GALLERY_SIZE = 2
        const val MAX_GALLERY_SIZE     = 5
    }
}
