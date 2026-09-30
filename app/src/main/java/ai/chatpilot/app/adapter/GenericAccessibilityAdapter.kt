package ai.chatpilot.app.adapter

import android.view.accessibility.AccessibilityNodeInfo

class GenericAccessibilityAdapter : ConversationSource {
    override val supportedPackage: String = "generic"
    override val appDisplayName: String = "Chat App"

    override fun extractConversation(rootNode: AccessibilityNodeInfo, packageName: String): ConversationContext? {
        val messages = mutableListOf<MessageBubble>()
        val candidates = mutableListOf<AccessibilityNodeInfo>()

        collectReadableText(rootNode, candidates)

        for (node in candidates) {
            val text = node.text?.toString()?.trim() ?: continue
            // Skip UI labels, status bars, timestamps, zero-length
            if (text.length < 2 || text.length > 500) continue
            if (text.matches(Regex("^[0-9]{1,2}:[0-9]{2}.*$"))) continue

            messages.add(
                MessageBubble(
                    sender = "Contact",
                    text = text,
                    isOutgoing = false
                )
            )
        }

        if (messages.size < 1) {
            return null // Return null to trigger ChatPilot fallback UI
        }

        return ConversationContext(
            appName = appDisplayName,
            packageName = packageName,
            chatTitle = null,
            messages = messages.takeLast(10),
            composerEditableId = null,
            canAutoInsert = true
        )
    }

    private fun collectReadableText(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        // Do not read password fields
        if (node.isPassword) return
        if (!node.text.isNullOrBlank() && !node.isEditable) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            collectReadableText(node.getChild(i), list)
        }
    }
}