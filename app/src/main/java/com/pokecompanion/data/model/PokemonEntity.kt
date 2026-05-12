package com.pokecompanion.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon")
data class PokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val type1: String,
    val type2: String?,
    val generation: Int
) {
    fun primaryType(): Type = Type.fromString(type1) ?: Type.NORMAL
    fun secondaryType(): Type? = type2?.let { Type.fromString(it) }
}
