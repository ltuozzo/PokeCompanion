package com.pokecompanion.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pokecompanion.data.model.PokemonEntity
import com.pokecompanion.data.model.ProfileEntity

@Database(
    entities = [PokemonEntity::class, ProfileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PokeDatabase : RoomDatabase() {

    abstract fun pokemonDao(): PokemonDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile private var instance: PokeDatabase? = null

        fun getInstance(context: Context): PokeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PokeDatabase::class.java,
                    "poke_companion.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `profiles` (
                        `id`                 INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name`               TEXT    NOT NULL,
                        `cropLeft`           INTEGER,
                        `cropTop`            INTEGER,
                        `cropRight`          INTEGER,
                        `cropBottom`         INTEGER,
                        `gen3Rules`          INTEGER NOT NULL DEFAULT 0,
                        `enabledGenerations` TEXT    NOT NULL DEFAULT '1,2,3,4,5,6,7,8,9',
                        `isLastUsed`         INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
