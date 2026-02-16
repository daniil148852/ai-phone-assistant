package com.aiassistant.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aiassistant.domain.model.AIAction
import com.aiassistant.domain.model.Bounds
import com.aiassistant.domain.model.ScreenState
import com.aiassistant.domain.model.ScrollDirection
import com.aiassistant.domain.model.UIElement
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class AIAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AIAccessibility"
        
        private var instance: AIAccessibilityService? = null
        
        val isRunning: Boolean get() = instance != null
        
        fun getInstance(): AIAccessibilityService? = instance
        
        private val _screenState = MutableStateFlow<ScreenState?>(null)
        val screenState: StateFlow<ScreenState?> = _screenState
        
        private val _actionResults = MutableSharedFlow<ActionResult>()
        val actionResults: SharedFlow<ActionResult> = _actionResults
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var elementsMap = mutableMapOf<String, AccessibilityNodeInfo>()
    
    data class ActionResult(
        val action: AIAction,
        val success: Boolean,
        val message: String? = null
    )
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility Service connected")
        updateScreenState()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Log.d(TAG, "Accessibility Service destroyed")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                updateScreenState()
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
    
    private fun updateScreenState() {
        val rootNode = rootInActiveWindow ?: return
        
        elementsMap.clear()
        val elements = parseNode(rootNode)
        
        val state = ScreenState(
            packageName = rootNode.packageName?.toString() ?: "unknown",
            activityName = null,
            elements = elements
        )
        
        serviceScope.launch {
            _screenState.emit(state)
        }
    }
    
    private fun parseNode(node: AccessibilityNodeInfo, depth: Int = 0): List<UIElement> {
        if (depth > 15) return emptyList() // Prevent infinite recursion
        
        val elements = mutableListOf<UIElement>()
        
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        // Only include visible elements
        if (rect.width() > 0 && rect.height() > 0) {
            val id = generateElementId(node)
            elementsMap[id] = node
            
            val children = mutableListOf<UIElement>()
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    children.addAll(parseNode(child, depth + 1))
                }
            }
            
            val element = UIElement(
                id = id,
                className = node.className?.toString() ?: "",
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
                isClickable = node.isClickable,
                isEditable = node.isEditable,
                isScrollable = node.isScrollable,
                resourceId = node.viewIdResourceName,
                children = children
            )
            
            elements.add(element)
        } else {
            // If node is not visible, still process children
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    elements.addAll(parseNode(child, depth + 1))
                }
            }
        }
        
        return elements
    }
    
    private fun generateElementId(node: AccessibilityNodeInfo): String {
        return node.viewIdResourceName ?: "elem_${node.hashCode()}"
    }
    
    // Execute actions
    suspend fun executeAction(action: AIAction): ActionResult {
        return when (action) {
            is AIAction.Click -> executeClick(action)
            is AIAction.LongClick -> executeLongClick(action)
            is AIAction.TypeText -> executeTypeText(action)
            is AIAction.Scroll -> executeScroll(action)
            is AIAction.OpenApp -> executeOpenApp(action)
            is AIAction.GoBack -> executeBack()
            is AIAction.GoHome -> executeHome()
            is AIAction.OpenRecents -> executeRecents()
            is AIAction.Wait -> executeWait(action)
            is AIAction.Speak -> ActionResult(action, true, action.message)
            is AIAction.TaskComplete -> ActionResult(action, true, "Task completed")
            is AIAction.Error -> ActionResult(action, false, action.message)
        }.also { result ->
            _actionResults.emit(result)
        }
    }
    
    private suspend fun executeClick(action: AIAction.Click): ActionResult {
        return try {
            val (x, y) = if (action.elementId != null) {
                val node = findNodeById(action.elementId)
                if (node != null) {
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    rect.centerX() to rect.centerY()
                } else {
                    return ActionResult(action, false, "Element not found: ${action.elementId}")
                }
            } else if (action.x != null && action.y != null) {
                action.x to action.y
            } else {
                return ActionResult(action, false, "No target specified")
            }
            
            performClick(x.toFloat(), y.toFloat())
            delay(300)
            ActionResult(action, true)
        } catch (e: Exception) {
            ActionResult(action, false, e.message)
        }
    }
    
    private suspend fun executeLongClick(action: AIAction.LongClick): ActionResult {
        return try {
            val (x, y) = if (action.elementId != null) {
                val node = findNodeById(action.elementId)
                if (node != null) {
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    rect.centerX() to rect.centerY()
                } else {
                    return ActionResult(action, false, "Element not found")
                }
            } else if (action.x != null && action.y != null) {
                action.x to action.y
            } else {
                return ActionResult(action, false, "No target specified")
            }
            
            performLongClick(x.toFloat(), y.toFloat())
            delay(500)
            ActionResult(action, true)
        } catch (e: Exception) {
            ActionResult(action, false, e.message)
        }
    }
    
    private suspend fun executeTypeText(action: AIAction.TypeText): ActionResult {
        return try {
            // Find editable field
            val node = if (action.elementId != null) {
                findNodeById(action.elementId)
            } else {
                findEditableNode()
            }
            
            if (node != null) {
                // Focus the field
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                delay(100)
                
                // Clear existing text
                val arguments = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        action.text
                    )
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                delay(200)
                
                ActionResult(action, true)
            } else {
                ActionResult(action, false, "No editable field found")
            }
        } catch (e: Exception) {
            ActionResult(action, false, e.message)
        }
    }
    
    private suspend fun executeScroll(action: AIAction.Scroll): ActionResult {
        return try {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            val (startX, startY, endX, endY) = when (action.direction) {
                ScrollDirection.UP -> listOf(
                    screenWidth / 2f,
                    screenHeight * 0.7f,
                    screenWidth / 2f,
                    screenHeight * 0.3f
                )
                ScrollDirection.DOWN -> listOf(
                    screenWidth / 2f,
                    screenHeight * 0.3f,
                    screenWidth / 2f,
                    screenHeight * 0.7f
                )
                ScrollDirection.LEFT -> listOf(
                    screenWidth * 0.8f,
                    screenHeight / 2f,
                    screenWidth * 0.2f,
                    screenHeight / 2f
                )
                ScrollDirection.RIGHT -> listOf(
                    screenWidth * 0.2f,
                    screenHeight / 2f,
                    screenWidth * 0.8f,
                    screenHeight / 2f
                )
            }
            
            performSwipe(startX, startY, endX, endY)
            delay(500)
            ActionResult(action, true)
        } catch (e: Exception) {
            ActionResult(action, false, e.message)
        }
    }
    
    private fun executeOpenApp(action: AIAction.OpenApp): ActionResult {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(action.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                ActionResult(action, true)
            } else {
                ActionResult(action, false, "App not found: ${action.packageName}")
            }
        } catch (e: Exception) {
            ActionResult(action, false, e.message)
        }
    }
    
    private fun executeBack(): ActionResult {
        return if (performGlobalAction(GLOBAL_ACTION_BACK)) {
            ActionResult(AIAction.GoBack, true)
        } else {
            ActionResult(AIAction.GoBack, false, "Failed to go back")
        }
    }
    
    private fun executeHome(): ActionResult {
        return if (performGlobalAction(GLOBAL_ACTION_HOME)) {
            ActionResult(AIAction.GoHome, true)
        } else {
            ActionResult(AIAction.GoHome, false, "Failed to go home")
        }
    }
    
    private fun executeRecents(): ActionResult {
        return if (performGlobalAction(GLOBAL_ACTION_RECENTS)) {
            ActionResult(AIAction.OpenRecents, true)
        } else {
            ActionResult(AIAction.OpenRecents, false, "Failed to open recents")
        }
    }
    
    private suspend fun executeWait(action: AIAction.Wait): ActionResult {
        delay(action.milliseconds)
        return ActionResult(action, true)
    }
    
    // Helper methods
    private fun findNodeById(id: String): AccessibilityNodeInfo? {
        return elementsMap[id] ?: run {
            // Search in current screen
            val rootNode = rootInActiveWindow ?: return null
            findNodeByIdRecursive(rootNode, id)
        }
    }
    
    private fun findNodeByIdRecursive(node: AccessibilityNodeInfo, targetId: String): AccessibilityNodeInfo? {
        if (node.viewIdResourceName == targetId || generateElementId(node) == targetId) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findNodeByIdRecursive(child, targetId)?.let { return it }
            }
        }
        
        return null
    }
    
    private fun findEditableNode(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findEditableNodeRecursive(rootNode)
    }
    
    private fun findEditableNodeRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocusable) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findEditableNodeRecursive(child)?.let { return it }
            }
        }
        
        return null
    }
    
    private fun performClick(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
    
    private fun performLongClick(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
    
    private fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
}
