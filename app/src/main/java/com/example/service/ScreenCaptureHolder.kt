package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object ScreenCaptureHolder {
    var resultCode: Int = 0
    var resultData: Intent? = null
    var isPermissionGranted: Boolean = false

    private var activeMediaProjection: MediaProjection? = null

    fun setPermission(code: Int, data: Intent?) {
        resultCode = code
        resultData = data
        isPermissionGranted = (code != 0 && data != null)
    }

    fun getMediaProjection(context: Context): MediaProjection? {
        if (activeMediaProjection != null) return activeMediaProjection
        val data = resultData ?: return null
        val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            ?: return null
        return try {
            val projection = mpManager.getMediaProjection(resultCode, data.clone() as Intent)
            activeMediaProjection = projection
            projection
        } catch (e: Exception) {
            null
        }
    }

    suspend fun captureScreen(context: Context): Bitmap? = withContext(Dispatchers.Main) {
        val projection = getMediaProjection(context) ?: return@withContext null
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val virtualDisplayDeferred = CompletableDeferred<Bitmap?>()

        var virtualDisplay: VirtualDisplay? = null
        val handler = Handler(Looper.getMainLooper())

        imageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener {
            private var captured = false
            override fun onImageAvailable(reader: ImageReader) {
                if (captured) return
                var image: Image? = null
                try {
                    image = reader.acquireLatestImage()
                    if (image != null) {
                        captured = true
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        val bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)

                        // Crop if rowPadding exists
                        val finalBitmap = if (rowPadding > 0) {
                            Bitmap.createBitmap(bitmap, 0, 0, width, height)
                        } else {
                            bitmap
                        }

                        if (!virtualDisplayDeferred.isCompleted) {
                            virtualDisplayDeferred.complete(finalBitmap)
                        }
                    }
                } catch (e: Exception) {
                    if (!virtualDisplayDeferred.isCompleted) {
                        virtualDisplayDeferred.complete(null)
                    }
                } finally {
                    image?.close()
                }
            }
        }, handler)

        try {
            virtualDisplay = projection.createVirtualDisplay(
                "NinjaTugasScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler
            )

            // Wait up to 2 seconds for a frame
            val result = withTimeoutOrNull(2000) {
                virtualDisplayDeferred.await()
            }
            return@withContext result
        } catch (e: Exception) {
            return@withContext null
        } finally {
            try {
                virtualDisplay?.release()
                imageReader.close()
            } catch (ignored: Exception) {
            }
        }
    }
}
