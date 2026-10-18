package app.spijun.dictionaries

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DictionaryEditingTest {
    private val base = CustomDictionary(
        id = "d1",
        name = "Party pack",
        language = WordLanguage.Croatian,
        categories = listOf(
            DictionaryCategory("Filmovi", listOf("Titanic", "Shrek")),
            DictionaryCategory("Hrana", emptyList()),
        ),
    )

    private fun Edit.dictionary(): CustomDictionary = assertIs<Edit.Done>(this).dictionary
    private fun Edit.problem(): TextProblem = assertIs<Edit.Refused>(this).problem

    @Test
    fun createCleansTheNameAndStartsEmpty() {
        val done = assertIs<Edit.Done>(
            DictionaryEditing.create("x", "  Moj   rječnik ", WordLanguage.English, emptyList()),
        )
        assertEquals("Moj rječnik", done.text)
        assertEquals(CustomDictionary("x", "Moj rječnik", WordLanguage.English, emptyList()), done.dictionary)
    }

    @Test
    fun namesAreRefusedWhenEmptyTooLongOrTakenIgnoringCase() {
        assertEquals(TextProblem.Empty, DictionaryEditing.create("x", "   ", WordLanguage.English, emptyList()).problem())
        assertEquals(
            TextProblem.TooLong,
            DictionaryEditing.create("x", "a".repeat(DictionaryRules.MAX_NAME_LENGTH + 1), WordLanguage.English, emptyList()).problem(),
        )
        assertEquals(
            TextProblem.Duplicate,
            DictionaryEditing.create("x", "PARTY PACK", WordLanguage.English, listOf("Party pack")).problem(),
        )
        // Longest allowed name is fine.
        assertIs<Edit.Done>(
            DictionaryEditing.create("x", "a".repeat(DictionaryRules.MAX_NAME_LENGTH), WordLanguage.English, emptyList()),
        )
    }

    @Test
    fun renameMayKeepOnlyChangeTheCaseOfItsOwnName() {
        // otherNames excludes the dictionary itself, so "party pack" is allowed here.
        assertEquals("party pack", DictionaryEditing.rename(base, "party pack", listOf("Drugi")).dictionary().name)
        assertEquals(TextProblem.Duplicate, DictionaryEditing.rename(base, "drugi", listOf("Drugi")).problem())
    }

    @Test
    fun wordDuplicateCheckIsCaseInsensitiveAndPerCategory() {
        assertEquals(TextProblem.Duplicate, DictionaryEditing.addWord(base, "Filmovi", "  titanic ").problem())
        // The same word in another category is fine.
        val added = DictionaryEditing.addWord(base, "Hrana", "Titanic")
        assertEquals(listOf("Titanic"), added.dictionary().category("Hrana")!!.words)
    }

    @Test
    fun addWordCleansAndAppends() {
        val done = assertIs<Edit.Done>(DictionaryEditing.addWord(base, "Filmovi", "  Zov   divljine "))
        assertEquals("Zov divljine", done.text)
        assertEquals(listOf("Titanic", "Shrek", "Zov divljine"), done.dictionary.category("Filmovi")!!.words)
    }

    @Test
    fun addWordRefusesEmptyAndTooLong() {
        assertEquals(TextProblem.Empty, DictionaryEditing.addWord(base, "Filmovi", " ").problem())
        assertEquals(
            TextProblem.TooLong,
            DictionaryEditing.addWord(base, "Filmovi", "x".repeat(DictionaryRules.MAX_WORD_LENGTH + 1)).problem(),
        )
    }

    @Test
    fun addingToAMissingCategoryIsAProgrammingError() {
        assertFailsWith<IllegalArgumentException> { DictionaryEditing.addWord(base, "Nema", "x") }
    }

    @Test
    fun editWordMayChangeCaseButNotCollideWithAnotherWord() {
        assertEquals(
            listOf("TITANIC", "Shrek"),
            DictionaryEditing.editWord(base, "Filmovi", "Titanic", "TITANIC").dictionary().category("Filmovi")!!.words,
        )
        assertEquals(TextProblem.Duplicate, DictionaryEditing.editWord(base, "Filmovi", "Titanic", "shrek").problem())
    }

    @Test
    fun deleteWordAndCategory() {
        assertEquals(listOf("Shrek"), DictionaryEditing.deleteWord(base, "Filmovi", "Titanic").category("Filmovi")!!.words)
        assertEquals(listOf("Hrana"), DictionaryEditing.deleteCategory(base, "Filmovi").categories.map { it.name })
        // Deleting what is not there changes nothing.
        assertEquals(base, DictionaryEditing.deleteWord(base, "Filmovi", "Nema"))
        assertEquals(base, DictionaryEditing.deleteCategory(base, "Nema"))
    }

    @Test
    fun categoryRulesMirrorWordRules() {
        assertEquals(TextProblem.Duplicate, DictionaryEditing.addCategory(base, "hrana").problem())
        assertEquals(TextProblem.Empty, DictionaryEditing.addCategory(base, "").problem())
        assertEquals(
            TextProblem.TooLong,
            DictionaryEditing.addCategory(base, "k".repeat(DictionaryRules.MAX_CATEGORY_LENGTH + 1)).problem(),
        )
        val added = DictionaryEditing.addCategory(base, "Glazba").dictionary()
        assertEquals(listOf("Filmovi", "Hrana", "Glazba"), added.categories.map { it.name })
    }

    @Test
    fun renameCategoryKeepsItsWords() {
        val renamed = DictionaryEditing.renameCategory(base, "Filmovi", "Serije").dictionary()
        assertEquals(listOf("Titanic", "Shrek"), renamed.category("Serije")!!.words)
        assertEquals(TextProblem.Duplicate, DictionaryEditing.renameCategory(base, "Filmovi", "hrana").problem())
        // Changing only the case of its own name is allowed.
        assertEquals("FILMOVI", DictionaryEditing.renameCategory(base, "Filmovi", "FILMOVI").dictionary().categories.first().name)
    }

    @Test
    fun copyGetsANewIdAndAFreeName() {
        val copy = DictionaryEditing.copyOf(base, "d2", listOf("Party pack"))
        assertEquals("d2", copy.id)
        assertEquals("Party pack (2)", copy.name)
        assertEquals(base.categories, copy.categories)
        assertEquals("Party pack (3)", uniqueName("Party pack", listOf("Party pack", "party pack (2)")))
        assertEquals("Novi", uniqueName("  Novi ", listOf("Party pack")))
    }

    @Test
    fun countsAndEngineWords() {
        assertEquals(2, base.categoryCount)
        assertEquals(2, base.wordCount)
        assertEquals(listOf("Titanic", "Shrek"), base.words("Filmovi").map { it.text })
        assertEquals(listOf("Filmovi"), base.words("Filmovi").map { it.category }.distinct())
        assertEquals(emptyList(), base.words("Nema"))
    }
}
