package com.pokecompanion.data.model

import android.graphics.Rect
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    /** Crop region stored as four nullable ints. All four null = full screenshot. */
    val cropLeft: Int? = null,
    val cropTop: Int? = null,
    val cropRight: Int? = null,
    val cropBottom: Int? = null,
    /** true = Gen 3 type rules (no Fairy; Steel resists Ghost/Dark). */
    val gen3Rules: Boolean = false,
    /** Comma-separated generation numbers enabled for name lookup. */
    val enabledGenerations: String = "1,2,3,4,5,6,7,8,9",
    val isLastUsed: Boolean = false
) {
    fun cropRect(): Rect? {
        if (cropLeft == null || cropTop == null || cropRight == null || cropBottom == null) return null
        return Rect(cropLeft, cropTop, cropRight, cropBottom)
    }

    fun generationList(): List<Int> =
        enabledGenerations.split(",").mapNotNull { it.trim().toIntOrNull() }
}
