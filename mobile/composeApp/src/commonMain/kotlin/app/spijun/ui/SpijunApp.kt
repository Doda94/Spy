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
import app.spijun.dictionaries.CustomDictionary
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
import app.spijun.ui.platform.TextStorage
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

/** An unusable library is moved aside under this name (then "-2", "-3"…), never deleted. */
private const val DAMAGED_PREFIX = "dictionaries.damaged"

private fun damagedName(n: Int) = if (n == 1) "$DAMAGED_PREFIX.json" else "$DAMAGED_PREFIX-$n.json"

/**
 * The player's saved dictionaries. A file that exists but can't be used (unreadable, damaged or
 * from a newer app) is first moved aside, so the next save can't replace it. Null when even that
 * failed: then nothing may be written this session.
 */
private suspend fun readLibrary(storage: TextStorage): List<CustomDictionary>? {
    val list = try {
        val text = storage.read(LIBRARY_FILE) ?: return emptyList()
        DictionaryLibrary.decode(text)
    } catch (_: Exception) {
        null
    }
    if (list != null) return list
    return try {
        var n = 1
        while (storage.exists(damagedName(n))) n++
        storage.rename(LIBRARY_FILE, damagedName(n))
        emptyList()
    } catch (_: Exception) {
        null
    }
}

@Composable
fun SpijunApp(
    controller: AppController = remember { AppController() },
    dictionaries: DictionariesController = remember { DictionariesController() },
) {
    val language = Locale.current.language
    var tab by remember { mutableStateOf(Tab.Play) }
    val storage = rememberTextStorage()

    LaunchedEffect(Unit) {
        if (controller.dictionaries == null) {
            controller.onDictionariesLoaded(loadBuiltInDictionaries(), preferredId = if (language == "en") "en" else "hr")
        }
    }

    // Load the player's own dictionaries once, then write the file after every change.
    LaunchedEffect(Unit) {
        if (!dictionaries.loaded) {
            // Unusable and stuck in place: `loaded` stays false, so nothing can be edited or saved over it.
            val list = readLibrary(storage) ?: return@LaunchedEffect
            dictionaries.onLoaded(list)
        }
        snapshotFlow { dictionaries.customs }.drop(1).collect { customs ->
            try {
                withContext(Dispatchers.Default) { storage.write(LIBRARY_FILE, DictionaryLibrary.encode(customs)) }
            } catch (_: Exception) {
                // Next change retries with the full list; nothing to tell the player mid-edit.
            }
        }
    }

    // Setup offers the player's dictionaries too, and drops ticked ones that were deleted or renamed.
    LaunchedEffect(Unit) {
        snapshotFlow { dictionaries.customs }.collect { controller.onCustomsChanged(it) }
    }

    // A dictionary link opens the import preview, but only from the menu screens, never over a game.
    val link = IncomingLinks.pending
    LaunchedEffect(link, dictionaries.loaded, controller.route) {
        if (link != null && dictionaries.loaded && controller.route == Route.Home) {
            tab = Tab.Dictionaries
            dictionaries.beginImport(link)
            IncomingLinks.consume()
        }
    }

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
                        onAddWords = { id ->
                            controller.goHome()
                            dictionaries.show(id)
                            tab = Tab.Dictionaries
                        },
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
                    onSelect = { tab = it },
                )
            }
        }
    }
}
