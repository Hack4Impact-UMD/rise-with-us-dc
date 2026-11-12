package org.hack4impact.risedc.core.data.recipes

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.data.TestData.recipe
import org.hack4impact.risedc.core.data.model.RecipeStatus
import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        settings.putString("recipes.v1", """[$good,{"id":"bad"}]""")

        assertEquals(listOf("good"), newStore().observeAll().first().map { it.id })
    }

    @Test
    fun unreadableRecipeIsKeptThroughLaterSaves() = runTest {
        settings.putString("recipes.v1", """[{"id":"bad"}]""")
        newStore().save(recipe(id = "new"))
        assertTrue("""{"id":"bad"}""" in settings.getString("recipes.v1", ""))
        assertEquals(listOf("new"), newStore().observeAll().first().map { it.id })
    }

    @Test
    fun savingOverAnUnreadableRecipeReplacesIt() = runTest {
        settings.putString("recipes.v1", """[{"id":"recipe-1"}]""")
        newStore().save(recipe())
        assertFalse("""{"id":"recipe-1"}""" in settings.getString("recipes.v1", ""))
    }

    @Test
    fun backupSurvivesTheNextSave() = runTest {
        settings.putString("recipes.v1", "not json")
        newStore().save(recipe())
        assertEquals("not json", settings.getStringOrNull("recipes.v1.unreadable"))
    }

    @Test
    fun aSecondUnreadableCopyGetsItsOwnBackup() = runTest {
        settings.putString("recipes.v1", "first")
        newStore().get("x")
        settings.putString("recipes.v1", "second")
        newStore().get("x")
        newStore().get("x")

        assertEquals("first", settings.getStringOrNull("recipes.v1.unreadable"))
        assertEquals("second", settings.getStringOrNull("recipes.v1.unreadable.2"))
        assertNull(settings.getStringOrNull("recipes.v1.unreadable.3"))
    }

    @Test
    fun readableDataIsNotBackedUp() = runTest {
        newStore().save(recipe())
        newStore().get("recipe-1")
        assertNull(settings.getStringOrNull("recipes.v1.unreadable"))
    }

    @Test
    fun savingAnEditKeepsItsPlace() = runTest {
        val store = newStore()
        listOf("r1", "r2", "r3").forEach { store.save(recipe(id = it)) }
        store.save(recipe(id = "r1").copy(title = "Edited"))
        assertEquals(listOf("r1", "r2", "r3"), store.observeAll().first().map { it.id })
    }

    @Test
    fun updateChangesOnlyThatRecipe() = runTest {
        val store = newStore()
        store.save(recipe(id = "r1"))
        store.save(recipe(id = "r2"))

        assertEquals("Edited", store.update("r1") { it.copy(title = "Edited") }?.title)
        assertEquals("Edited", newStore().get("r1")?.title)
        assertEquals(recipe(id = "r2"), newStore().get("r2"))
    }

    @Test
    fun updateOfAMissingRecipeReturnsNull() = runTest {
        assertNull(newStore().update("missing") { it.copy(title = "x") })
    }

    @Test
    fun concurrentUpdatesAreAllKept() = runTest {
        val store = newStore()
        store.save(recipe().copy(utensils = emptyList()))
        (1..20).map { n -> async { store.update("recipe-1") { it.copy(utensils = it.utensils + "u$n") } } }.awaitAll()
        assertEquals(20, newStore().get("recipe-1")?.utensils?.size)
    }

    @Test
    fun deleteRemovesTheRecipe() = runTest {
        val store = newStore()
        store.save(recipe(id = "r1"))
        store.save(recipe(id = "r2"))
        store.delete("r1")

        assertEquals(listOf("r2"), newStore().observeAll().first().map { it.id })
        store.delete("missing")
        assertEquals(listOf("r2"), store.observeAll().first().map { it.id })
    }

    @Test
    fun unknownStatusFallsBackToDraft() = runTest {
        newStore().save(recipe())
        settings.putString("recipes.v1", settings.getString("recipes.v1", "").replace("PUBLISHED", "ARCHIVED"))
        assertEquals(RecipeStatus.DRAFT, newStore().get("recipe-1")?.status)
    }
}
