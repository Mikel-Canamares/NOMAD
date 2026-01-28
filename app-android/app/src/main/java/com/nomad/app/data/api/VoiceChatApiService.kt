package com.nomad.app.data.api

import com.nomad.app.data.dto.VoiceChatRequest
import com.nomad.app.data.dto.VoiceChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface VoiceChatApiService {

    @POST("voice/chat")
    suspend fun chat(@Body request: VoiceChatRequest): VoiceChatResponse
}
