package com.ricca.futacollector.data.api

data class ApiCard(
    val card_name: String = "",
    val card_image: String? = null,
    val market_price: Double? = null,
    val set_name: String = "",
    val set_id: String = "",
    val card_image_id: String = "",
    val card_text: String?,
    val card_set_id: String = "", // numero seriale della carta
    val rarity: String = ""
)