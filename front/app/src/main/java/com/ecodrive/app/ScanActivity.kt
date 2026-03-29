package com.ecodrive.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecodrive.app.camera.CameraManager
import com.ecodrive.app.camera.PreviewManager
import com.ecodrive.app.databinding.ActivityScanBinding
import com.ecodrive.app.vehicle.plate.PlateAnalyzer
import com.ecodrive.app.vehicle.plate.data.PlateInformation
import com.ecodrive.app.vehicle.plate.extraction.ExtractionResult
import com.ecodrive.app.vehicle.plate.extraction.TextExtractor

class ScanActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    private lateinit var binding: ActivityScanBinding

    // Managers des sous-composants
    private lateinit var cameraManager: CameraManager
    private lateinit var previewManager: PreviewManager
    private lateinit var textExtractor: TextExtractor
    private val plateAnalyzer = PlateAnalyzer()

    /** Empêche un double appui sur le bouton pendant un traitement en cours */
    private var isProcessing = false
    private var isUserQuery = false
    private var isAutoScanRunning = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())


    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupManagers()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stop()
        textExtractor.release()
    }

    private fun goNextActivity(msg: String, finishCurrent: Boolean = true) {
        Log.d(TAG, "\n=============\nGo to AddVehicleActivity.\n================\n")
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        binding.root.postDelayed({
            val intent = Intent(this, AddVehicleActivity::class.java)
            startActivity(intent)
            if (finishCurrent) finish()
        }, 500)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults) {
            goNextActivity("Permission caméra requise pour scanner une plaque")
        }
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    private fun setupManagers() {
        Log.d(TAG, "Get preview UI component.")
        previewManager = PreviewManager(this)
        previewManager.setOnClickListener {
            if (! isUserQuery) {
                Log.d(TAG, "Start of user query analysis mode.")
                isUserQuery = true
                onScanRequested()
            } else {
                Log.d(
                    TAG,
                    "User query plate analysis. " + (
                            if (isProcessing) "Already in process"
                            else "Launch analysis")
                )
                if (!isProcessing) onScanRequested()
            }
        }
        Log.d(TAG, "Launch camera. WARNING : trigger is ${if (previewManager.isActivated) "activated" else "in sleep state"}")
        cameraManager  = CameraManager(this, REQUEST_CAMERA, previewManager.getPreview())
        textExtractor  = TextExtractor()

        Log.d(TAG, "Open full screen mode")
        previewManager.enterImmersiveMode()

        Log.d(TAG, "Start camera and show stream of camera")
        cameraManager.safeStart(this, this) {
            goNextActivity("Impossible d'ouvrir la caméra : ${it.message}")
        }

        binding.root.postDelayed({
            Log.d(TAG, "Start auto-scan")
            startAutoScan()
            binding.root.postDelayed({
                Log.d(TAG, (if (previewManager.isActivated) "trigger is already activated" else "activated trigger."))
                previewManager.activatedTrigger()
            }, 500)
        }, 500)
    }

    private val autoScanRunnable = object : Runnable {
        override fun run() {
            if (!isProcessing) {
                Log.d(TAG, "Start ${if (isUserQuery) "user query" else "auto"} analysis")
                onScanRequested()
            } else {
                Log.d(TAG, "Auto-analysis is blocked because an analysis is already launched.")
            }
            handler.postDelayed(this, AUTO_SCAN_INTERVAL)
        }
    }

    // -------------------------------------------------------------------------
    // Auto-scan control
    // -------------------------------------------------------------------------

    private fun startAutoScan() {
        if (isAutoScanRunning) return
        isAutoScanRunning = true
        handler.post(autoScanRunnable)
    }

    private fun stopAutoScan() {
        isAutoScanRunning = false
        handler.removeCallbacks(autoScanRunnable)
    }

    // -------------------------------------------------------------------------
    // Pipeline de traitement
    // -------------------------------------------------------------------------

    /**
     * Étape 1 : capture photo.
     */
    private fun onScanRequested() {
        if (!cameraManager.isReady) {
            Toast.makeText(this, "Caméra non prête", Toast.LENGTH_SHORT).show()
            return checkIfContinue()
        }

        isProcessing = true

        Log.d(TAG, "Try take photo")
        cameraManager.capture(
            onSuccess = { bitmap -> extractText(bitmap) },
            onError   = { checkIfContinue() }
        )
    }

    /**
     * Étape 2 : extraction OCR + couleurs.
     */
    private fun extractText(bitmap: android.graphics.Bitmap) {
        Log.d(TAG, "Extract text from camera")
        textExtractor.extract(
            bitmap    = bitmap,
            onResult  = { result -> analyzePlate(result) },
            onError   = { checkIfContinue() }
        )
    }

    /**
     * Étape 3 : analyse de la plaque.
     */
    private fun analyzePlate(result: ExtractionResult) {
        Log.d(TAG, "Analysis presence of plates with the text extract : $result")
        val info = plateAnalyzer.analyze(result)
        isProcessing = false
        onPlateResult(info)
    }

    /**
     * Étape 4 : exploitation du résultat.
     */
    private fun onPlateResult(info: PlateInformation?) {
        Log.d(TAG, if (info == null) "No plate detected, but continu to the next activity." else "Plate detected. Extract car informations.")
        isProcessing = false
        if (info != null) {
            val intent = Intent(this, AddVehicleActivity::class.java)
                .putExtra(EXTRA_PLATE_INFO, info)
            startActivity(intent)
        } else {
            goNextActivity("Aucune plaque d'immatriculation détecté.", false)
        }
    }

    private fun checkIfContinue() {
        isProcessing = false
        if (isUserQuery) onPlateResult(null)
    }

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    companion object {
        private const val TAG = "ScanActivity"
        const val REQUEST_CAMERA   = 100
        const val AUTO_SCAN_INTERVAL: Long = 500
        const val EXTRA_PLATE_INFO = "extra_plate_info"
    }
}
