package com.ricca.futacollector.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = CardSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["set_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("set_id")]
)
data class Card(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,

    @ColumnInfo(name = "nome") val name: String?,
    @ColumnInfo(name = "market_price") val marketPrice: Double = 0.0,
    @ColumnInfo(name = "card_image") val image: String?,
    @ColumnInfo(name = "rarity") val rarity: String?,
    @ColumnInfo(name = "card_color") val color: String?,
    @ColumnInfo(name = "card_type") val type: String?,
    @ColumnInfo(name = "card_cost") val cost: String?,
    @ColumnInfo(name = "card_power") val power: String?,
    @ColumnInfo(name = "counter_amount") val counter: String?,
    @ColumnInfo(name = "attribute") val attribute: String?,
    @ColumnInfo(name = "api_image_id") val apiImageId: String?,
    @ColumnInfo(name = "card_text") val effect: String?,
    @ColumnInfo(name = "set_id") val setId: String?,
    @ColumnInfo(name = "sub_types") val subTypes: String? = ""

)