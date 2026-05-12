package com.pokecompanion.detection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pokecompanion.data.database.PokemonDao
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Crops a Bitmap to [cropRect], runs ML Kit OCR, and looks up candidate
 * strings in the Room database.  Two DB hits → 2v2; one hit → 1v1; none → idle.
 *
 * The recognizer is created once and reused across calls (ML Kit manages its lifecycle).
 *
 * @param cropRect  Region of the captured bitmap that contains enemy name(s).
 *                  Null = use the full bitmap (useful for testing).
 * @param generations  List of generation numbers the current profile has enabled.
 */
class OcrPipeline(
    private val dao: PokemonDao,
    var cropRect: Rect? = null,
    var generations: List<Int> = (1..9).toList()
) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Run the full OCR → lookup pipeline on [bitmap].
     * Must be called from a coroutine (uses [suspendCancellableCoroutine] internally).
     */
    suspend fun process(bitmap: Bitmap): DetectionResult {
        val cropped = crop(bitmap)
        val image = InputImage.fromBitmap(cropped, 0)

        val mlkitText = recognizeText(image)
        if (cropped !== bitmap) cropped.recycle()

        if (mlkitText == null) return DetectionResult.None

        // Collect all text blocks recognised in the crop region.
        // Each TextBlock typically corresponds to one visual "word cluster".
        val candidates = mlkitText.textBlocks
            .map { it.text.trim() }
            .filter { it.isNotBlank() }

        Log.d(TAG, "OCR candidates: $candidates")

        // Try to match candidates to Pokemon names (stop once we have 2 hits).
        val hits = mutableListOf<com.pokecompanion.data.model.PokemonEntity>()
        for (candidate in candidates) {
            if (hits.size >= 2) break
            val pokemon = dao.findByName(candidate, generations)
            if (pokemon != null) hits.add(pokemon)
        }

        return when (hits.size) {
            0 -> DetectionResult.None
            1 -> DetectionResult.Single(hits[0])
            else -> DetectionResult.Double(hits[0], hits[1])
        }
    }

    /** Crops [bitmap] to [cropRect], or returns the original if cropRect is null. */
    private fun crop(bitmap: Bitmap): Bitmap {
        val rect = cropRect ?: return bitmap
        val safeLeft   = rect.left.coerceIn(0, bitmap.width)
        val safeTop    = rect.top.coerceIn(0, bitmap.height)
        val safeRight  = rect.right.coerceIn(safeLeft, bitmap.width)
        val safeBottom = rect.bottom.coerceIn(safeTop, bitmap.height)
        if (safeLeft == 0 && safeTop == 0 && safeRight == bitmap.width && safeBottom == bitmap.height) {
            return bitmap
        }
        return Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeRight - safeLeft, safeBottom - safeTop)
    }

    /**
     * Wraps ML Kit's Task-based API in a coroutine.
     * Returns null if recognition fails.
     */
    private suspend fun recognizeText(image: InputImage): com.google.mlkit.vision.text.Text? =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { text -> cont.resume(text) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ML Kit recognition failed: ${e.message}")
                    cont.resume(null)
                }
        }

    fun close() = recognizer.close()

    companion object {
        private const val TAG = "PokeCompanion"
    }
}
