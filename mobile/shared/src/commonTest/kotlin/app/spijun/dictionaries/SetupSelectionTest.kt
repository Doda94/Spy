package app.spijun.dictionaries

import app.spijun.engine.Word
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SetupSelectionTest {
    private val hr = SetupSource("hr", listOf(SetupCategory("Mjesta", listOf("Plaža", "Škola")), SetupCategory("Hrana", listOf("Sir"))))
    private val party = CustomDictionary(
        id = "c1",
        name = "Party pack",
        language = WordLanguage.Croatian,
        categories = listOf(
            DictionaryCategory("Filmovi", listOf("Titanic", "Shrek")),
            DictionaryCategory("Sportaši", emptyList()),
        ),
    ).asSetupSource()
    private val sources = listOf(hr, party)

    private fun key(dict: String, category: String) = SetupSelection.key(dict, category)

    @Test
    fun customDictionaryBecomesASourceWithItsCategoriesInOrder() {
        assertEquals("c1", party.id)
        assertEquals(listOf("Filmovi", "Sportaši"), party.categories.map { it.name })
        assertEquals(listOf("Titanic", "Shrek"), party.categories.first().words)
    }

    @Test
    fun tickingADictionaryTicksAllItsCategoriesAndUntickingRemovesThem() {
        val on = SetupSelection.toggleDictionary(sources, SetupPicks(setOf("hr"), setOf(key("hr", "Mjesta"))), "c1")
        assertEquals(setOf("hr", "c1"), on.dictionaries)
        assertEquals(setOf(key("hr", "Mjesta"), key("c1", "Filmovi"), key("c1", "Sportaši")), on.categories)

        val off = SetupSelection.toggleDictionary(sources, on, "c1")
        assertEquals(SetupPicks(setOf("hr"), setOf(key("hr", "Mjesta"))), off)
    }

    @Test
    fun togglingAnUnknownDictionaryChangesNothing() {
        val picks = SetupPicks(setOf("hr"), setOf(key("hr", "Mjesta")))
        assertSame(picks, SetupSelection.toggleDictionary(sources, picks, "gone"))
    }

    @Test
    fun theSameCategoryNameInTwoDictionariesIsTwoSeparateKeys() {
        val twin = SetupSource("c2", listOf(SetupCategory("Mjesta", listOf("Park"))))
        val picks = SetupPicks(setOf("hr", "c2"), setOf(key("c2", "Mjesta")))
        assertEquals(listOf(Word("Park", "Mjesta")), SetupSelection.pool(listOf(hr, twin), picks))
        assertEquals(1, SetupSelection.categoryCount(listOf(hr, twin), picks))
    }

    @Test
    fun poolHoldsTickedCategoriesOfTickedDictionariesAcrossBuiltInAndCustom() {
        val picks = SetupPicks(
            setOf("hr", "c1"),
            setOf(key("hr", "Hrana"), key("c1", "Filmovi")),
        )
        assertEquals(
            listOf(Word("Sir", "Hrana"), Word("Titanic", "Filmovi"), Word("Shrek", "Filmovi")),
            SetupSelection.pool(sources, picks),
        )
        assertEquals(2, SetupSelection.categoryCount(sources, picks))
    }

    @Test
    fun aTickedCategoryOfAnUntickedDictionaryIsIgnored() {
        val picks = SetupPicks(setOf("hr"), setOf(key("c1", "Filmovi")))
        assertEquals(emptyList(), SetupSelection.pool(sources, picks))
        assertEquals(0, SetupSelection.categoryCount(sources, picks))
    }

    @Test
    fun emptyCategoriesCountAsPickedButAddNoWords() {
        val picks = SetupPicks(setOf("c1"), setOf(key("c1", "Sportaši")))
        assertEquals(1, SetupSelection.categoryCount(sources, picks))
        assertEquals(emptyList(), SetupSelection.pool(sources, picks))
    }

    @Test
    fun anEmptyDictionaryContributesNothingAndHasNoCategoriesToTick() {
        val empty = SetupSource("c9", emptyList())
        val on = SetupSelection.toggleDictionary(listOf(hr, empty), SetupPicks(), "c9")
        assertEquals(SetupPicks(setOf("c9")), on)
        assertEquals(0, SetupSelection.categoryCount(listOf(hr, empty), on))
        assertEquals(emptyList(), SetupSelection.pool(listOf(hr, empty), on))
    }

    @Test
    fun selectAllAndNoneOnlyTouchTickedDictionaries() {
        val picks = SetupPicks(setOf("c1"), setOf(key("c1", "Filmovi")))
        assertTrue(!SetupSelection.allSelected(sources, picks))

        val all = SetupSelection.setAll(sources, picks, true)
        assertEquals(setOf(key("c1", "Filmovi"), key("c1", "Sportaši")), all.categories)
        assertTrue(SetupSelection.allSelected(sources, all))

        val none = SetupSelection.setAll(sources, all, false)
        assertEquals(emptySet(), none.categories)
        assertEquals(setOf("c1"), none.dictionaries)
    }

    @Test
    fun reconcileDropsADeletedDictionaryWithItsCategories() {
        val picks = SetupPicks(setOf("hr", "c1"), setOf(key("hr", "Mjesta"), key("c1", "Filmovi")))
        val after = SetupSelection.reconcile(listOf(hr), picks)
        assertEquals(SetupPicks(setOf("hr"), setOf(key("hr", "Mjesta"))), after)
    }

    @Test
    fun reconcileDropsADeletedCategoryButKeepsTheDictionary() {
        val picks = SetupPicks(setOf("c1"), setOf(key("c1", "Filmovi"), key("c1", "Sportaši")))
        val withoutSports = SetupSource("c1", listOf(party.categories.first()))
        assertEquals(SetupPicks(setOf("c1"), setOf(key("c1", "Filmovi"))), SetupSelection.reconcile(listOf(withoutSports), picks))
    }

    @Test
    fun reconcileDropsARenamedCategoryEvenWhenOnlyTheCaseChanged() {
        val picks = SetupPicks(setOf("c1"), setOf(key("c1", "Filmovi")))
        val renamed = SetupSource("c1", listOf(SetupCategory("Serije", listOf("Dark")), SetupCategory("filmovi", listOf("Shrek"))))
        val after = SetupSelection.reconcile(listOf(renamed), picks)
        assertEquals(SetupPicks(setOf("c1"), emptySet()), after)
        assertEquals(emptyList(), SetupSelection.pool(listOf(renamed), after))
    }

    @Test
    fun reconcileKeepsEverythingThatStillExistsAndReturnsTheSameValue() {
        val picks = SetupPicks(setOf("hr", "c1"), setOf(key("hr", "Mjesta"), key("c1", "Filmovi")))
        assertSame(picks, SetupSelection.reconcile(sources, picks))
    }

    @Test
    fun reconcileRemovesStrayCategoryKeysOfUntickedDictionaries() {
        val picks = SetupPicks(setOf("hr"), setOf(key("hr", "Mjesta"), key("c1", "Filmovi")))
        assertEquals(SetupPicks(setOf("hr"), setOf(key("hr", "Mjesta"))), SetupSelection.reconcile(sources, picks))
    }

    @Test
    fun withEmptyPickListsEveryDictionaryThatNeedsWords() {
        val fam = SetupSource("c2", listOf(SetupCategory("Rođaci", emptyList())))
        val all = listOf(hr, party, fam)
        val picks = SetupPicks(setOf("hr", "c1", "c2"), setOf(key("hr", "Mjesta"), key("c1", "Sportaši"), key("c2", "Rođaci")))
        assertEquals(setOf("c1", "c2"), SetupSelection.withEmptyPick(all, picks))
        assertEquals(emptySet(), SetupSelection.withEmptyPick(all, SetupPicks(setOf("c1"), setOf(key("c1", "Filmovi")))))
        // An empty category that is not ticked is no reason to complain.
        assertEquals(emptySet(), SetupSelection.withEmptyPick(all, SetupPicks(setOf("c1"), emptySet())))
    }
}
