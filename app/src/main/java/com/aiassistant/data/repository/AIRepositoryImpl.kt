package com.aiassistant.data.repository

import android.util.Log
import com.aiassistant.data.api.GroqApi
import com.aiassistant.data.api.GroqPrompts
import com.aiassistant.data.api.model.AIResponse
import com.aiassistant.data.api.model.ActionData
import com.aiassistant.data.api.model.GroqMessage
import com.aiassistant.data.api.model.GroqRequest
import com.aiassistant.domain.model.AIAction
import com.aiassistant.domain.model.Bounds
import com.aiassistant.domain.model.ScreenState
import com.aiassistant.domain.model.ScrollDirection
import com.aiassistant.domain.model.UIElement
import com.aiassistant.domain.repository.AIRepository
import com.aiassistant.domain.repository.SettingsRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepositoryImpl @Inject constructor(
    private val groqApi: GroqApi,
    private val settingsRepository: SettingsRepository,
    private val gson: Gson
) : AIRepository {
    
    companion object {
        private const val TAG = "AIRepository"
    }
    
    override suspend fun processCommand(
        userCommand: String,
        screenState: ScreenState,
        conversationHistory: List<String>
    ): Result<List<AIAction>> = runCatching {
        val settings = settingsRepository.settings.first()
        
        if (settings.groqApiKey.isBlank()) {
            throw IllegalStateException("Groq API key not configured")
        }
        
        val screenStateJson = formatScreenState(screenState)
        
        val messages = buildList {
            add(GroqMessage("system", GroqPrompts.createSystemPrompt()))
            
            // Add conversation history
            conversationHistory.forEach { msg ->
                add(GroqMessage("user", msg))
            }
            
            add(GroqMessage("user", GroqPrompts.createUserPrompt(userCommand, screenStateJson)))
        }
        
        val request = GroqRequest(
            model = settings.selectedModel,
            messages = messages
        )
        
        Log.d(TAG, "Sending request to Groq API...")
        
        val response = groqApi.chat(
            authorization = "Bearer ${settings.groqApiKey}",
            request = request
        )
        
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Empty response from API")
        
        Log.d(TAG, "API Response: $content")
        
        parseActions(content)
    }
    
    private fun formatScreenState(state: ScreenState): String {
        val sb = StringBuilder()
        sb.appendLine("Package: ${state.packageName}")
        state.activityName?.let { sb.appendLine("Activity: $it") }
        sb.appendLine("\nUI Elements:")
        
        state.elements.forEachIndexed { index, element ->
            formatElement(sb, element, index, 0)
        }
        
        return sb.toString()
    }
    
    private fun formatElement(sb: StringBuilder, element: UIElement, index: Int, depth: Int) {
        val indent = "  ".repeat(depth)
        val flags = buildList {
            if (element.isClickable) add("clickable")
            if (element.isEditable) add("editable")
            if (element.isScrollable) add("scrollable")
        }.joinToString(",")
        
        sb.append("$indent[$index] ${element.className.substringAfterLast('.')}")
        
        if (element.resourceId != null) {
            sb.append(" id=\"${element.resourceId}\"")
        }
        if (!element.text.isNullOrBlank()) {
            sb.append(" text=\"${element.text.take(50)}\"")
        }
        if (!element.contentDescription.isNullOrBlank()) {
            sb.append(" desc=\"${element.contentDescription.take(50)}\"")
        }
        if (flags.isNotEmpty()) {
            sb.append(" [$flags]")
        }
        sb.append(" bounds=${element.bounds.left},${element.bounds.top}-${element.bounds.right},${element.bounds.bottom}")
        sb.appendLine()
        
        element.children.forEachIndexed { childIndex, child ->
            formatElement(sb, child, index * 100 + childIndex, depth + 1)
        }
    }
    
    private fun parseActions(json: String): List<AIAction> {
        // Clean JSON if wrapped in markdown
        val cleanJson = json
            .replace("```json", "")
            .replace("```", "")
            .trim()
        
        val response = gson.fromJson(cleanJson, AIResponse::class.java)
        
        return response.actions.mapNotNull { actionData ->
            parseAction(actionData)
        }
    }
    
    private fun parseAction(data: ActionData): AIAction? {
        return when (data.type.lowercase()) {
            "click" -> {
                val elementId = data.params?.get("element_id")?.toString()
                val x = (data.params?.get("x") as? Number)?.toInt()
                val y = (data.params?.get("y") as? Number)?.toInt()
                AIAction.Click(elementId, x, y)
            }
            "long_click" -> {
                val elementId = data.params?.get("element_id")?.toString()
                val x = (data.params?.get("x") as? Number)?.toInt()
                val y = (data.params?.get("y") as? Number)?.toInt()
                AIAction.LongClick(elementId, x, y)
            }
            "type_text" -> {
                val text = data.params?.get("text")?.toString() ?: return null
                val elementId = data.params["element_id"]?.toString()
                AIAction.TypeText(text, elementId)
            }
            "scroll" -> {
                val direction = when (data.params?.get("direction")?.toString()?.lowercase()) {
                    "up" -> ScrollDirection.UP
                    "down" -> ScrollDirection.DOWN
                    "left" -> ScrollDirection.LEFT
                    "right" -> ScrollDirection.RIGHT
                    else -> ScrollDirection.DOWN
                }
                AIAction.Scroll(direction)
            }
            "open_app" -> {
                val packageName = data.params?.get("package")?.toString() ?: return null
                AIAction.OpenApp(packageName)
            }
            "back" -> AIAction.GoBack
            "home" -> AIAction.GoHome
            "recents" -> AIAction.OpenRecents
            "wait" -> {
                val ms = (data.params?.get("ms") as? Number)?.toLong() ?: 1000L
                AIAction.Wait(ms)
            }
            "speak" -> {
                val message = data.params?.get("message")?.toString() ?: return null
                AIAction.Speak(message)
            }
            "complete" -> AIAction.TaskComplete
            "error" -> {
                val message = data.params?.get("message")?.toString() ?: "Unknown error"
                AIAction.Error(message)
            }
            else -> {
                Log.w(TAG, "Unknown action type: ${data.type}")
                null
            }
        }
    }
}
