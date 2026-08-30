package com.ricca.futacollector.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface SnapshotApiService {
    // Retrofit unisce questo path relativo alla BASE_URL definita sotto.
    @GET("Riccardo-Preto/optcg-data/refs/heads/main/snapshot.json")
    suspend fun getSnapshot(): SnapshotResponse
}

object SnapshotRetrofitInstance {

    private const val BASE_URL = "https://raw.githubusercontent.com/"

    val api: SnapshotApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SnapshotApiService::class.java)
    }
}
