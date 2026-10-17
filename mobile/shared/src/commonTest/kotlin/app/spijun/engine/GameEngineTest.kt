package app.spijun.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineTest {
    private val pool = listOf(Word("Plaža", "Mjesta"), Word("Liječnik", "Zanimanja"), Word("Čekić", "Predmeti"))

    private fun config(n: Int = 5, spies: Int = 1, voting: VotingMode = VotingMode.InApp, timer: Int? = 300) =
        GameConfig(List(n) { Player(it, "P$it") }, spies, timer, voting)

    /** Rigs the roles so tests don't depend on the shuffle. */
    private fun game(n: Int = 5, spyIds: Set<Int> = setOf(0), voting: VotingMode = VotingMode.InApp): GameState {
        val base = GameEngine.newGame(config(n, spyIds.size, voting), pool, Random(1))
        return base.copy(spyIds = spyIds, stage = Stage.Discussion)
    }

    private fun GameState.vote(vararg targets: Int): GameState {
        var s = this
        for (t in targets) s = GameEngine.castVote(s, t)
        return s
    }

    @Test
    fun maxSpiesFollowsWinCondition() {
        assertEquals(listOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5), (3..12).map { Rules.maxSpies(it) })
        for (n in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS) {
            val spies = Rules.maxSpies(n)
            assertTrue(spies < n - spies, "game must not be over before it starts at n=$n")
        }
    }

    @Test
    fun newGameAssignsRequestedSpiesAndWordFromPool() {
        repeat(50) { seed ->
            val s = GameEngine.newGame(config(8, 3), pool, Random(seed))
            assertEquals(3, s.spyIds.size)
            assertTrue(Word(s.word, s.category) in pool)
            assertEquals(Stage.Reveal(0), s.stage)
        }
    }

    @Test
    fun spiesSeeEachOtherButNotThemselves() {
        val s = game(6, setOf(1, 4))
        assertEquals(listOf(4), s.otherSpiesOf(1).map { it.id })
        assertEquals(listOf(1, 4), s.otherSpiesOf(0).map { it.id })
    }

    @Test
    fun newGameRejectsBadInput() {
        assertFailsWith<IllegalArgumentException> { GameEngine.newGame(config(2), pool) }
        assertFailsWith<IllegalArgumentException> { GameEngine.newGame(config(4, 2), pool) }
        assertFailsWith<IllegalArgumentException> { GameEngine.newGame(config(4), emptyList()) }
    }

    @Test
    fun revealPassesEverySeatThenStartsDiscussion() {
        var s = GameEngine.newGame(config(3), pool, Random(2))
        assertEquals(Stage.Reveal(0), s.stage)
        s = GameEngine.finishReveal(s)
        assertEquals(Stage.Reveal(1), s.stage)
        s = GameEngine.finishReveal(s)
        assertEquals(Stage.Reveal(2), s.stage)
        s = GameEngine.finishReveal(s)
        assertEquals(Stage.Discussion, s.stage)
    }

    @Test
    fun votingIsSequentialAndSecretUntilTheLastBallot() {
        var s = GameEngine.openVote(game(4, setOf(0)))
        val voting = s.stage as Stage.Voting
        assertEquals(listOf(0, 1, 2, 3), voting.voters)
        s = GameEngine.castVote(s, 1)
        assertIs<Stage.Voting>(s.stage)
        assertEquals(1, (s.stage as Stage.Voting).currentVoter)
    }

    @Test
    fun cannotVoteForYourselfOrEliminatedPlayers() {
        val s = GameEngine.openVote(game(5, setOf(0)))
        assertFailsWith<IllegalArgumentException> { GameEngine.castVote(s, 0) }
        val afterOut = GameEngine.openVote(
            GameEngine.continueAfterResult(game(6, setOf(0, 1)).vote5Out(5))
        )
        assertFailsWith<IllegalArgumentException> { GameEngine.castVote(afterOut, 5) }
        assertEquals(listOf(0, 1, 2, 3, 4), (afterOut.stage as Stage.Voting).voters)
    }

    /** Everyone alive votes for [target] (the target votes for someone else). */
    private fun GameState.vote5Out(target: Int): GameState {
        var s = GameEngine.openVote(this)
        val voters = (s.stage as Stage.Voting).voters
        for (v in voters) s = GameEngine.castVote(s, if (v == target) voters.first { it != target } else target)
        return s
    }

    @Test
    fun majorityKnocksOutOnePlayerAndRolesStayHidden() {
        val s = game(6, setOf(0, 1)).vote5Out(5)
        val result = s.stage as Stage.Result
        assertEquals(Outcome.KnockedOut(5), result.outcome)
        assertEquals(5, result.tally.first().playerId)
        assertEquals(listOf(5), s.eliminated)
        assertEquals(listOf(RoundRecord(1, 5)), s.history)
        val next = GameEngine.continueAfterResult(s)
        assertEquals(Stage.Discussion, next.stage)
        assertEquals(2, next.round)
    }

    @Test
    fun tieTriggersRevoteBetweenTiedPlayersOnly() {
        // 4 players, 2-2 split between players 2 and 3.
        var s = GameEngine.openVote(game(4, setOf(0)))
        s = s.vote(2, 3, 3, 2)
        val tie = (s.stage as Stage.Result).outcome
        assertEquals(Outcome.Tie(listOf(2, 3)), tie)
        assertTrue(s.eliminated.isEmpty())

        s = GameEngine.continueAfterResult(s)
        val voting = s.stage as Stage.Voting
        assertTrue(voting.isRevote)
        assertEquals(listOf(2, 3), voting.candidates)
        assertEquals(listOf(0, 1, 2, 3), voting.voters)
        assertFailsWith<IllegalArgumentException> { GameEngine.castVote(s, 1) }
    }

    @Test
    fun secondTieKnocksNobodyOut() {
        var s = GameEngine.openVote(game(4, setOf(0)))
        s = s.vote(2, 3, 3, 2)
        s = GameEngine.continueAfterResult(s)
        // voters 0,1 split; 2 must pick 3; 3 must pick 2 -> 2 votes each.
        s = s.vote(2, 3, 3, 2)
        val result = s.stage as Stage.Result
        assertEquals(Outcome.SecondTie(listOf(2, 3)), result.outcome)
        assertTrue(s.eliminated.isEmpty())
        val next = GameEngine.continueAfterResult(s)
        assertEquals(Stage.Discussion, next.stage)
        assertEquals(2, next.round)
        assertEquals(RoundRecord(1, null), next.history.last())
    }

    @Test
    fun citizensWinWhenLastSpyIsOut() {
        val s = game(5, setOf(4)).vote5Out(4)
        assertEquals(Stage.GameOver(Winner.Citizens), s.stage)
    }

    @Test
    fun spiesWinWhenTheyEqualRemainingCitizens() {
        // 5 players, 2 spies. Knock out a citizen -> 2 spies vs 2 citizens.
        val s = game(5, setOf(0, 1)).vote5Out(4)
        assertEquals(Stage.GameOver(Winner.Spies), s.stage)
    }

    @Test
    fun gameContinuesWhileCitizensOutnumberSpies() {
        // 7 players, 2 spies. Citizen out -> 2 vs 4: continues.
        val s = game(7, setOf(0, 1)).vote5Out(6)
        assertIs<Stage.Result>(s.stage)
        assertNull(GameEngine.winnerOf(s))
    }

    @Test
    fun outLoudKnockOutAndNobodyOut() {
        var s = game(7, setOf(0, 1), VotingMode.OutLoud)
        assertFailsWith<IllegalStateException> { GameEngine.openVote(s) }
        s = GameEngine.knockOutLoud(s, null)
        assertEquals(2, s.round)
        assertEquals(Stage.Discussion, s.stage)
        s = GameEngine.knockOutLoud(s, 6)
        val result = s.stage as Stage.Result
        assertEquals(Outcome.KnockedOut(6), result.outcome)
        assertTrue(result.tally.isEmpty())
        assertEquals(listOf(RoundRecord(1, null), RoundRecord(2, 6)), s.history)
    }

    @Test
    fun toStringKeepsTheSecretOut() {
        val s = game(5, setOf(3))
        val text = s.toString()
        assertTrue(s.word !in text, text)
        assertTrue("spy" !in text.lowercase(), text)
    }

    @Test
    fun playAgainKeepsSettingsAndResetsState() {
        val over = game(5, setOf(4)).vote5Out(4)
        val again = GameEngine.playAgain(over, pool, Random(9))
        assertEquals(over.config, again.config)
        assertEquals(Stage.Reveal(0), again.stage)
        assertEquals(1, again.round)
        assertTrue(again.eliminated.isEmpty() && again.history.isEmpty())
    }

    @Test
    fun setupValidation() {
        fun check(players: Int = 4, spies: Int = 1, v: VotingMode = VotingMode.OutLoud, names: List<String> = emptyList(),
                  cats: Int = 1, pool: Int = 10) = SetupValidation.check(players, spies, v, names, cats, pool)
        assertNull(check())
        assertEquals(SetupProblem.PlayerCount, check(players = 2))
        assertEquals(SetupProblem.SpyCount, check(players = 4, spies = 2))
        assertEquals(SetupProblem.NoCategories, check(cats = 0))
        assertEquals(SetupProblem.EmptyPool, check(pool = 0))
        assertEquals(SetupProblem.MissingNames, check(v = VotingMode.InApp, names = listOf("A", "B", "C", " ")))
        assertEquals(SetupProblem.DuplicateNames, check(v = VotingMode.InApp, names = listOf("A", "b", "B", "D")))
        assertNull(check(v = VotingMode.InApp, names = listOf("A", "B", "C", "D")))
    }

    @Test
    fun tensionThresholds() {
        assertEquals(Tension.Calm, tensionFor(null))
        assertEquals(Tension.Calm, tensionFor(31))
        assertEquals(Tension.Uneasy, tensionFor(30))
        assertEquals(Tension.Nervous, tensionFor(10))
        assertEquals(Tension.Frozen, tensionFor(0))
    }
}
