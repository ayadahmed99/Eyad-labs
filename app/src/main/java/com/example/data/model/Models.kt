package com.example.data.model

data class UserProfile(
    val id: String = "default_user",
    val name: String = "المستخدم",
    val email: String = "user@aiassistant.local"
)

data class AttachmentItem(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val mimeType: String,
    val uriString: String,
    val isImage: Boolean,
    val textContentSnippet: String? = null
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class MessageStatus {
    IDLE,
    GENERATING,
    COMPLETED,
    ERROR
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.COMPLETED,
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null
)

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val userId: String = "default_user",
    val lastMessagePreview: String = ""
)

data class AiConfig(
    val apiUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String = "أنت مساعد ذكاء اصطناعي ذكي ومفيد ومتعاون. أجب بإتقان ووضوح باللغة العربية مع دعم تنسيق Markdown والرموز البرمجية.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val isStreamingEnabled: Boolean = true,
    val useSimulatedProviderIfNoKey: Boolean = true
)
