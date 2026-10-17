package app.spijun.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import app.spijun.ui.design.SpyHaptic
import app.spijun.ui.design.SpyHaptics
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityAnnouncementNotification
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityVoiceOverStatusDidChangeNotification
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

@Composable
actual fun rememberSpyHaptics(enabled: Boolean): SpyHaptics = remember(enabled) { IosHaptics(enabled) }

private class IosHaptics(private val enabled: Boolean) : SpyHaptics {
    private fun impact(style: UIImpactFeedbackStyle, afterMs: Long = 0) = later(afterMs) {
        UIImpactFeedbackGenerator(style).apply { prepare(); impactOccurred() }
    }

    private fun notify(type: UINotificationFeedbackType, afterMs: Long = 0) = later(afterMs) {
        UINotificationFeedbackGenerator().apply { prepare(); notificationOccurred(type) }
    }

    private fun selection() = UISelectionFeedbackGenerator().apply { prepare(); selectionChanged() }

    private fun later(ms: Long, block: () -> Unit) {
        if (ms == 0L) block()
        else dispatch_after(dispatch_time(DISPATCH_TIME_NOW, ms * 1_000_000L), dispatch_get_main_queue()) { block() }
    }

    override fun perform(h: SpyHaptic) {
        if (!enabled) return
        when (h) {
            SpyHaptic.StepperTick -> selection()
            SpyHaptic.RevealCitizen -> impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
            SpyHaptic.RevealSpy -> {
                impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
                impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy, 90)
            }
            SpyHaptic.HideCard -> impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleSoft)
            SpyHaptic.VoteCast, SpyHaptic.ScanFound -> notify(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            SpyHaptic.SuspenseTick -> impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
            SpyHaptic.KnockOut -> impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
            SpyHaptic.GameOverReveal -> {
                impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
                impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy, 120)
                notify(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess, 260)
            }
            SpyHaptic.ThirtySeconds -> notify(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
            SpyHaptic.TimeUp -> {
                notify(UINotificationFeedbackType.UINotificationFeedbackTypeError)
                notify(UINotificationFeedbackType.UINotificationFeedbackTypeError, 450)
                notify(UINotificationFeedbackType.UINotificationFeedbackTypeError, 900)
            }
            SpyHaptic.Error -> notify(UINotificationFeedbackType.UINotificationFeedbackTypeError)
        }
    }
}

/** Follows Reduce Motion live: the setting can change while the app is open. */
@Composable
actual fun rememberReduceMotion(): Boolean {
    var reduce by remember { mutableStateOf(UIAccessibilityIsReduceMotionEnabled()) }
    DisposableEffect(Unit) {
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIAccessibilityReduceMotionStatusDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> reduce = UIAccessibilityIsReduceMotionEnabled() }
        reduce = UIAccessibilityIsReduceMotionEnabled()
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(token) }
    }
    return reduce
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button; every screen has an on-screen control.
}

@Composable
actual fun OnAppBackground(onBackground: () -> Unit) {
    val current by rememberUpdatedState(onBackground)
    DisposableEffect(Unit) {
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> current() }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(token) }
    }
}

@Composable
actual fun rememberScreenReaderOn(): Boolean {
    var on by remember { mutableStateOf(UIAccessibilityIsVoiceOverRunning()) }
    DisposableEffect(Unit) {
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIAccessibilityVoiceOverStatusDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> on = UIAccessibilityIsVoiceOverRunning() }
        on = UIAccessibilityIsVoiceOverRunning()
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(token) }
    }
    return on
}

@Composable
actual fun rememberAnnouncer(): (String) -> Unit = remember {
    { text -> UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, text) }
}

@Composable
actual fun SecureWhile(active: Boolean) {
    // iOS blurs the app-switcher snapshot natively in iosApp (to do with the SwiftUI host).
}

actual val isIos: Boolean = true
