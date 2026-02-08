package com.ricca.futacollector

import retrofit2.http.GET

import com.google.gson.annotations.SerializedName
import retrofit2.http.Query

data class CardSet(
    @SerializedName("set_id")
    val card_id: String = "",

    @SerializedName("set_name")
    val name: String = "",

    val image: String? = null // opzionale, l’API non ce l’ha
)

interface CardApiService {

    @GET("allSets/")
    suspend fun getAllSets(): List<CardSet>

    @GET("sets/filtered/")
    suspend fun getFilteredCards(
        @Query("card_name") cardName: String? = null,
        @Query("set_id") setId: String? = null,
        @Query("set_name") setName: String? = null,
        @Query("color") color: String? = null,
        @Query("rarity") rarity: String? = null
        // puoi aggiungere altri filtri se vuoi
    ): List<Card>
}
