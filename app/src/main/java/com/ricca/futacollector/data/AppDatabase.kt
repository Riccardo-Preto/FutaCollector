package com.ricca.futacollector.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Card::class,
        CardSetEntity::class,
        UserCardEntity::class,
        Deck::class,      // Nuova
        DeckCard::class   // Nuova
    ],
    version = 5
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun deckDao(): DeckDao // Nuovo DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "futa_database"
                )
                    .createFromAsset("collezione_one_piece.db")
                    .fallbackToDestructiveMigration() // Necessario per gestire le nuove tabelle deck
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}