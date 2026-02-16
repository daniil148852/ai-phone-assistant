package com.aiassistant.domain.repository

import com.aiassistant.domain.model.CommandHistory
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistory(): Flow<List<CommandHistory>>
    suspend fun addCommand(history: CommandHistory)
    suspend fun clearHistory()
}
