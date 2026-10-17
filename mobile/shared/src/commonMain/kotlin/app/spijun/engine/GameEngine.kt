package app.spijun.engine

import kotlin.random.Random

/**
 * Pure game rules for one-phone play. Every function takes a [GameState] and returns the next
 * one; nothing here knows about UI, time or storage. Calling a function in the wrong stage
 * throws [IllegalStateException] so UI bugs surface in tests instead of as silent no-ops.
 */
object GameEngine {

    fun newGame(config: GameConfig, pool: List<Word>, random: Random = Random): GameState {
        require(config.players.size in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS) { "players" }
        require(config.spyCount in 1..Rules.maxSpies(config.players.size)) { "spies" }
        require(pool.isNotEmpty()) { "empty word pool" }
        val chosen = pool.random(random)
        val spies = config.players.map { it.id }.shuffled(random).take(config.spyCount).toSet()
        return GameState(config = config, word = chosen.text, category = chosen.category, spyIds = spies)
    }

    /** Same settings, new word, new roles. */
    fun playAgain(state: GameState, pool: List<Word>, random: Random = Random): GameState =
        newGame(state.config, pool, random)

    /** The player at the current seat has looked and hidden their card; pass the phone on. */
    fun finishReveal(state: GameState): GameState {
        val stage = state.stage as? Stage.Reveal ?: error("not revealing")
        val next = stage.seat + 1
        return state.copy(stage = if (next < state.players.size) Stage.Reveal(next) else Stage.Discussion)
    }

    /** In-app voting: discussion ends (host tap or timer) and the phone starts going round. */
    fun openVote(state: GameState): GameState {
        check(state.stage == Stage.Discussion) { "not in discussion" }
        check(state.config.voting == VotingMode.InApp) { "voting is out loud" }
        val alive = state.alive.map { it.id }
        return state.copy(stage = Stage.Voting(candidates = alive, voters = alive))
    }

    fun castVote(state: GameState, targetId: Int): GameState {
        val stage = state.stage as? Stage.Voting ?: error("not voting")
        val voter = stage.currentVoter ?: error("everyone has voted")
        require(targetId in stage.candidates) { "not a candidate" }
        require(targetId != voter) { "cannot vote for yourself" }
        val updated = stage.copy(ballots = stage.ballots + (voter to targetId))
        return if (updated.currentVoter == null) tally(state, updated) else state.copy(stage = updated)
    }

    /** Out-loud mode: the host marks who the group knocked out, or null for "nobody". */
    fun knockOutLoud(state: GameState, playerId: Int?): GameState {
        check(state.stage == Stage.Discussion) { "not in discussion" }
        check(state.config.voting == VotingMode.OutLoud) { "voting is in the app" }
        if (playerId == null) {
            return state.copy(round = state.round + 1, history = state.history + RoundRecord(state.round, null))
        }
        require(playerId in state.alive.map { it.id }) { "not in the game" }
        return applyKnockOut(state, playerId, tally = emptyList(), isRevote = false)
    }

    /** From the result screen: re-vote after a tie, or on to the next round. */
    fun continueAfterResult(state: GameState): GameState {
        val stage = state.stage as? Stage.Result ?: error("no result")
        return when (val outcome = stage.outcome) {
            is Outcome.Tie -> {
                val alive = state.alive.map { it.id }
                state.copy(stage = Stage.Voting(candidates = outcome.playerIds, voters = alive, isRevote = true))
            }
            is Outcome.SecondTie, is Outcome.KnockedOut -> nextRound(state)
        }
    }

    private fun nextRound(state: GameState): GameState =
        state.copy(round = state.round + 1, stage = Stage.Discussion)

    private fun tally(state: GameState, voting: Stage.Voting): GameState {
        val counts = voting.candidates.map { id -> VoteCount(id, voting.ballots.values.count { it == id }) }
            .sortedByDescending { it.votes }
        val top = counts.first().votes
        val leaders = counts.filter { it.votes == top }.map { it.playerId }
        return when {
            leaders.size == 1 -> applyKnockOut(state, leaders.first(), counts, voting.isRevote)
            voting.isRevote -> finishRound(state, null, Stage.Result(counts, Outcome.SecondTie(leaders), true))
            else -> state.copy(stage = Stage.Result(counts, Outcome.Tie(leaders), false))
        }
    }

    private fun applyKnockOut(state: GameState, playerId: Int, tally: List<VoteCount>, isRevote: Boolean): GameState {
        val out = state.copy(
            eliminated = state.eliminated + playerId,
            history = state.history + RoundRecord(state.round, playerId),
        )
        val winner = winnerOf(out)
        // The result screen is skipped when the knock-out ends the game.
        val stage = if (winner != null) Stage.GameOver(winner)
        else Stage.Result(tally, Outcome.KnockedOut(playerId), isRevote)
        return out.copy(stage = stage)
    }

    private fun finishRound(state: GameState, knockedOut: Int?, stage: Stage): GameState =
        state.copy(history = state.history + RoundRecord(state.round, knockedOut), stage = stage)

    fun winnerOf(state: GameState): Winner? = when {
        state.aliveSpies == 0 -> Winner.Citizens
        state.aliveSpies >= state.aliveCitizens -> Winner.Spies
        else -> null
    }
}
