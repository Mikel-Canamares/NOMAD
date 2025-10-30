package com.nomad.app.data.repository

import com.nomad.app.data.api.RetrofitClient
import com.nomad.app.data.dto.AskRequest
import com.nomad.app.data.dto.AskResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AskRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Envía una pregunta al backend
     */
    suspend fun ask(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<AskResponse> = withContext(Dispatchers.IO) {
        try {
            val request = AskRequest(
                query = query,
                latitude = latitude,
                longitude = longitude
            )
            val response = apiService.ask(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
