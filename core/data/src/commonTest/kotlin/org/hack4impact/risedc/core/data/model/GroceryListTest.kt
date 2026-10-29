package org.hack4impact.risedc.core.data.model

import org.hack4impact.risedc.core.data.TestData.item
import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroceryListTest {
    @Test
    fun groceryListRoundTripsUnchanged() {
        val list =
            GroceryList(
                id = "list-1",
                items = listOf(item("Milk", gotIt = true, fromRecipeIds = setOf("r1")), item("Apples", amount = "")),
            )
        val encoded = RiseJson.encodeToString(GroceryList.serializer(), list)
        assertEquals(list, RiseJson.decodeFromString(GroceryList.serializer(), encoded))
    }

    @Test
    fun emptyListGroupsToNothing() {
        assertTrue(GroceryList(id = "empty").groupedBySection().isEmpty())
    }

    @Test
    fun manySectionsComeOutInStoreOrder() {
        val list =
            GroceryList(
                id = "list-1",
                items =
                    listOf(
                        item("Ice cream", section = StoreSection.FROZEN),
                        item("Milk", section = StoreSection.DAIRY_AND_EGGS),
                        item("Apples", section = StoreSection.PRODUCE),
                        item("Bananas", section = StoreSection.PRODUCE),
                    ),
            )
        val grouped = list.groupedBySection()

        assertEquals(
            listOf(StoreSection.PRODUCE, StoreSection.DAIRY_AND_EGGS, StoreSection.FROZEN),
            grouped.keys.toList(),
        )
        assertEquals(listOf("Apples", "Bananas"), grouped.getValue(StoreSection.PRODUCE).map { it.name })
    }
}