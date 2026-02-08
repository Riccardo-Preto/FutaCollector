package com.ricca.futacollector.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "collection")
data class Card(
    @PrimaryKey(autoGenerate = true)
    val internalId: Int = 0, // Questo ID lo gestisce Room automaticamente
    val id: String,          // Il card_set_id dell'API (ora può essere duplicato nel DB)
    val name: String,
    val image: String,
    val setName: String,
    val inventoryPrice: String,
    val marketPrice: String,
    val dateAdded: Long
)