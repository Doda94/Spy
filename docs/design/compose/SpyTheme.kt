// Reference implementation of the Špijun token set as a Compose Multiplatform theme.
// Generated from docs/design/tokens.json — values are the source of truth there.
// The canvas (H·01) names this file but never shipped it; treat this as a starting point.
//
// Fonts: bundle Bricolage Grotesque (700, 800) and Hanken Grotesk (400, 500, 600, 700)
// as composeResources/font/*.ttf (both OFL). Resource names below are placeholders.

package app.spijun.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// ───────────────────────── Colours ─────────────────────────

@Immutable
data class SpyColors(
    val bg: Color, val surface: Color, val surface2: Color, val line: Color,
    val text: Color, val textMuted: Color, val eliminated: Color,
    val primary: Color, val onPrimary: Color,
    val spy: Color, val citizen: Color,
    val success: Color, val warning: Color, val onWarning: Color, val warningText: Color,
    val danger: Color, val dangerFill: Color, val onDangerFill: Color,
    val table: Color, val tableRing: Color,
    val disabledFill: Color, val checkboxOutline: Color,
    val glass: Color, val glassLine: Color, val glassHighlight: Color,
    val tabSelected: Color, val optionFill: Color, val scrim: Color, val sheetGlass: Color,
    // Theme-independent card faces
    val cardSpyFace: Color = Color(0xFF0F0D0B),
    val cardSpyFaceStroke: Color = Color(0xFF3A322B),
    val cardCitizenFace: Color = Color(0xFFF3EDE2),
    val cardCitizenInk: Color = Color(0xFF1F3FA8),
    val cardCitizenText: Color = Color(0xFF1A1714),
    val cardBack: Color = Color(0xFF2A221C),
    val cardBackStroke: Color = Color(0xFF4A3D33),
    val cardBackLattice: Color = Color(0xFFFF6B3D).copy(alpha = 0.22f),
    // The same back as it sits on the table: warmer and rimmed in dark, so seats stand off the table.
    val seatBack: Color = cardBack,
    val seatBackStroke: Color = cardBackStroke,
    val seatBackLattice: Color = cardBackLattice,
    val roomCodeCard: Color = Color(0xFFF3EDE2),
    val vermilion: Color = Color(0xFFFF6B3D),
    val onVermilionFill: Color = Color(0xFF1A0905),
)

val SpyDarkColors = SpyColors(
    bg = Color(0xFF121110), surface = Color(0xFF1C1A18), surface2 = Color(0xFF262320), line = Color(0xFF34302C),
    text = Color(0xFFF3EDE2), textMuted = Color(0xFFA39C94), eliminated = Color(0xFF8F8880),
    primary = Color(0xFFF3EDE2), onPrimary = Color(0xFF121110),
    spy = Color(0xFFFF6B3D), citizen = Color(0xFF6E9BFF),
    success = Color(0xFF4CC38A), warning = Color(0xFFF5B83D), onWarning = Color(0xFF1A1206), warningText = Color(0xFFF5B83D),
    danger = Color(0xFFFF6B78), dangerFill = Color(0xFFC42B3A), onDangerFill = Color.White,
    table = Color(0xFF221E1A), tableRing = Color(0xFF34302C),
    disabledFill = Color(0xFF4A443F), checkboxOutline = Color(0xFF6B645C),
    glass = Color.White.copy(alpha = 0.08f), glassLine = Color.White.copy(alpha = 0.16f), glassHighlight = Color.White.copy(alpha = 0.20f),
    tabSelected = Color.White.copy(alpha = 0.14f), optionFill = Color.White.copy(alpha = 0.06f),
    scrim = Color.Black.copy(alpha = 0.60f), sheetGlass = Color(0xFF262320).copy(alpha = 0.86f),
    seatBack = Color(0xFF33291F), seatBackStroke = Color(0xFFF3EDE2).copy(alpha = 0.34f),
    seatBackLattice = Color(0xFFFF6B3D).copy(alpha = 0.30f),
)

