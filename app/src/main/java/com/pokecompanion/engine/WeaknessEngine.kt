package com.pokecompanion.engine

import com.pokecompanion.data.TypeChart
import com.pokecompanion.data.model.Type

data class WeaknessResult(
    val quadWeak: List<Type>,       // ×4
    val weak: List<Type>,           // ×2
    val halfResist: List<Type>,     // ×0.5
    val quarterResist: List<Type>,  // ×0.25
    val immune: List<Type>          // ×0
)

object WeaknessEngine {

    fun compute(type1: Type, type2: Type?, gen3Rules: Boolean = false): WeaknessResult {
        val attackingTypes = if (gen3Rules) Type.gen3Types else Type.entries.toList()
        val buckets = mutableMapOf<Float, MutableList<Type>>()

        for (attacker in attackingTypes) {
            val mult = TypeChart.getMultiplier(attacker, type1, gen3Rules) *
                (type2?.let { TypeChart.getMultiplier(attacker, it, gen3Rules) } ?: 1f)
            if (mult != 1f) {
                buckets.getOrPut(mult) { mutableListOf() }.add(attacker)
            }
        }

        return WeaknessResult(
            quadWeak      = buckets[4f]    ?: emptyList(),
            weak          = buckets[2f]    ?: emptyList(),
            halfResist    = buckets[0.5f]  ?: emptyList(),
            quarterResist = buckets[0.25f] ?: emptyList(),
            immune        = buckets[0f]    ?: emptyList()
        )
    }
}
