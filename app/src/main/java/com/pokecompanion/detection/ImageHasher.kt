package com.pokecompanion.detection

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Computes an average hash (aHash) of a bitmap for fast change detection.
 *
 * The approach: scale to 8×8, convert to luma, compare each pixel to the
 * average brightness. Output is a 64-bit integer. Similar images produce
 * hashes with a small Hamming distance; identical images produce the same hash.
 */
object ImageHasher {

    private const val HASH_SIZE = 8 // 8×8 = 64-bit hash

    fun hash(bitmap: Bitmap, cropRect: Rect? = null): Long {
        val source = if (cropRect != null) {
            Bitmap.createBitmap(
                bitmap,
                cropRect.left, cropRect.top,
                cropRect.width(), cropRect.height()
            )
        } else {
            bitmap
        }

        val scaled = Bitmap.createScaledBitmap(source, HASH_SIZE, HASH_SIZE, true)
        if (source !== bitmap) source.recycle()

        val pixels = IntArray(HASH_SIZE * HASH_SIZE)
        scaled.getPixels(pixels, 0, HASH_SIZE, 0, 0, HASH_SIZE, HASH_SIZE)
        scaled.recycle()

        val luma = pixels.map { px ->
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            (r * 299 + g * 587 + b * 114) / 1000 // ITU-R BT.601 luma
        }

        val avg = luma.average()

        var hash = 0L
        luma.forEachIndexed { i, v ->
            if (v >= avg) hash = hash or (1L shl i)
        }

        return hash
    }

    /** Hamming distance: number of differing bits. 0 = identical, 64 = opposite. */
    fun distance(h1: Long, h2: Long): Int =
        java.lang.Long.bitCount(h1 xor h2)

    /** True if images are close enough to be considered unchanged. */
    fun isSame(h1: Long, h2: Long, threshold: Int = 5): Boolean =
        distance(h1, h2) < threshold
}
