package com.ecodrive.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class CameraManager(
    private val context: Context,
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
    fun start(
        lifecycleOwner: LifecycleOwner,
        onError: (Exception) -> Unit = { Log.e(TAG, "Erreur caméra", it) }
    ) {
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

    /**
     * Arrête la caméra et libère les ressources.
     */
    fun stop() {
        cameraProvider?.unbindAll()
        isStarted = false
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
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Caméra non démarrée"))
            return
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
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
                    onError(exception)
                }
            }
        )
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