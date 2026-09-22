package com.example.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

/**
 * Helper class to handle photo capture permissions and CameraX image acquisition
 * for the messaging interface.
 */
class CameraCaptureHelper(private val context: Context) {

    companion object {
        private const val TAG = "CameraCaptureHelper"
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"

        /**
         * Checks whether CAMERA permission is granted.
         */
        fun hasCameraPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                CAMERA_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Creates a temporary output image file in the app's cache directory.
         */
        fun createImageFile(context: Context): File {
            val mediaDir = File(context.cacheDir, "camera_images").apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
            return File(mediaDir, "IMG_${timeStamp}.jpg")
        }

        /**
         * Obtains a secure content Uri for the image file using FileProvider.
         */
        fun getUriForFile(context: Context, file: File): Uri {
            return try {
                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, file)
            } catch (e: Exception) {
                Log.w(TAG, "FileProvider failed, falling back to Uri.fromFile: ${e.message}")
                Uri.fromFile(file)
            }
        }

        /**
         * Creates a temporary image file and returns both the File and its content Uri.
         */
        fun prepareTempImageUri(context: Context): Pair<File, Uri> {
            val file = createImageFile(context)
            val uri = getUriForFile(context, file)
            return Pair(file, uri)
        }

        /**
         * Cycles through flash modes: Auto -> On -> Off -> Auto.
         */
        fun cycleFlashMode(currentMode: Int): Int {
            return when (currentMode) {
                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                else -> ImageCapture.FLASH_MODE_AUTO
            }
        }

        /**
         * Toggles lens facing between Back and Front cameras.
         */
        fun toggleLensFacing(currentLensFacing: Int): Int {
            return if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
        }
    }

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Checks if camera permission is currently granted.
     */
    fun isPermissionGranted(): Boolean = hasCameraPermission(context)

    /**
     * Binds CameraX lifecycle to preview and image capture use cases.
     *
     * @param lifecycleOwner LifecycleOwner for binding camera lifecycle.
     * @param previewView Camera preview surface view.
     * @param lensFacing CameraSelector lens facing (Back or Front).
     * @param flashMode ImageCapture flash mode.
     * @param onBound Callback when camera and ImageCapture use case are successfully initialized.
     * @param onError Callback on binding failure.
     */
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        flashMode: Int = ImageCapture.FLASH_MODE_AUTO,
        onBound: (Camera, ImageCapture) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                // Unbind previous use cases before rebinding
                provider.unbindAll()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashMode)
                    .build()

                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                onBound(camera, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                onError(exc)
            }
        }, mainExecutor)
    }

    /**
     * Unbinds all active use cases and releases camera.
     */
    fun unbind() {
        cameraProvider?.unbindAll()
    }

    /**
     * Captures a high-resolution photo and saves it to cache storage, returning the Uri.
     *
     * @param imageCapture Initialized ImageCapture use case.
     * @param onPhotoCaptured Callback with the generated content Uri.
     * @param onError Callback on capture failure.
     */
    fun takePhoto(
        imageCapture: ImageCapture,
        onPhotoCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val photoFile = createImageFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: getUriForFile(context, photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    onPhotoCaptured(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    onError(exception)
                }
            }
        )
    }
}
