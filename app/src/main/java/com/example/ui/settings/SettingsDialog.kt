package com.example.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    currentConfig: AiConfig,
    currentTheme: String, // "system", "light", "dark"
    onThemeChange: (String) -> Unit,
    onSaveConfig: (AiConfig) -> Unit,
    onClearAllConversations: () -> Unit,
    onTestConnection: suspend (AiConfig) -> Result<String>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("عام", "المطور و API", "التخزين", "حول")

    // State copies
    var apiUrl by remember { mutableStateOf(currentConfig.apiUrl) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var model by remember { mutableStateOf(currentConfig.model) }
    var systemPrompt by remember { mutableStateOf(currentConfig.systemPrompt) }
    var temperature by remember { mutableFloatStateOf(currentConfig.temperature) }
    var isStreaming by remember { mutableStateOf(currentConfig.isStreamingEnabled) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultDialogText by remember { mutableStateOf<String?>(null) }
    var showConfirmDeleteAllDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "إعدادات التطبيق والمساعد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab contents
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> {
                        // General Tab
                        GeneralSettingsSection(
                            currentTheme = currentTheme,
                            onThemeChange = onThemeChange,
                            currentModel = model,
                            onModelChange = {
                                model = it
                                onSaveConfig(currentConfig.copy(model = it))
                            },
                            isStreaming = isStreaming,
                            onStreamingChange = {
                                isStreaming = it
                                onSaveConfig(currentConfig.copy(isStreamingEnabled = it))
                            },
                            apiKeyStatus = if (apiKey.isNotBlank() && apiKey != "MY_AI_API_KEY") "مفتاح API مخصص نشط" else "الوضع الذكي التجريبي مفعّل"
                        )
                    }
                    1 -> {
                        // Developer & API Tab
                        DeveloperApiSection(
                            apiUrl = apiUrl,
                            onApiUrlChange = { apiUrl = it },
                            apiKey = apiKey,
                            onApiKeyChange = { apiKey = it },
                            isApiKeyVisible = isApiKeyVisible,
                            onToggleApiKeyVisibility = { isApiKeyVisible = !isApiKeyVisible },
                            model = model,
                            onModelChange = { model = it },
                            systemPrompt = systemPrompt,
                            onSystemPromptChange = { systemPrompt = it },
                            temperature = temperature,
                            onTemperatureChange = { temperature = it },
                            isStreaming = isStreaming,
                            onStreamingChange = { isStreaming = it },
                            isTestingConnection = isTestingConnection,
                            onTestConnection = {
                                scope.launch {
                                    isTestingConnection = true
                                    val testConfig = currentConfig.copy(
                                        apiUrl = apiUrl,
                                        apiKey = apiKey,
                                        model = model,
                                        systemPrompt = systemPrompt,
                                        temperature = temperature,
                                        isStreamingEnabled = isStreaming
                                    )
                                    val result = onTestConnection(testConfig)
                                    isTestingConnection = false
                                    if (result.isSuccess) {
                                        testResultDialogText = "تم الاتصال بنجاح! المزود استجاب للطلب بشكل صحيح."
                                    } else {
                                        testResultDialogText = "فشل الاتصال: ${result.exceptionOrNull()?.localizedMessage}"
                                    }
                                }
                            },
                            onSave = {
                                val newConfig = currentConfig.copy(
                                    apiUrl = apiUrl,
                                    apiKey = apiKey,
                                    model = model,
                                    systemPrompt = systemPrompt,
                                    temperature = temperature,
                                    isStreamingEnabled = isStreaming
                                )
                                onSaveConfig(newConfig)
                                Toast.makeText(context, "تم حفظ إعدادات API بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    2 -> {
                        // Storage & Attachments Tab
                        StorageSection(
                            onClearAll = { showConfirmDeleteAllDialog = true }
                        )
                    }
                    3 -> {
                        // About Tab
                        AboutSection()
                    }
                }
            }
        }
    }

    // Dialog for Test Connection result
    if (testResultDialogText != null) {
        AlertDialog(
            onDismissRequest = { testResultDialogText = null },
            title = { Text("نتيجة اختبار الاتصال") },
            text = { Text(testResultDialogText ?: "") },
            confirmButton = {
                TextButton(onClick = { testResultDialogText = null }) {
                    Text("حسناً")
                }
            }
        )
    }

    // Dialog for confirming delete all conversations
    if (showConfirmDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("حذف جميع المحادثات؟") },
            text = { Text("هل أنت متأكد من رغبتك في حذف جميع المحادثات والرسائل؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllConversations()
                        showConfirmDeleteAllDialog = false
                        Toast.makeText(context, "تم حذف جميع المحادثات", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، احذف الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteAllDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun GeneralSettingsSection(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    currentModel: String,
    onModelChange: (String) -> Unit,
    isStreaming: Boolean,
    onStreamingChange: (Boolean) -> Unit,
    apiKeyStatus: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Theme selector
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "مظهر التطبيق (Theme)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentTheme == "system",
                        onClick = { onThemeChange("system") },
                        label = { Text("تلقائي النظام") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = currentTheme == "light",
                        onClick = { onThemeChange("light") },
                        label = { Text("فاتح") },
                        leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = currentTheme == "dark",
                        onClick = { onThemeChange("dark") },
                        label = { Text("داكن") },
                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Active Provider & Model
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نموذج الذكاء الاصطناعي",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = apiKeyStatus,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val models = listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo", "claude-3-5-sonnet", "custom")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (m in models.take(3)) {
                        FilterChip(
                            selected = currentModel == m,
                            onClick = { onModelChange(m) },
                            label = { Text(m, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (m in models.drop(3)) {
                        FilterChip(
                            selected = currentModel == m,
                            onClick = { onModelChange(m) },
                            label = { Text(m, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Streaming toggle
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "البث المباشر للإجابة (Streaming)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "ظهور كلمات الإجابة تدريجياً في الوقت الفعلي أثناء كتابة النموذج",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    )
                }
                Switch(checked = isStreaming, onCheckedChange = onStreamingChange)
            }
        }
    }
}

@Composable
private fun DeveloperApiSection(
    apiUrl: String,
    onApiUrlChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isApiKeyVisible: Boolean,
    onToggleApiKeyVisibility: () -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    systemPrompt: String,
    onSystemPromptChange: (String) -> Unit,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    isStreaming: Boolean,
    onStreamingChange: (Boolean) -> Unit,
    isTestingConnection: Boolean,
    onTestConnection: () -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "بيانات المطور محمية. يمكنك تغيير الرابط والمفتاح والنموذج لأي خدمة متوافقة مع OpenAI دون إعادة بناء التطبيق.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // API URL
        OutlinedTextField(
            value = apiUrl,
            onValueChange = onApiUrlChange,
            label = { Text("API URL (عنوان الخادم)") },
            placeholder = { Text("https://api.openai.com/v1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
        )

        // API Key
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key (مفتاح الواجهة)") },
            placeholder = { Text("sk-...") },
            singleLine = true,
            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onToggleApiKeyVisibility) {
                    Icon(
                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "تبديل الرؤية"
                    )
                }
            }
        )

        // Model
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text("اسم النموذج (Model Name)") },
            placeholder = { Text("gpt-4o-mini") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) }
        )

        // System Prompt
        OutlinedTextField(
            value = systemPrompt,
            onValueChange = onSystemPromptChange,
            label = { Text("التوجيه الأساسي (System Prompt)") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        // Temperature
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("درجة الإبداع (Temperature)", style = MaterialTheme.typography.bodySmall)
                Text(String.format(java.util.Locale.US, "%.2f", temperature), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
            Slider(
                value = temperature,
                onValueChange = onTemperatureChange,
                valueRange = 0.0f..1.5f,
                steps = 14
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onTestConnection,
                enabled = !isTestingConnection,
                modifier = Modifier.weight(1f)
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جاري الاختبار...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختبار الاتصال", fontSize = 12.sp)
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حفظ الإعدادات", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StorageSection(
    onClearAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "سياسة وضوابط المرفقات",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "• الحد الأقصى لحجم الملف الواحد: 10 ميجابايت.\n• الأنواع المدعومة: الصور (JPEG, PNG, WebP)، مستندات PDF، ملفات النصوص (TXT, Markdown)، الأكواد البرمجية (JSON, XML, Kotlin, Python, JS).\n• يتم استخراج نصوص الملفات النصية محلياً ومعالجتها بأمان دون إرسال ملفات ثنائية غير ضرورية.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "إدارة البيانات المحلية (Room Database)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "يتم حفظ جميع محادثاتك ورسائلك بأمان في قاعدة بيانات محلية بجهازك SQLite / Room.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف جميع المحادثات والرسائل")
                }
            }
        }
    }
}

@Composable
private fun AboutSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "AI Assistant",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "الإصدار 1.0.0 - منصة ذكاء اصطناعي متكاملة",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "تطبيق محادثة متطور يدعم إرفاق الصور والمستندات، بث الإجابات لحظياً، حفظ السجل، وتنسيق الأكواد والجداول باحترافية تامة.",
            style = MaterialTheme.typography.bodySmall.copy(
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
