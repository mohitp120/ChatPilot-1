package ai.chatpilot.app.adapter

import android.view.accessibility.AccessibilityNodeInfo

class InstagramAdapter : ConversationSource {
    override val supportedPackage: String = "com.instagram.android"
    override val appDisplayName: String = "Instagram"

    override fun extractConversation(rootNode: AccessibilityNodeInfo, packageName: String): ConversationContext? {
        val messages = mutableListOf<MessageBubble>()
        var chatTitle: String? = null

        // 1. Thread title
        val titleNodes = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/action_bar_title")
        if (titleNodes.isNotEmpty()) {
            chatTitle = titleNodes[0].text?.toString()
        }

        // 2. Direct message items
        val messageNodes = mutableListOf<AccessibilityNodeInfo>()
        traverseForMessages(rootNode, messageNodes)

        for (node in messageNodes) {
            val text = node.text?.toString()?.trim() ?: continue
            if (text.isEmpty() || text.length > 600) continue
            if (text.startsWith("Seen") || text == "Active now") continue

            val isOutgoing = node.viewIdResourceName?.contains("outgoing") == true ||
                             node.viewIdResourceName?.contains("direct_row_message_right") == true

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
            chatTitle = chatTitle ?: "Instagram Direct",
            messages = messages,
            composerEditableId = "com.instagram.android:id/row_thread_composer_edittext",
            canAutoInsert = true
        )
    }

    private fun traverseForMessages(node: AccessibilityNodeInfo?, result: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        val res = node.viewIdResourceName ?: ""
        if (res.contains("message_text") || res.contains("direct_text_message")) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            traverseForMessages(node.getChild(i), result)
        }
    }
}