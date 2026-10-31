package app.spijun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import app.spijun.resources.Res
import app.spijun.resources.tab_dictionaries
import app.spijun.resources.tab_play
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpyTabBar
import app.spijun.ui.design.SpyTheme
import app.spijun.ui.design.TabItem
import app.spijun.ui.dictionaries.DictRoute
import app.spijun.ui.dictionaries.DictSheet
import app.spijun.ui.platform.rememberReduceMotion
import app.spijun.ui.platform.rememberSpyHaptics
import app.spijun.ui.platform.rememberTextStorage
import app.spijun.ui.screens.GameScreen
import app.spijun.ui.screens.HomeScreen
import app.spijun.ui.screens.SetupScreen
import app.spijun.ui.screens.dictionaries.DictionariesScreen
import org.jetbrains.compose.resources.stringResource

/** The whole app as one Compose tree, as Android draws it. iOS hosts each screen in SwiftUI instead (IosShell). */
@Composable
fun SpijunApp(shell: AppShell = remember { AppShell() }) {
    val controller = shell.app
    val dictionaries = shell.dictionaries
    val tab = shell.tab
    val language = Locale.current.language
    val storage = rememberTextStorage()

    LaunchedEffect(Unit) { shell.run(storage, language) }

    SpyTheme(haptics = rememberSpyHaptics(enabled = true), reduceMotion = rememberReduceMotion()) {
        val inGame = controller.route != Route.Home
        val showTabs = !inGame && when (tab) {
            Tab.Play -> true
            Tab.Dictionaries -> dictionaries.route == DictRoute.List && dictionaries.sheet !is DictSheet.Qr
        }

        Column(Modifier.fillMaxSize().background(Spy.colors.bg)) {
            Box(Modifier.weight(1f).consumeWindowInsets(if (showTabs) WindowInsets.navigationBars else WindowInsets(0))) {
                when (controller.route) {
                    Route.Home -> when (tab) {
                        Tab.Play -> HomeScreen(onOnePhone = controller::openSetup)
                        Tab.Dictionaries -> DictionariesScreen(dictionaries, controller.dictionaries.orEmpty())
                    }
                    Route.Setup -> SetupScreen(
                        controller,
                        onBack = controller::goHome,
                        onAddWords = shell::addWordsFromSetup,
                        onStart = { controller.startGame(it) },
                    )
                    Route.Game -> controller.game?.let { GameScreen(it, controller) }
                }
            }
            if (showTabs) {
                SpyTabBar(
                    items = listOf(
                        TabItem(Tab.Play, stringResource(Res.string.tab_play), SpyIcons.TabPlay, SpyIcons.TabPlaySelected),
                        TabItem(Tab.Dictionaries, stringResource(Res.string.tab_dictionaries), SpyIcons.TabDictionaries),
                    ),
                    selected = tab,
                    onSelect = { shell.tab = it },
                )
            }
        }
    }
}
