package com.pokecompanion.data.profile

import android.content.Context
import com.pokecompanion.data.database.PokeDatabase
import com.pokecompanion.data.database.PokemonDao
import com.pokecompanion.data.database.ProfileDao
import com.pokecompanion.data.model.PokemonEntity
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

    @Volatile private var profileDao: ProfileDao? = null
    @Volatile private var pokemonDao: PokemonDao? = null

    private val _profiles = MutableStateFlow<List<ProfileEntity>>(emptyList())
    val profiles = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<ProfileEntity?>(null)
    val activeProfile = _activeProfile.asStateFlow()

    fun init(context: Context) {
        if (profileDao != null) return
        val db = PokeDatabase.getInstance(context)
        profileDao = db.profileDao()
        pokemonDao  = db.pokemonDao()
        scope.launch { refresh() }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    /** Query Pokemon names, filtered by the active profile's enabled generations. */
    suspend fun search(query: String): List<PokemonEntity> {
        val gens = _activeProfile.value?.generationList() ?: (1..9).toList()
        return pokemonDao?.search(query, gens) ?: emptyList()
    }

    // ── Internal refresh ──────────────────────────────────────────────────────

    private suspend fun refresh() {
        val d = profileDao ?: return
        if (d.count() == 0) {
            d.insert(ProfileEntity(name = "Default", isLastUsed = true))
        }
        _activeProfile.value = d.getLastUsed()
        _profiles.value = d.getAll()
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun create(name: String, gen3Rules: Boolean = false) {
        scope.launch {
            profileDao?.insert(ProfileEntity(name = name, gen3Rules = gen3Rules))
            _profiles.value = profileDao?.getAll() ?: emptyList()
        }
    }

    fun delete(profile: ProfileEntity) {
        scope.launch {
            profileDao?.delete(profile)
            if (_activeProfile.value?.id == profile.id) {
                val next = profileDao?.getAll()?.firstOrNull()
                switchTo(next)
            } else {
                _profiles.value = profileDao?.getAll() ?: emptyList()
            }
        }
    }

    fun switchTo(profile: ProfileEntity?) {
        scope.launch {
            val d = profileDao ?: return@launch
            d.clearLastUsed()
            if (profile != null) d.markLastUsed(profile.id)
            _activeProfile.value = profile
            _profiles.value = d.getAll()
        }
    }

    fun saveCrop(profileId: Int, left: Int, top: Int, right: Int, bottom: Int) {
        scope.launch {
            val d = profileDao ?: return@launch
            val profile = _profiles.value.find { it.id == profileId } ?: return@launch
            val updated = profile.copy(cropLeft = left, cropTop = top, cropRight = right, cropBottom = bottom)
            d.update(updated)
            _profiles.value = d.getAll()
            if (_activeProfile.value?.id == profileId) _activeProfile.value = updated
        }
    }

    fun updateGen3Rules(profileId: Int, gen3Rules: Boolean) {
        scope.launch {
            val d = profileDao ?: return@launch
            val profile = _profiles.value.find { it.id == profileId } ?: return@launch
            val updated = profile.copy(gen3Rules = gen3Rules)
            d.update(updated)
            _profiles.value = d.getAll()
            if (_activeProfile.value?.id == profileId) _activeProfile.value = updated
        }
    }
}
