package com.ricca.futacollector

import com.google.gson.annotations.SerializedName

data class Card(
    @SerializedName("card_set_id") val id: String = "",
    @SerializedName("card_name") val name: String = "",
    @SerializedName("card_image") val image: String? = null,
    @SerializedName("set_name") val setName: String = "",
    @SerializedName("inventory_price") val inventoryPrice: Double? = null,
    @SerializedName("market_price") val marketPrice: Double? = null
)
