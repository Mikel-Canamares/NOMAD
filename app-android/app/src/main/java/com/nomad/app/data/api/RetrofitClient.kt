package com.nomad.app.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nomad.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit con URL configurable dinámicamente.
 * Usa BuildConfig para URLs por defecto según build type (debug/release).
 */
class RetrofitClient(baseUrl: String = BuildConfig.DEFAULT_BACKEND_URL) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: NomadApiService = retrofit.create(NomadApiService::class.java)
    val voiceChatApiService: VoiceChatApiService = retrofit.create(VoiceChatApiService::class.java)

    companion object {
        // Instancia singleton con URL por defecto
        @Volatile
        private var instance: RetrofitClient? = null

        fun getInstance(baseUrl: String? = null): RetrofitClient {
            return if (baseUrl != null) {
                // Crear nueva instancia con URL personalizada
                RetrofitClient(baseUrl)
            } else {
                // Usar instancia singleton con URL por defecto
                instance ?: synchronized(this) {
                    instance ?: RetrofitClient().also { instance = it }
                }
            }
        }
    }
}
