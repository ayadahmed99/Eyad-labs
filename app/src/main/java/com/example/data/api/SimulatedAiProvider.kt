package com.example.data.api

import com.example.data.model.AiConfig
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SimulatedAiProvider : AiProvider {

    override val providerId: String = "simulated_ai"
    override val displayName: String = "المساعد الذكي (وضع المحاكاة التجريبي)"

    override suspend fun generateStream(
        messages: List<ChatMessage>,
        config: AiConfig,
        onChunk: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.Default) {
        val lastUserMessage = messages.lastOrNull { it.role == com.example.data.model.MessageRole.USER }
        val prompt = lastUserMessage?.content ?: "مرحباً"
        val attachments = lastUserMessage?.attachments ?: emptyList()

        val responseText = generateSmartResponse(prompt, attachments, config.model)

        val fullOutput = StringBuilder()
        // Split text into tokens / words for realistic streaming
        val tokens = splitIntoStreamingChunks(responseText)

        for (token in tokens) {
            delay(28) // Realistic streaming delay
            fullOutput.append(token)
            onChunk(token)
        }

        Result.success(fullOutput.toString())
    }

    private fun generateSmartResponse(
        prompt: String,
        attachments: List<com.example.data.model.AttachmentItem>,
        model: String
    ): String {
        val lower = prompt.lowercase()

        val attachmentNotes = if (attachments.isNotEmpty()) {
            val sb = StringBuilder()
            sb.append("\n\n### 📎 المرفقات التي تم تحليلها:\n")
            for (att in attachments) {
                sb.append("- **${att.name}** (${att.formattedSize})\n")
                if (att.isImage) {
                    sb.append("  > تم التعرف على الصورة بنجاح وتجهيز أبعادها لمعالجة الرؤية الحاسوبية.\n")
                } else if (!att.textContentSnippet.isNullOrBlank()) {
                    sb.append("  > تم قراءة محتوى الملف النصي ودمجه في سياق المحادثة.\n")
                } else {
                    sb.append("  > تم فحص صيغة الملف بنجاح (${att.mimeType}).\n")
                }
            }
            sb.toString()
        } else ""

        val mainAnswer = when {
            lower.contains("كود") || lower.contains("برمج") || lower.contains("code") || lower.contains("python") || lower.contains("kotlin") -> {
                """
أهلاً بك! إليك مثال تطبيقي نظيف يوضح كيفية بناء وظيفة متكاملة مع معالجة الأخطاء:

```kotlin
// نموذج استدعاء واجهة برمجة التطبيقات مع تدفق البيانات
suspend fun fetchAiStream(prompt: String): Flow<String> = flow {
    println("بدء تدفق البيانات للطلب: ${'$'}prompt")
    val chunks = listOf("مرحباً", " بك", " في", " عالم", " الذكاء", " الاصطناعي!")
    for (chunk in chunks) {
        delay(100)
        emit(chunk)
    }
}
```

#### مميزات هذا الكود:
1. يعتمد على **Kotlin Coroutines Flow** لبث البيانات تدريجياً دون تجميد الواجهة.
2. يدعم إلغاء المعالجة الفوري عند ضغط زر الإيقاف.
3. يحافظ على كفاءة استهلاك الذاكرة.
                """.trimIndent()
            }

            lower.contains("من أنت") || lower.contains("who are you") || lower.contains("عرف نفسك") -> {
                """
أنا **مساعد الذكاء الاصطناعي الذكي** (AI Assistant).

تم تصميم هذا التطبيق لتقديم تجربة محادثة حديثة وسلسة تشبه أرقى منصات الذكاء الاصطناعي:
- **دعم المرفقات المتعددة:** صور، ملفات PDF، مستندات نصية، وكود برمجي.
- **بث الاستجابات (Streaming):** تظهر الكلمات تباعاً وبسلاسة تامة.
- **تنسيق Markdown غني:** عناوين، قوائم، جداول، وأكواد برمجية قابلة للنسخ بنقرة واحدة.
- **بنية مفتوحة للمزودين:** يمكنك ربط أي مفتاح API متوافق مع OpenAI بسهولة تامة من صفحة الإعدادات.
                """.trimIndent()
            }

            lower.contains("جدول") || lower.contains("table") || lower.contains("مقارنة") -> {
                """
إليك جدول منظم يوضح مقارنة بين نماذج الذكاء الاصطناعي المدعومة:

| النموذج | السرعة | الدقة | المهام المناسبة |
| :--- | :---: | :---: | :--- |
| **GPT-4o Mini** | فائقة ⚡ | عالية جداً | الاستخدام اليومي، المساعدة السريعة |
| **GPT-4o** | متوسطة | الأفضل 🎯 | التحليل العميق والبرمجة المتقدمة |
| **Claude 3.5 Sonnet** | سريعة | متفوقة 🧠 | الكتابة الإبداعية والتحليل الرياضي |

يمكنك تغيير النموذج في أي وقت من قائمة **الإعدادات** في الزاوية العلوية.
                """.trimIndent()
            }

            lower.contains("مرحبا") || lower.contains("أهلا") || lower.contains("سلام") || lower.contains("hello") || lower.contains("hi") -> {
                """
مرحباً بك! يسعدني جداً مساعدتك اليوم. 

كيف يمكنني دعمك في مشروعك أو الإجابة على استفسارك؟
- يمكنك كتابة أي سؤال أو مسألة.
- يمكنك إرفاق مستند أو صورة لتحليلها فوراً.
- يمكنك طلب كتابة أو مراجعة كود برمجي.
                """.trimIndent()
            }

            else -> {
                """
لقد تلقيت رسالتك بعناية:
> "${prompt.take(120)}${if (prompt.length > 120) "..." else ""}"

بصفتي مساعد الذكاء الاصطناعي المدمج، قمت بمعالجة طلبك بنجاح عبر نموذج **$model**.

- **الحالة:** تم الاتصال بالطبقة البرمجية واستلام الاستجابة في الوقت الفعلي.
- **التنسيق:** يدعم نصوص Markdown العريضة، المائلة، والقوائم النقطية والرقمية.
- **التحكم:** يمكنك نسخ هذه الإجابة، أو إعادة توليدها في أي وقت من خلال أزرار الإجراءات في الأسفل.
                """.trimIndent()
            }
        }

        return mainAnswer + attachmentNotes
    }

    private fun splitIntoStreamingChunks(text: String): List<String> {
        val list = mutableListOf<String>()
        val length = text.length
        var i = 0
        while (i < length) {
            val chunkSize = when {
                i + 4 <= length && text[i].isWhitespace() -> 2
                i + 3 <= length -> (2..4).random()
                else -> length - i
            }
            list.add(text.substring(i, (i + chunkSize).coerceAtMost(length)))
            i += chunkSize
        }
        return list
    }
}
