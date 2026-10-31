package app.spijun.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.spijun.dictionaries.CustomDictionary
import app.spijun.dictionaries.SetupPicks
import app.spijun.dictionaries.SetupSelection
import app.spijun.dictionaries.SetupSource
import app.spijun.dictionaries.asSetupSource
import app.spijun.engine.GameConfig
import app.spijun.engine.GameEngine
import app.spijun.engine.GameState
import app.spijun.engine.Player
import app.spijun.engine.Rules
import app.spijun.engine.SetupProblem
import app.spijun.engine.SetupValidation
import app.spijun.engine.Stage
import app.spijun.engine.VotingMode
import app.spijun.engine.Word
import app.spijun.ui.data.Dictionary
import app.spijun.ui.data.asSetupSource

enum class Route { Home, Setup, Game }

/** How a screen-reader user chose to see their role, asked once before the first reveal. */
enum class ScreenReaderReveal { Aloud, Silent }

data class SetupState(
    val players: Int = 4,
    val spies: Int = 1,
    /** Typed names by seat; blank means "use the default name" (only allowed when voting out loud). */
    val names: List<String> = List(Rules.MAX_PLAYERS) { "" },
    val timerSeconds: Int? = 300,
    val voting: VotingMode = VotingMode.InApp,
    val dictionaries: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
)

@Stable
class AppController {
    var route by mutableStateOf(Route.Home)
        private set
    var setup by mutableStateOf(SetupState())
        private set
    var dictionaries by mutableStateOf<List<Dictionary>?>(null)
        private set
    /** The player's own dictionaries, once they have been read from the phone; empty before. */
    var customs by mutableStateOf<List<CustomDictionary>>(emptyList())
        private set
    /** Built-ins, then customs, as setup offers them. Null while the built-ins are loading. */
    var sources by mutableStateOf<List<SetupSource>?>(null)
        private set
    var game by mutableStateOf<GameState?>(null)
        private set
    /** Null until a screen-reader user has answered `a11y_sr_warning` (once per app run). */
    var screenReaderReveal by mutableStateOf<ScreenReaderReveal?>(null)

    private val picks get() = SetupPicks(setup.dictionaries, setup.categories)
    private val offered get() = sources.orEmpty()

    fun onDictionariesLoaded(list: List<Dictionary>, preferredId: String) {
        dictionaries = list
        refreshSources()
        val initial = list.firstOrNull { it.id == preferredId } ?: list.first()
        setup = setup.copy(
            dictionaries = setOf(initial.id),
            categories = initial.categories.keys.map { SetupSelection.key(initial.id, it) }.toSet(),
        )
    }

    /**
     * The Rječnici tab changed the player's dictionaries. Anything ticked that is gone, or was
     * renamed, leaves the selection here instead of lingering as a key nothing matches.
     */
    fun onCustomsChanged(list: List<CustomDictionary>) {
        customs = list
        val before = offered
        refreshSources()
        val kept = SetupSelection.reconcile(before, offered, picks)
        if (kept != picks) setup = setup.copy(dictionaries = kept.dictionaries, categories = kept.categories)
    }

    private fun refreshSources() {
        sources = dictionaries?.let { built -> built.map { it.asSetupSource() } + customs.map { it.asSetupSource() } }
    }

    fun update(transform: (SetupState) -> SetupState) {
        val next = transform(setup)
        setup = next.copy(spies = Rules.clampSpies(next.spies, next.players))
    }

    private fun pick(change: (SetupPicks) -> SetupPicks) = update { s ->
        val next = change(SetupPicks(s.dictionaries, s.categories))
        s.copy(dictionaries = next.dictionaries, categories = next.categories)
    }

    fun toggleDictionary(id: String) = pick { SetupSelection.toggleDictionary(offered, it, id) }

    fun toggleCategory(key: String) = pick { SetupSelection.toggleCategory(it, key) }

    fun setAllCategories(selected: Boolean) = pick { SetupSelection.setAll(offered, it, selected) }

    // Setup reads these on every recomposition (each keystroke in a name), so they are computed
    // only when the dictionaries or the picks change, not on every read.
    private val poolState = derivedStateOf { SetupSelection.pool(offered, picks) }
    private val categoryCountState = derivedStateOf { SetupSelection.categoryCount(offered, picks) }
    private val allSelectedState = derivedStateOf { SetupSelection.allSelected(offered, picks) }
    private val toFillState = derivedStateOf { SetupSelection.withEmptyPick(offered, picks) }

    fun allCategoriesSelected(): Boolean = allSelectedState.value

    /** Words from every selected category of every selected dictionary. */
    fun pool(): List<Word> = poolState.value

    fun selectedCategoryCount(): Int = categoryCountState.value

    /** Selected dictionaries where setup offers "add words" because a selected category is empty. */
    fun dictionariesToFill(): Set<String> = toFillState.value

    fun problem(): SetupProblem? = SetupValidation.check(
        setup.players, setup.spies, setup.voting, setup.names, selectedCategoryCount(), pool().size,
    )

    fun openSetup() { route = Route.Setup }
    fun goHome() { route = Route.Home; game = null }

    /** [defaultName] fills blanks; it only matters when voting out loud. */
    fun startGame(defaultName: (Int) -> String) {
        if (problem() != null) return
        val players = List(setup.players) { i ->
            Player(i, setup.names[i].trim().ifEmpty { defaultName(i + 1) })
        }
        val config = GameConfig(players, setup.spies, setup.timerSeconds, setup.voting)
        game = GameEngine.newGame(config, pool())
        route = Route.Game
    }

    // Every action is a no-op unless the game is still where the caller saw it: a double tap, or a
    // tap on a screen that is fading out, must not reach the engine (it throws on wrong-stage calls)
    // or act twice (a second ballot for the next voter, a second "nobody is out").
    fun playAgain() = applyIn<Stage.GameOver> { g, _ -> GameEngine.playAgain(g, pool()) }
    fun finishReveal(seat: Int) = applyIn<Stage.Reveal> { g, s -> if (s.seat == seat) GameEngine.finishReveal(g) else g }
    fun openVote() = applyIn<Stage.Discussion> { g, _ -> GameEngine.openVote(g) }
    fun castVote(voter: Int, target: Int) =
        applyIn<Stage.Voting> { g, s -> if (s.currentVoter == voter) GameEngine.castVote(g, target) else g }
    fun knockOutLoud(round: Int, playerId: Int?) =
        applyIn<Stage.Discussion> { g, _ -> if (g.round == round) GameEngine.knockOutLoud(g, playerId) else g }
    fun continueAfterResult() = applyIn<Stage.Result> { g, _ -> GameEngine.continueAfterResult(g) }

    private inline fun <reified S : Stage> applyIn(step: (GameState, S) -> GameState) {
        val g = game ?: return
        val stage = g.stage as? S ?: return
        game = step(g, stage)
    }
}
