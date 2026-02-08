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
        @Query("card_name") cardName: String
    ): List<ApiCard>

    @GET("sets/{set_id}/")
    suspend fun getCardsBySet(
        @retrofit2.http.Path("set_id") setId: String
    ): List<ApiCard>

}
