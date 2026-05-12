package com.pokecompanion.data

import com.pokecompanion.data.model.Type
import com.pokecompanion.data.model.Type.*

/**
 * Type effectiveness chart.
 * Maps attacking type → defending type → damage multiplier.
 * Only non-1x entries are stored; default is 1f.
 */
object TypeChart {

    private val gen6: Map<Type, Map<Type, Float>> = mapOf(
        NORMAL   to mapOf(GHOST to 0f),
        FIRE     to mapOf(
            GRASS to 2f, ICE to 2f, BUG to 2f, STEEL to 2f,
            FIRE to 0.5f, WATER to 0.5f, ROCK to 0.5f, DRAGON to 0.5f
        ),
        WATER    to mapOf(
            FIRE to 2f, GROUND to 2f, ROCK to 2f,
            WATER to 0.5f, GRASS to 0.5f, DRAGON to 0.5f
        ),
        ELECTRIC to mapOf(
            WATER to 2f, FLYING to 2f,
            ELECTRIC to 0.5f, GRASS to 0.5f, DRAGON to 0.5f,
            GROUND to 0f
        ),
        GRASS    to mapOf(
            WATER to 2f, GROUND to 2f, ROCK to 2f,
            FIRE to 0.5f, GRASS to 0.5f, POISON to 0.5f,
            FLYING to 0.5f, BUG to 0.5f, DRAGON to 0.5f, STEEL to 0.5f
        ),
        ICE      to mapOf(
            GRASS to 2f, GROUND to 2f, FLYING to 2f, DRAGON to 2f,
            WATER to 0.5f, ICE to 0.5f, STEEL to 0.5f
        ),
        FIGHTING to mapOf(
            NORMAL to 2f, ICE to 2f, ROCK to 2f, DARK to 2f, STEEL to 2f,
            POISON to 0.5f, FLYING to 0.5f, PSYCHIC to 0.5f, BUG to 0.5f, FAIRY to 0.5f,
            GHOST to 0f
        ),
        POISON   to mapOf(
            GRASS to 2f, FAIRY to 2f,
            POISON to 0.5f, GROUND to 0.5f, ROCK to 0.5f, GHOST to 0.5f,
            STEEL to 0f
        ),
        GROUND   to mapOf(
            FIRE to 2f, ELECTRIC to 2f, POISON to 2f, ROCK to 2f, STEEL to 2f,
            GRASS to 0.5f, BUG to 0.5f,
            FLYING to 0f
        ),
        FLYING   to mapOf(
            GRASS to 2f, FIGHTING to 2f, BUG to 2f,
            ELECTRIC to 0.5f, ROCK to 0.5f, STEEL to 0.5f
        ),
        PSYCHIC  to mapOf(
            FIGHTING to 2f, POISON to 2f,
            PSYCHIC to 0.5f, STEEL to 0.5f,
            DARK to 0f
        ),
        BUG      to mapOf(
            GRASS to 2f, PSYCHIC to 2f, DARK to 2f,
            FIRE to 0.5f, FIGHTING to 0.5f, FLYING to 0.5f,
            GHOST to 0.5f, STEEL to 0.5f, FAIRY to 0.5f
        ),
        ROCK     to mapOf(
            FIRE to 2f, ICE to 2f, FLYING to 2f, BUG to 2f,
            FIGHTING to 0.5f, GROUND to 0.5f, STEEL to 0.5f
        ),
        GHOST    to mapOf(
            GHOST to 2f, PSYCHIC to 2f,
            DARK to 0.5f,
            NORMAL to 0f
        ),
        DRAGON   to mapOf(
            DRAGON to 2f,
            STEEL to 0.5f,
            FAIRY to 0f
        ),
        DARK     to mapOf(
            GHOST to 2f, PSYCHIC to 2f,
            FIGHTING to 0.5f, DARK to 0.5f, FAIRY to 0.5f
        ),
        STEEL    to mapOf(
            ICE to 2f, ROCK to 2f, FAIRY to 2f,
            STEEL to 0.5f, FIRE to 0.5f, WATER to 0.5f, ELECTRIC to 0.5f
        ),
        FAIRY    to mapOf(
            FIGHTING to 2f, DRAGON to 2f, DARK to 2f,
            FIRE to 0.5f, POISON to 0.5f, STEEL to 0.5f
        )
    )

    // Gen 3: no Fairy type; Steel resists Ghost and Dark (removed in Gen 6)
    private val gen3: Map<Type, Map<Type, Float>> by lazy {
        val mutable = gen6
            .filterKeys { it != FAIRY }
            .mapValues { (_, v) -> v.filterKeys { it != FAIRY } }
            .toMutableMap()
        mutable[GHOST] = (mutable[GHOST] ?: emptyMap()) + mapOf(STEEL to 0.5f)
        mutable[DARK] = (mutable[DARK] ?: emptyMap()) + mapOf(STEEL to 0.5f)
        mutable
    }

    fun getMultiplier(attacker: Type, defender: Type, gen3Rules: Boolean = false): Float {
        val chart = if (gen3Rules) gen3 else gen6
        return chart[attacker]?.get(defender) ?: 1f
    }
}
