package com.pokecompanion.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.pokecompanion.detection.PokeAccessibilityService

/**
 * Thin wrapper around SharedPreferences for app-wide settings.
 * Call [init] once from MainActivity (and from the service for safety — it's idempotent).
 * Writes are immediately reflected in the relevant companion vars so the service
 * picks them up on its next poll cycle without a restart.
 */
object SettingsManager {

    private const val PREFS       = "poke_settings"
    private const val KEY_DISPLAY  = "display_id"
    private const val KEY_POLL_MS  = "poll_interval_ms"
    private const val KEY_GEN3     = "default_gen3_rules"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Apply persisted display ID immediately.
        PokeAccessibilityService.displayId = displayId
    }

    var displayId: Int
        get() = prefs?.getInt(KEY_DISPLAY, 0) ?: 0
        set(v) {
            prefs?.edit()?.putInt(KEY_DISPLAY, v)?.apply()
            PokeAccessibilityService.displayId = v
        }

    /** Milliseconds between screenshot polls. Defaults to 1 000 ms. */
    var pollIntervalMs: Long
        get() = prefs?.getLong(KEY_POLL_MS, 1_000L) ?: 1_000L
        set(v) { prefs?.edit()?.putLong(KEY_POLL_MS, v)?.apply() }

    /** Whether newly created profiles default to Gen 3 type rules. */
    var defaultGen3Rules: Boolean
        get() = prefs?.getBoolean(KEY_GEN3, false) ?: false
        set(v) { prefs?.edit()?.putBoolean(KEY_GEN3, v)?.apply() }
}
