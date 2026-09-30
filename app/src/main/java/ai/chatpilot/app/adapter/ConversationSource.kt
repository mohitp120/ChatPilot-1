package ai.chatpilot.app.adapter

import android.view.accessibility.AccessibilityNodeInfo

data class MessageBubble(
    val sender: String,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: String? = null
)

data class ConversationContext(
    val appName: String,
    val packageName: String,
    val chatTitle: String?,
    val messages: List<MessageBubble>,
    val composerEditableId: String? = null,
    val canAutoInsert: Boolean = true
) {
    fun toFormattedContext(maxMessages: Int = 8): String {
        return messages.takeLast(maxMessages).joinToString("\n") { msg ->
            val senderLabel = if (msg.isOutgoing) "Me" else (chatTitle ?: "Them")
            "$senderLabel: ${msg.text}"
        }
    }
}

/**
 * Modular adapter contract.
 * Each messaging application presents a unique AccessibilityNodeInfo hierarchy.
 */
interface ConversationSource {
    val supportedPackage: String
    val appDisplayName: String
    
    fun extractConversation(rootNode: AccessibilityNodeInfo, packageName: String): ConversationContext?
}

object AdapterRegistry {
    private val adapters = mapOf(
        "com.whatsapp" to WhatsAppAdapter(),
        "com.whatsapp.w4b" to WhatsAppAdapter(),
        "com.instagram.android" to InstagramAdapter(),
        "org.telegram.messenger" to TelegramAdapter(),
        "com.linkedin.android" to LinkedInAdapter()
    )

    private val genericAdapter = GenericAccessibilityAdapter()

    fun getAdapterForPackage(packageName: String): ConversationSource {
        return adapters[packageName] ?: genericAdapter
    }
}