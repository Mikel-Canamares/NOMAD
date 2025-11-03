package com.nomad.app.data.repository

import android.util.Log
import com.nomad.app.data.api.RetrofitClient
import com.nomad.app.data.dto.AskRequest
import com.nomad.app.data.dto.AskResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AskRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Envía una pregunta al backend con retry y backoff
     */
    suspend fun ask(
        text: String,
        locale: String = "es",
        poiId: String? = null
    ): Result<AskResponse> = withContext(Dispatchers.IO) {
        try {
            val response = retryWithBackoff(maxRetries = 3) {
                val request = AskRequest(
                    text = text,
                    locale = locale,
                    poiId = poiId
                )
                Log.d("AskRepository", "Sending ask request: text=$text, locale=$locale, poiId=$poiId")
                apiService.ask(request)
            }

            Log.d("AskRepository", "Ask response received: markdown length=${response.markdown.length}, noData=${response.noData}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("AskRepository", "Error calling /ask endpoint: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Retry con backoff exponencial simple
     */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Log.w("AskRepository", "Intento ${attempt + 1} falló, reintentando en ${currentDelay}ms: ${e.message}")
                    delay(currentDelay)
                    currentDelay *= 2 // Backoff exponencial
                }
            }
        }

        throw lastException ?: Exception("Retry failed with unknown error")
    }
}
