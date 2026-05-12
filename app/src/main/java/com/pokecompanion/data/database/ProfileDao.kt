package com.pokecompanion.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pokecompanion.data.model.ProfileEntity

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY name")
    suspend fun getAll(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE isLastUsed = 1 LIMIT 1")
    suspend fun getLastUsed(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("UPDATE profiles SET isLastUsed = 0")
    suspend fun clearLastUsed()

    @Query("UPDATE profiles SET isLastUsed = 1 WHERE id = :id")
    suspend fun markLastUsed(id: Int)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int
}
