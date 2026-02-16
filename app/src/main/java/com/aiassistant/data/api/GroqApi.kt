package com.aiassistant.data.api

import com.aiassistant.data.api.model.GroqRequest
import com.aiassistant.data.api.model.GroqResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): GroqResponse
}
