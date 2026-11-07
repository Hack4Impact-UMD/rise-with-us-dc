package org.hack4impact.risedc.core.data.grocery

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.data.TestData.item
import org.hack4impact.risedc.core.data.TestData.recipe
import org.hack4impact.risedc.core.data.model.GroceryList
import org.hack4impact.risedc.core.data.model.StoreSection
import org.hack4impact.risedc.core.data.network.RiseJson
import org.hack4impact.risedc.core.data.recipes.RecipeStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroceryListStoreTest {
    private val settings = MapSettings()

    @Test
    fun listIsSavedAndReloaded() = runTest {
        val list = GroceryList(id = "list-1", items = listOf(item("Milk", gotIt = true), item("Eggs")))
        GroceryListStore(settings, RiseJson).save(list)
        assertEquals(list, GroceryListStore(settings, RiseJson).get("list-1"))
    }

    @Test
    fun unknownStoreSectionFallsBackToOtherInsteadOfHidingTheList() = runTest {
        val brie = """{"name":"Brie","amount":"1","storeSection":"CHEESE"}"""
        settings.putString("grocery_lists.v1", """[{"id":"a","items":[{"name":"Milk","amount":"1"},$brie]}]""")
        val items = GroceryListStore(settings, RiseJson).get("a")?.items.orEmpty()
        assertEquals(listOf(StoreSection.OTHER, StoreSection.OTHER), items.map { it.storeSection })
    }

    @Test
    fun listWithABrokenItemIsKeptWhenAnotherListIsSaved() = runTest {
        val broken = """{"id":"a","items":[{"name":"Milk","amount":"1"},{"name":"Brie","amount":{}}]}"""
        settings.putString("grocery_lists.v1", "[$broken]")
        GroceryListStore(settings, RiseJson).save(GroceryList(id = "b"))
        assertTrue(broken in settings.getString("grocery_lists.v1", ""))
    }

    @Test
    fun recipesAndGroceryListsDoNotOverwriteEachOther() = runTest {
        RecipeStore(settings, RiseJson).save(recipe())
        GroceryListStore(settings, RiseJson).save(GroceryList(id = "list-1"))
        assertEquals(recipe(), RecipeStore(settings, RiseJson).get("recipe-1"))
    }
}
