package com.example.data.api

import com.example.data.model.AiConfig
import com.example.data.model.ChatMessage
import com.example.data.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class OpenAiCompatibleProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val providerId: String = "openai_compatible"
    override val displayName: String = "OpenAI Compatible API"

    override suspend fun generateStream(
        messages: List<ChatMessage>,
        config: AiConfig,
        onChunk: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            var endpoint = config.apiUrl.trimEnd('/')
            if (!endpoint.endsWith("/chat/completions")) {
                endpoint = "$endpoint/chat/completions"
            }

            val rootJson = JSONObject()
            rootJson.put("model", config.model)
            rootJson.put("temperature", config.temperature.toDouble())
            rootJson.put("max_tokens", config.maxTokens)
            rootJson.put("stream", config.isStreamingEnabled)

            val messagesArray = JSONArray()

            // System prompt
            if (config.systemPrompt.isNotBlank()) {
                val sysObj = JSONObject()
                sysObj.put("role", "system")
                sysObj.put("content", config.systemPrompt)
                messagesArray.put(sysObj)
            }

            // Chat history
            for (msg in messages) {
                val msgObj = JSONObject()
                msgObj.put("role", when (msg.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                })
                msgObj.put("content", msg.content)
                messagesArray.put(msgObj)
            }
            rootJson.put("messages", messagesArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(body)

            if (config.apiKey.isNotBlank() && config.apiKey != "MY_AI_API_KEY") {
                requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
            }

            val response = client.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                val errorMsg = when (response.code) {
                    401 -> "فشل المصادقة (401): مفتاح API غير صالح أو غير موجود. يرجى التحقق من إعدادات API."
                    403 -> "ممنوع الوصول (403): ليس لديك صلاحية للوصول إلى هذا النموذج."
                    404 -> "العنوان غير موجود (404): تأكد من صحة رابط الـ API URL: $endpoint"
                    429 -> "تم تجاوز حد الطلبات (429): تجاوزت الحصة المسموح بها في حسابك."
                    500, 502, 503 -> "خطأ في خادم AI (${response.code}): الخادم يواجه ضغطاً أو صيانة."
                    else -> "خطأ من مزود AI (${response.code}): $errBody"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body ?: return@withContext Result.failure(Exception("استجابة فارغة من الخادم"))

            val completeResponseBuilder = StringBuilder()

            if (config.isStreamingEnabled) {
                val reader = BufferedReader(InputStreamReader(responseBody.byteStream(), Charsets.UTF_8))
                var line = reader.readLine()

                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        val dataContent = trimmed.substring(5).trim()
                        if (dataContent == "[DONE]") {
                            break
                        }
                        if (dataContent.isNotEmpty()) {
                            try {
                                val chunkJson = JSONObject(dataContent)
                                val choices = chunkJson.optJSONArray("choices")
                                if (choices != null && choices.length() > 0) {
                                    val choice = choices.getJSONObject(0)
                                    val delta = choice.optJSONObject("delta")
                                    val text = delta?.optString("content", "") ?: ""
                                    if (text.isNotEmpty()) {
                                        completeResponseBuilder.append(text)
                                        onChunk(text)
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip unparseable chunks
                            }
                        }
                    }
                    line = reader.readLine()
                }
            } else {
                val rawJson = responseBody.string()
                val json = JSONObject(rawJson)
                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    val content = message.getString("content")
                    completeResponseBuilder.append(content)
                    onChunk(content)
                }
            }

            val fullText = completeResponseBuilder.toString()
            Result.success(fullText)
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال بخدمة AI: ${e.localizedMessage ?: e.message}"))
        }
    }
}
