package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AiRepository
import com.example.data.db.AppDatabase
import com.example.data.file.FileProcessResult
import com.example.data.file.FileProcessor
import com.example.data.model.AiConfig
import com.example.data.model.AttachmentItem
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageRole
import com.example.data.model.MessageStatus
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val chatRepository = ChatRepository(database)
    val aiRepository = AiRepository(application)
    val fileProcessor = FileProcessor(application)

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _themeMode = MutableStateFlow(
        application.getSharedPreferences("app_settings", Application.MODE_PRIVATE)
            .getString("theme_mode", "system") ?: "system"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    val conversations: StateFlow<List<Conversation>> =
        chatRepository.getConversations(_userProfile.value.id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<AttachmentItem>>(emptyList())
    val pendingAttachments: StateFlow<List<AttachmentItem>> = _pendingAttachments.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeGeneratingMessageId = MutableStateFlow<String?>(null)
    val activeGeneratingMessageId: StateFlow<String?> = _activeGeneratingMessageId.asStateFlow()

    private val _toastNotification = MutableStateFlow<String?>(null)
    val toastNotification: StateFlow<String?> = _toastNotification.asStateFlow()

    val aiConfig: StateFlow<AiConfig> = aiRepository.aiConfig

    private var currentGenerationJob: Job? = null
    private var messagesObservationJob: Job? = null

    init {
        // Automatically select the most recent conversation if available, or create initial
        viewModelScope.launch {
            conversations.collectLatest { list ->
                if (_currentConversation.value == null && list.isNotEmpty()) {
                    selectConversation(list.first())
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun clearToast() {
        _toastNotification.value = null
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        getApplication<Application>()
            .getSharedPreferences("app_settings", Application.MODE_PRIVATE)
            .edit()
            .putString("theme_mode", mode)
            .apply()
    }

    fun selectConversation(conversation: Conversation) {
        _currentConversation.value = conversation
        observeMessagesForConversation(conversation.id)
    }

    fun createNewConversation() {
        _currentConversation.value = null
        _messages.value = emptyList()
        _pendingAttachments.value = emptyList()
        _inputText.value = ""
    }

    private fun observeMessagesForConversation(conversationId: String) {
        messagesObservationJob?.cancel()
        messagesObservationJob = viewModelScope.launch {
            chatRepository.getMessages(conversationId).collectLatest { msgs ->
                // Do not overwrite local generating message state with outdated database snapshots
                val activeGenId = _activeGeneratingMessageId.value
                if (activeGenId != null) {
                    val existing = _messages.value
                    val currentGenMsg = existing.find { it.id == activeGenId }
                    if (currentGenMsg != null) {
                        val merged = msgs.map { if (it.id == activeGenId) currentGenMsg else it }
                        _messages.value = merged
                        return@collectLatest
                    }
                }
                _messages.value = msgs
            }
        }
    }

    fun addAttachmentUri(uri: Uri) {
        viewModelScope.launch {
            when (val result = fileProcessor.processUri(uri)) {
                is FileProcessResult.Success -> {
                    _pendingAttachments.value = _pendingAttachments.value + result.item
                }
                is FileProcessResult.Error -> {
                    _toastNotification.value = result.message
                }
            }
        }
    }

    fun removeAttachment(attachment: AttachmentItem) {
        _pendingAttachments.value = _pendingAttachments.value.filter { it.id != attachment.id }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val attachments = _pendingAttachments.value

        if (text.isBlank() && attachments.isEmpty()) return
        if (_isGenerating.value) return

        val userPrompt = if (text.isBlank() && attachments.isNotEmpty()) {
            "يرجى مراجعة وتحليل المرفقات المرفقة."
        } else text

        viewModelScope.launch {
            // 1. Ensure active conversation exists
            var conversation = _currentConversation.value
            if (conversation == null) {
                val title = if (userPrompt.length > 28) userPrompt.take(28) + "..." else userPrompt
                conversation = chatRepository.createConversation(
                    title = title,
                    userId = _userProfile.value.id
                )
                _currentConversation.value = conversation
                observeMessagesForConversation(conversation.id)
            }

            val conversationId = conversation.id

            // 2. Create User Message
            val userMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = userPrompt,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.COMPLETED,
                attachments = attachments
            )

            // 3. Clear inputs
            _inputText.value = ""
            _pendingAttachments.value = emptyList()

            // Save user message to database
            chatRepository.saveMessage(userMsg)

            // 4. Create Assistant Placeholder Message
            val assistantMsgId = UUID.randomUUID().toString()
            val assistantMsg = ChatMessage(
                id = assistantMsgId,
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.GENERATING,
                attachments = emptyList()
            )

            _messages.value = _messages.value + assistantMsg
            _activeGeneratingMessageId.value = assistantMsgId
            _isGenerating.value = true

            // Save initial assistant message
            chatRepository.saveMessage(assistantMsg)

            // 5. Build prompt with attachment context for AI
            val fullPromptForAi = fileProcessor.buildPromptContextWithAttachments(userPrompt, attachments)

            // Gather conversation context
            val historyForAi = _messages.value.filter {
                it.id != assistantMsgId && it.status == MessageStatus.COMPLETED
            }.map { msg ->
                if (msg.id == userMsg.id) {
                    msg.copy(content = fullPromptForAi)
                } else msg
            }

            executeAiStream(assistantMsgId, conversationId, historyForAi)
        }
    }

    private fun executeAiStream(
        assistantMsgId: String,
        conversationId: String,
        history: List<ChatMessage>
    ) {
        currentGenerationJob?.cancel()
        currentGenerationJob = viewModelScope.launch {
            val responseBuffer = StringBuilder()

            val result = aiRepository.generateStream(history) { chunk ->
                responseBuffer.append(chunk)
                val updatedContent = responseBuffer.toString()
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMsgId) {
                        msg.copy(content = updatedContent, status = MessageStatus.GENERATING)
                    } else msg
                }
            }

            _isGenerating.value = false
            _activeGeneratingMessageId.value = null

            if (result.isSuccess) {
                val finalContent = responseBuffer.toString()
                val completedMsg = ChatMessage(
                    id = assistantMsgId,
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = finalContent,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.COMPLETED
                )
                _messages.value = _messages.value.map {
                    if (it.id == assistantMsgId) completedMsg else it
                }
                chatRepository.updateMessage(completedMsg)
            } else {
                val errorText = result.exceptionOrNull()?.localizedMessage ?: "حدث خطأ غير متوقع أثناء الاتصال بالـ API"
                val errorMsg = ChatMessage(
                    id = assistantMsgId,
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = responseBuffer.toString(),
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.ERROR,
                    errorMessage = errorText
                )
                _messages.value = _messages.value.map {
                    if (it.id == assistantMsgId) errorMsg else it
                }
                chatRepository.updateMessage(errorMsg)
            }
        }
    }

    fun stopGeneration() {
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        val activeId = _activeGeneratingMessageId.value

        if (activeId != null) {
            val currentMsg = _messages.value.find { it.id == activeId }
            if (currentMsg != null) {
                val stoppedMsg = currentMsg.copy(
                    content = if (currentMsg.content.isBlank()) "تم إيقاف التوليد من قِبل المستخدم." else currentMsg.content,
                    status = MessageStatus.COMPLETED
                )
                _messages.value = _messages.value.map {
                    if (it.id == activeId) stoppedMsg else it
                }
                viewModelScope.launch {
                    chatRepository.updateMessage(stoppedMsg)
                }
            }
        }

        _isGenerating.value = false
        _activeGeneratingMessageId.value = null
    }

    fun regenerateMessage(assistantMessage: ChatMessage) {
        if (_isGenerating.value) return
        val currentMsgs = _messages.value
        val index = currentMsgs.indexOfFirst { it.id == assistantMessage.id }
        if (index <= 0) return

        val priorUserMessage = currentMsgs.subList(0, index).lastOrNull { it.role == MessageRole.USER } ?: return

        viewModelScope.launch {
            val resetMsg = assistantMessage.copy(
                content = "",
                status = MessageStatus.GENERATING,
                errorMessage = null
            )
            _messages.value = _messages.value.map {
                if (it.id == assistantMessage.id) resetMsg else it
            }
            _activeGeneratingMessageId.value = assistantMessage.id
            _isGenerating.value = true

            val history = currentMsgs.subList(0, index).filter { it.status == MessageStatus.COMPLETED }
            executeAiStream(assistantMessage.id, assistantMessage.conversationId, history)
        }
    }

    fun retryMessage(message: ChatMessage) {
        regenerateMessage(message)
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            chatRepository.renameConversation(conversationId, newTitle.trim())
            if (_currentConversation.value?.id == conversationId) {
                _currentConversation.value = _currentConversation.value?.copy(title = newTitle.trim())
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
            if (_currentConversation.value?.id == conversationId) {
                val remaining = conversations.value.filter { it.id != conversationId }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first())
                } else {
                    createNewConversation()
                }
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            chatRepository.deleteAllConversations(_userProfile.value.id)
            createNewConversation()
        }
    }

    fun updateAiConfig(newConfig: AiConfig) {
        aiRepository.updateConfig(newConfig)
    }

    suspend fun testConnection(config: AiConfig): Result<String> {
        return aiRepository.testConnection(config)
    }
}
