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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import app.spijun.dictionaries.DictionaryLibrary
import app.spijun.resources.Res
import app.spijun.resources.tab_dictionaries
import app.spijun.resources.tab_play
import app.spijun.ui.data.loadBuiltInDictionaries
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpyTabBar
import app.spijun.ui.design.SpyTheme
import app.spijun.ui.design.TabItem
import app.spijun.ui.dictionaries.DictRoute
import app.spijun.ui.dictionaries.DictSheet
import app.spijun.ui.dictionaries.DictionariesController
import app.spijun.ui.platform.rememberReduceMotion
import app.spijun.ui.platform.rememberSpyHaptics
import app.spijun.ui.platform.rememberTextStorage
import app.spijun.ui.screens.GameScreen
import app.spijun.ui.screens.HomeScreen
import app.spijun.ui.screens.SetupScreen
import app.spijun.ui.screens.dictionaries.DictionariesScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private enum class Tab { Play, Dictionaries }

private const val LIBRARY_FILE = "dictionaries.json"

/** An unreadable library is kept under this name, so a bad read never costs the player their words. */
private const val DAMAGED_FILE = "dictionaries.damaged.json"

@Composable
fun SpijunApp(
    controller: AppController = remember { AppController() },
    dictionaries: DictionariesController = remember { DictionariesController() },
) {
    val language = Locale.current.language
    var tabRequest by remember { mutableStateOf<Tab?>(null) }
    val storage = rememberTextStorage()

    LaunchedEffect(Unit) {
        if (controller.dictionaries == null) {
            controller.onDictionariesLoaded(loadBuiltInDictionaries(), preferredId = if (language == "en") "en" else "hr")
        }
    }

    // Load the player's own dictionaries once, then write the file after every change.
    LaunchedEffect(Unit) {
        if (dictionaries.loaded) return@LaunchedEffect
        val text = try { storage.read(LIBRARY_FILE) } catch (_: Exception) { null }
        val list = text?.let { DictionaryLibrary.decode(it) }
        if (text != null && list == null) {
            try { storage.write(DAMAGED_FILE, text) } catch (_: Exception) {}
        }
        dictionaries.onLoaded(list.orEmpty())
        snapshotFlow { dictionaries.customs }.drop(1).collect { customs ->
            try {
                withContext(Dispatchers.Default) { storage.write(LIBRARY_FILE, DictionaryLibrary.encode(customs)) }
            } catch (_: Exception) {
                // Next change retries with the full list; nothing to tell the player mid-edit.
            }
        }
    }

    // A dictionary link opens the import preview, but only from the menu screens, never over a game.
    val link = IncomingLinks.pending
    LaunchedEffect(link, dictionaries.loaded, controller.route) {
        if (link != null && dictionaries.loaded && controller.route == Route.Home) {
            tabRequest = Tab.Dictionaries
            dictionaries.beginImport(link)
            IncomingLinks.consume()
        }
    }

    SpyTheme(haptics = rememberSpyHaptics(enabled = true), reduceMotion = rememberReduceMotion()) {
        var tab by remember { mutableStateOf(Tab.Play) }
        tabRequest?.let { tab = it; tabRequest = null }
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
                    Route.Setup -> SetupScreen(controller, onBack = controller::goHome, onStart = { controller.startGame(it) })
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
                    onSelect = { tab = it },
                )
            }
        }
    }
}
