/*  Copyright (C) 2026 quompacc

    This file is part of YuneBridge, a fork of Gadgetbridge.

    YuneBridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    YuneBridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package io.github.quompacc.yunebridge.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R

/**
 * Farben, die über das Material-Farbschema hinausgehen: Karten sowie die festen
 * Kategoriefarben für Gesundheitsdaten. [ink] ist jeweils die Textvariante mit
 * ausreichendem Kontrast (WCAG 4.5:1) auf [card].
 */
@Immutable
data class YuneColors(
    val background: Color,
    val card: Color,
    val text: Color,
    val secondaryText: Color,
    val separator: Color,
    val fill: Color,
    val tint: Color,
    val tintSoft: Color,
    val track: Color,
    val steps: Color, val stepsInk: Color,
    val active: Color, val activeInk: Color,
    val distance: Color, val distanceInk: Color,
    val heart: Color, val heartInk: Color,
    val sleep: Color, val sleepInk: Color,
    val stress: Color, val stressInk: Color,
    val calories: Color, val caloriesInk: Color,
    val sleepAwake: Color, val sleepRem: Color, val sleepLight: Color, val sleepDeep: Color,
    val connected: Color, val connectedInk: Color,
)

val YuneLightColors = YuneColors(
    background = Color(0xFFF3F2EE), card = Color(0xFFFFFFFF),
    text = Color(0xFF161816), secondaryText = Color(0xFF686C65),
    separator = Color(0xFFDEDCD5), fill = Color(0xFFF1EFEA),
    tint = Color(0xFFC4471A), tintSoft = Color(0xFFFBE8DE), track = Color(0xFFECEAE4),
    steps = Color(0xFFF06A22), stepsInk = Color(0xFFB4480F),
    active = Color(0xFF6FA334), activeInk = Color(0xFF4E7A22),
    distance = Color(0xFF2A93C4), distanceInk = Color(0xFF1B6F94),
    heart = Color(0xFFE5404F), heartInk = Color(0xFFB8283A),
    sleep = Color(0xFF5856D6), sleepInk = Color(0xFF4B49C9),
    stress = Color(0xFFE3A21A), stressInk = Color(0xFF8A5A00),
    calories = Color(0xFFD2468E), caloriesInk = Color(0xFFA8306E),
    sleepAwake = Color(0xFFF2A65A), sleepRem = Color(0xFF4FB8E0), sleepLight = Color(0xFF7B79F0), sleepDeep = Color(0xFF3B37A8),
    connected = Color(0xFF34A853), connectedInk = Color(0xFF2B7A3D),
)

val YuneDarkColors = YuneColors(
    background = Color(0xFF0B0D0C), card = Color(0xFF1A1E1B),
    text = Color(0xFFF4F0E6), secondaryText = Color(0xFFA3A99B),
    separator = Color(0xFF2B302C), fill = Color(0xFF262B27),
    tint = Color(0xFFFF854D), tintSoft = Color(0xFF3A2419), track = Color(0xFF2B302C),
    steps = Color(0xFFFF854D), stepsInk = Color(0xFFFF854D),
    active = Color(0xFFA6D46A), activeInk = Color(0xFFA6D46A),
    distance = Color(0xFF5CC2E6), distanceInk = Color(0xFF5CC2E6),
    heart = Color(0xFFFF6B78), heartInk = Color(0xFFFF6B78),
    sleep = Color(0xFF8E8CF5), sleepInk = Color(0xFF9F9DF7),
    stress = Color(0xFFF5BE45), stressInk = Color(0xFFF5BE45),
    calories = Color(0xFFF07AC0), caloriesInk = Color(0xFFF07AC0),
    sleepAwake = Color(0xFFFFB870), sleepRem = Color(0xFF67D0F0), sleepLight = Color(0xFF9C9AF7), sleepDeep = Color(0xFF5D59E0),
    connected = Color(0xFF5BD27A), connectedInk = Color(0xFF7BE095),
)

/** AMOLED-Variante: echtes Schwarz als Hintergrund, Karten bleiben leicht abgesetzt. */
val YuneBlackColors = YuneDarkColors.copy(background = Color.Black, card = Color(0xFF121513))

val LocalYuneColors = staticCompositionLocalOf { YuneLightColors }

/** Inter als variable Schrift (Gewichte ab Android 8, davor Regular). */
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val InterFamily = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
    inter(FontWeight.ExtraBold),
)

/** Typografie nach der Design-Vorschau: große, kräftige Titel, ruhiger Fließtext. */
val YuneTypography = Typography(
    displaySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.025).em),
    headlineSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.02).em),
    titleLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.03).em),
    titleMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em),
    titleSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp),
)

val YuneShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun YuneColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = tint,
        onPrimary = if (dark) Color(0xFF141715) else Color.White,
        primaryContainer = tintSoft,
        onPrimaryContainer = text,
        secondaryContainer = tintSoft,
        onSecondaryContainer = text,
        background = background,
        onBackground = text,
        surface = background,
        onSurface = text,
        surfaceVariant = fill,
        onSurfaceVariant = secondaryText,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = fill,
        surfaceContainerHighest = track,
        outline = secondaryText,
        outlineVariant = separator,
    )
}

/**
 * YuneBridge-Theme für Compose-Screens. Hell/Dunkel und AMOLED folgen denselben
 * Einstellungen wie der Rest der App (Standard: Systemeinstellung).
 */
@Composable
fun YuneTheme(
    dark: Boolean = GBApplication.isDarkThemeEnabled(),
    amoled: Boolean = dark && GBApplication.isAmoledBlackEnabled(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        amoled -> YuneBlackColors
        dark -> YuneDarkColors
        else -> YuneLightColors
    }
    CompositionLocalProvider(LocalYuneColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(dark),
            typography = YuneTypography,
            shapes = YuneShapes,
            content = content,
        )
    }
}

object Yune {
    val colors: YuneColors
        @Composable get() = LocalYuneColors.current
}
