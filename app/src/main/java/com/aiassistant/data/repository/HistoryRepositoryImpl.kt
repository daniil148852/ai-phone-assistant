package com.aiassistant.data.repository

import com.aiassistant.data.local.HistoryDao
import com.aiassistant.data.local.toDomain
import com.aiassistant.data.local.toEntity
import com.aiassistant.domain.model.CommandHistory
import com.aiassistant.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {
    
    override fun getHistory(): Flow<List<CommandHistory>> {
        return historyDao.getHistory().map { entities ->
            entities.map { entity ->
                entity.toDomain(entity.actions.split("|||").filter { it.isNotBlank() })
            }
        }
    }
    
    override suspend fun addCommand(history: CommandHistory) {
        historyDao.insert(history.toEntity())
    }
    
    override suspend fun clearHistory() {
        historyDao.clearAll()
    }
}
