package com.aiassistant.domain.model

import java.util.UUID

// Представление UI элемента на экране
data class UIElement(
    val id: String,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: Bounds,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val resourceId: String?,
    val children: List<UIElement> = emptyList()
)

data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

// Действия, которые может выполнить AI
sealed class AIAction {
    data class Click(val elementId: String? = null, val x: Int? = null, val y: Int? = null) : AIAction()
    data class LongClick(val elementId: String? = null, val x: Int? = null, val y: Int? = null) : AIAction()
    data class TypeText(val text: String, val elementId: String? = null) : AIAction()
    data class Scroll(val direction: ScrollDirection, val elementId: String? = null) : AIAction()
    data class OpenApp(val packageName: String) : AIAction()
    data object GoBack : AIAction()
    data object GoHome : AIAction()
    data object OpenRecents : AIAction()
    data class Wait(val milliseconds: Long) : AIAction()
    data class Speak(val message: String) : AIAction()
    data object TaskComplete : AIAction()
    data class Error(val message: String) : AIAction()
}

enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }

// Состояние экрана
data class ScreenState(
    val packageName: String,
    val activityName: String?,
    val elements: List<UIElement>,
    val timestamp: Long = System.currentTimeMillis()
)

// История команд
data class CommandHistory(
    val id: String = UUID.randomUUID().toString(),
    val userCommand: String,
    val actions: List<String>,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// Настройки приложения
data class AppSettings(
    val groqApiKey: String = "",
    val voiceEnabled: Boolean = true,
    val floatingButtonEnabled: Boolean = true,
    val selectedModel: String = "llama-3.3-70b-versatile"
)
