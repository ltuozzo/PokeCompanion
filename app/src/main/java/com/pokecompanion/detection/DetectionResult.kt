package com.pokecompanion.detection

import com.pokecompanion.data.model.PokemonEntity

/**
 * Result of one OCR + DB lookup pass.
 *
 * - [NONE]: no Pokemon name found in the crop region.
 * - [SINGLE]: exactly one Pokemon recognised (standard 1v1).
 * - [DOUBLE]: two Pokemon recognised simultaneously (2v2 / double battle).
 */
sealed class DetectionResult {
    data object None : DetectionResult()
    data class Single(val pokemon: PokemonEntity) : DetectionResult()
    data class Double(val pokemon1: PokemonEntity, val pokemon2: PokemonEntity) : DetectionResult()
}
