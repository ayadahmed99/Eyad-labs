package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AttachmentItem

/**
 * Reusable ImagePreview composable that displays selected or captured images
 * with a high-contrast 'remove' button overlay, image thumbnail, and optional
 * metadata badge. Seamlessly integrates with the message input area and previews.
 */
@Composable
fun ImagePreview(
    imageUri: String,
    modifier: Modifier = Modifier,
    fileName: String? = null,
    fileSize: String? = null,
    size: Dp = 72.dp,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "معاينة الصورة المحددة"
) {
    Box(
        modifier = modifier
            .size(size)
            .padding(top = 4.dp, end = 4.dp)
    ) {
        // Main Image Container
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle bottom gradient for metadata if file size/name is provided
                if (!fileSize.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = fileSize,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Overlay Remove ('x') Button
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141821))
                    .border(1.dp, Color(0xFF252A36), CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إزالة الصورة",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Overload of ImagePreview accepting an [AttachmentItem].
 */
@Composable
fun ImagePreview(
    item: AttachmentItem,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ImagePreview(
        imageUri = item.uriString,
        fileName = item.name,
        fileSize = item.formattedSize,
        modifier = modifier,
        size = size,
        onRemove = onRemove,
        onClick = onClick,
        contentDescription = item.name
    )
}
