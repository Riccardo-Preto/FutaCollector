package com.ricca.futacollector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist")
data class WishlistEntity(
    @PrimaryKey val cardId: String,
    val quantity: Int = 1,
    val reason: String = "General",
    val addedDate: Long = System.currentTimeMillis()
)