package com.example.ui.components

import android.Manifest
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraCaptureHelper

/**
 * CameraX photo capture modal interface for messaging.
 * Handles permissions, camera preview, lens switching, flash mode,
 * and image acquisition directly into the chat conversation.
 */
@Composable
fun CameraCaptureModal(
    onDismiss: () -> Unit,
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraHelper = remember { CameraCaptureHelper(context) }
    var hasPermission by remember { mutableStateOf(cameraHelper.isPermissionGranted()) }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var activeImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraBindingError by remember { mutableStateOf<String?>(null) }

    // Fallback system camera capture launcher
    var fallbackTempUri by remember { mutableStateOf<Uri?>(null) }
    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && fallbackTempUri != null) {
            onImageCaptured(fallbackTempUri!!)
            onDismiss()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "إذن الكاميرا مطلوب لالتقاط الصور وإرفاقها في المحادثة",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(CameraCaptureHelper.CAMERA_PERMISSION)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraHelper.unbind()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (!hasPermission) {
                // Permission Request / Rationale Screen
                CameraPermissionRationaleContent(
                    onRequestPermission = {
                        permissionLauncher.launch(CameraCaptureHelper.CAMERA_PERMISSION)
                    },
                    onDismiss = onDismiss
                )
            } else {
                // CameraX Active Viewfinder & Capture Interface
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Camera Preview Surface
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                previewViewRef = this
                            }
                        },
                        update = { previewView ->
                            cameraHelper.bindCamera(
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                lensFacing = lensFacing,
                                flashMode = flashMode,
                                onBound = { _, imageCapture ->
                                    activeImageCapture = imageCapture
                                    cameraBindingError = null
                                },
                                onError = { error ->
                                    cameraBindingError = error.localizedMessage
                                }
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Error fallback banner if camera cannot be initialized (e.g. emulator without camera)
                    if (cameraBindingError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "تعذر فتح معاينة الكاميرا المباشرة",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "يمكنك التقاط صورة عبر تطبيق الكاميرا الأساسي للجهاز:",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val (_, uri) = CameraCaptureHelper.prepareTempImageUri(context)
                                        fallbackTempUri = uri
                                        systemCameraLauncher.launch(uri)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("فتح كاميرا النظام")
                                }
                            }
                        }
                    }

                    // 2. Top Controls Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق الكاميرا",
                                tint = Color.White
                            )
                        }

                        // Title indicator
                        Text(
                            text = "التقاط صورة للرسالة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        // Flash Toggle Button
                        IconButton(
                            onClick = {
                                val nextMode = CameraCaptureHelper.cycleFlashMode(flashMode)
                                flashMode = nextMode
                                previewViewRef?.let { pView ->
                                    cameraHelper.bindCamera(
                                        lifecycleOwner = lifecycleOwner,
                                        previewView = pView,
                                        lensFacing = lensFacing,
                                        flashMode = flashMode,
                                        onBound = { _, imgCap -> activeImageCapture = imgCap },
                                        onError = { cameraBindingError = it.localizedMessage }
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            val flashIcon = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                                else -> Icons.Default.FlashAuto
                            }
                            Icon(
                                imageVector = flashIcon,
                                contentDescription = "تبديل الفلاش",
                                tint = if (flashMode == ImageCapture.FLASH_MODE_ON) Color(0xFFFFD54F) else Color.White
                            )
                        }
                    }

                    // 3. Bottom Controls Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Spacer or Gallery Fallback button
                        Box(modifier = Modifier.size(52.dp)) {
                            IconButton(
                                onClick = {
                                    val (_, uri) = CameraCaptureHelper.prepareTempImageUri(context)
                                    fallbackTempUri = uri
                                    systemCameraLauncher.launch(uri)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "استخدام كاميرا النظام",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .border(3.dp, Color.White, CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .background(if (isCapturing) Color.Gray else Color.White)
                                .clickable(enabled = !isCapturing && activeImageCapture != null) {
                                    val imageCapture = activeImageCapture
                                    if (imageCapture != null) {
                                        isCapturing = true
                                        cameraHelper.takePhoto(
                                            imageCapture = imageCapture,
                                            onPhotoCaptured = { uri ->
                                                isCapturing = false
                                                onImageCaptured(uri)
                                                onDismiss()
                                            },
                                            onError = { exc: ImageCaptureException ->
                                                isCapturing = false
                                                Toast.makeText(
                                                    context,
                                                    "فشل التقاط الصورة: ${exc.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        // Lens Switch (Front / Back) Button
                        IconButton(
                            onClick = {
                                lensFacing = CameraCaptureHelper.toggleLensFacing(lensFacing)
                                previewViewRef?.let { pView ->
                                    cameraHelper.bindCamera(
                                        lifecycleOwner = lifecycleOwner,
                                        previewView = pView,
                                        lensFacing = lensFacing,
                                        flashMode = flashMode,
                                        onBound = { _, imgCap -> activeImageCapture = imgCap },
                                        onError = { cameraBindingError = it.localizedMessage }
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "تبديل الكاميرا الأمامية/الخلفية",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRationaleContent(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "إذن الكاميرا مطلوب",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "يتيح هذا التطبيق التقاط الصور مباشرة بواسطة الكاميرا وتحليلها فورياً ومشاركتها في محادثاتك الذكية. يرجى منح الإذن للمتابعة.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray, lineHeight = 22.sp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("منح الإذن والمتابعة", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.15f),
                contentColor = Color.White
            )
        ) {
            Text("إلغاء")
        }
    }
}
