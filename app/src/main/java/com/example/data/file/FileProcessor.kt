package com.example.data.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.AttachmentItem
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

sealed class FileProcessResult {
    data class Success(val item: AttachmentItem) : FileProcessResult()
    data class Error(val message: String) : FileProcessResult()
}

class FileProcessor(private val context: Context) {

    companion object {
        const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB
        const val MAX_TEXT_EXTRACT_BYTES = 50 * 1024 // 50 KB text preview
    }

    fun processUri(uri: Uri): FileProcessResult {
        val contentResolver = context.contentResolver
        var fileName = "file_${System.currentTimeMillis()}"
        var fileSize = 0L

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val mimeType = contentResolver.getType(uri) ?: guessMimeType(fileName)
        val formattedSize = formatFileSize(fileSize)

        // Validate file size limit
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            return FileProcessResult.Error(
                "الملف '$fileName' كبير جداً ($formattedSize). الحد الأقصى المسموح به هو 10 ميجابايت."
            )
        }

        val isImage = mimeType.startsWith("image/")

        var textSnippet: String? = null
        if (!isImage && isTextExtractable(mimeType, fileName)) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                    val sb = StringBuilder()
                    var line = reader.readLine()
                    var totalChars = 0
                    while (line != null && totalChars < MAX_TEXT_EXTRACT_BYTES) {
                        sb.append(line).append("\n")
                        totalChars += line.length
                        line = reader.readLine()
                    }
                    textSnippet = sb.toString().trim()
                }
            } catch (e: Exception) {
                textSnippet = "[تعذر استخراج النص من الملف بالكامل: ${e.localizedMessage}]"
            }
        }

        val item = AttachmentItem(
            id = UUID.randomUUID().toString(),
            name = fileName,
            sizeBytes = fileSize,
            formattedSize = formattedSize,
            mimeType = mimeType,
            uriString = uri.toString(),
            isImage = isImage,
            textContentSnippet = textSnippet
        )

        return FileProcessResult.Success(item)
    }

    private fun isTextExtractable(mimeType: String, fileName: String): Boolean {
        if (mimeType.startsWith("text/")) return true
        if (mimeType.contains("json") || mimeType.contains("xml") || mimeType.contains("csv")) return true
        val lowerName = fileName.lowercase()
        return lowerName.endsWith(".txt") || lowerName.endsWith(".md") ||
                lowerName.endsWith(".json") || lowerName.endsWith(".csv") ||
                lowerName.endsWith(".xml") || lowerName.endsWith(".kt") ||
                lowerName.endsWith(".java") || lowerName.endsWith(".py") ||
                lowerName.endsWith(".js") || lowerName.endsWith(".ts") ||
                lowerName.endsWith(".html") || lowerName.endsWith(".css") ||
                lowerName.endsWith(".log") || lowerName.endsWith(".yaml") ||
                lowerName.endsWith(".yml")
    }

    private fun guessMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".md") -> "text/markdown"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".doc") -> "application/msword"
            lower.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 بايت"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun buildPromptContextWithAttachments(userPrompt: String, attachments: List<AttachmentItem>): String {
        if (attachments.isEmpty()) return userPrompt

        val sb = StringBuilder()
        sb.append(userPrompt.trim()).append("\n\n")
        sb.append("--- [المرفقات التابعة للرسالة] ---\n")

        for (attachment in attachments) {
            sb.append("• مرفق: ").append(attachment.name)
                .append(" (الحجم: ").append(attachment.formattedSize)
                .append(", النوع: ").append(attachment.mimeType).append(")\n")

            if (attachment.isImage) {
                sb.append("  [مرفق صورة: تم إرفاق صورة للعرض والتحليل]\n")
            } else if (!attachment.textContentSnippet.isNullOrBlank()) {
                sb.append("  [محتوى الملف المستخرج]:\n```\n")
                    .append(attachment.textContentSnippet)
                    .append("\n```\n")
            } else {
                sb.append("  [مستند ثنائي/بي دي إف جاهز للمعالجة السياقية]\n")
            }
        }
        sb.append("----------------------------------")
        return sb.toString()
    }
}
