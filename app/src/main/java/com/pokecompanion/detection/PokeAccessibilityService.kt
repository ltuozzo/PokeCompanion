package com.pokecompanion.detection

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.pokecompanion.data.database.DatabasePopulator
import com.pokecompanion.data.database.PokeDatabase
import com.pokecompanion.data.profile.ProfileManager
import com.pokecompanion.data.settings.SettingsManager
import kotlinx.coroutines.*

class PokeAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null
    private var lastHash = EMPTY_HASH

    private lateinit var ocr: OcrPipeline

    override fun onServiceConnected() {
        super.onServiceConnected()
        NotificationHelper.createChannel(this)
        try {
            startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.build(this))
        } catch (e: Exception) {
            Log.d(TAG, "Foreground notification skipped: ${e.message}")
        }

        serviceScope.launch(Dispatchers.IO) {
            val db = PokeDatabase.getInstance(applicationContext)
            val dao = db.pokemonDao()
            DatabasePopulator(applicationContext).populateIfEmpty(dao)

            // ProfileManager and SettingsManager — both idempotent.
            ProfileManager.init(applicationContext)
            SettingsManager.init(applicationContext)

            ocr = OcrPipeline(dao = dao)

            // Keep OCR pipeline in sync with the active profile.
            serviceScope.launch {
                ProfileManager.activeProfile.collect { profile ->
                    ocr.cropRect = profile?.cropRect()
                    ocr.generations = profile?.generationList() ?: (1..9).toList()
                    Log.d(TAG, "Profile updated: ${profile?.name ?: "none"} crop=${ocr.cropRect} gens=${ocr.generations}")
                }
            }

            Log.d(TAG, "DB + OCR pipeline ready")
            startPolling()
        }

        Log.d(TAG, "Service connected — polling display $displayId every ${SettingsManager.pollIntervalMs}ms")
    }

    private fun startPolling() {
        pollingJob = serviceScope.launch {
            while (isActive) {
                captureAndCheck()
                delay(SettingsManager.pollIntervalMs)
            }
        }
    }

    private fun captureAndCheck() {
        // Skip screenshot entirely when auto is off AND no calibration pending.
        if (!DetectionState.isAutoEnabled.value && !DetectionState.pendingCalibration.value) return

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
                    // Clear pending calibration so the UI doesn't wait forever.
                    if (DetectionState.pendingCalibration.value) {
                        DetectionState.pendingCalibration.value = false
                    }
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

        // Calibration takes priority — post the raw bitmap and skip OCR.
        if (DetectionState.pendingCalibration.value) {
            DetectionState.pendingCalibration.value = false
            DetectionState.postCalibrationBitmap(bitmap)
            Log.d(TAG, "Calibration screenshot posted")
            return
        }

        val hash = ImageHasher.hash(bitmap)

        if (ImageHasher.isSame(hash, lastHash)) {
            bitmap.recycle()
            return
        }

        lastHash = hash
        Log.d(TAG, "Screen changed (hash=$hash) — running OCR")

        serviceScope.launch(Dispatchers.IO) {
            val detection = ocr.process(bitmap)
            bitmap.recycle()

            withContext(Dispatchers.Main) {
                handleDetection(detection)
            }
        }
    }

    private fun handleDetection(result: DetectionResult) {
        when (result) {
            is DetectionResult.None -> {
                Log.d(TAG, "No Pokemon detected — keeping last result")
            }
            is DetectionResult.Single -> {
                DetectionState.postResult(result)
                Log.d(TAG, "Detected: ${result.pokemon.name}")
            }
            is DetectionResult.Double -> {
                DetectionState.postResult(result)
                Log.d(TAG, "Detected 2v2: ${result.pokemon1.name} + ${result.pokemon2.name}")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        if (::ocr.isInitialized) ocr.close()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PokeCompanion"
        private const val EMPTY_HASH = -1L

        // Assumed top screen display ID — verify on device.
        // If the bottom screen is captured instead, change to 1 in Settings (Session 6).
        var displayId = 0
    }
}
