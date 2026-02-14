package com.ricca.futacollector.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val leaderCardId: String // L'ID della carta Leader (es. "OP01-001")
)

@Entity(
    tableName = "deck_cards",
    primaryKeys = ["deckId", "cardId"],
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE // Se cancelli il mazzo, si cancellano le sue carte
        )
    ]
)
data class DeckCard(
    val deckId: Int,
    val cardId: String,
    val quantity: Int
)