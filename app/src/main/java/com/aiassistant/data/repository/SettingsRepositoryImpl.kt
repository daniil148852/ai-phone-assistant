package com.aiassistant.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiassistant.domain.model.AppSettings
import com.aiassistant.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    
    companion object {
        private val KEY_API_KEY = stringPreferencesKey("groq_api_key")
        private val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        private val KEY_FLOATING_BUTTON = booleanPreferencesKey("floating_button_enabled")
        private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
    }
    
    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            groqApiKey = prefs[KEY_API_KEY] ?: "",
            voiceEnabled = prefs[KEY_VOICE_ENABLED] ?: true,
            floatingButtonEnabled = prefs[KEY_FLOATING_BUTTON] ?: true,
            selectedModel = prefs[KEY_SELECTED_MODEL] ?: "llama-3.3-70b-versatile"
        )
    }
    
    override suspend fun updateApiKey(apiKey: String) {
        context.dataStore.edit { it[KEY_API_KEY] = apiKey }
    }
    
    override suspend fun updateVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VOICE_ENABLED] = enabled }
    }
    
    override suspend fun updateFloatingButtonEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLOATING_BUTTON] = enabled }
    }
    
    override suspend fun updateSelectedModel(model: String) {
        context.dataStore.edit { it[KEY_SELECTED_MODEL] = model }
    }
}
