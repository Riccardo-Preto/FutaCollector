package com.ricca.futacollector.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sets")
data class CardSetEntity(
    @PrimaryKey
    val id: String, // La PrimaryKey solitamente Room la accetta non-null,
    // ma se crasha ancora metti String? come suggerito dal logcat Found.

    @ColumnInfo(name = "api_id")
    val apiId: String?,

    @ColumnInfo(name = "nome")
    val nome: String?,

    @ColumnInfo(name = "ordine_utente")
    val ordineUtente: Int?,

    @ColumnInfo(name = "set_cover_image")
    val coverImage: String?

)