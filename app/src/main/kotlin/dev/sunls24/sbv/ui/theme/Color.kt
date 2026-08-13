package dev.sunls24.sbv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Shared semantic colors for the always-dark TV experience.
 *
 * The TV and regular Material 3 color schemes use different types, so both
 * themes are built from this single semantic palette in Theme.kt.
 */
internal object SBVColorTokens {
    val primary = Color(0xFFFB7299)
    val onPrimary = Color(0xFF2E0714)
    val primaryContainer = Color(0xFF64243B)
    val onPrimaryContainer = Color(0xFFFFD9E3)

    val secondary = Color(0xFFD8BBC4)
    val onSecondary = Color(0xFF3D2930)
    val secondaryContainer = Color(0xFF554049)
    val onSecondaryContainer = Color(0xFFF5DCE4)

    val background = Color(0xFF0B0D10)
    val onBackground = Color(0xFFE9EEF2)
    val surface = Color(0xFF13171B)
    val onSurface = Color(0xFFE9EEF2)
    val surfaceVariant = Color(0xFF22282E)
    val onSurfaceVariant = Color(0xFFB9C2C9)

    val focus = Color(0xFFF4F7F9)
    val borderVariant = Color(0xFF6A5A60)

    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)
    val scrim = Color.Black
}
