package com.pokecompanion.detection

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class PokeAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null
    private var lastHash = EMPTY_HASH

    override fun onServiceConnected() {
        super.onServiceConnected()
        NotificationHelper.createChannel(this)
        try {
            startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.build(this))
        } catch (e: Exception) {
            Log.d(TAG, "Foreground notification skipped: ${e.message}")
        }
        startPolling()
        Log.d(TAG, "Service connected — polling display $displayId every ${POLL_INTERVAL_MS}ms")
    }

    private fun startPolling() {
        pollingJob = serviceScope.launch {
            while (isActive) {
                captureAndCheck()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun captureAndCheck() {
        takeScreenshot(
            displayId,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    processScreenshot(result)
                }

                override fun onFailure(errorCode: Int) {
                    Log.w(TAG, "Screenshot failed (code=$errorCode) — " +
                        "check that display $displayId is the correct top screen")
                }
            }
        )
    }

    private fun processScreenshot(result: ScreenshotResult) {
        val hwBitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
        val bitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
        hwBitmap?.recycle()
        result.close()

        if (bitmap == null) {
            Log.w(TAG, "Failed to decode screenshot bitmap")
            return
        }

        val hash = ImageHasher.hash(bitmap)
        bitmap.recycle()

        if (!ImageHasher.isSame(hash, lastHash)) {
            lastHash = hash
            Log.d(TAG, "Screen changed (hash=$hash)")
            // Session 3: pass bitmap to OCR pipeline here
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PokeCompanion"
        private const val POLL_INTERVAL_MS = 1000L
        private const val EMPTY_HASH = -1L

        // Assumed top screen display ID — verify on device.
        // If the bottom screen is captured instead, change to 1 in Settings (Session 6).
        var displayId = 0
    }
}