val SpyLightColors = SpyColors(
    bg = Color(0xFFF6F2EA), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEEE8DE), line = Color(0xFFDDD5C8),
    text = Color(0xFF1A1714), textMuted = Color(0xFF6B645C), eliminated = Color(0xFF716A62),
    primary = Color(0xFF1A1714), onPrimary = Color(0xFFF3EDE2),
    spy = Color(0xFFC2410C), citizen = Color(0xFF2F5BD8),
    success = Color(0xFF1E7A4F), warning = Color(0xFFF5B83D), onWarning = Color(0xFF1A1206), warningText = Color(0xFF8A5A00),
    danger = Color(0xFFC42B3A), dangerFill = Color(0xFFC42B3A), onDangerFill = Color.White,
    table = Color(0xFFE8E0D3), tableRing = Color(0xFFD6CCBD),
    disabledFill = Color(0xFFC9C2B9), checkboxOutline = Color(0xFFA39C94),
    glass = Color.White.copy(alpha = 0.72f), glassLine = Color(0xFF1A1714).copy(alpha = 0.10f), glassHighlight = Color.White.copy(alpha = 0.90f),
    tabSelected = Color(0xFF1A1714).copy(alpha = 0.08f), optionFill = Color(0xFF1A1714).copy(alpha = 0.04f),
    scrim = Color(0xFF1A1714).copy(alpha = 0.35f), sheetGlass = Color.White.copy(alpha = 0.86f),
)

// Increase Contrast overrides (tokens.json › color.accessibility.increaseContrast).
// Design specifies the dark values (line → n400, muted → n700); light values are derived (darker muted n300).
fun SpyColors.highContrast(dark: Boolean) =
    if (dark) copy(line = Color(0xFF6B645C), textMuted = Color(0xFFC9C2B9))
    else copy(line = Color(0xFFA39C94), textMuted = Color(0xFF4A443F))

// ───────────────────────── Type ─────────────────────────

@Immutable
data class SpyType(
    val wordDisplay: TextStyle, val roleDisplay: TextStyle, val countdown: TextStyle,
    val titleLarge: TextStyle, val titleMedium: TextStyle, val headline: TextStyle,
    val bodyLarge: TextStyle, val body: TextStyle, val label: TextStyle,
    val caption: TextStyle, val button: TextStyle,
)

