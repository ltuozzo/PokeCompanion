package com.pokecompanion.ui.theme

import androidx.compose.ui.graphics.Color
import com.pokecompanion.data.model.Type

object TypeColors {

    fun background(type: Type): Color = when (type) {
        Type.NORMAL   -> Color(0xFFA8A878)
        Type.FIRE     -> Color(0xFFF08030)
        Type.WATER    -> Color(0xFF6890F0)
        Type.GRASS    -> Color(0xFF78C850)
        Type.ELECTRIC -> Color(0xFFF8D030)
        Type.ICE      -> Color(0xFF98D8D8)
        Type.FIGHTING -> Color(0xFFC03028)
        Type.POISON   -> Color(0xFFA040A0)
        Type.GROUND   -> Color(0xFFE0C068)
        Type.FLYING   -> Color(0xFFA890F0)
        Type.PSYCHIC  -> Color(0xFFF85888)
        Type.BUG      -> Color(0xFFA8B820)
        Type.ROCK     -> Color(0xFFB8A038)
        Type.GHOST    -> Color(0xFF705898)
        Type.DRAGON   -> Color(0xFF7038F8)
        Type.DARK     -> Color(0xFF705848)
        Type.STEEL    -> Color(0xFFB8B8D0)
        Type.FAIRY    -> Color(0xFFEE99AC)
    }

    /** Dark types get white text; light backgrounds get dark text. */
    fun content(type: Type): Color = when (type) {
        Type.ELECTRIC, Type.ICE, Type.NORMAL,
        Type.GROUND, Type.FLYING, Type.STEEL,
        Type.GRASS, Type.BUG -> Color(0xFF2A2A2A)
        else -> Color.White
    }
}
