package com.pokecompanion.data.database

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pokecompanion.data.model.PokemonEntity

class DatabasePopulator(private val context: Context) {

    private data class PokemonJson(
        val id: Int,
        val name: String,
        val type1: String,
        val type2: String?,
        val gen: Int
    )

    suspend fun populateIfEmpty(dao: PokemonDao) {
        if (dao.count() > 0) return
        val json = context.assets.open("pokemon.json").bufferedReader().readText()
        val listType = object : TypeToken<List<PokemonJson>>() {}.type
        val list: List<PokemonJson> = Gson().fromJson(json, listType)
        dao.insertAll(list.map {
            PokemonEntity(id = it.id, name = it.name, type1 = it.type1, type2 = it.type2, generation = it.gen)
        })
    }
}
