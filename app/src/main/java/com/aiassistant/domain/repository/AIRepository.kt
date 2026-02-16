package com.aiassistant.domain.repository

import com.aiassistant.domain.model.AIAction
import com.aiassistant.domain.model.ScreenState
import kotlinx.coroutines.flow.Flow

interface AIRepository {
    suspend fun processCommand(
        userCommand: String,
        screenState: ScreenState,
        conversationHistory: List<String> = emptyList()
    ): Result<List<AIAction>>
}
