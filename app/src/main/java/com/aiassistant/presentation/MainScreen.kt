package com.aiassistant.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiassistant.presentation.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isListening by viewModel.speechManager.isListening.collectAsStateWithLifecycle()
    
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Phone Assistant") },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, "История")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            StatusCard(
                isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                onRequestAccessibility = onRequestAccessibility,
                onRequestOverlay = onRequestOverlay
            )
            
            // Command Input
            CommandInputSection(
                command = uiState.currentCommand,
                onCommandChange = viewModel::updateCommand,
                onSend = { viewModel.processCommand() },
                onVoiceInput = {
                    if (isListening) {
                        viewModel.stopVoiceInput()
                    } else {
                        viewModel.startVoiceInput()
                    }
                },
                isListening = isListening,
                isProcessing = uiState.isProcessing,
                isEnabled = uiState.isAccessibilityEnabled && uiState.settings.groqApiKey.isNotBlank()
            )
            
            // Status Message
            if (uiState.statusMessage.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.statusMessage.contains("Ошибка"))
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = uiState.statusMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Execution Log
            if (uiState.executionLog.isNotEmpty()) {
                ExecutionLogSection(logs = uiState.executionLog)
            }
            
            // Quick Commands
            if (!uiState.isProcessing) {
                QuickCommandsSection(
                    onCommandSelected = { viewModel.processCommand(it) },
                    isEnabled = uiState.isAccessibilityEnabled && uiState.settings.groqApiKey.isNotBlank()
                )
            }
        }
    }
    
    // Settings Dialog
    if (showSettings) {
        SettingsDialog(
            settings = uiState.settings,
            onDismiss = { showSettings = false },
            onApiKeyChange = viewModel::updateApiKey,
            onModelChange = viewModel::updateSelectedModel
        )
    }
    
    // History Dialog
    if (showHistory) {
        HistoryDialog(
            history = uiState.history,
            onDismiss = { showHistory = false },
            onClear = viewModel::clearHistory,
            onSelect = { command ->
                viewModel.updateCommand(command)
                showHistory = false
            }
        )
    }
}

@Composable
fun StatusCard(
    isAccessibilityEnabled: Boolean,
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAccessibilityEnabled)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isAccessibilityEnabled) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = if (isAccessibilityEnabled) "Сервис активен" else "Требуется настройка",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (!isAccessibilityEnabled) {
                Text(
                    text = "Для работы приложения необходимо включить Accessibility Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onRequestAccessibility) {
                        Text("Accessibility")
                    }
                    OutlinedButton(onClick = onRequestOverlay) {
                        Text("Overlay")
                    }
                }
            }
        }
    }
}

@Composable
fun CommandInputSection(
    command: String,
    onCommandChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
    isListening: Boolean,
    isProcessing: Boolean,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Введите команду",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = command,
                onValueChange = onCommandChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Например: Открой WhatsApp и напиши маме 'Привет'") },
                enabled = isEnabled && !isProcessing,
                maxLines = 3,
                trailingIcon = {
                    IconButton(
                        onClick = onVoiceInput,
                        enabled = isEnabled && !isProcessing
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Голосовой ввод",
                            tint = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
            
            Button(
                onClick = onSend,
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled && command.isNotBlank() && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Выполняю...")
                } else {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Выполнить")
                }
            }
        }
    }
}

@Composable
fun ExecutionLogSection(logs: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Лог выполнения",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                logs.forEach { log ->
                    Text(
                        text = "• $log",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            log.contains("✓") -> Color(0xFF4CAF50)
                            log.contains("✗") || log.contains("Ошибка") -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickCommandsSection(
    onCommandSelected: (String) -> Unit,
    isEnabled: Boolean
) {
    val quickCommands = listOf(
        "Открой настройки",
        "Включи фонарик",
        "Открой камеру",
        "Открой YouTube",
        "Позвони маме"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Быстрые команды",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickCommands.forEach { command ->
                    SuggestionChip(
                        onClick = { onCommandSelected(command) },
                        label = { Text(command) },
                        enabled = isEnabled
                    )
                }
            }
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simple flow row implementation
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: com.aiassistant.domain.model.AppSettings,
    onDismiss: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(settings.groqApiKey) }
    var selectedModel by remember { mutableStateOf(settings.selectedModel) }
    
    val models = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768",
        "gemma2-9b-it"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Groq API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text("Модель AI", style = MaterialTheme.typography.labelMedium)
                
                Column {
                    models.forEach { model ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = model == selectedModel,
                                onClick = { selectedModel = model }
                            )
                            Text(
                                text = model,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApiKeyChange(apiKey)
                    onModelChange(selectedModel)
                    onDismiss()
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun HistoryDialog(
    history: List<com.aiassistant.domain.model.CommandHistory>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("История команд")
                if (history.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, "Очистить")
                    }
                }
            }
        },
        text = {
            if (history.isEmpty()) {
                Text("История пуста")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { item ->
                        Card(
                            onClick = { onSelect(item.userCommand) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (item.success) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = item.userCommand,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
