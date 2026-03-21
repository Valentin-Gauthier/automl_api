package com.example.ecodrive

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.ecodrive.plate.PlateAnalyser
import com.example.ecodrive.plate.rectangleDetector.RectangleDetector
import com.example.ecodrive.plate.PlateType
import com.example.ecodrive.plate.PlateInformation
import com.example.ecodrive.plate.rectangleDetector.DetectionAccuracy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.YearMonth
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Matcher
import java.util.regex.Pattern

class ScanActivity : AppCompatActivity() {
    private val TAG = "ScanActivity"

    // Camera controller
    private lateinit var cameraExecutor: ExecutorService
    private var lastAnalyzedImage: Bitmap? = null

    // PlateManagement
    private val defaultPlateInformation = PlateInformation()
    private lateinit var rectangleDetector: RectangleDetector

    // UI Components
    private lateinit var cameraPreview: TextureView
    private lateinit var btnTriggerScan: ImageButton
    private lateinit var tvInstruction: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check OpenCV availability
        if (!OpenCVLoader.waitOpenCV()) {
            // OpenCV is not load => Rectangle detection is impossible
            // => Plate detection is impossible => plate extraction is impossible
            // => Abort scan and auto write in the add vehicle form
            Log.e(TAG, "OpenCV not loaded")
            goToNextActivity(defaultPlateInformation, withinReturn = true)
            return
        }
        setContentView(R.layout.scan_activity)

        // Get UI components
        cameraPreview = findViewById(R.id.cameraPreview)
        btnTriggerScan = findViewById(R.id.btnTriggerScan)
        tvInstruction = findViewById(R.id.tvInstruction)

        // Setup RectangleDetector with passive mode for faster detection
        rectangleDetector = RectangleDetector.getInstance()

        // Start activity
        btnTriggerScan.setOnClickListener {
            // Setup RectangleDetector with Aggressive mode for better detection
            val oldRectangleDetector = rectangleDetector
            rectangleDetector = RectangleDetector.getInstance(DetectionAccuracy.Aggressive)
            detectPlate(userDeclenched = true)
            rectangleDetector = oldRectangleDetector
        }
        startCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    fun goToNextActivity(plateInformation: PlateInformation, withinReturn: Boolean = false) {
        val intent = Intent(this, AddVehicleActivity::class.java).apply {
            putExtra("immatriculation", plateInformation.number)
            putExtra("territorialId", plateInformation.territorialId)
            putExtra("endValidity", plateInformation.endValidity)
            putExtra("plateType", plateInformation.plateType)
        }
        startActivity(intent)
        if (withinReturn)
            finish()// This activity is unusable => don't return on this activity
    }

    /**
     * Start the camera and link her video stream to the TextureView component
     */
    private fun startCamera() {
        // Get video stream of camera
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Show video stream
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview as Preview.SurfaceProvider?)// NOTE : cameraPreview is TextureView => cameraPreview doesn't have surfaceProvider method
            }
            // Analysis one on five image in the video stream
            var imageSkipCounter = 0
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        lastAnalyzedImage = imageProxy.toBitmap()
                        imageProxy.close()
                        if (imageSkipCounter < 5) {
                            imageSkipCounter++
                        } else {
                            imageSkipCounter = 0
                            detectPlate(userDeclenched = false)
                        }
                    }
                }
            // Read back camera video stream
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun detectPlate(userDeclenched: Boolean) {
        // Get the last image of the video stream
        val image = lastAnalyzedImage ?: return

        // detect rectangles in the image
        val detectionResult = rectangleDetector.detect(image)
        val img = detectionResult.image
        val rectangles = detectionResult.rectangles.sortedBy { it.circumferenceLength }

        // Analysis rectangles (potentials plates)
        var plateInfo: PlateInformation? = null
        var seuil = 80
        for (rectangle in rectangles) {
            val (score, plateInformation) = PlateAnalyser.plateAnalyse(rectangle,img, 50)
            if (score == 0)
                continue

            // Choose if this rectangle is a plate
            if (score > seuil) {
                plateInfo = plateInformation
                if (userDeclenched) {
                    if (score > seuil * 4)
                        break// This rectangle contains the best plate
                } else if (score > seuil * 2) {
                    break// This rectangle contains an acceptable plate
                }
                seuil =
                    score// Up seuil to score for warranty the position of most similar to plate
            }
        }

        // If no plate is detected but the user have manually triggered the scan
        if (plateInfo == null && userDeclenched) {
            plateInfo = defaultPlateInformation
        }

        // Go to the next activity
        plateInfo?.let {
            goToNextActivity(it)
        }
    }

    private fun getDominantColor() {

    }
}