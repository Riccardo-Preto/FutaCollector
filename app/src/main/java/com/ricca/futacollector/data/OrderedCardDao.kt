package com.ricca.futacollector.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderedCardDao {

    @Query("""
        SELECT oc.id, oc.card_id, oc.quantity, oc.note, oc.ordered_date,
               c.nome, c.card_image, c.market_price, c.rarity, c.set_id, c.card_color
        FROM ordered_cards oc
        JOIN cards c ON oc.card_id = c.id
        ORDER BY oc.ordered_date DESC
    """)
    fun getAllOrderedCards(): Flow<List<OrderedCardWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderedCard(card: OrderedCardEntity)

    @Query("DELETE FROM ordered_cards WHERE id = :id")
    suspend fun deleteOrderedCard(id: Int)

    @Query("SELECT * FROM ordered_cards WHERE card_id = :cardId LIMIT 1")
    suspend fun getOrderedCardByCardId(cardId: String): OrderedCardEntity?

    @Query("SELECT * FROM ordered_cards WHERE id = :id LIMIT 1")
    suspend fun getOrderedCardById(id: Int): OrderedCardEntity?
}

data class OrderedCardWithDetails(
    val id: Int,
    @ColumnInfo(name = "card_id") val cardId: String,
    val quantity: Int,
    val note: String,
    @ColumnInfo(name = "ordered_date") val orderedDate: Long,
    @ColumnInfo(name = "nome") val name: String?,
    @ColumnInfo(name = "card_image") val image: String?,
    @ColumnInfo(name = "market_price") val marketPrice: Double,
    val rarity: String?,
    @ColumnInfo(name = "set_id") val setId: String?,
    @ColumnInfo(name = "card_color") val color: String?
)