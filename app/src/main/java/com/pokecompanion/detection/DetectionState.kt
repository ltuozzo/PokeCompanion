package com.pokecompanion.detection

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state shared between [PokeAccessibilityService] (writer)
 * and the Compose UI (reader).
 */
object DetectionState {

    // ── Battle detection ─────────────────────────────────────────────────────

    private val _result = MutableStateFlow<DetectionResult>(DetectionResult.None)
    val result = _result.asStateFlow()

    fun postResult(result: DetectionResult) { _result.value = result }

    // ── Auto mode ────────────────────────────────────────────────────────────

    private val _isAutoEnabled = MutableStateFlow(true)
    val isAutoEnabled = _isAutoEnabled.asStateFlow()

    fun setAutoEnabled(enabled: Boolean) { _isAutoEnabled.value = enabled }

    // ── Calibration screenshot ───────────────────────────────────────────────
    // UI sets pendingCalibration = true; service captures the next frame and
    // posts it here; UI reads it and clears it when done.

    val pendingCalibration = MutableStateFlow(false)

    private val _calibrationBitmap = MutableStateFlow<Bitmap?>(null)
    val calibrationBitmap = _calibrationBitmap.asStateFlow()

    fun postCalibrationBitmap(bitmap: Bitmap) { _calibrationBitmap.value = bitmap }
    fun clearCalibrationBitmap() { _calibrationBitmap.value = null }
}
