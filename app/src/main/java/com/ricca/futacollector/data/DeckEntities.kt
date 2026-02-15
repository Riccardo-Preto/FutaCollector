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

// In com.ricca.futacollector.data.DeckCard
@Entity(
    tableName = "deck_cards",
    primaryKeys = ["deckId", "cardId", "isConsidering"],
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DeckCard(
    val deckId: Int,
    val cardId: String,
    val quantity: Int,
    val isConsidering: Boolean = false,
    val orderedQuantity: Int = 0
)