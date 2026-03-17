package com.ricca.futacollector.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Card::class,
        CardSetEntity::class,
        UserCardEntity::class,
        Deck::class,
        DeckCard::class,
        WishlistEntity::class,
        OrderedCardEntity::class
    ],
    version = 10
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    abstract fun deckDao(): DeckDao

    abstract fun orderedCardDao(): OrderedCardDao

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
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_collection ADD COLUMN added_date INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS ordered_cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                card_id TEXT NOT NULL,
                quantity INTEGER NOT NULL DEFAULT 1,
                note TEXT NOT NULL DEFAULT '',
                ordered_date INTEGER NOT NULL
            )
        """)
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ricrea deck_cards senza orderedQuantity
                db.execSQL("CREATE TABLE deck_cards_new (deckId INTEGER NOT NULL, cardId TEXT NOT NULL, quantity INTEGER NOT NULL, isConsidering INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(deckId, cardId, isConsidering), FOREIGN KEY(deckId) REFERENCES decks(id) ON DELETE CASCADE)")
                db.execSQL("INSERT INTO deck_cards_new (deckId, cardId, quantity, isConsidering) SELECT deckId, cardId, quantity, isConsidering FROM deck_cards")
                db.execSQL("DROP TABLE deck_cards")
                db.execSQL("ALTER TABLE deck_cards_new RENAME TO deck_cards")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ricrea ordered_cards con unique constraint su card_id
                db.execSQL("CREATE TABLE ordered_cards_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, card_id TEXT NOT NULL, quantity INTEGER NOT NULL DEFAULT 1, note TEXT NOT NULL DEFAULT '', ordered_date INTEGER NOT NULL, UNIQUE(card_id))")
                // Copia solo la prima riga per ogni card_id (elimina eventuali duplicati esistenti)
                db.execSQL("INSERT INTO ordered_cards_new SELECT id, card_id, quantity, note, ordered_date FROM ordered_cards GROUP BY card_id")
                db.execSQL("DROP TABLE ordered_cards")
                db.execSQL("ALTER TABLE ordered_cards_new RENAME TO ordered_cards")
            }
        }
    }
}