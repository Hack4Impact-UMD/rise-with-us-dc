package org.hack4impact.risedc.core.data.recipes

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.data.TestData.recipe
import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeStoreTest {
    private val settings = MapSettings()

    private fun newStore() = RecipeStore(settings, RiseJson)

    @Test
    fun emptyStoreHasNoRecipes() = runTest {
        assertTrue(newStore().observeAll().first().isEmpty())
        assertNull(newStore().get("missing"))
    }

    @Test
    fun savedRecipeCanBeRead() = runTest {
        val store = newStore()
        store.save(recipe())
        assertEquals(recipe(), store.get("recipe-1"))
    }

    @Test
    fun manyRecipesAreAllKept() = runTest {
        val store = newStore()
        listOf("r1", "r2", "r3").forEach { store.save(recipe(id = it)) }
        assertEquals(listOf("r1", "r2", "r3"), store.observeAll().first().map { it.id })
    }

    @Test
    fun savingSameIdReplacesInsteadOfDuplicating() = runTest {
        val store = newStore()
        store.save(recipe())
        val edited = recipe().copy(title = "Edited title")
        store.save(edited)
        assertEquals(listOf(edited), store.observeAll().first())
    }

    @Test
    fun recipesSurviveARelaunch() = runTest {
        // A new store on the same storage is what an app relaunch looks like.
        newStore().save(recipe())
        assertEquals(recipe(), newStore().get("recipe-1"))
    }

    @Test
    fun unreadableSavedDataStartsEmptyAndIsBackedUp() = runTest {
        settings.putString("recipes.v1", "not json")
        assertTrue(newStore().observeAll().first().isEmpty())
        assertEquals("not json", settings.getStringOrNull("recipes.v1.unreadable"))
    }

    @Test
    fun oneUnreadableRecipeDoesNotHideTheRest() = runTest {
        newStore().save(recipe(id = "good"))
        val good = settings.getString("recipes.v1", "").removePrefix("[").removeSuffix("]")
        val raw = """[$good,{"id":"bad"}]"""
        settings.putString("recipes.v1", raw)

        assertEquals(listOf("good"), newStore().observeAll().first().map { it.id })
        assertEquals(raw, settings.getStringOrNull("recipes.v1.unreadable"))
    }

    @Test
    fun backupSurvivesTheNextSave() = runTest {
        settings.putString("recipes.v1", "not json")
        newStore().save(recipe())
        assertEquals("not json", settings.getStringOrNull("recipes.v1.unreadable"))
    }

    @Test
    fun readableDataIsNotBackedUp() = runTest {
        newStore().save(recipe())
        newStore()
        assertNull(settings.getStringOrNull("recipes.v1.unreadable"))
    }
}
