package com.teacher.vocabcheck.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.teacher.vocabcheck.engine.WordTier

/**
 * M3 的 container / surfaceVariant 系列必须一起覆盖，否则未定义的角色会退回默认紫色，
 * 和绿色主色直接撞色。
 */
private val Palette = lightColorScheme(
    primary = Color(0xFF00693F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFF0D3),
    onPrimaryContainer = Color(0xFF00210F),

    secondary = Color(0xFF4C6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8D6),
    onSecondaryContainer = Color(0xFF092014),

    tertiary = Color(0xFF7C5800),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDFB5),
    onTertiaryContainer = Color(0xFF271900),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF6FAF5),
    onBackground = Color(0xFF191D1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191D1A),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF41493F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F0),
    surfaceContainer = Color(0xFFEBEFEA),
    surfaceContainerHigh = Color(0xFFE5EAE4),
    surfaceContainerHighest = Color(0xFFDFE5DE),
    outline = Color(0xFF71796F),
    outlineVariant = Color(0xFFC0C9BE)
).copy(
    // Snackbar 这类反色容器取的是 inverse* 三件套，lightColorScheme 不暴露它们，漏了会退回默认紫色
    inversePrimary = Color(0xFF8FD9B4),
    inverseSurface = Color(0xFF2E312E),
    inverseOnSurface = Color(0xFFEFF1EC)
)

/** 档位色：正文标色、图例、清单标题三处共用 */
private val ColorCoreExam = Color(0xFFC62828)
private val ColorCoreJunior = Color(0xFF1565C0)
private val ColorBase = Color(0xFF2E7D32)

fun tierColor(tier: WordTier): Color = when (tier) {
    WordTier.CORE_EXAM -> ColorCoreExam
    WordTier.CORE_JUNIOR -> ColorCoreJunior
    WordTier.BASE -> ColorBase
}

fun WordTier.label(): String = when (this) {
    WordTier.CORE_EXAM -> "高考核心688"
    WordTier.CORE_JUNIOR -> "中考核心688"
    WordTier.BASE -> "高考词汇基线"
}

fun WordTier.shortLabel(): String = when (this) {
    WordTier.CORE_EXAM -> "高考核心"
    WordTier.CORE_JUNIOR -> "中考核心"
    WordTier.BASE -> "高考基线"
}

private val Body = TextStyle(
    fontSize = 17.sp,
    lineHeight = 27.sp,
    letterSpacing = 0.01.em
)

private val Type = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.03.em),
    bodyLarge = Body,
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, letterSpacing = 0.02.em),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 0.02.em)
)

@Composable
fun VocabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette, typography = Type, content = content)
}
