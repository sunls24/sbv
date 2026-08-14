package dev.sunls24.sbv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SbvTitleLarge = TextStyle(
    fontSize = 22.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.Medium,
)

internal val SBVPageTitle = TextStyle(
    fontSize = 28.sp,
    lineHeight = 36.sp,
    fontWeight = FontWeight.Medium,
)

private val SbvTitleMedium = TextStyle(
    fontSize = 20.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.Medium,
)

private val SbvBodyLarge = TextStyle(
    fontSize = 18.sp,
    lineHeight = 26.sp,
)

private val SbvBodyMedium = TextStyle(
    fontSize = 16.sp,
    lineHeight = 24.sp,
)

private val SbvBodySmall = TextStyle(
    fontSize = 14.sp,
    lineHeight = 20.sp,
)

private val SbvLabelLarge = TextStyle(
    fontSize = 18.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Medium,
)

private val SbvLabelMedium = TextStyle(
    fontSize = 16.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Medium,
)

private val SbvLabelSmall = TextStyle(
    fontSize = 14.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Medium,
)

internal val SBVTypography = androidx.tv.material3.Typography(
    headlineSmall = SbvTitleLarge,
    titleLarge = SbvTitleLarge,
    titleMedium = SbvTitleMedium,
    bodyLarge = SbvBodyLarge,
    bodyMedium = SbvBodyMedium,
    bodySmall = SbvBodySmall,
    labelLarge = SbvLabelLarge,
    labelMedium = SbvLabelMedium,
    labelSmall = SbvLabelSmall,
)

internal val SBVMaterial3Typography = androidx.compose.material3.Typography(
    headlineSmall = SbvTitleLarge,
    titleLarge = SbvTitleLarge,
    titleMedium = SbvTitleMedium,
    bodyLarge = SbvBodyLarge,
    bodyMedium = SbvBodyMedium,
    bodySmall = SbvBodySmall,
    labelLarge = SbvLabelLarge,
    labelMedium = SbvLabelMedium,
    labelSmall = SbvLabelSmall,
)
