// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object RasailColors {
    val NightPlum = Color(0xFF1A0B2E)
    val Aubergine = Color(0xFF2B1250)
    val Violet = Color(0xFF4B2A8A)
    val Gold = Color(0xFFD4AF37)
    val PaleGold = Color(0xFFF1DFA0)
    val Ivory = Color(0xFFF4EEE0)
}

private val RasailScheme = darkColorScheme(
    primary = RasailColors.Gold,
    onPrimary = RasailColors.NightPlum,
    primaryContainer = RasailColors.Violet,
    onPrimaryContainer = RasailColors.Ivory,
    secondary = RasailColors.PaleGold,
    background = RasailColors.NightPlum,
    onBackground = RasailColors.Ivory,
    surface = RasailColors.Aubergine,
    onSurface = RasailColors.Ivory,
    outline = RasailColors.Gold,
    outlineVariant = RasailColors.Gold.copy(alpha = 0.35f)
)

private val RasailTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 38.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
)

@Composable
fun RasailTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RasailScheme, typography = RasailTypography, content = content)
}
