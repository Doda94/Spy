package app.spijun.ui.platform

import androidx.compose.runtime.Composable
import app.spijun.ui.design.SpyHaptics

/** Haptics go through the platform's own engine (motion.md §7). [enabled] = Settings › Vibracija. */
@Composable
expect fun rememberSpyHaptics(enabled: Boolean): SpyHaptics

/** iOS Reduce Motion / Android "Remove animations" (motion.md §9). */
@Composable
expect fun rememberReduceMotion(): Boolean

/** System back (Android). No-op on iOS, where navigation is on-screen. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** Called when the app leaves the screen: face-up cards must flip back instantly (motion.md §11). */
@Composable
expect fun OnAppBackground(onBackground: () -> Unit)

/** Blocks screenshots and app-switcher thumbnails while a role can be on screen (Android FLAG_SECURE). */
@Composable
expect fun SecureWhile(active: Boolean)

/** iOS gets 56 dp capsule buttons; Android 64 dp, radius 16 morphing to full on press (screens.md global rules). */
expect val isIos: Boolean
