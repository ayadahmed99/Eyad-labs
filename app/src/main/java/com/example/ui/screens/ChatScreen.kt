package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageRole
import com.example.ui.components.AccountProfileDialog
import com.example.ui.components.AttachmentOptionsBottomSheet
import com.example.ui.components.CameraCaptureModal
import com.example.ui.components.MessageBubble
import com.example.ui.components.PendingAttachmentsBar
import com.example.ui.components.PrivacyInfoDialog
import com.example.ui.settings.SettingsBottomSheet
import com.example.ui.theme.AiLogoGradient
import com.example.ui.theme.UserMessageGradient
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val conversations by viewModel.conversations.collectAsState()
    val currentConversation by viewModel.currentConversation.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val pendingAttachments by viewModel.pendingAttachments.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val toastNotification by viewModel.toastNotification.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showCameraCapture by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    val listState = rememberLazyListState()

    // Show toast alerts (such as file size limit warning)
    LaunchedEffect(toastNotification) {
        toastNotification?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    // Auto-scroll to bottom when messages update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachmentUri(it) }
    }

    // Document picker launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachmentUri(it) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 2.dp,
                modifier = Modifier.width(320.dp)
            ) {
                DrawerContent(
                    conversations = conversations,
                    currentConversation = currentConversation,
                    onSelectConversation = { conv ->
                        viewModel.selectConversation(conv)
                        scope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        viewModel.createNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    onRenameConversation = { conv ->
                        conversationToRename = conv
                        renameInputText = conv.title
                    },
                    onDeleteConversation = { conv ->
                        conversationToDelete = conv
                    },
                    onShareConversation = { conv ->
                        shareConversationText(context, conv, messages)
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        showSettingsSheet = true
                    },
                    onOpenAccount = {
                        scope.launch { drawerState.close() }
                        showAccountDialog = true
                    },
                    onOpenPrivacy = {
                        scope.launch { drawerState.close() }
                        showPrivacyDialog = true
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AiLogoGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = currentConversation?.title ?: "المساعد الذكي",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "فتح القائمة الجانبية",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.createNewConversation() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "محادثة جديدة",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showSettingsSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                ChatBottomComposer(
                    inputText = inputText,
                    onInputChange = viewModel::onInputTextChanged,
                    pendingAttachments = pendingAttachments,
                    onRemoveAttachment = viewModel::removeAttachment,
                    isGenerating = isGenerating,
                    onSend = viewModel::sendMessage,
                    onStop = viewModel::stopGeneration,
                    onOpenAttachmentOptions = { showAttachmentSheet = true },
                    onPickImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPickDocument = {
                        documentPickerLauncher.launch("*/*")
                    },
                    onMicClick = {
                        Toast.makeText(context, "ميزة التسجيل الصوتي قيد التطوير وستتوفر قريباً", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (messages.isEmpty()) {
                    EmptyChatGreeting(
                        onSuggestionClick = { prompt ->
                            viewModel.onInputTextChanged(prompt)
                        },
                        onAnalyzeImage = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onSummarizeDoc = {
                            documentPickerLauncher.launch("*/*")
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(
                            items = messages,
                            key = { it.id }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isGeneratingThisMessage = isGenerating && message.status == com.example.data.model.MessageStatus.GENERATING,
                                onRegenerate = { viewModel.regenerateMessage(message) },
                                onStopGeneration = { viewModel.stopGeneration() },
                                onRetry = { viewModel.retryMessage(message) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    conversationToRename?.let { conv ->
        AlertDialog(
            onDismissRequest = { conversationToRename = null },
            title = { Text("إعادة تسمية المحادثة") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text("عنوان المحادثة") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInputText.isNotBlank()) {
                            viewModel.renameConversation(conv.id, renameInputText.trim())
                        }
                        conversationToRename = null
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRename = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    conversationToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("حذف المحادثة") },
            text = { Text("هل أنت متأكد من رغبتك في حذف محادثة \"${conv.title}\"؟ لن تتمكن من التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConversation(conv.id)
                        conversationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Attachment Options Bottom Sheet (+ button)
    if (showAttachmentSheet) {
        AttachmentOptionsBottomSheet(
            onDismiss = { showAttachmentSheet = false },
            onPickImage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickDocument = {
                documentPickerLauncher.launch("*/*")
            },
            onCapturePhoto = {
                showCameraCapture = true
            }
        )
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            currentConfig = aiConfig,
            currentTheme = themeMode,
            onThemeChange = viewModel::setThemeMode,
            onSaveConfig = viewModel::updateAiConfig,
            onClearAllConversations = viewModel::clearAllConversations,
            onTestConnection = viewModel::testConnection,
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Camera Capture Modal
    if (showCameraCapture) {
        CameraCaptureModal(
            onDismiss = { showCameraCapture = false },
            onImageCaptured = { uri ->
                viewModel.addAttachmentUri(uri)
                showCameraCapture = false
            }
        )
    }

    // Account Profile Dialog
    if (showAccountDialog) {
        AccountProfileDialog(
            onDismiss = { showAccountDialog = false }
        )
    }

    // Privacy & Security Dialog
    if (showPrivacyDialog) {
        PrivacyInfoDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }
}

@Composable
private fun DrawerContent(
    conversations: List<Conversation>,
    currentConversation: Conversation?,
    onSelectConversation: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onRenameConversation: (Conversation) -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    onShareConversation: (Conversation) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        // App header in drawer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AiLogoGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "المساعد الذكي",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "تطبيق ذكاء اصطناعي فائق التطور",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // New Chat Button
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "محادثة جديدة",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("محادثة جديدة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "المحادثات الأخيرة",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )

        // List of conversations
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(conversations, key = { it.id }) { conv ->
                val isSelected = conv.id == currentConversation?.id
                var showItemMenu by remember { mutableStateOf(false) }

                NavigationDrawerItem(
                    selected = isSelected,
                    onClick = { onSelectConversation(conv) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = conv.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = formatRelativeDateArabic(conv.updatedAt),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    },
                    badge = {
                        Box {
                            IconButton(
                                onClick = { showItemMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "خيارات المحادثة",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showItemMenu,
                                onDismissRequest = { showItemMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("إعادة تسمية") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showItemMenu = false
                                        onRenameConversation(conv)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مشاركة") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showItemMenu = false
                                        onShareConversation(conv)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showItemMenu = false
                                        onDeleteConversation(conv)
                                    }
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Drawer Footer: Settings, Account, App Info
        DrawerFooterItem(
            icon = Icons.Default.Settings,
            label = "الإعدادات ومزود AI",
            onClick = onOpenSettings
        )

        DrawerFooterItem(
            icon = Icons.Default.Person,
            label = "الحساب",
            onClick = onOpenAccount
        )

        DrawerFooterItem(
            icon = Icons.Default.Shield,
            label = "الخصوصية ومعلومات التطبيق",
            onClick = onOpenPrivacy
        )
    }
}

@Composable
private fun DrawerFooterItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun EmptyChatGreeting(
    onSuggestionClick: (String) -> Unit,
    onAnalyzeImage: () -> Unit,
    onSummarizeDoc: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Central Logo Motif
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AiLogoGradient)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "كيف يمكنني مساعدتك؟",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "مساعدك الذكي جاهز للإجابة، تحليل الصور والمستندات، وكتابة الكود والملخصات.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Suggestion Cards
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SuggestionCard(
                icon = Icons.Default.Image,
                title = "حلل صورة",
                subtitle = "التقط أو اختر صورة لمعرفة تفاصيلها واستخراج النصوص",
                onClick = onAnalyzeImage
            )

            SuggestionCard(
                icon = Icons.Default.AttachFile,
                title = "لخص ملف",
                subtitle = "ارفع ملف PDF أو مستند نصي للحصول على استنتاجات سريعة",
                onClick = onSummarizeDoc
            )

            SuggestionCard(
                icon = Icons.Default.Edit,
                title = "اكتب لي كود",
                subtitle = "كتابة دوال برمجية، حل مشاكل وتطوير خوارزميات",
                onClick = { onSuggestionClick("اكتب لي كود برمجي احترافي في Kotlin لتطبيق ") }
            )

            SuggestionCard(
                icon = Icons.Default.AutoAwesome,
                title = "اشرح لي هذا الموضوع",
                subtitle = "شرح مبسط وشامل لأي فكرة علمية أو تقنية",
                onClick = { onSuggestionClick("اشرح لي بالتفصيل وبشكل مبسط مفهوم ") }
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Modern Message Composer adhering to the requested design:
 * - '+' button for attachment options
 * - Image button
 * - Document button
 * - Large flexible input text field
 * - Mic button (voice affordance)
 * - Send / Stop button
 * - Respects IME / keyboard padding so it stays above virtual keyboard.
 */
@Composable
fun ChatBottomComposer(
    inputText: String,
    onInputChange: (String) -> Unit,
    pendingAttachments: List<com.example.data.model.AttachmentItem>,
    onRemoveAttachment: (com.example.data.model.AttachmentItem) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onOpenAttachmentOptions: () -> Unit,
    onPickImage: () -> Unit,
    onPickDocument: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Reusable ImagePreview and pending attachments bar
            PendingAttachmentsBar(
                attachments = pendingAttachments,
                onRemove = onRemoveAttachment
            )

            // Composer Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // '+' Add Attachment Options Button
                IconButton(
                    onClick = onOpenAttachmentOptions,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "خيارات المرفقات",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Quick Image Button
                IconButton(
                    onClick = onPickImage,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "إرفاق صورة من المعرض",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Document Button
                IconButton(
                    onClick = onPickDocument,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "إرفاق ملف أو مستند",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Main Flexible Text Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = "اكتب رسالتك للمساعد الذكي...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() || pendingAttachments.isNotEmpty()) {
                                onSend()
                            }
                        }
                    ),
                    maxLines = 4
                )

                // Microphone button for voice input preparation
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "التسجيل الصوتي",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Send or Stop Generation Button
                if (isGenerating) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "إيقاف التوليد",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    val canSend = inputText.isNotBlank() || pendingAttachments.isNotEmpty()
                    IconButton(
                        onClick = {
                            if (canSend) onSend()
                        },
                        enabled = canSend,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال الرسالة",
                                tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats a timestamp into relative Arabic description (الآن، اليوم، أمس، منذ X أيام).
 */
private fun formatRelativeDateArabic(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val oneMinute = 60 * 1000L
    val oneHour = 60 * oneMinute
    val oneDay = 24 * oneHour

    return when {
        diff < 2 * oneMinute -> "الآن"
        diff < oneHour -> "منذ ${(diff / oneMinute)} دقيقة"
        diff < oneDay -> "اليوم"
        diff < 2 * oneDay -> "أمس"
        diff < 7 * oneDay -> "منذ ${(diff / oneDay)} أيام"
        else -> SimpleDateFormat("d MMM", Locale.forLanguageTag("ar")).format(Date(timestamp))
    }
}

/**
 * Shares the conversation content via standard Android Share intent.
 */
private fun shareConversationText(
    context: Context,
    conversation: Conversation,
    messages: List<ChatMessage>
) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        val shareBody = buildString {
            appendLine("=== محادثة: ${conversation.title} ===")
            appendLine()
            for (msg in messages) {
                val sender = if (msg.role == MessageRole.USER) "المستخدم" else "المساعد الذكي"
                appendLine("[$sender]:")
                appendLine(msg.content)
                appendLine()
            }
        }
        putExtra(Intent.EXTRA_TEXT, shareBody)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "مشاركة المحادثة")
    context.startActivity(shareIntent)
}
