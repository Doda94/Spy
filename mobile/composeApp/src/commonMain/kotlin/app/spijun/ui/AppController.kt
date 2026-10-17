package app.spijun.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import app.spijun.ui.data.categoryKey

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
    var game by mutableStateOf<GameState?>(null)
        private set
    /** Null until a screen-reader user has answered `a11y_sr_warning` (once per app run). */
    var screenReaderReveal by mutableStateOf<ScreenReaderReveal?>(null)

    fun onDictionariesLoaded(list: List<Dictionary>, preferredId: String) {
        dictionaries = list
        val initial = list.firstOrNull { it.id == preferredId } ?: list.first()
        setup = setup.copy(
            dictionaries = setOf(initial.id),
            categories = initial.categories.keys.map { categoryKey(initial.id, it) }.toSet(),
        )
    }

    fun update(transform: (SetupState) -> SetupState) {
        val next = transform(setup)
        setup = next.copy(spies = Rules.clampSpies(next.spies, next.players))
    }

    fun toggleDictionary(id: String) {
        val dict = dictionaries?.firstOrNull { it.id == id } ?: return
        update { s ->
            val keys = dict.categories.keys.map { categoryKey(id, it) }.toSet()
            if (id in s.dictionaries) s.copy(dictionaries = s.dictionaries - id, categories = s.categories - keys)
            else s.copy(dictionaries = s.dictionaries + id, categories = s.categories + keys)
        }
    }

    fun toggleCategory(key: String) = update { s ->
        s.copy(categories = if (key in s.categories) s.categories - key else s.categories + key)
    }

    fun setAllCategories(selected: Boolean) = update { s ->
        val all = dictionaries.orEmpty().filter { it.id in s.dictionaries }
            .flatMap { d -> d.categories.keys.map { categoryKey(d.id, it) } }.toSet()
        s.copy(categories = if (selected) s.categories + all else s.categories - all)
    }

    /** Words from every selected category of every selected dictionary. */
    fun pool(): List<Word> = dictionaries.orEmpty().filter { it.id in setup.dictionaries }.flatMap { dict ->
        dict.categories.keys.filter { categoryKey(dict.id, it) in setup.categories }.flatMap { dict.words(it) }
    }

    fun selectedCategoryCount(): Int = dictionaries.orEmpty().filter { it.id in setup.dictionaries }
        .sumOf { d -> d.categories.keys.count { categoryKey(d.id, it) in setup.categories } }

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
