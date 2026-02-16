package com.ricca.futacollector.data

import androidx.room.*
import com.ricca.futacollector.data.api.ApiCard // Assicurati che l'import sia corretto per il tuo modello API
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CardDao {

    // 1. La tua funzione esistente (Resta uguale, è la nostra "base")
    @Query("""
    SELECT * FROM cards 
    WHERE nome LIKE '%' || :query || '%' 
    OR id LIKE '%' || :query || '%' 
    OR card_type LIKE '%' || :query || '%' -- <--- AGGIUNGI QUESTA RIGA
    ORDER BY id ASC
""")
    abstract suspend fun searchInDatabase(query: String): List<Card>

    open suspend fun searchAdvanced(query: String): List<Card> {
        // Dividiamo la stringa in parole qui dentro
        val terms = query.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()

        // Prima passata SQL usando la prima parola
        val firstResults = searchInDatabase(terms[0])

        if (terms.size == 1) return firstResults

        // Raffinamento Kotlin usando .name (come in Card.kt)
        return firstResults.filter { card ->
            terms.all { term ->
                val nameMatch = card.name?.lowercase()?.contains(term) == true
                val idMatch = card.id.lowercase().contains(term)
                val subTypesMatch = card.subTypes?.lowercase()?.contains(term) == true
                val typeMatch = card.type?.lowercase()?.contains(term) == true

                nameMatch || idMatch || subTypesMatch || typeMatch
            }
        }
    }
    @Query("SELECT * FROM cards WHERE card_type = 'Leader' OR card_type = 'LEADER' ORDER BY id ASC")
    abstract suspend fun getLeadersForSelection(): List<Card>

    @Query("SELECT * FROM cards WHERE set_id = :setId ORDER BY id ASC")
    abstract suspend fun getCardsBySet(setId: String): List<Card>

    @Query("SELECT * FROM cards WHERE id LIKE :prefix || '%' ORDER BY id ASC")
    abstract suspend fun getCardsByPrefix(prefix: String): List<Card>

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    abstract suspend fun getCardById(cardId: String): Card?

    @Query("SELECT * FROM user_collection WHERE card_id = :cardId LIMIT 1")
    abstract suspend fun getUserCardById(cardId: String): UserCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserCard(userCard: UserCardEntity)

    @Transaction
    @Query("SELECT * FROM user_collection")
    abstract fun getAllCollectionItems(): Flow<List<UserCardEntity>>

    @Query("DELETE FROM user_collection WHERE card_id = :cardId")
    abstract suspend fun deleteUserCard(cardId: String)

    @Query("DELETE FROM user_collection")
    abstract suspend fun deleteAllUserCards()

    @Query("SELECT * FROM sets ORDER BY ordine_utente ASC")
    abstract fun getAllSets(): Flow<List<CardSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSets(sets: List<CardSetEntity>)

    // --- LOGICA AGGIORNAMENTO PREZZI ---

    @Query("""
        UPDATE cards 
        SET market_price = :price,
            card_text = CASE WHEN (card_text IS NULL OR card_text = '') THEN :text ELSE card_text END
        WHERE id LIKE :apiId || '%' 
        AND LOWER(nome) = LOWER(:fullName)
    """)
    abstract suspend fun updatePriceByExactNameAndPrefix(price: Double, text: String?, apiId: String, fullName: String): Int

    @Query("""
        UPDATE cards 
        SET market_price = :price,
            card_text = CASE WHEN (card_text IS NULL OR card_text = '') THEN :text ELSE card_text END
        WHERE card_image = :imageUrl
    """)
    abstract suspend fun updatePriceByImageUrl(price: Double, text: String?, imageUrl: String): Int

    /**
     * Esegue l'intero aggiornamento in una singola transazione atomica.
     * Molto più veloce e previene i lag della UI.
     */
    @Transaction
    open suspend fun updateAllPricesSmart(apiCards: List<com.ricca.futacollector.data.api.ApiCard>): Int {
        var totalUpdated = 0
        apiCards.forEach { apiCard ->
            val price = apiCard.market_price ?: 0.0
            if (price > 0) {
                // Tentativo 1: URL Immagine
                var rows = 0
                if (!apiCard.card_image.isNullOrBlank()) {
                    rows = updatePriceByImageUrl(price, apiCard.card_text, apiCard.card_image)
                }

                // Tentativo 2: Nome + Prefisso ID
                if (rows == 0) {
                    rows = updatePriceByExactNameAndPrefix(
                        price,
                        apiCard.card_text,
                        apiCard.card_set_id,
                        apiCard.card_name
                    )
                }
                totalUpdated += rows
            }
        }
        return totalUpdated
    }

    @Query("SELECT * FROM wishlist ORDER BY addedDate DESC")
    abstract fun getWishlistItems(): Flow<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addToWishlist(item: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE cardId = :cardId")
    abstract suspend fun removeFromWishlist(cardId: String)
}