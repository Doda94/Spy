@file:Suppress("unused") // called from iosApp (Swift)

package app.spijun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import app.spijun.resources.Res
import app.spijun.resources.tab_dictionaries
import app.spijun.resources.tab_play
import app.spijun.ui.dictionaries.DictRoute
import app.spijun.ui.dictionaries.DictSheet
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyTheme
import app.spijun.ui.platform.BarAction
import app.spijun.ui.platform.BarSpec
import app.spijun.ui.platform.IosTextStorage
import app.spijun.ui.platform.LocalNativeBar
import app.spijun.ui.platform.NativeBar
import app.spijun.ui.platform.rememberFileActions
import app.spijun.ui.platform.rememberReduceMotion
import app.spijun.ui.platform.rememberSpyHaptics
import app.spijun.ui.screens.GameScreen
import app.spijun.ui.screens.HomeScreen
import app.spijun.ui.screens.SetupScreen
import app.spijun.ui.screens.dictionaries.DictionaryPage
import app.spijun.ui.screens.dictionaries.DictionarySheets
import app.spijun.ui.screens.dictionaries.ToastHost
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import platform.UIKit.UIViewController

/** What SwiftUI needs to draw the tabs and stacks. */
data class ShellState(
    /** [Tab] ordinal. */
    val tab: Int,
    val tabTitles: List<String>,
    val setupOpen: Boolean,
    val homeSheet: Boolean,
    val inGame: Boolean,
    /** Keys of the Rječnici screens over the list, bottom first. */
    val dictionaryPath: List<String>,
    /** 0 nothing, 1 a sheet or dialog over the Rječnici tab, 2 the full-screen QR. */
    val overlay: Int,
)

/** A top-bar button as SwiftUI draws it; [IosScreen.tap] finds the live action by position. */
data class BarItem(val symbol: String, val label: String, val danger: Boolean, val menu: List<BarItem>)

data class BarShape(val title: String?, val items: List<BarItem>, val cancel: BarItem?)

private fun BarAction.item(): BarItem = BarItem(symbol, label, danger, menu.map { it.item() })

/**
 * One Compose screen inside a SwiftUI NavigationStack destination. Its [TopBar][app.spijun.ui.screens.dictionaries.TopBar]
 * arrives here instead of being drawn, and SwiftUI turns it into the native toolbar.
 */
class IosScreen internal constructor(nativeBar: Boolean, content: @Composable () -> Unit) : NativeBar {
    private var spec: BarSpec? = null
    private var shape: BarShape? = null

    /** Set by SwiftUI; called only when what the bar shows changes, not on every recomposition. */
    var onBarChange: ((BarShape?) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(shape)
        }

    val viewController: UIViewController = ComposeUIViewController {
        SpyTheme(haptics = rememberSpyHaptics(enabled = true), reduceMotion = rememberReduceMotion()) {
            CompositionLocalProvider(LocalNativeBar provides if (nativeBar) this else null) {
                Box(Modifier.fillMaxSize().background(Spy.colors.bg)) { content() }
            }
        }
    }

    override fun show(spec: BarSpec?) {
        this.spec = spec
        val next = spec?.let { s -> BarShape(s.title, s.actions.map { it.item() }, s.cancel?.item()) }
        if (next != shape) {
            shape = next
            onBarChange?.invoke(next)
        }
    }

    /** [menuItem] is -1 for the button itself. */
    fun tap(item: Int, menuItem: Int) {
        val action = spec?.actions?.getOrNull(item) ?: return
        if (menuItem < 0) action.onClick() else action.menu.getOrNull(menuItem)?.onClick?.invoke()
    }

    fun tapCancel() {
        spec?.cancel?.onClick?.invoke()
    }
}

/**
 * Stable across category renames: the category name is not part of the key, so renaming a category
 * updates the existing destination in place instead of pushing a replacement controller.
 */
private fun routeKey(depth: Int, route: DictRoute): String = when (route) {
    DictRoute.List -> "List"
    is DictRoute.BuiltIn -> "$depth:BuiltIn:${route.id}"
    is DictRoute.Custom -> "$depth:Custom:${route.id}"
    is DictRoute.Category -> "$depth:Category:${route.dictId}"
}

