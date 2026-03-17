package com.ricca.futacollector.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_collection")
data class UserCardEntity(
    @PrimaryKey
    @ColumnInfo(name = "card_id")
    val cardId: String,
    @ColumnInfo(name = "count")
    val count: Int = 1,
    @ColumnInfo(name = "added_date")
    val addedDate: Long = System.currentTimeMillis()
)
