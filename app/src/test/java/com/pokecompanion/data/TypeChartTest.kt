package com.pokecompanion.data

import com.pokecompanion.data.model.Type
import org.junit.Assert.*
import org.junit.Test

class TypeChartTest {

    @Test
    fun `Water supereffective against Fire`() {
        assertEquals(2f, TypeChart.getMultiplier(Type.WATER, Type.FIRE))
    }

    @Test
    fun `Electric has no effect against Ground`() {
        assertEquals(0f, TypeChart.getMultiplier(Type.ELECTRIC, Type.GROUND))
    }

    @Test
    fun `Normal has no effect against Ghost`() {
        assertEquals(0f, TypeChart.getMultiplier(Type.NORMAL, Type.GHOST))
    }

    @Test
    fun `Ghost neutral against Steel in Gen6`() {
        assertEquals(1f, TypeChart.getMultiplier(Type.GHOST, Type.STEEL, gen3Rules = false))
    }

    @Test
    fun `Ghost resisted by Steel in Gen3`() {
        assertEquals(0.5f, TypeChart.getMultiplier(Type.GHOST, Type.STEEL, gen3Rules = true))
    }

    @Test
    fun `Dark resisted by Steel in Gen3`() {
        assertEquals(0.5f, TypeChart.getMultiplier(Type.DARK, Type.STEEL, gen3Rules = true))
    }

    @Test
    fun `Dragon has no effect against Fairy in Gen6`() {
        assertEquals(0f, TypeChart.getMultiplier(Type.DRAGON, Type.FAIRY, gen3Rules = false))
    }

    @Test
    fun `Fairy attacking Dragon in Gen3 returns 1f (type does not exist)`() {
        assertEquals(1f, TypeChart.getMultiplier(Type.FAIRY, Type.DRAGON, gen3Rules = true))
    }

    @Test
    fun `Poison has no effect against Steel`() {
        assertEquals(0f, TypeChart.getMultiplier(Type.POISON, Type.STEEL))
    }

    @Test
    fun `Ground has no effect against Flying`() {
        assertEquals(0f, TypeChart.getMultiplier(Type.GROUND, Type.FLYING))
    }
}
