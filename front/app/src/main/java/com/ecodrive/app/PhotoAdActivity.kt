package com.ecodrive.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PhotoAdActivity : AppCompatActivity() {
    /*

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    private lateinit var binding: ActivityScanBinding

    // Managers des sous-composants
    private lateinit var cameraManager: CameraManager
    private lateinit var previewManager: PreviewManager

    // Alignment guides
    private lateinit var rotateAlignmentGuide: SeekBar
    private lateinit var tangageAlignmentGuide: SeekBar

    // Sensors
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var lastAccelerometerReading = FloatArray(3)
    private var lastMagnetometerReading = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private var orientationAngles = FloatArray(3)

    // Photo storage
    private var currentPhotoName: String = ""
    private var currentPhotoIndex: Int = 0
    private val photoInstructions = listOf(
        "Prenez une photo de face du véhicule, centré et bien éclairé.",
        "Prenez une photo de l'arrière du véhicule, centré et bien éclairé.",
        "Prenez une photo du côté conducteur, en incluant les roues.",
        "Prenez une photo du côté passager, en incluant les roues.",
        "Prenez une photo de l'intérieur (tableau de bord et sièges avant).",
        "Prenez une photo du compteur kilométrique."
    )
*/
    /** Empêche un double appui sur le bouton pendant un traitement en cours */
    private var isProcessing = false

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
/*
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        previewManager = PreviewManager(this)
        previewManager.setOnClickListener {
            if (!isProcessing) onPhotoRequested()
        }
        cameraManager  = CameraManager(this, previewManager.getPreview())

        // Initialize UI components
        rotateAlignmentGuide = findViewById(R.id.rotateAlignmentGuide)
        tangageAlignmentGuide = findViewById(R.id.tangageAlignmentGuide)

        // Initialize sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Set initial instruction
        updateInstruction()

        previewManager.enterImmersiveMode()

        if (hasPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
 */
    }
/*
    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stop()
        textExtractor.release()
    }

    override fun onResume() {
        super.onResume()
        // Register sensor listeners
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister sensor listeners
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

            // Image capture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Erreur lors de l'ouverture de la caméra", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Create output file
        val photoFile = createPhotoFile()

        // Set up image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image if using front camera
            isReversedHorizontal = false
        }

        // Set up image capture listener
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()

        // Take the picture
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        Toast.makeText(this@PhotoAdActivity, "Photo sauvegardée", Toast.LENGTH_SHORT).show()
                        currentPhotoIndex++
                        if (currentPhotoIndex < photoInstructions.size) {
                            updateInstruction()
                        } else {
                            finish()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(this@PhotoAdActivity, "Erreur lors de la prise de photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun createPhotoFile(): File {
        // Get vehicle name from intent (or use default)
        val vehicleName = intent.getStringExtra("vehicleName") ?: "vehicle_${System.currentTimeMillis()}"

        // Create time-stamped filename
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        currentPhotoName = "${vehicleName}_${timeStamp}_${currentPhotoIndex}"

        // Get or create the photos directory
        val storageDir = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "vehiclesphoto"
        )
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, "$currentPhotoName.jpg")
    }

    private fun updateInstruction() {
        tvInstruction.text = photoInstructions[currentPhotoIndex]

        // Update alignment guides visibility based on instruction
        when (currentPhotoIndex) {
            0, 1 -> { // Front and back photos - need precise alignment
                rotateAlignmentGuide.visibility = View.VISIBLE
                tangageAlignmentGuide.visibility = View.VISIBLE
            }
            2, 3 -> { // Side photos - less strict alignment
                rotateAlignmentGuide.visibility = View.VISIBLE
                tangageAlignmentGuide.visibility = View.INVISIBLE
            }
            4, 5 -> { // Interior photos - alignment not critical
                rotateAlignmentGuide.visibility = View.INVISIBLE
                tangageAlignmentGuide.visibility = View.INVISIBLE
            }
        }
    }

    // SensorEventListener implementation
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelerometerReading, 0, event.values.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMagnetometerReading, 0, event.values.size)
            }
        }

        // Update orientation angles
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            lastAccelerometerReading,
            lastMagnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // Update alignment guides based on device orientation
        updateAlignmentGuides()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun updateAlignmentGuides() {
        // Convert radians to degrees
        val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val pitchDegrees = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val rollDegrees = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        // Update rotation guide (azimuth - left/right tilt)
        val rotationProgress = (50 + (azimuthDegrees / 2)).toInt().coerceIn(0, 100)
        rotateAlignmentGuide.progress = rotationProgress
        rotateAlignmentGuide.alpha = 1 - (Math.abs(azimuthDegrees) / 45).coerceAtMost(1f)

        // Update tangage guide (pitch - forward/backward tilt)
        val tangageProgress = (50 + (pitchDegrees / 2)).toInt().coerceIn(0, 100)
        tangageAlignmentGuide.progress = tangageProgress
        tangageAlignmentGuide.alpha = 1 - (Math.abs(pitchDegrees) / 30).coerceAtMost(1f)
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
 */

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    companion object {
        private const val REQUEST_CAMERA   = 100
    }
}