package app.spijun.ui.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.database.ContentObserver
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.spijun.ui.design.SpyHaptic
import android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
import android.os.VibrationEffect.Composition.PRIMITIVE_THUD
import android.os.VibrationEffect.Composition.PRIMITIVE_TICK
import app.spijun.ui.design.SpyHaptics
import java.util.WeakHashMap

@Composable
actual fun rememberSpyHaptics(enabled: Boolean): SpyHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, enabled) { AndroidHaptics(view, context, enabled) }
}

private class AndroidHaptics(private val view: View, context: Context, private val enabled: Boolean) : SpyHaptics {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun feedback(constant: Int, fallback: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
        val c = if (Build.VERSION.SDK_INT >= 30 || constant < 20) constant else fallback
        view.performHapticFeedback(c)
    }

    /** Fallback when compositions are unavailable: a one-shot waveform (off/on timings). */
    private fun pattern(vararg timings: Long) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (v.hasAmplitudeControl()) {
            val amplitudes = IntArray(timings.size) { if (it % 2 == 0) 0 else 255 }
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            v.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    }

    /**
     * Plays [primitives] (primitive to delay before it, ms) as one `VibrationEffect.Composition`
     * (motion.md §7). Returns false when the device can't play all of them, so the caller falls back.
     */
    private fun composition(vararg primitives: Pair<Int, Int>): Boolean {
        if (Build.VERSION.SDK_INT < 30) return false
        val v = vibrator ?: return false
        if (!v.hasVibrator() || !v.areAllPrimitivesSupported(*primitives.map { it.first }.toIntArray())) return false
        val effect = VibrationEffect.startComposition()
        for ((primitive, delay) in primitives) effect.addPrimitive(primitive, 1f, delay)
        v.vibrate(effect.compose())
        return true
    }

    private fun tick() = feedback(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK,
    )

    private fun confirm() = feedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP,
    )

    override fun perform(h: SpyHaptic) {
        if (!enabled) return
        when (h) {
            SpyHaptic.StepperTick -> tick()
            SpyHaptic.SuspenseTick -> if (!composition(PRIMITIVE_TICK to 0)) tick()
            SpyHaptic.HideCard -> feedback(HapticFeedbackConstants.CLOCK_TICK)
            SpyHaptic.RevealCitizen, SpyHaptic.VoteCast, SpyHaptic.ScanFound -> confirm()
            SpyHaptic.RevealSpy -> if (!composition(PRIMITIVE_THUD to 0, PRIMITIVE_THUD to 90)) pattern(0, 45, 90, 70)
            SpyHaptic.KnockOut -> if (!composition(PRIMITIVE_THUD to 0)) pattern(0, 90)
            SpyHaptic.GameOverReveal ->
                if (composition(PRIMITIVE_THUD to 0, PRIMITIVE_THUD to 100)) view.postDelayed({ confirm() }, 260)
                else pattern(0, 60, 100, 80, 160, 30)
            SpyHaptic.ThirtySeconds -> if (!composition(PRIMITIVE_QUICK_RISE to 0)) pattern(0, 25, 60, 25)
            SpyHaptic.TimeUp -> pattern(0, 300, 200, 300, 200, 300)
            SpyHaptic.Error -> feedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
            )
        }
    }
}

/** Follows "Remove animations" live: the setting can change while the app is open. */
@Composable
actual fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduce by remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduce = read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        reduce = read()
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduce
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun OnAppBackground(onBackground: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val current by rememberUpdatedState(onBackground)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) current() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
actual fun rememberScreenReaderOn(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }
    var on by remember(manager) { mutableStateOf(manager.isTouchExplorationEnabled) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { on = it }
        manager.addTouchExplorationStateChangeListener(listener)
        on = manager.isTouchExplorationEnabled
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }
    return on
}

@Composable
actual fun rememberAnnouncer(): (String) -> Unit {
    val view = LocalView.current
    // Deprecated in API 36 in favour of live regions, but a live region re-announces whenever its
    // text changes or it gains focus, and a secret role must be spoken exactly once.
    @Suppress("DEPRECATION")
    return remember(view) { { text -> view.announceForAccessibility(text) } }
}

/** Active [SecureWhile] holders per window: overlapping screens (e.g. a crossfade) share the flag. */
private val secureHolds = WeakHashMap<Window, Int>()

@Composable
actual fun SecureWhile(active: Boolean) {
    val view = LocalView.current
    // Debug builds skip the flag so screenshots and screen recording work during development.
    val debuggable = (view.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    DisposableEffect(active, view) {
        val window = view.context.findActivity()?.window
        if (!active || debuggable || window == null) return@DisposableEffect onDispose {}
        val holds = (secureHolds[window] ?: 0) + 1
        secureHolds[window] = holds
        if (holds == 1) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            val left = (secureHolds[window] ?: 1) - 1
            if (left > 0) secureHolds[window] = left
            else {
                secureHolds.remove(window)
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

actual val isIos: Boolean = false
