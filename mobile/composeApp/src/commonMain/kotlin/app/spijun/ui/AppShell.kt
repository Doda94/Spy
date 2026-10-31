package app.spijun.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import app.spijun.dictionaries.CustomDictionary
import app.spijun.dictionaries.DictionaryLibrary
import app.spijun.ui.data.loadBuiltInDictionaries
import app.spijun.ui.dictionaries.DictRoute
import app.spijun.ui.dictionaries.DictionariesController
import app.spijun.ui.platform.TextStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab { Play, Dictionaries }

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

/**
 * The whole app above the screens: the game, the Rječnici tab and which tab is showing. Android
 * draws it in one Compose tree ([SpijunApp]); iOS hands the tabs and stacks to SwiftUI and hosts
 * each screen on its own, so this state can't live in a composition.
 */
@Stable
class AppShell(
    val app: AppController = AppController(),
    val dictionaries: DictionariesController = DictionariesController(),
) {
    var tab by mutableStateOf(Tab.Play)

    /** Set while a dictionary opened from setup's "Dodaj riječi" is on screen; back returns to setup. */
    private var fillingFromSetup by mutableStateOf(false)

    fun addWordsFromSetup(dictId: String) {
        if (dictionaries.show(dictId)) {
            app.goHome()
            tab = Tab.Dictionaries
            fillingFromSetup = true
        }
    }

    /** Loading, saving and the links between the tabs; runs for as long as the app does. */
    suspend fun run(storage: TextStorage, language: String) = coroutineScope {
        launch {
            if (app.dictionaries == null) {
                app.onDictionariesLoaded(loadBuiltInDictionaries(), preferredId = if (language == "en") "en" else "hr")
            }
        }

        // Load the player's own dictionaries once, then write the file after every change.
        launch {
            if (!dictionaries.loaded) {
                // Unusable and stuck in place: `loaded` stays false, so nothing can be edited or saved over it.
                val list = readLibrary(storage) ?: return@launch
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
        launch {
            snapshotFlow { dictionaries.customs }.collect { app.onCustomsChanged(it) }
        }

        // Leaving the dictionary that setup sent the player to (back to the list) goes back to setup.
        launch {
            snapshotFlow { fillingFromSetup && tab == Tab.Dictionaries && dictionaries.route == DictRoute.List }.collect { back ->
                if (back) {
                    fillingFromSetup = false
                    tab = Tab.Play
                    app.openSetup()
                }
            }
        }

        // A dictionary link opens the import preview, but only from the menu screens, never over a game.
        launch {
            snapshotFlow { Triple(IncomingLinks.pending, dictionaries.loaded, app.route) }.collect { (link, loaded, route) ->
                if (link != null && loaded && route == Route.Home) {
                    tab = Tab.Dictionaries
                    dictionaries.beginImport(link)
                    IncomingLinks.consume()
                }
            }
        }
    }
}
