package com.pokecompanion.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state shared between [PokeAccessibilityService] (writer)
 * and the Compose UI (reader).  Using a plain object + StateFlow avoids
 * any service binding boilerplate for now.
 */
object DetectionState {

    private val _result = MutableStateFlow<DetectionResult>(DetectionResult.None)
    val result = _result.asStateFlow()

    private val _isAutoEnabled = MutableStateFlow(true)
    val isAutoEnabled = _isAutoEnabled.asStateFlow()

    fun postResult(result: DetectionResult) {
        _result.value = result
    }

    fun setAutoEnabled(enabled: Boolean) {
        _isAutoEnabled.value = enabled
    }
}
