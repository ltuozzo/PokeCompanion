package com.pokecompanion.engine

import com.pokecompanion.data.model.Type
import org.junit.Assert.*
import org.junit.Test

class WeaknessEngineTest {

    @Test
    fun `Gengar Ghost-Poison has quad Psychic weakness and correct immunities`() {
        val result = WeaknessEngine.compute(Type.GHOST, Type.POISON)
        // Psychic: 2x (Ghost weak) * 2x (Poison weak) = 4x
        assertTrue(Type.PSYCHIC in result.quadWeak)
        // Ground: 1x (Ghost neutral) * 2x (Poison weak) = 2x
        assertTrue(Type.GROUND in result.weak)
        // Dark: 2x (Ghost weak) * 1x (Poison neutral) = 2x
        assertTrue(Type.DARK in result.weak)
        // Normal: 0x (Ghost immune) * 1x = 0x
        assertTrue(Type.NORMAL in result.immune)
        // Fighting: 0x (Ghost immune) * 0.5x (Poison resists) = 0x
        assertTrue(Type.FIGHTING in result.immune)
        // Bug: 0.5x (Ghost resists) * 0.5x (Poison resists) = 0.25x
        assertTrue(Type.BUG in result.quarterResist)
    }

    @Test
    fun `Charizard Fire-Flying has quad Rock weakness`() {
        val result = WeaknessEngine.compute(Type.FIRE, Type.FLYING)
        // Rock: 2x (Fire weak) * 2x (Flying weak) = 4x — the famous Stealth Rock problem
        assertTrue(Type.ROCK in result.quadWeak)
        // Water: 2x (Fire weak) * 1x (Flying neutral) = 2x
        assertTrue(Type.WATER in result.weak)
        // Electric: 1x (Fire neutral) * 2x (Flying weak) = 2x
        assertTrue(Type.ELECTRIC in result.weak)
        // Ground: 2x (Fire weak) * 0x (Flying immune) = 0x
        assertTrue(Type.GROUND in result.immune)
    }

    @Test
    fun `pure Water correct weaknesses and resistances`() {
        val result = WeaknessEngine.compute(Type.WATER, null)
        assertTrue(Type.ELECTRIC in result.weak)
        assertTrue(Type.GRASS in result.weak)
        assertTrue(Type.WATER in result.halfResist)
        assertTrue(Type.FIRE in result.halfResist)
        assertTrue(Type.ICE in result.halfResist)
        assertTrue(Type.STEEL in result.halfResist)
    }

    @Test
    fun `Gen3 Steel resists Ghost and Dark but not in Gen6`() {
        val gen6 = WeaknessEngine.compute(Type.STEEL, null, gen3Rules = false)
        val gen3 = WeaknessEngine.compute(Type.STEEL, null, gen3Rules = true)
        assertFalse(Type.GHOST in gen6.halfResist)
        assertFalse(Type.DARK in gen6.halfResist)
        assertTrue(Type.GHOST in gen3.halfResist)
        assertTrue(Type.DARK in gen3.halfResist)
    }

    @Test
    fun `Fairy absent from Gen3 calculation for Dragon type`() {
        val gen6 = WeaknessEngine.compute(Type.DRAGON, null, gen3Rules = false)
        val gen3 = WeaknessEngine.compute(Type.DRAGON, null, gen3Rules = true)
        // Dragon is weak to Fairy in Gen 6+
        assertTrue(Type.FAIRY in gen6.weak)
        // Fairy type doesn't exist in Gen 3
        assertFalse(Type.FAIRY in gen3.weak)
        assertFalse(Type.FAIRY in gen3.quadWeak)
        assertFalse(Type.FAIRY in gen3.immune)
    }

    @Test
    fun `Shedinja Bug-Ghost has correct immunities`() {
        val result = WeaknessEngine.compute(Type.BUG, Type.GHOST)
        // Normal: 1x Bug * 0x Ghost = 0x immune
        assertTrue(Type.NORMAL in result.immune)
        // Fighting: 0.5x Bug * 0x Ghost = 0x immune
        assertTrue(Type.FIGHTING in result.immune)
        // Rock: 2x Bug * 1x Ghost = 2x weak
        assertTrue(Type.ROCK in result.weak)
        // Flying: 2x Bug * 1x Ghost = 2x weak
        assertTrue(Type.FLYING in result.weak)
        // Fire: 2x Bug * 1x Ghost = 2x weak
        assertTrue(Type.FIRE in result.weak)
    }
}
