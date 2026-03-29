package com.ecodrive.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupManagers()

        previewManager.enterImmersiveMode()

        if (hasPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stop()
        textExtractor.release()
    }

    private fun goNextActivity(msg: String, finishCurrent: Boolean = true) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        binding.root.postDelayed({
            val intent = Intent(this, AddVehicleActivity::class.java)
            startActivity(intent)
            if (finishCurrent) finish()
        }, 500)
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    private fun setupManagers() {
        previewManager = PreviewManager(this)
        previewManager.setOnClickListener {
            if (!isProcessing) onScanRequested()
        }
        cameraManager  = CameraManager(this, previewManager.getPreview())
        textExtractor  = TextExtractor()
    }

    // -------------------------------------------------------------------------
    // Caméra
    // -------------------------------------------------------------------------

    private fun startCamera() {
        cameraManager.start(
            lifecycleOwner = this,
            onError        = { e ->
                goNextActivity("Impossible d'ouvrir la caméra : ${e.message}")
            }
        )
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
            return
        }

        isProcessing = true

        cameraManager.capture(
            onSuccess = { bitmap -> extractText(bitmap) },
            onError   = { isProcessing = false }
        )
    }

    /**
     * Étape 2 : extraction OCR + couleurs.
     */
    private fun extractText(bitmap: android.graphics.Bitmap) {
        textExtractor.extract(
            bitmap    = bitmap,
            onResult  = { result -> analyzePlate(result) },
            onError   = { isProcessing = false }
        )
    }

    /**
     * Étape 3 : analyse de la plaque.
     */
    private fun analyzePlate(result: ExtractionResult) {
        val info = plateAnalyzer.analyze(result)
        isProcessing = false
        onPlateResult(info)
    }

    /**
     * Étape 4 : exploitation du résultat.
     */
    private fun onPlateResult(info: PlateInformation?) {
        if (info != null) {
            val intent = Intent(this, AddVehicleActivity::class.java)
                .putExtra(EXTRA_PLATE_INFO, info)
            startActivity(intent)
        } else {
            goNextActivity("Aucune plaque d'immatriculation détecté.", false)
        }
    }

    // -------------------------------------------------------------------------
    // Permissions
    // -------------------------------------------------------------------------

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                goNextActivity("Permission caméra requise pour scanner une plaque")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    companion object {
        private const val REQUEST_CAMERA   = 100
        const val EXTRA_PLATE_INFO         = "extra_plate_info"
    }
}
