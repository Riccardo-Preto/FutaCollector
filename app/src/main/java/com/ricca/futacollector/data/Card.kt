package com.ricca.futacollector.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "collection") // Room ora sa che questa è una tabella
data class Card(
    @PrimaryKey
    @SerializedName("card_set_id") val id: String = "",
    @SerializedName("card_name") val name: String = "",
    @SerializedName("card_image") val image: String? = null,
    @SerializedName("set_name") val setName: String = "",
    @SerializedName("inventory_price") val inventoryPrice: Double? = null,
    @SerializedName("market_price") val marketPrice: Double? = null,
    val dateAdded: Long = System.currentTimeMillis() // Utile per ordinare la collezione
)