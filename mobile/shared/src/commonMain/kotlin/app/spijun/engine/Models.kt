package app.spijun.engine

import kotlinx.serialization.Serializable

@Serializable
data class Word(val text: String, val category: String)

@Serializable
data class Player(val id: Int, val name: String)

enum class VotingMode { InApp, OutLoud }

enum class Winner { Citizens, Spies }

enum class Role { Citizen, Spy }

@Serializable
data class GameConfig(
    /** Seat order: the table is laid out, and the phone is passed, in this order. */
    val players: List<Player>,
    val spyCount: Int,
    /** Discussion length per round; null means no timer. */
    val timerSeconds: Int?,
    val voting: VotingMode,
)

@Serializable
data class VoteCount(val playerId: Int, val votes: Int)

@Serializable
sealed interface Outcome {
    /** The group knocked this player out. Their role stays hidden until game over. */
    @Serializable
    data class KnockedOut(val playerId: Int) : Outcome

    /** First tie: re-vote, only between these players. */
    @Serializable
    data class Tie(val playerIds: List<Int>) : Outcome

    /** The re-vote tied again: nobody is knocked out. */
    @Serializable
    data class SecondTie(val playerIds: List<Int>) : Outcome
}

@Serializable
sealed interface Stage {
    /** [seat] is the index (into the player list) of the player who is looking now. */
    @Serializable
    data class Reveal(val seat: Int) : Stage

    @Serializable
    data object Discussion : Stage

    /**
     * One-phone voting: the phone is passed between [voters] in seat order. [ballots] holds
     * the votes cast so far (voter id → target id) and stays secret until everyone has voted.
     */
    @Serializable
    data class Voting(
        val candidates: List<Int>,
        val voters: List<Int>,
        val ballots: Map<Int, Int> = emptyMap(),
        val isRevote: Boolean = false,
    ) : Stage {
        val currentVoter: Int? get() = voters.getOrNull(ballots.size)
    }

    @Serializable
    data class Result(val tally: List<VoteCount>, val outcome: Outcome, val isRevote: Boolean) : Stage

    @Serializable
    data class GameOver(val winner: Winner) : Stage
}

/** One line of the game-over recap. [knockedOut] is null when a round ended with nobody out. */
@Serializable
data class RoundRecord(val round: Int, val knockedOut: Int?)

@Serializable
data class GameState(
    val config: GameConfig,
    val word: String,
    val category: String,
    val spyIds: Set<Int>,
    val eliminated: List<Int> = emptyList(),
    val round: Int = 1,
    val stage: Stage = Stage.Reveal(0),
    val history: List<RoundRecord> = emptyList(),
) {
    val players: List<Player> get() = config.players
    val alive: List<Player> get() = players.filter { it.id !in eliminated }
    val aliveSpies: Int get() = alive.count { it.id in spyIds }
    val aliveCitizens: Int get() = alive.size - aliveSpies

    fun player(id: Int): Player = players.first { it.id == id }
    fun roleOf(id: Int): Role = if (id in spyIds) Role.Spy else Role.Citizen
    fun otherSpiesOf(id: Int): List<Player> = players.filter { it.id in spyIds && it.id != id }
}
