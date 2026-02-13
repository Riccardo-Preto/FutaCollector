package com.ricca.futacollector.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Card::class, CardSetEntity::class, UserCardEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "futa_database" // Nome del DB interno all'app
                )
                    // Questa riga dice a Room: "Invece di creare un DB vuoto,
                    // copia quello che trovi nella cartella assets"
                    .createFromAsset("collezione_one_piece.db")
                    //.fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}