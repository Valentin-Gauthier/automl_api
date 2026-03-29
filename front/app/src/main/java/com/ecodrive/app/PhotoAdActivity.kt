package com.ecodrive.app

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecodrive.app.camera.CameraManager
import com.ecodrive.app.camera.PreviewManager
import kotlin.math.abs

class PhotoAdActivity : AppCompatActivity(), SensorEventListener {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Managers des sous-composants
    private lateinit var cameraManager: CameraManager
    private lateinit var previewManager: PreviewManager

    // Composant définis dans le layout
    private lateinit var tvProgress: TextView
    private lateinit var rotateGuide: SeekBar
    private lateinit var pitchGuide: SeekBar

    // Capteurs de mouvement
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor?  = null

    private val accel = FloatArray(3)
    private val magnet = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    // State
    private var currentIndex = 0
    private var isProcessing = false
    private var vehiclePath: String? = null

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_ad)

        vehiclePath = intent.getStringExtra(EXTRA_VEHICLE_PATH)
        if (vehiclePath == null) {
            abort("Dossier de sauvegarde invalide")
            return
        }

        currentIndex = intent.getIntExtra(EXTRA_INDEX, 0)

        bindViews()
        setupCamera()
        rotateGuide.visibility = View.GONE
        pitchGuide.visibility = View.GONE
        setupSensors()

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.also  { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stop()
    }

    private fun abort(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        window.decorView.postDelayed({ finish() }, 500)
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    private fun bindViews() {
        tvProgress  = findViewById(R.id.tvPhotoProgress)
        rotateGuide = findViewById(R.id.rotateAlignmentGuide)
        pitchGuide  = findViewById(R.id.tangageAlignmentGuide)
    }

    private fun setupCamera() {
        Thread {
            Log.d(TAG, "Obtention dela caméra.")
            previewManager = PreviewManager(this)
            Log.d(TAG, "Connection de la caméra l'UI.")
            cameraManager = CameraManager(this, REQUEST_CAMERA, previewManager.getPreview())

            previewManager.setOnClickListener {
                if (!isProcessing) takePhoto()
            }

            previewManager.enterImmersiveMode()
            Log.d(TAG, "Allumage de la caméra.")
            cameraManager.safeStart(this, this) {
                abort("Impossible d'ouvrir la caméra : ${it.message}")
            }
        }.start()
    }

    private fun setupSensors() {
        Thread {
            Log.d(TAG, "Allumage des capteurs de mesures gyroscopiques et accélérométries.")
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            Log.d(TAG, "Affichage des résultats des capteurs.")
            rotateGuide.visibility = View.VISIBLE
            pitchGuide.visibility = View.VISIBLE
        }.start()
    }

    // -------------------------------------------------------------------------
    // Caméra
    // -------------------------------------------------------------------------

    private fun takePhoto() {
        if (!cameraManager.isReady) return

        isProcessing = true

        cameraManager.capture(
            onSuccess = {
                runOnUiThread {
                    isProcessing = false
                    currentIndex++

                    if (currentIndex >= PHOTO_INSTRUCTIONS.size) {
                        Toast.makeText(this, "Photos terminées ✔", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        updateUI()
                        Toast.makeText(this, "Photo $currentIndex prise ✔", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = {
                runOnUiThread {
                    isProcessing = false
                    Toast.makeText(this, "Erreur capture", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        tvProgress.text = "${currentIndex + 1} / ${PHOTO_INSTRUCTIONS.size}"
        previewManager.write(PHOTO_INSTRUCTIONS[currentIndex])
    }

    // -------------------------------------------------------------------------
    // Capteurs d'orientation
    // -------------------------------------------------------------------------

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER ->
                System.arraycopy(event.values, 0, accel, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD ->
                System.arraycopy(event.values, 0, magnet, 0, 3)
        }

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, orientation)

            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll  = Math.toDegrees(orientation[2].toDouble()).toFloat()

            updateGuides(pitch, roll)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateGuides(pitch: Float, roll: Float) {
        rotateGuide.progress = (50 + roll).toInt().coerceIn(0, 100)
        rotateGuide.alpha    = 1f - (abs(roll) / 45f).coerceAtMost(1f)

        pitchGuide.progress = (50 + pitch).toInt().coerceIn(0, 100)
        pitchGuide.alpha    = 1f - (abs(pitch) / 30f).coerceAtMost(1f)
    }

    // -------------------------------------------------------------------------
    // Permissions caméra
    // -------------------------------------------------------------------------

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults) {
            abort("Permission caméra requise pour prendre en photo le véhicule.")
        }
    }

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    companion object {
        private const val TAG = "Take photo for ad activity"
        const val REQUEST_CAMERA = 100
        const val EXTRA_VEHICLE_PATH = "extra_vehicle_path"
        const val EXTRA_INDEX        = "extra_index"

        private val PHOTO_INSTRUCTIONS = listOf(
            "Vue de face, centré et bien éclairé.",
            "Vue de l'arrière, centré et bien éclairé.",
            "Côté conducteur, roues visibles.",
            "Côté passager, roues visibles.",
            "Intérieur : tableau de bord et sièges.",
            "Compteur kilométrique."
        )
        val size get() = PHOTO_INSTRUCTIONS.size
    }
}