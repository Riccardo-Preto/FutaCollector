package com.ricca.futacollector.data.api

import retrofit2.http.GET

import com.google.gson.annotations.SerializedName
import com.ricca.futacollector.data.api.ApiCard

interface CardApiService {
    @GET("allSetCards/")
    suspend fun getAllSetCards(): List<ApiCard>

    @GET("allSTCards/")
    suspend fun getAllSTCards(): List<ApiCard>

    @GET("allPromos/")
    suspend fun getAllPromos(): List<ApiCard>
}