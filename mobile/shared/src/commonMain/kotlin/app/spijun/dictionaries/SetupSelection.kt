package app.spijun.dictionaries

import app.spijun.engine.Word

/** A category as the setup screen sees it, built-in or custom alike. */
data class SetupCategory(val name: String, val words: List<String>)

/** A dictionary offered in setup: the built-ins first, then the player's own. */
data class SetupSource(val id: String, val categories: List<SetupCategory>)

/**
 * What the player has ticked in setup. [categories] holds [SetupSelection.key]s and only of
 * dictionaries that are in [dictionaries].
 */
data class SetupPicks(
    val dictionaries: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
)

fun CustomDictionary.asSetupSource() = SetupSource(id, categories.map { SetupCategory(it.name, it.words) })

/** The pure part of the setup picker, so it can be tested without a screen. */
object SetupSelection {
    /** The same category name may exist in several dictionaries. */
    fun key(dictId: String, category: String) = "$dictId/$category"

    private fun SetupSource.keys(): Set<String> = categories.map { key(id, it.name) }.toSet()

    /** Ticks a dictionary together with all its categories, or unticks it with them. */
    fun toggleDictionary(sources: List<SetupSource>, picks: SetupPicks, id: String): SetupPicks {
        val source = sources.firstOrNull { it.id == id } ?: return picks
        return if (id in picks.dictionaries) {
            SetupPicks(picks.dictionaries - id, picks.categories - source.keys())
        } else {
            SetupPicks(picks.dictionaries + id, picks.categories + source.keys())
        }
    }

    fun toggleCategory(picks: SetupPicks, key: String): SetupPicks =
        picks.copy(categories = if (key in picks.categories) picks.categories - key else picks.categories + key)

    /** "Odaberi sve": every category of the ticked dictionaries, or none of them. */
    fun setAll(sources: List<SetupSource>, picks: SetupPicks, selected: Boolean): SetupPicks {
        val all = sources.filter { it.id in picks.dictionaries }.flatMap { it.keys() }.toSet()
        return picks.copy(categories = if (selected) picks.categories + all else picks.categories - all)
    }

    fun allSelected(sources: List<SetupSource>, picks: SetupPicks): Boolean =
        sources.filter { it.id in picks.dictionaries }.all { picks.categories.containsAll(it.keys()) }

    /**
     * Drops what no longer exists: a deleted dictionary, a deleted category, or a renamed one
     * (the name is part of the key, so the new name starts unticked).
     */
    fun reconcile(sources: List<SetupSource>, picks: SetupPicks): SetupPicks {
        val alive = sources.filter { it.id in picks.dictionaries }
        val keys = alive.flatMap { it.keys() }.toSet()
        val next = SetupPicks(alive.map { it.id }.toSet(), picks.categories.filter { it in keys }.toSet())
        return if (next == picks) picks else next
    }

    /** Words from every ticked category of every ticked dictionary. Empty categories add none. */
    fun pool(sources: List<SetupSource>, picks: SetupPicks): List<Word> =
        sources.filter { it.id in picks.dictionaries }.flatMap { source ->
            source.categories.filter { key(source.id, it.name) in picks.categories }
                .flatMap { c -> c.words.map { Word(it, c.name) } }
        }

    fun categoryCount(sources: List<SetupSource>, picks: SetupPicks): Int =
        sources.filter { it.id in picks.dictionaries }
            .sumOf { s -> s.categories.count { key(s.id, it.name) in picks.categories } }

    /** Ticked dictionaries with a ticked category that has no words: each needs a way to add some. */
    fun withEmptyPick(sources: List<SetupSource>, picks: SetupPicks): Set<String> =
        sources.filter { it.id in picks.dictionaries }.filter { s ->
            s.categories.any { it.words.isEmpty() && key(s.id, it.name) in picks.categories }
        }.map { it.id }.toSet()
}
