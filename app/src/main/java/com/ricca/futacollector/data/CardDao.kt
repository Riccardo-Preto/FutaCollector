package com.ricca.futacollector.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card)

    @Query("SELECT * FROM collection ORDER BY dateAdded DESC")
    fun getAllCards(): Flow<List<Card>>

    @Delete
    suspend fun deleteCard(card: Card)
}