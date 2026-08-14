package dev.sunls24.sbv.ui.theme

import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.darkColorScheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dev.sunls24.sbv.R

@Composable
internal fun focusedTextColor(hasFocus: Boolean) =
    if (hasFocus) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SBVTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val backgroundColor = colorResource(R.color.window_background)

    val colorSchemeTv = darkColorScheme(
        primary = SBVColorTokens.primary,
        onPrimary = SBVColorTokens.onPrimary,
        primaryContainer = SBVColorTokens.primaryContainer,
        onPrimaryContainer = SBVColorTokens.onPrimaryContainer,
        secondary = SBVColorTokens.secondary,
        onSecondary = SBVColorTokens.onSecondary,
        secondaryContainer = SBVColorTokens.secondaryContainer,
        onSecondaryContainer = SBVColorTokens.onSecondaryContainer,
        background = backgroundColor,
        onBackground = SBVColorTokens.onBackground,
        surface = SBVColorTokens.surface,
        onSurface = SBVColorTokens.onSurface,
        surfaceVariant = SBVColorTokens.surfaceVariant,
        onSurfaceVariant = SBVColorTokens.onSurfaceVariant,
        surfaceTint = SBVColorTokens.primary,
        inverseSurface = SBVColorTokens.onSurface,
        inverseOnSurface = SBVColorTokens.surface,
        error = SBVColorTokens.error,
        onError = SBVColorTokens.onError,
        errorContainer = SBVColorTokens.errorContainer,
        onErrorContainer = SBVColorTokens.onErrorContainer,
        border = SBVColorTokens.focus,
        borderVariant = SBVColorTokens.borderVariant,
        scrim = SBVColorTokens.scrim,
    )
    val colorSchemeCommon = androidx.compose.material3.darkColorScheme(
        primary = SBVColorTokens.primary,
        onPrimary = SBVColorTokens.onPrimary,
        primaryContainer = SBVColorTokens.primaryContainer,
        onPrimaryContainer = SBVColorTokens.onPrimaryContainer,
        secondary = SBVColorTokens.secondary,
        onSecondary = SBVColorTokens.onSecondary,
        secondaryContainer = SBVColorTokens.secondaryContainer,
        onSecondaryContainer = SBVColorTokens.onSecondaryContainer,
        background = backgroundColor,
        onBackground = SBVColorTokens.onBackground,
        surface = SBVColorTokens.surface,
        onSurface = SBVColorTokens.onSurface,
        surfaceVariant = SBVColorTokens.surfaceVariant,
        onSurfaceVariant = SBVColorTokens.onSurfaceVariant,
        inverseSurface = SBVColorTokens.onSurface,
        inverseOnSurface = SBVColorTokens.surface,
        error = SBVColorTokens.error,
        onError = SBVColorTokens.onError,
        errorContainer = SBVColorTokens.errorContainer,
        onErrorContainer = SBVColorTokens.onErrorContainer,
        outline = SBVColorTokens.focus,
        outlineVariant = SBVColorTokens.borderVariant,
        scrim = SBVColorTokens.scrim,
    )
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorSchemeTv.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorSchemeTv,
        shapes = SBVShapes,
        typography = SBVTypography,
    ) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = colorSchemeCommon,
            shapes = SBVMaterial3Shapes,
            typography = SBVMaterial3Typography,
        ) {
            CompositionLocalProvider(
                LocalRippleConfiguration provides null
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = SBVShapeTokens.extraSmall,
                        colors = SurfaceDefaults.colors(
                            containerColor = backgroundColor,
                        ),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
