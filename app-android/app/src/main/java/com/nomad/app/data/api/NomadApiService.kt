package com.nomad.app.data.api

import com.nomad.app.data.dto.AskRequest
import com.nomad.app.data.dto.AskResponse
import com.nomad.app.data.dto.PoiDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NomadApiService {

    @GET("poi/nearby")
    suspend fun getNearbyPois(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double,
        @Query("cat") category: String? = null
    ): List<PoiDto>

    @POST("ask")
    suspend fun ask(
        @Body request: AskRequest
    ): AskResponse
}
