package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.AiConfig
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AiRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_assistant_prefs", Context.MODE_PRIVATE)

    private val _aiConfig = MutableStateFlow(loadConfig())
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()

    private val openAiProvider = OpenAiCompatibleProvider()
    private val simulatedProvider = SimulatedAiProvider()

    private fun loadConfig(): AiConfig {
        // BuildConfig fallback from Secrets plugin / .env
        val defaultUrl = try { BuildConfig.AI_API_URL.ifBlank { "https://api.openai.com/v1" } } catch (e: Throwable) { "https://api.openai.com/v1" }
        val defaultKey = try { BuildConfig.AI_API_KEY } catch (e: Throwable) { "" }
        val defaultModel = try { BuildConfig.AI_MODEL.ifBlank { "gpt-4o-mini" } } catch (e: Throwable) { "gpt-4o-mini" }

        val apiUrl = prefs.getString("api_url", defaultUrl) ?: defaultUrl
        val apiKey = prefs.getString("api_key", defaultKey) ?: defaultKey
        val model = prefs.getString("model", defaultModel) ?: defaultModel
        val systemPrompt = prefs.getString(
            "system_prompt",
            "أنت مساعد ذكاء اصطناعي ذكي ومفيد ومتعاون. أجب بإتقان ووضوح باللغة العربية مع دعم تنسيق Markdown والرموز البرمجية."
        ) ?: ""
        val temperature = prefs.getFloat("temperature", 0.7f)
        val maxTokens = prefs.getInt("max_tokens", 2048)
        val isStreaming = prefs.getBoolean("is_streaming", true)
        val useSimulated = prefs.getBoolean("use_simulated_fallback", true)

        return AiConfig(
            apiUrl = apiUrl,
            apiKey = apiKey,
            model = model,
            systemPrompt = systemPrompt,
            temperature = temperature,
            maxTokens = maxTokens,
            isStreamingEnabled = isStreaming,
            useSimulatedProviderIfNoKey = useSimulated
        )
    }

    fun updateConfig(newConfig: AiConfig) {
        prefs.edit()
            .putString("api_url", newConfig.apiUrl)
            .putString("api_key", newConfig.apiKey)
            .putString("model", newConfig.model)
            .putString("system_prompt", newConfig.systemPrompt)
            .putFloat("temperature", newConfig.temperature)
            .putInt("max_tokens", newConfig.maxTokens)
            .putBoolean("is_streaming", newConfig.isStreamingEnabled)
            .putBoolean("use_simulated_fallback", newConfig.useSimulatedProviderIfNoKey)
            .apply()

        _aiConfig.value = newConfig
    }

    fun getActiveProvider(config: AiConfig = _aiConfig.value): AiProvider {
        val hasCustomKey = config.apiKey.isNotBlank() && config.apiKey != "MY_AI_API_KEY"
        return if (hasCustomKey) {
            openAiProvider
        } else {
            simulatedProvider
        }
    }

    suspend fun generateStream(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit
    ): Result<String> {
        val config = _aiConfig.value
        val provider = getActiveProvider(config)
        return provider.generateStream(messages, config, onChunk)
    }

    suspend fun testConnection(config: AiConfig): Result<String> {
        val provider = if (config.apiKey.isNotBlank() && config.apiKey != "MY_AI_API_KEY") {
            openAiProvider
        } else {
            simulatedProvider
        }
        val testMessage = listOf(
            ChatMessage(
                id = "test_ping",
                conversationId = "test",
                role = com.example.data.model.MessageRole.USER,
                content = "ping"
            )
        )
        return provider.generateStream(testMessage, config) { /* ignore streaming chunk */ }
    }
}
