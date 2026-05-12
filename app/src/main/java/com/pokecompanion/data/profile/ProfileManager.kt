package com.pokecompanion.data.profile

import android.content.Context
import com.pokecompanion.data.database.PokeDatabase
import com.pokecompanion.data.database.ProfileDao
import com.pokecompanion.data.model.ProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Singleton that owns the active profile state.
 *
 * Call [init] once (from the service or MainActivity) before using any other function.
 * Multiple calls to [init] with the same context are safe — only the first initialises.
 */
object ProfileManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile private var dao: ProfileDao? = null

    private val _profiles = MutableStateFlow<List<ProfileEntity>>(emptyList())
    val profiles = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<ProfileEntity?>(null)
    val activeProfile = _activeProfile.asStateFlow()

    fun init(context: Context) {
        if (dao != null) return
        dao = PokeDatabase.getInstance(context).profileDao()
        scope.launch { refresh() }
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    private suspend fun refresh() {
        val d = dao ?: return
        // Create a Default profile if none exist yet.
        if (d.count() == 0) {
            val id = d.insert(ProfileEntity(name = "Default", isLastUsed = true))
            _activeProfile.value = d.getLastUsed()
        } else {
            _activeProfile.value = d.getLastUsed()
        }
        _profiles.value = d.getAll()
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    fun create(name: String) {
        scope.launch {
            dao?.insert(ProfileEntity(name = name))
            _profiles.value = dao?.getAll() ?: emptyList()
        }
    }

    fun delete(profile: ProfileEntity) {
        scope.launch {
            dao?.delete(profile)
            // If we deleted the active profile, switch to the first remaining one.
            if (_activeProfile.value?.id == profile.id) {
                val next = dao?.getAll()?.firstOrNull()
                switchTo(next)
            } else {
                _profiles.value = dao?.getAll() ?: emptyList()
            }
        }
    }

    fun switchTo(profile: ProfileEntity?) {
        scope.launch {
            val d = dao ?: return@launch
            d.clearLastUsed()
            if (profile != null) d.markLastUsed(profile.id)
            _activeProfile.value = profile
            _profiles.value = d.getAll()
        }
    }

    /** Save calibration crop rect to the given profile. */
    fun saveCrop(profileId: Int, left: Int, top: Int, right: Int, bottom: Int) {
        scope.launch {
            val d = dao ?: return@launch
            val profile = _profiles.value.find { it.id == profileId } ?: return@launch
            val updated = profile.copy(cropLeft = left, cropTop = top, cropRight = right, cropBottom = bottom)
            d.update(updated)
            _profiles.value = d.getAll()
            if (_activeProfile.value?.id == profileId) _activeProfile.value = updated
        }
    }
}
