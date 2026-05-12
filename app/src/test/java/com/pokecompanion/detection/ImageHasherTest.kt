package com.pokecompanion.detection

import org.junit.Assert.*
import org.junit.Test

class ImageHasherTest {

    @Test
    fun `identical hashes have distance zero`() {
        assertEquals(0, ImageHasher.distance(42L, 42L))
    }

    @Test
    fun `all bits different gives distance 64`() {
        // 0L vs -1L (all bits set) = 64 differing bits
        assertEquals(64, ImageHasher.distance(0L, -1L))
    }

    @Test
    fun `single bit difference gives distance 1`() {
        assertEquals(1, ImageHasher.distance(0b1000L, 0b1001L))
    }

    @Test
    fun `isSame returns true when distance below threshold`() {
        val h1 = 0b11110000L
        val h2 = 0b11100000L // 1 bit different
        assertTrue(ImageHasher.isSame(h1, h2, threshold = 5))
    }

    @Test
    fun `isSame returns false when distance equals threshold`() {
        // distance = 5, threshold = 5 → NOT same (requires strictly less than)
        val h1 = 0b00000000L
        val h2 = 0b00011111L // 5 bits different
        assertFalse(ImageHasher.isSame(h1, h2, threshold = 5))
    }

    @Test
    fun `isSame returns false for completely different hashes`() {
        assertFalse(ImageHasher.isSame(0L, -1L, threshold = 5))
    }

    @Test
    fun `isSame returns true for identical hashes regardless of threshold`() {
        assertTrue(ImageHasher.isSame(0xDEADBEEFL, 0xDEADBEEFL, threshold = 0))
    }
}
