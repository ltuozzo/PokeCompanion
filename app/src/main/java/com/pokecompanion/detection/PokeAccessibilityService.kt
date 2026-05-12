package com.pokecompanion.detection

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.pokecompanion.data.database.DatabasePopulator
import com.pokecompanion.data.database.PokeDatabase
import kotlinx.coroutines.*

class PokeAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null
    private var lastHash = EMPTY_HASH

    private lateinit var ocr: OcrPipeline

    // The last successful detection — held until a new one arrives.
    // Session 4 will expose this to the UI via a StateFlow / SharedFlow.
    var lastResult: DetectionResult = DetectionResult.None
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        NotificationHelper.createChannel(this)
        try {
            startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.build(this))
        } catch (e: Exception) {
            Log.d(TAG, "Foreground notification skipped: ${e.message}")
        }

        // Initialise DB + OCR pipeline asynchronously so we don't block the main thread.
        serviceScope.launch(Dispatchers.IO) {
            val db = PokeDatabase.getInstance(applicationContext)
            val dao = db.pokemonDao()
            DatabasePopulator(applicationContext).populateIfEmpty(dao)
            ocr = OcrPipeline(
                dao = dao,
                cropRect = defaultCropRect,     // hardcoded for now; Session 5 makes it configurable
                generations = enabledGenerations
            )
            Log.d(TAG, "DB + OCR pipeline ready")
            startPolling()
        }

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

        if (ImageHasher.isSame(hash, lastHash)) {
            // Screen unchanged — skip OCR to save CPU/battery.
            bitmap.recycle()
            return
        }

        lastHash = hash
        Log.d(TAG, "Screen changed (hash=$hash) — running OCR")

        // Run OCR on IO dispatcher; bitmap ownership passes to the coroutine.
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
                // Keep showing lastResult — don't clear on overworld / menus.
                Log.d(TAG, "No Pokemon detected — keeping last result")
            }
            is DetectionResult.Single -> {
                lastResult = result
                Log.d(TAG, "Detected: ${result.pokemon.name}")
            }
            is DetectionResult.Double -> {
                lastResult = result
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
        private const val POLL_INTERVAL_MS = 1000L
        private const val EMPTY_HASH = -1L

        // Assumed top screen display ID — verify on device.
        // If the bottom screen is captured instead, change to 1 in Settings (Session 6).
        var displayId = 0

        // All generations enabled by default.
        // Session 5 will replace this with per-profile settings.
        var enabledGenerations: List<Int> = (1..9).toList()

        // Hardcoded crop rect for enemy name bar (GBA-style games, portrait layout).
        // Session 5 will let the user calibrate this via the profile system.
        // null = full screenshot (use for initial testing before calibration).
        var defaultCropRect: android.graphics.Rect? = null
    }
}
