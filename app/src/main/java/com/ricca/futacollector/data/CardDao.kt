package com.ricca.futacollector.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    // -----------------------------
    // CATALOGO CARTE (cards)
    // -----------------------------

    // Ricerca globale
    @Query("""
        SELECT * FROM cards 
        WHERE nome LIKE '%' || :query || '%'
        OR id LIKE '%' || :query || '%'
        ORDER BY id ASC
    """)
    suspend fun searchInDatabase(query: String): List<Card>


    // Carte per set (NUOVA VERSIONE CORRETTA)
    @Query("""
        SELECT * FROM cards
        WHERE set_id = :setId
        ORDER BY id ASC
    """)
    suspend fun getCardsBySet(setId: String): List<Card>


    // (OPZIONALE - puoi eliminarla più avanti)
    // Compatibilità vecchia logica prefix
    @Query("""
        SELECT * FROM cards 
        WHERE id LIKE :prefix || '%'
        ORDER BY id ASC
    """)
    suspend fun getCardsByPrefix(prefix: String): List<Card>


    // Singola carta
    @Query("""
        SELECT * FROM cards
        WHERE id = :cardId
        LIMIT 1
    """)
    suspend fun getCardById(cardId: String): Card?


    // -----------------------------
    // COLLEZIONE UTENTE
    // -----------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCard(userCard: UserCardEntity)


    @Transaction
    @Query("SELECT * FROM user_collection")
    fun getAllCollectionItems(): Flow<List<CollectionItem>>


    @Query("DELETE FROM user_collection WHERE card_id = :cardId")
    suspend fun deleteUserCard(cardId: String)


    @Query("DELETE FROM user_collection")
    suspend fun deleteAllUserCards()


    // -----------------------------
    // SETS
    // -----------------------------

    @Query("""
        SELECT * FROM sets
        ORDER BY ordine_utente ASC
    """)
    fun getAllSetsOrdered(): Flow<List<CardSetEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<CardSetEntity>)
}
