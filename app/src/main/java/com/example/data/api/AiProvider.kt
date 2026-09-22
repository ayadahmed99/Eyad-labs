package com.example.data.api

import com.example.data.model.AiConfig
import com.example.data.model.ChatMessage

interface AiProvider {
    val providerId: String
    val displayName: String

    suspend fun generateStream(
        messages: List<ChatMessage>,
        config: AiConfig,
        onChunk: (String) -> Unit
    ): Result<String>
}
