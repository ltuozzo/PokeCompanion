package com.pokecompanion.data.model

enum class Type {
    NORMAL, FIRE, WATER, GRASS, ELECTRIC, ICE,
    FIGHTING, POISON, GROUND, FLYING, PSYCHIC, BUG,
    ROCK, GHOST, DRAGON, DARK, STEEL, FAIRY;

    companion object {
        /** All types available in Gen 1–5 (excludes Fairy). */
        val gen3Types: List<Type> = entries.filter { it != FAIRY }

        fun fromString(s: String): Type? = entries.find { it.name == s.uppercase() }
    }
}
