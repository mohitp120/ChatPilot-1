package ai.chatpilot.app.adapter

import android.view.accessibility.AccessibilityNodeInfo

class WhatsAppAdapter : ConversationSource {
    override val supportedPackage: String = "com.whatsapp"
    override val appDisplayName: String = "WhatsApp"

    override fun extractConversation(rootNode: AccessibilityNodeInfo, packageName: String): ConversationContext? {
        val messages = mutableListOf<MessageBubble>()
        var chatTitle: String? = null

        // 1. Extract contact or group title
        val titleNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name")
        if (titleNodes.isNotEmpty()) {
            chatTitle = titleNodes[0].text?.toString()
        }

        // 2. Locate conversation bubbles
        // WhatsApp messages usually reside inside ViewGroup with text view or message_text id
        val textNodes = mutableListOf<AccessibilityNodeInfo>()
        collectTextNodes(rootNode, textNodes)

        for (node in textNodes) {
            val text = node.text?.toString()?.trim() ?: continue
            if (text.isEmpty() || text.length > 800) continue
            // Ignore UI buttons and timestamps
            if (text.matches(Regex("^[0-9]{1,2}:[0-9]{2}\\s*(AM|PM|am|pm)?$"))) continue
            if (text == "ONLINE" || text == "TYPING..." || text == "TODAY") continue

            // Determine incoming vs outgoing using node parent or content description
            val contentDesc = node.contentDescription?.toString() ?: ""
            val isOutgoing = contentDesc.contains("You", ignoreCase = true) || 
                             node.viewIdResourceName?.contains("outgoing", ignoreCase = true) == true

            messages.add(
                MessageBubble(
                    sender = if (isOutgoing) "Me" else (chatTitle ?: "Them"),
                    text = text,
                    isOutgoing = isOutgoing
                )
            )
        }

        if (messages.isEmpty()) {
            return null
        }

        return ConversationContext(
            appName = appDisplayName,
            packageName = packageName,
            chatTitle = chatTitle ?: "WhatsApp Chat",
            messages = messages,
            composerEditableId = "com.whatsapp:id/entry",
            canAutoInsert = true
        )
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo?, result: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        val resName = node.viewIdResourceName ?: ""
        if (resName.contains("message_text") || resName.contains("conversation_text")) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            collectTextNodes(node.getChild(i), result)
        }
    }
}