/**
 * The iOS side of [AppShell]. SwiftUI owns the TabView and the NavigationStacks (components.md ›
 * Tab bar, Navigation bar), so swipe-back, the glass bars and the tab bar are the system's own;
 * each screen is still the shared Compose one, hosted on its own.
 */
object IosShell {
    private val shell = AppShell()
    private val scope = MainScope()
    private var tabTitles by mutableStateOf(listOf("", ""))
    private var homeSheet by mutableStateOf(false)

    init {
        scope.launch { shell.run(IosTextStorage(), Locale.current.language) }
        scope.launch { tabTitles = listOf(getString(Res.string.tab_play), getString(Res.string.tab_dictionaries)) }
    }

    private fun currentDictionaryPath(): List<String> =
        shell.dictionaries.stack.drop(1).mapIndexed { i, r -> routeKey(i + 1, r) }

    private fun state(): ShellState {
        val d = shell.dictionaries
        return ShellState(
            tab = shell.tab.ordinal,
            tabTitles = tabTitles,
            setupOpen = shell.app.route == Route.Setup,
            homeSheet = homeSheet,
            inGame = shell.app.route == Route.Game,
            dictionaryPath = currentDictionaryPath(),
            overlay = when (d.sheet) {
                null -> 0
                is DictSheet.Qr -> 2
                else -> 1
            },
        )
    }

    /** Calls [listener] now and after every change. */
    fun observe(listener: (ShellState) -> Unit) {
        scope.launch { snapshotFlow { state() }.collect { listener(it) } }
    }

    fun selectTab(index: Int) {
        shell.tab = Tab.entries[index]
    }

    fun closeSetup() {
        if (shell.app.route == Route.Setup) shell.app.goHome()
    }

    /**
     * SwiftUI popped back along the stack (swipe or back button). Only pops when [path] is a prefix
     * of the current Kotlin stack, so a stale path echoed while Kotlin replaces the stack is ignored.
     */
    fun popDictionaries(path: List<String>) {
        val current = currentDictionaryPath()
        if (path.size < current.size && current.take(path.size) == path) {
            shell.dictionaries.popTo(path.size)
        }
    }

    fun home() = IosScreen(nativeBar = false) {
        HomeScreen(onOnePhone = shell.app::openSetup, onSheetChange = { homeSheet = it })
    }

    fun setup() = IosScreen(nativeBar = true) {
        SetupScreen(shell.app, onBack = ::closeSetup, onAddWords = shell::addWordsFromSetup, onStart = { shell.app.startGame(it) })
    }

    /** The game is immersive (no system bars); it draws its own ✕ and asks before quitting. */
    fun game() = IosScreen(nativeBar = false) {
        // Keeps drawing the game while the fullScreenCover slides down after quitting or Game over.
        val last = remember { arrayOfNulls<app.spijun.engine.GameState>(1) }
        shell.app.game?.let { last[0] = it }
        last[0]?.let { GameScreen(it, shell.app) }
    }

    fun dictionaryList() = dictionary(routeKey(0, DictRoute.List))

    fun dictionary(key: String) = IosScreen(nativeBar = true) { DictionaryHost(key) }

    @Composable
    private fun DictionaryHost(key: String) {
        val d = shell.dictionaries
        val depth = if (key == "List") 0 else key.substringBefore(':').toIntOrNull() ?: return
        // Keeps drawing the screen while the native pop slides it away, after it left the stack.
        val last = remember { arrayOfNulls<DictRoute>(1) }
        d.stack.getOrNull(depth)?.takeIf { routeKey(depth, it) == key }?.let { last[0] = it }
        val route = last[0] ?: return
        val files = rememberFileActions(onPicked = d::beginImport)
        val onTop = d.stack.lastIndex == depth && d.route == route
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            DictionaryPage(d, shell.app.dictionaries.orEmpty(), route, files)
            if (onTop) ToastHost(d.toast, d::dismissToast, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
        }
        if (onTop) DictionarySheets(d, files)
    }
}
