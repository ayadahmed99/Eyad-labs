package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AttachmentJsonConverter
import com.example.data.file.FileProcessor
import com.example.data.model.AttachmentItem
import com.example.ui.components.MarkdownElement
import com.example.ui.components.parseMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI Assistant", appName)
    }

    @Test
    fun `test markdown parser headers and code blocks`() {
        val markdown = """
            # Header One
            Here is a paragraph with **bold** text.
            ```kotlin
            fun hello() = "world"
            ```
            - Bullet 1
            - Bullet 2
        """.trimIndent()

        val elements = parseMarkdown(markdown)
        assertTrue(elements.any { it is MarkdownElement.Header && it.text == "Header One" })
        assertTrue(elements.any { it is MarkdownElement.CodeBlock && it.language == "kotlin" })
        assertTrue(elements.any { it is MarkdownElement.BulletItem && it.text == "Bullet 1" })
    }

    @Test
    fun `test markdown parser GFM tables`() {
        val markdown = """
            | اللغة | الإطار | الاستخدام |
            | :--- | :---: | ---: |
            | Kotlin | Jetpack Compose | تطوير أندرويد |
            | Python | FastAPI | الذكاء الاصطناعي |
            | TypeScript | React | واجهات الويب |
        """.trimIndent()

        val elements = parseMarkdown(markdown)
        val table = elements.filterIsInstance<MarkdownElement.Table>().firstOrNull()
        assertNotNull(table)
        assertEquals(3, table?.headers?.size)
        assertEquals("اللغة", table?.headers?.get(0))
        assertEquals(3, table?.rows?.size)
        assertEquals("Kotlin", table?.rows?.get(0)?.get(0))
        assertEquals("FastAPI", table?.rows?.get(1)?.get(1))
    }

    @Test
    fun `test markdown parser ordered and nested lists`() {
        val markdown = """
            1. الخطوة الأولى
            2. الخطوة الثانية
               - تفصيل فرعي 1
               - تفصيل فرعي 2
            3. الخطوة الثالثة
        """.trimIndent()

        val elements = parseMarkdown(markdown)
        val numbered = elements.filterIsInstance<MarkdownElement.NumberedItem>()
        val bullets = elements.filterIsInstance<MarkdownElement.BulletItem>()

        assertEquals(3, numbered.size)
        assertEquals("1", numbered[0].number)
        assertEquals("الخطوة الأولى", numbered[0].text)
        assertTrue(bullets.any { it.text == "تفصيل فرعي 1" })
    }

    @Test
    fun `test markdown parser streaming unclosed code block`() {
        val streamingMarkdown = """
            جاري التفكير وكتابة الكود:
            ```python
            def compute_answer():
                return 42
        """.trimIndent()

        val elements = parseMarkdown(streamingMarkdown)
        assertTrue(elements.any { it is MarkdownElement.CodeBlock && it.language == "python" })
    }

    @Test
    fun `test attachment json converter serialization and deserialization`() {
        val original = listOf(
            AttachmentItem(
                id = "att-1",
                name = "document.pdf",
                sizeBytes = 2048,
                formattedSize = "2.0 KB",
                mimeType = "application/pdf",
                uriString = "content://test/doc",
                isImage = false,
                textContentSnippet = null
            ),
            AttachmentItem(
                id = "att-2",
                name = "photo.png",
                sizeBytes = 1048576,
                formattedSize = "1.0 MB",
                mimeType = "image/png",
                uriString = "content://test/img",
                isImage = true,
                textContentSnippet = null
            )
        )

        val json = AttachmentJsonConverter.toJson(original)
        assertNotNull(json)
        val restored = AttachmentJsonConverter.fromJson(json)
        assertEquals(2, restored.size)
        assertEquals("document.pdf", restored[0].name)
        assertEquals("photo.png", restored[1].name)
        assertTrue(restored[1].isImage)
    }

    @Test
    fun `test file processor format file size`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val processor = FileProcessor(context)

        assertEquals("500 B", processor.formatFileSize(500))
        assertEquals("1.0 KB", processor.formatFileSize(1024))
        assertEquals("2.0 MB", processor.formatFileSize(2 * 1024 * 1024))
    }
}
