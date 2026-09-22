package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.AttachmentJsonConverter
import com.example.data.db.ConversationEntity
import com.example.data.db.MessageEntity
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageRole
import com.example.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(private val database: AppDatabase) {

    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()

    fun getConversations(userId: String = "default_user"): Flow<List<Conversation>> {
        return conversationDao.getConversationsForUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getMessagesSnapshot(conversationId: String): List<ChatMessage> {
        return messageDao.getMessagesSnapshot(conversationId).map { it.toDomain() }
    }

    suspend fun createConversation(
        title: String = "محادثة جديدة",
        userId: String = "default_user"
    ): Conversation {
        val now = System.currentTimeMillis()
        val conv = Conversation(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = now,
            updatedAt = now,
            userId = userId,
            lastMessagePreview = ""
        )
        conversationDao.insertConversation(conv.toEntity())
        return conv
    }

    suspend fun renameConversation(id: String, newTitle: String) {
        conversationDao.renameConversation(id, newTitle, System.currentTimeMillis())
    }

    suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversation(id)
    }

    suspend fun deleteAllConversations(userId: String = "default_user") {
        conversationDao.deleteAllConversations(userId)
    }

    suspend fun saveMessage(message: ChatMessage) {
        messageDao.insertMessage(message.toEntity())
        // Update conversation's updatedAt and lastMessagePreview
        val preview = message.content.take(60).replace("\n", " ")
        conversationDao.getConversationById(message.conversationId)?.let { conv ->
            val updated = conv.copy(
                updatedAt = System.currentTimeMillis(),
                lastMessagePreview = preview
            )
            conversationDao.updateConversation(updated)
        }
    }

    suspend fun updateMessage(message: ChatMessage) {
        messageDao.updateMessage(message.toEntity())
    }

    suspend fun deleteMessage(id: String) {
        messageDao.deleteMessage(id)
    }

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        userId = userId,
        lastMessagePreview = lastMessagePreview
    )

    private fun Conversation.toEntity() = ConversationEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        userId = userId,
        lastMessagePreview = lastMessagePreview
    )

    private fun MessageEntity.toDomain(): ChatMessage {
        val parsedRole = try { MessageRole.valueOf(role) } catch (e: Exception) { MessageRole.USER }
        val parsedStatus = try { MessageStatus.valueOf(status) } catch (e: Exception) { MessageStatus.COMPLETED }
        val attachmentsList = AttachmentJsonConverter.fromJson(attachmentsJson)
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = parsedRole,
            content = content,
            timestamp = timestamp,
            status = parsedStatus,
            attachments = attachmentsList,
            errorMessage = errorMessage
        )
    }

    private fun ChatMessage.toEntity(): MessageEntity {
        return MessageEntity(
            id = id,
            conversationId = conversationId,
            role = role.name,
            content = content,
            timestamp = timestamp,
            status = status.name,
            attachmentsJson = AttachmentJsonConverter.toJson(attachments),
            errorMessage = errorMessage
        )
    }
}
