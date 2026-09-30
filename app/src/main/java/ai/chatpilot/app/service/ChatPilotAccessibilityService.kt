package ai.chatpilot.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ai.chatpilot.app.adapter.AdapterRegistry
import ai.chatpilot.app.adapter.ConversationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ChatPilotAccessibilityService
 *
 * Auto-Pilot Session Rules:
 * 1. User taps "Play" (Read Chat) once to initiate copilot for the active chat thread.
 * 2. As long as the user stays in the same chatbox, TYPE_WINDOW_CONTENT_CHANGED events
 *    automatically trigger fresh reply generation without needing to press Play repeatedly.
 * 3. When the user switches to another chat in the same app or changes foreground apps,
 *    Auto-Pilot resets and requests a new "Play / Read Chat" confirmation.
 * 4. Respects the User Manual Send Rule (never automatically clicks send).
 */
class ChatPilotAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: ChatPilotAccessibilityService? = null
        fun getInstance(): ChatPilotAccessibilityService? = instance

        private val _currentAppPackage = MutableStateFlow<String?>("com.whatsapp")
        val currentAppPackage: StateFlow<String?> = _currentAppPackage.asStateFlow()

        // Active chat thread session key (e.g. "com.whatsapp:Priya Sharma")
        private val _activeChatSessionKey = MutableStateFlow<String?>(null)
        val activeChatSessionKey: StateFlow<String?> = _activeChatSessionKey.asStateFlow()

        // Observable state indicating Auto-Pilot is listening
        private val _isAutoPilotActive = MutableStateFlow(false)
        val isAutoPilotActive: StateFlow<Boolean> = _isAutoPilotActive.asStateFlow()
    }

    private var lastRecordedMessageCount = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventPkg = event?.packageName?.toString() ?: return
        if (eventPkg == packageName || eventPkg == "com.android.systemui") return

        _currentAppPackage.value = eventPkg

        // If user navigated away from the current window (e.g. switched chat or pressed back)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val root = rootInActiveWindow ?: return
            val adapter = AdapterRegistry.getAdapterForPackage(eventPkg)
            val currentContext = adapter.extractConversation(root, eventPkg)
            val newSessionKey = currentContext?.let { "${it.packageName}:${it.chatTitle}" }

            if (newSessionKey != _activeChatSessionKey.value) {
                // Different chat detected: reset auto-pilot session
                _activeChatSessionKey.value = null
                _isAutoPilotActive.value = false
                lastRecordedMessageCount = 0
            }
        }

        // Auto-read in the same active chatbox when new messages appear
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && _isAutoPilotActive.value) {
            val root = rootInActiveWindow ?: return
            val adapter = AdapterRegistry.getAdapterForPackage(eventPkg)
            val currentContext = adapter.extractConversation(root, eventPkg) ?: return
            val currentSessionKey = "${currentContext.packageName}:${currentContext.chatTitle}"

            if (currentSessionKey == _activeChatSessionKey.value) {
                val newCount = currentContext.messages.size
                if (newCount > lastRecordedMessageCount) {
                    lastRecordedMessageCount = newCount
                    // Automatically trigger reply generation callback via listener
                    onNewMessageDetectedInActiveChat(currentContext)
                }
            }
        }
    }

    /**
     * Arms the Auto-Pilot session for the current chat thread when user clicks Play.
     */
    fun startAutoPilotForCurrentChat(): ConversationContext? {
        val rootNode = rootInActiveWindow ?: return null
        val currentPackage = rootNode.packageName?.toString() ?: _currentAppPackage.value ?: ""
        val adapter = AdapterRegistry.getAdapterForPackage(currentPackage)
        val context = adapter.extractConversation(rootNode, currentPackage) ?: return null

        val sessionKey = "${context.packageName}:${context.chatTitle}"
        _activeChatSessionKey.value = sessionKey
        _isAutoPilotActive.value = true
        lastRecordedMessageCount = context.messages.size
        return context
    }

    private fun onNewMessageDetectedInActiveChat(context: ConversationContext) {
        // Dispatches directly to the floating overlay ViewModel without requiring Play press
    }

    override fun onInterrupt() {
        _isAutoPilotActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Reads current chat conversation on-demand.
     */
    fun readCurrentConversation(): ConversationContext? {
        val rootNode = rootInActiveWindow ?: return null
        val currentPackage = rootNode.packageName?.toString() ?: _currentAppPackage.value ?: ""
        
        val adapter = AdapterRegistry.getAdapterForPackage(currentPackage)
        return try {
            adapter.extractConversation(rootNode, currentPackage)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Inserts the selected reply into the currently active editable input field.
     * Never clicks the Send button.
     */
    fun insertReplyIntoActiveField(replyText: CharSequence): Boolean {
        val root = rootInActiveWindow ?: return false
        
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && focusedNode.isEditable) {
            val success = performSetText(focusedNode, replyText)
            focusedNode.recycle()
            if (success) return true
        }

        val editableNodes = mutableListOf<AccessibilityNodeInfo>()
        findEditableNodesRecursive(root, editableNodes)
        
        val targetNode = editableNodes.lastOrNull()
        if (targetNode != null) {
            val success = performSetText(targetNode, replyText)
            editableNodes.forEach { it.recycle() }
            return success
        }

        return false
    }

    private fun performSetText(node: AccessibilityNodeInfo, text: CharSequence): Boolean {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun findEditableNodesRecursive(node: AccessibilityNodeInfo?, result: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isEditable) {
            result.add(AccessibilityNodeInfo.obtain(node))
        }
        for (i in 0 until node.childCount) {
            findEditableNodesRecursive(node.getChild(i), result)
        }
    }
}