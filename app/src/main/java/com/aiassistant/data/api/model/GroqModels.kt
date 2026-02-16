package com.aiassistant.data.api.model

import com.google.gson.annotations.SerializedName

data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.1,
    @SerializedName("max_tokens")
    val maxTokens: Int = 2048,
    @SerializedName("response_format")
    val responseFormat: ResponseFormat? = ResponseFormat("json_object")
)

data class ResponseFormat(
    val type: String
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqResponse(
    val id: String,
    val choices: List<GroqChoice>,
    val usage: GroqUsage?
)

data class GroqChoice(
    val index: Int,
    val message: GroqMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class GroqUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

// Response from AI
data class AIResponse(
    val thinking: String? = null,
    val actions: List<ActionData>
)

data class ActionData(
    val type: String,
    val params: Map<String, Any>? = null
)
