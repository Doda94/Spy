package app.spijun.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A button in a pushed screen's top bar. Compose draws [icon] (a SpyIcons path) on Android; on iOS the native
 * navigation bar draws the SF Symbol [symbol] (components.md › Glass button). A non-empty [menu]
 * opens those actions instead of calling [onClick].
 */
class BarAction(
    val icon: String,
    val symbol: String,
    val label: String,
    val danger: Boolean = false,
    val menu: List<BarAction> = emptyList(),
    val onClick: () -> Unit = {},
)

/** What a screen wants in its top bar. [cancel] replaces back (selection mode leaves with ✕). */
class BarSpec(val title: String?, val actions: List<BarAction>, val cancel: BarAction?)

/** Where a native navigation bar draws the top bar (iOS NavigationStack). */
interface NativeBar {
    /** Null when the screen that set the bar leaves. */
    fun show(spec: BarSpec?)
}

/** Null where Compose draws its own top bar (Android). */
val LocalNativeBar = staticCompositionLocalOf<NativeBar?> { null }