fun spyType(display: FontFamily, text: FontFamily) = SpyType(
    // auto-fit 56 → 52 → 48 → 44 → 40, max 2 lines, break only between words; cap font scale at 1.3×
    wordDisplay = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = (-0.03).em),
    roleDisplay = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-0.03).em),
    countdown = TextStyle(fontFamily = text, fontWeight = FontWeight.Bold, fontSize = 96.sp, lineHeight = 96.sp, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum"),
    titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.02).em),
    titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headline = TextStyle(fontFamily = text, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp),
    body = TextStyle(fontFamily = text, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    label = TextStyle(fontFamily = text, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.08.em), // UPPERCASE in resources
    caption = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    button = TextStyle(fontFamily = text, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
)

const val DisplayMaxFontScale = 1.3f

// ───────────────────────── Shape / space ─────────────────────────

object SpyShape {
    val xs = RoundedCornerShape(6.dp)    // tags
    val sm = RoundedCornerShape(10.dp)   // inputs, chips
    val md = RoundedCornerShape(16.dp)   // Android button (morphs → full on press)
    val lg = RoundedCornerShape(22.dp)   // grouped panel
    val xl = RoundedCornerShape(28.dp)   // sheet, hero panel
    val full = RoundedCornerShape(50)    // iOS button, pills
    fun card(cut: Float = 22f) = CutCornerShape(cut.dp)       // 300×420 → 22; scale with card width (56 wide → 5)
    fun cardFrame(cut: Float = 14f) = CutCornerShape(cut.dp)  // inner frame, inset 12
}

object SpySpace {
    val s0_5 = 2.dp; val s1 = 4.dp; val s2 = 8.dp; val s3 = 12.dp; val s4 = 16.dp; val s5 = 20.dp
    val s6 = 24.dp; val s8 = 32.dp; val s10 = 40.dp; val s14 = 56.dp
    val gutter = 20.dp; val minTarget = 48.dp; val rowHeight = 52.dp
    val buttonIos = 56.dp; val buttonAndroid = 64.dp; val chip = 48.dp; val pill = 40.dp
}

// ───────────────────────── Motion ─────────────────────────

object SpyMotion {
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Emphasized = CubicBezierEasing(0.3f, 0f, 0f, 1f)
    const val PRESS_MS = 100; const val STANDARD_MS = 220; const val EMPHASIZED_MS = 400
    const val FLIP_IN_MS = 160; const val FLIP_BACK_MS = 300; const val LIFT_MS = 480; const val TABLE_TURN_MS = 700
    const val DEAL_STAGGER_MS = 70; const val CONTENT_STAGGER_MS = 60; const val DIGIT_MS = 120
    const val SUSPENSE_MS = 700; const val KNOCK_OUT_MS = 280; const val GAME_OVER_STAGGER_MS = 120
    const val REDUCED_FADE_MS = 200; const val SHAKE_MS = 300; const val TOAST_MS = 4000
    const val AUTO_HIDE_DEFAULT_MS = 8000; const val AMBIENT_TURN_MS = 90_000
    const val PRESS_SCALE = 0.97f; const val LIFT_SCALE = 1.04f; const val CAMERA_PUSH = 1.3f; const val PULSE_SCALE = 1.07f
    fun <T> card() = spring<T>(dampingRatio = 0.75f, stiffness = 400f)
    fun <T> deal() = spring<T>(dampingRatio = 0.8f, stiffness = 200f)
    fun <T> standard() = tween<T>(STANDARD_MS, easing = Standard)
    fun <T> emphasized(ms: Int = EMPHASIZED_MS) = tween<T>(ms, easing = Emphasized)
}

// ───────────────────────── Haptics ─────────────────────────

enum class SpyHaptic { StepperTick, RevealCitizen, RevealSpy, HideCard, VoteCast, SuspenseTick, KnockOut, GameOverReveal, ThirtySeconds, TimeUp, Error, ScanFound }

/** expect/actual: iOS → UIKit feedback generators / Core Haptics; Android → HapticFeedbackConstants / VibrationEffect.Composition. See motion.md §7. */
interface SpyHaptics { fun perform(h: SpyHaptic) }

// ───────────────────────── Theme ─────────────────────────

enum class ThemeSetting { System, Light, Dark }

val LocalSpyColors = staticCompositionLocalOf { SpyDarkColors }
val LocalSpyType = staticCompositionLocalOf<SpyType> { error("SpyTheme missing") }
val LocalSpyHaptics = staticCompositionLocalOf<SpyHaptics> { object : SpyHaptics { override fun perform(h: SpyHaptic) {} } }

object Spy {
    val colors: SpyColors @Composable get() = LocalSpyColors.current
    val type: SpyType @Composable get() = LocalSpyType.current
    val haptics: SpyHaptics @Composable get() = LocalSpyHaptics.current
}

@Composable
fun SpyTheme(
    themeSetting: ThemeSetting = ThemeSetting.System,
    displayFamily: FontFamily,   // bricolage()
    textFamily: FontFamily,      // hanken()
    haptics: SpyHaptics,
    content: @Composable () -> Unit,
) {
    val dark = when (themeSetting) { ThemeSetting.System -> isSystemInDarkTheme(); ThemeSetting.Dark -> true; ThemeSetting.Light -> false }
    val c = if (dark) SpyDarkColors else SpyLightColors
    // Primary is neutral on purpose; dynamic colour is OFF. tertiary = spy tonal T70 / T40.
    val scheme = if (dark) darkColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, tertiary = Color(0xFFFF8B68),
        background = c.bg, surface = c.bg, surfaceContainer = c.surface, surfaceContainerHigh = c.surface2,
        onSurface = c.text, onBackground = c.text, onSurfaceVariant = c.textMuted, outline = c.line, error = c.danger,
    ) else lightColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, tertiary = Color(0xFFAE3203),
        background = c.bg, surface = c.bg, surfaceContainer = c.surface, surfaceContainerHigh = c.surface2,
        onSurface = c.text, onBackground = c.text, onSurfaceVariant = c.textMuted, outline = c.line, error = c.danger,
    )
    val t = spyType(displayFamily, textFamily)
    val m3Type = Typography(
        displayLarge = t.roleDisplay, displayMedium = t.wordDisplay,
        headlineLarge = t.titleLarge, headlineMedium = t.titleMedium, headlineSmall = t.headline,
        bodyLarge = t.bodyLarge, bodyMedium = t.body, bodySmall = t.caption,
        labelLarge = t.button, labelMedium = t.label,
    )
    CompositionLocalProvider(LocalSpyColors provides c, LocalSpyType provides t, LocalSpyHaptics provides haptics) {
        MaterialTheme(colorScheme = scheme, typography = m3Type, content = content)
    }
}
