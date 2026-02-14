package com.ricca.futacollector.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Query("SELECT * FROM decks ORDER BY id DESC")
    fun getAllDecks(): Flow<List<Deck>>

    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Query("UPDATE decks SET name = :newName WHERE id = :deckId")
    suspend fun renameDeck(deckId: Int, newName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckCard(deckCard: DeckCard)

    @Query("DELETE FROM deck_cards WHERE deckId = :deckId AND cardId = :cardId")
    suspend fun removeCardFromDeck(deckId: Int, cardId: String)

    @Transaction
    @Query("""
    SELECT 
        dc.cardId, 
        dc.quantity as countInDeck,
        c.nome as cardName, 
        c.card_image as cardImage, 
        c.card_color as cardColor,
        c.card_type as cardType,
        c.card_cost as cardCost,
        (SELECT count FROM user_collection WHERE card_id = dc.cardId) as countInCollection
    FROM deck_cards dc
    JOIN cards c ON dc.cardId = c.id
    JOIN decks d ON dc.deckId = d.id
    WHERE dc.deckId = :deckId 
    AND dc.cardId != d.leaderCardId  -- <--- Esclude il leader dal conteggio e dalla lista
""")
    fun getDeckDetails(deckId: Int): Flow<List<DeckWithCount>>
}

// Data class per mappare i risultati della query
data class DeckWithCount(
    val cardId: String,
    val countInDeck: Int,
    val cardName: String?,
    val cardImage: String?,
    val cardColor: String?,
    val cardType: String?,
    val cardCost: String?,
    val countInCollection: Int? // Se nullo, l'utente ha 0 copie
)

// Creiamo una classe di supporto per il risultato della JOIN
data class DeckCardDetails(
    val cardId: String,
    val name: String,
    val image: String?,
    val quantity: Int,
    val color: String?,
    val cost: Int,
    val type: String?
)
