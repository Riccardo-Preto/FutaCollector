package com.ricca.futacollector.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card)

    @Query("SELECT * FROM collection ORDER BY dateAdded DESC")
    fun getAllCards(): Flow<List<Card>>

    @Query("SELECT * FROM collection WHERE id = :cardId AND image = :cardImage")
    suspend fun getCardsByIdAndImage(cardId: String, cardImage: String): List<Card>

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("DELETE FROM collection") // Sostituisci cards_table con il nome reale della tua tabella se è diverso
    suspend fun deleteAll()
}