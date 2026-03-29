package com.ecodrive.app.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class CameraManager(
    private val context: Context,
    val requestCamera: Int,
    private val previewUI: TextureView
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var isStarted = false

    // =========================================================================
    // Démarrage
    // =========================================================================

    /**
     * Démarre l'aperçu caméra.
     * Doit être appelé après avoir obtenu la permission [Manifest.permission.CAMERA].
     *
     * @param lifecycleOwner  Généralement l'Activity ou le Fragment appelant
     * @param onError         Appelé si la caméra ne peut pas démarrer
     */
    private fun baseStart(
        lifecycleOwner: LifecycleOwner,
        onError: (Exception) -> Unit = { Log.e(TAG, "Erreur caméra", it) }
    ) {
        if (isStarted) {
            cameraProvider?: {
                Log.e(TAG, "Camera is lost. try to restart.")
                isStarted = false
                baseStart(lifecycleOwner,onError)
            }
        } else {
            cameraProvider?.let {
                Log.e(TAG, "Camera was mad closed. try to restart.")
                stop()
            }
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    cameraProvider = future.get()
                    bindUseCases(lifecycleOwner)
                    isStarted = true
                } catch (e: Exception) {
                    onError(e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    fun safeStart(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        onError: (Exception) -> Unit = { Log.e(TAG, "Erreur caméra", it) }
    ) {
        if (hasPermission()) {
            if (previewUI.isAvailable) {
                baseStart(lifecycleOwner, onError)
            } else {
                previewUI.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        baseStart(lifecycleOwner, onError)
                        previewUI.surfaceTextureListener = null // remove listener after start
                    }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = true
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        } else {
            requestCameraPermission(activity)
        }
    }

    /**
     * Arrête la caméra et libère les ressources.
     */
    fun stop() {
        if (isStarted) {
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.w(TAG, "Erreur stop caméra (ignorée)", e)
            }
            isStarted = false
        } else {
            if (cameraProvider == null) {
                Log.e(TAG, "Camera was mad closed. Retry to closed.")
                isStarted = true
                stop()
            }
        }
    }

    fun onRequestPermissionsResult(
        lifecycleOwner: LifecycleOwner,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onFail: () -> Unit
    ) {
        if (requestCode == requestCamera) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
                baseStart(lifecycleOwner)
            else onFail()
        }
    }

    // -------------------------------------------------------------------------
    // Permissions
    // -------------------------------------------------------------------------

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            requestCamera
        )
    }

    // =========================================================================
    // Capture
    // =========================================================================

    /**
     * Capture une photo.
     *
     * @param onSuccess  Appelé avec le [Bitmap] résultant, correctement orienté
     * @param onError    Appelé en cas d'échec
     */
    fun capture(
        onSuccess: (Bitmap) -> Unit,
        onError: (Exception) -> Unit = { Log.e(TAG, "Erreur capture", it) }
    ) {
        Log.d(TAG, "Test if camera has launched")
        if (!isReady) {
            onError(IllegalStateException("Caméra non prête ou TextureView non disponible"))
            return
        }
        if (!previewUI.isAvailable) {
            onError(IllegalStateException("TextureView non disponible"))
            return
        }

        val capture = imageCapture!!

        Log.d(TAG, "Try capture one image in camera stream")
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    Log.d(TAG, "One image has been captured")
                    try {
                        val bitmap = imageProxyToBitmap(imageProxy)
                        onSuccess(bitmap)
                    } catch (e: Exception) {
                        onError(e)
                    } finally {
                        imageProxy.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(TAG, "No image was captured")
                    onError(exception)
                }
            }
        )
        Log.d(TAG, "End try to capture image.")
    }

    // =========================================================================
    // Privé : liaison des use cases CameraX
    // =========================================================================

    private fun bindUseCases(lifecycleOwner: LifecycleOwner) {
        val provider = cameraProvider ?: return

        // — Aperçu —
        // CameraX avec TextureView nécessite un SurfaceProvider custom.
        // On utilise PreviewView côté layout si possible, mais ici, on s'adapte
        // au TextureView fourni via un SurfaceProvider manuel.
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()

        preview.setSurfaceProvider { request ->
            val surface = Surface(previewUI.surfaceTexture)
            request.provideSurface(surface, ContextCompat.getMainExecutor(context)) {}
        }

        // — Capture —
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()

        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
    }

    // =========================================================================
    // Privé : conversion ImageProxy → Bitmap avec correction de rotation
    // =========================================================================

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes  = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Corrige la rotation EXIF
        val rotation = imageProxy.imageInfo.rotationDegrees
        return if (rotation != 0) rotateBitmap(raw, rotation) else raw
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // =========================================================================
    // Accesseurs
    // =========================================================================

    val isReady: Boolean get() = isStarted && imageCapture != null

    companion object {
        private const val TAG = "CameraManager"
    }
}
