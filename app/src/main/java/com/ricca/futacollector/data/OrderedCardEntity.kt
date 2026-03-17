package com.ricca.futacollector.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ordered_cards",
    indices = [Index(value = ["card_id"], unique = true)]
)
data class OrderedCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    @ColumnInfo(name = "quantity")
    val quantity: Int = 1,
    @ColumnInfo(name = "note")
    val note: String = "",
    @ColumnInfo(name = "ordered_date")
    val orderedDate: Long = System.currentTimeMillis()
)