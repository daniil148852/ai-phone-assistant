package com.aiassistant.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.domain.model.AIAction
import com.aiassistant.domain.model.AppSettings
import com.aiassistant.domain.model.CommandHistory
import com.aiassistant.domain.model.ScreenState
import com.aiassistant.domain.repository.AIRepository
import com.aiassistant.domain.repository.HistoryRepository
import com.aiassistant.domain.repository.SettingsRepository
import com.aiassistant.service.accessibility.AIAccessibilityService
import com.aiassistant.service.speech.SpeechRecognitionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isProcessing: Boolean = false,
    val currentCommand: String = "",
    val statusMessage: String = "",
    val executionLog: List<String> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val history: List<CommandHistory> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    val speechManager: SpeechRecognitionManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        observeSettings()
        observeHistory()
        observeAccessibilityState()
        observeSpeechRecognition()
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }
    
    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.getHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }
    
    private fun observeAccessibilityState() {
        viewModelScope.launch {
            while (true) {
                _uiState.update { 
                    it.copy(isAccessibilityEnabled = AIAccessibilityService.isRunning) 
                }
                delay(1000)
            }
        }
    }
    
    private fun observeSpeechRecognition() {
        viewModelScope.launch {
            speechManager.recognizedText.filterNotNull().collect { text ->
                speechManager.clearRecognizedText()
                processCommand(text)
            }
        }
    }
    
    fun updateCommand(command: String) {
        _uiState.update { it.copy(currentCommand = command) }
    }
    
    fun processCommand(command: String = _uiState.value.currentCommand) {
        if (command.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isProcessing = true, 
                    statusMessage = "Обработка команды...",
                    executionLog = emptyList()
                ) 
            }
            
            val screenState = AIAccessibilityService.screenState.value
            
            if (screenState == null) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Ошибка: Accessibility Service не активен"
                    )
                }
                return@launch
            }
            
            try {
                val result = aiRepository.processCommand(command, screenState)
                
                result.fold(
                    onSuccess = { actions ->
                        executeActions(actions, command)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "AI processing error", error)
                        addLog("Ошибка AI: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                statusMessage = "Ошибка: ${error.message}"
                            )
                        }
                        saveHistory(command, emptyList(), false)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Processing error", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }
    
    private suspend fun executeActions(actions: List<AIAction>, originalCommand: String) {
        val service = AIAccessibilityService.getInstance()
        
        if (service == null) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    statusMessage = "Accessibility Service не доступен"
                )
            }
            return
        }
        
        val executedActions = mutableListOf<String>()
        var success = true
        
        for (action in actions) {
            addLog("Выполняю: ${formatAction(action)}")
            
            val result = service.executeAction(action)
            executedActions.add("${formatAction(action)}: ${if (result.success) "✓" else "✗"}")
            
            if (!result.success) {
                addLog("Ошибка: ${result.message}")
                success = false
                break
            }
            
            when (action) {
                is AIAction.Speak -> {
                    speechManager.speak(action.message)
                    addLog("Говорю: ${action.message}")
                }
                is AIAction.TaskComplete -> {
                    addLog("✓ Задача выполнена!")
                }
                is AIAction.Error -> {
                    addLog("✗ Ошибка: ${action.message}")
                    success = false
                }
                else -> {
                    // Wait for UI to update
                    delay(500)
                }
            }
        }
        
        saveHistory(originalCommand, executedActions, success)
        
        _uiState.update {
            it.copy(
                isProcessing = false,
                currentCommand = "",
                statusMessage = if (success) "Команда выполнена" else "Команда выполнена с ошибками"
            )
        }
    }
    
    private fun formatAction(action: AIAction): String {
        return when (action) {
            is AIAction.Click -> "Клик ${action.elementId ?: "(${action.x}, ${action.y})"}"
            is AIAction.LongClick -> "Долгое нажатие ${action.elementId ?: "(${action.x}, ${action.y})"}"
            is AIAction.TypeText -> "Ввод текста: \"${action.text}\""
            is AIAction.Scroll -> "Прокрутка ${action.direction}"
            is AIAction.OpenApp -> "Открытие ${action.packageName}"
            is AIAction.GoBack -> "Назад"
            is AIAction.GoHome -> "Домой"
            is AIAction.OpenRecents -> "Недавние"
            is AIAction.Wait -> "Ожидание ${action.milliseconds}мс"
            is AIAction.Speak -> "Озвучивание"
            is AIAction.TaskComplete -> "Завершено"
            is AIAction.Error -> "Ошибка: ${action.message}"
        }
    }
    
    private fun addLog(message: String) {
        _uiState.update { 
            it.copy(executionLog = it.executionLog + message)
        }
    }
    
    private suspend fun saveHistory(command: String, actions: List<String>, success: Boolean) {
        historyRepository.addCommand(
            CommandHistory(
                userCommand = command,
                actions = actions,
                success = success
            )
        )
    }
    
    fun startVoiceInput() {
        speechManager.startListening()
    }
    
    fun stopVoiceInput() {
        speechManager.stopListening()
    }
    
    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateApiKey(apiKey)
        }
    }
    
    fun updateSelectedModel(model: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedModel(model)
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        speechManager.release()
    }
}
