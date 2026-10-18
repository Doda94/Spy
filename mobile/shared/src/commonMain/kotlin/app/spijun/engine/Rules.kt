package app.spijun.engine

object Rules {
    const val MIN_PLAYERS = 3
    const val MAX_PLAYERS = 12
    const val MAX_NAME_LENGTH = 16

    /**
     * Open issue decisions.md §12-1: the design allowed players − 1 spies, but spies win as
     * soon as they equal the citizens, so that would end the game before it starts.
     * The product owner chose ⌊(players − 1) / 2⌋. Change it here and only here.
     */
    fun maxSpies(players: Int): Int = ((players - 1) / 2).coerceAtLeast(1)

    fun clampSpies(spies: Int, players: Int): Int = spies.coerceIn(1, maxSpies(players))

    const val TIMER_EXTRA_SECONDS = 60
    const val WARNING_SECONDS = 30
    const val NERVOUS_SECONDS = 10
}

enum class SetupProblem { NoCategories, EmptyPool, MissingNames, DuplicateNames, PlayerCount, SpyCount }

object SetupValidation {
    /** Returns the first problem that blocks starting, or null when the setup is playable. */
    fun check(
        playerCount: Int,
        spyCount: Int,
        voting: VotingMode,
        names: List<String>,
        selectedCategories: Int,
        poolSize: Int,
    ): SetupProblem? {
        if (playerCount !in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS) return SetupProblem.PlayerCount
        if (spyCount !in 1..Rules.maxSpies(playerCount)) return SetupProblem.SpyCount
        if (selectedCategories == 0) return SetupProblem.NoCategories
        if (poolSize == 0) return SetupProblem.EmptyPool
        if (voting == VotingMode.InApp) {
            val trimmed = names.take(playerCount).map { it.trim() }
            if (trimmed.size < playerCount || trimmed.any { it.isEmpty() }) return SetupProblem.MissingNames
            if (trimmed.map { it.lowercase() }.toSet().size != trimmed.size) return SetupProblem.DuplicateNames
        }
        return null
    }
}

enum class Tension { Calm, Uneasy, Nervous, Frozen }

/** Discussion tension for the table (motion.md §3). A null [remainingSeconds] means no timer. */
fun tensionFor(remainingSeconds: Int?): Tension = when {
    remainingSeconds == null -> Tension.Calm
    remainingSeconds <= 0 -> Tension.Frozen
    remainingSeconds <= Rules.NERVOUS_SECONDS -> Tension.Nervous
    remainingSeconds <= Rules.WARNING_SECONDS -> Tension.Uneasy
    else -> Tension.Calm
}
