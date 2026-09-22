package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Brand Palette
val BrandPrimary = Color(0xFF7C5CFF)       // Violet primary
val BrandPrimaryLight = Color(0xFF6D4AFF)  // Light mode primary
val BrandSecondary = Color(0xFF4DA3FF)     // Soft blue
val BrandAccent = Color(0xFFA78BFA)        // Gentle accent lavender

// Dark Theme Palette
val DarkBackground = Color(0xFF0B0D12)     // Deep obsidian background
val DarkSurface = Color(0xFF141821)        // Refined card surface
val DarkSurfaceVariant = Color(0xFF1B202D) // Elevated container
val DarkOutline = Color(0xFF252A36)        // Subtle borders
val DarkOutlineVariant = Color(0xFF1F2430) // Fainter borders
val DarkOnBackground = Color(0xFFF5F7FA)   // Pristine primary text
val DarkOnSurface = Color(0xFFF5F7FA)      // Card primary text
val DarkOnSurfaceVariant = Color(0xFF9CA3AF) // Muted secondary text

// Light Theme Palette
val LightBackground = Color(0xFFF7F8FC)    // Clean light canvas
val LightSurface = Color(0xFFFFFFFF)       // Crisp white surface
val LightSurfaceVariant = Color(0xFFEFF2F8) // Muted light container
val LightOutline = Color(0xFFE2E8F0)       // Subtle light borders
val LightOutlineVariant = Color(0xFFCBD5E1) // Divider light borders
val LightOnBackground = Color(0xFF0F172A)  // Deep contrast text
val LightOnSurface = Color(0xFF0F172A)     // Card text
val LightOnSurfaceVariant = Color(0xFF64748B) // Slate secondary text

// User Message Gradients
val UserMessageGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF7C5CFF),
        Color(0xFF4DA3FF)
    )
)

val UserMessageGradientLight = Brush.linearGradient(
    colors = listOf(
        Color(0xFF6D4AFF),
        Color(0xFF3B82F6)
    )
)

val AiLogoGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF7C5CFF),
        Color(0xFF4DA3FF)
    )
)
