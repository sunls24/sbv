package dev.sunls24.sbv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.Shapes
import androidx.compose.ui.unit.dp

internal object SBVShapeTokens {
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(6.dp)
    val medium = RoundedCornerShape(8.dp)
    val large = RoundedCornerShape(12.dp)
    val extraLarge = RoundedCornerShape(16.dp)
}

internal val SBVShapes = Shapes(
    extraSmall = SBVShapeTokens.extraSmall,
    small = SBVShapeTokens.small,
    medium = SBVShapeTokens.medium,
    large = SBVShapeTokens.large,
    extraLarge = SBVShapeTokens.extraLarge,
)

internal val SBVMaterial3Shapes = androidx.compose.material3.Shapes(
    extraSmall = SBVShapeTokens.extraSmall,
    small = SBVShapeTokens.small,
    medium = SBVShapeTokens.medium,
    large = SBVShapeTokens.large,
    extraLarge = SBVShapeTokens.extraLarge,
)
