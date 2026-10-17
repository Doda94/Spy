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

    private fun pattern(vararg timings: Long) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val amplitudes = IntArray(timings.size) { if (it % 2 == 0) 0 else 255 }
        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
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
            SpyHaptic.StepperTick, SpyHaptic.SuspenseTick -> tick()
            SpyHaptic.HideCard -> feedback(HapticFeedbackConstants.CLOCK_TICK)
            SpyHaptic.RevealCitizen, SpyHaptic.VoteCast, SpyHaptic.ScanFound -> confirm()
            SpyHaptic.RevealSpy -> pattern(0, 45, 90, 70)
            SpyHaptic.KnockOut -> pattern(0, 90)
            SpyHaptic.GameOverReveal -> pattern(0, 60, 100, 80, 160, 30)
            SpyHaptic.ThirtySeconds -> pattern(0, 25, 60, 25)
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
