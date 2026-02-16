package com.aiassistant.domain.repository

import com.aiassistant.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateApiKey(apiKey: String)
    suspend fun updateVoiceEnabled(enabled: Boolean)
    suspend fun updateFloatingButtonEnabled(enabled: Boolean)
    suspend fun updateSelectedModel(model: String)
}
