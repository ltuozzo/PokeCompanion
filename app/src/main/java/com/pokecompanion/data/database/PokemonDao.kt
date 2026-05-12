package com.pokecompanion.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pokecompanion.data.model.PokemonEntity

@Dao
interface PokemonDao {

    @Query("""
        SELECT * FROM pokemon
        WHERE LOWER(name) = LOWER(:name)
        AND generation IN (:generations)
        LIMIT 1
    """)
    suspend fun findByName(name: String, generations: List<Int>): PokemonEntity?

    @Query("""
        SELECT * FROM pokemon
        WHERE LOWER(name) LIKE LOWER(:query) || '%'
        AND generation IN (:generations)
        ORDER BY name
        LIMIT 20
    """)
    suspend fun search(query: String, generations: List<Int>): List<PokemonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pokemon: List<PokemonEntity>)

    @Query("SELECT COUNT(*) FROM pokemon")
    suspend fun count(): Int
}
