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

    @Transaction
    @Query("""
        SELECT 
            dc.cardId, 
            dc.quantity as countInDeck,
            dc.isConsidering,
            dc.orderedQuantity,
            c.nome as cardName, 
            c.card_image as cardImage, 
            c.card_color as cardColor,
            c.card_type as cardType,
            c.card_cost as cardCost,
            c.card_power as cardPower,    
            c.counter_amount as cardCounter, 
            c.market_price as marketPrice, 
            COALESCE((SELECT uc.count FROM user_collection uc WHERE uc.card_id = dc.cardId), 0) as countInCollection
        FROM deck_cards dc
        JOIN cards c ON dc.cardId = c.id
        JOIN decks d ON dc.deckId = d.id
        WHERE dc.deckId = :deckId 
        AND dc.cardId != d.leaderCardId
    """)
    fun getDeckDetails(deckId: Int): Flow<List<DeckWithCount>>

    @Query("UPDATE deck_cards SET orderedQuantity = :newOrderedQty WHERE deckId = :deckId AND cardId = :cardId AND isConsidering = :isConsidering")
    suspend fun updateOrderedQuantity(deckId: Int, cardId: String, isConsidering: Boolean, newOrderedQty: Int)

    @Query("UPDATE deck_cards SET quantity = :newQty WHERE deckId = :deckId AND cardId = :cardId AND isConsidering = :isConsidering")
    suspend fun updateCardQuantity(deckId: Int, cardId: String, isConsidering: Boolean, newQty: Int)

    @Query("SELECT * FROM deck_cards WHERE deckId = :deckId AND cardId = :cardId AND isConsidering = :isConsidering")
    suspend fun getSpecificDeckCard(deckId: Int, cardId: String, isConsidering: Boolean): DeckCard?

    @Query("DELETE FROM deck_cards WHERE deckId = :deckId AND cardId = :cardId AND isConsidering = :isConsidering")
    suspend fun deleteSpecificDeckCard(deckId: Int, cardId: String, isConsidering: Boolean)

    // Elimina tutti i mazzi
    @Query("DELETE FROM decks")
    suspend fun deleteAllDecks()

    // Elimina tutte le carte associate ai mazzi (utile se non hai il CASCADE)
    @Query("DELETE FROM deck_cards")
    suspend fun deleteAllDeckCards()

    // Operazione atomica per resettare tutto il comparto mazzi
    @Transaction
    suspend fun nukeAllDeckData() {
        deleteAllDeckCards()
        deleteAllDecks()
    }

}

data class DeckWithCount(
    val cardId: String,
    val countInDeck: Int,
    val cardName: String?,
    val cardImage: String?,
    val cardColor: String?,
    val cardType: String?,
    val cardCost: String?,
    val cardPower: String?,
    val cardCounter: String?,
    val marketPrice: Double?,
    val countInCollection: Int,
    val isConsidering: Boolean,
    val orderedQuantity: Int
)