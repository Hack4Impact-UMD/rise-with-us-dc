package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.Serializable

/** A grocery list. Items are shown grouped by [StoreSection], see [groupedBySection]. */
@Serializable
data class GroceryList(val id: String, val items: List<GroceryItem> = emptyList())

@Serializable
data class GroceryItem(
    val name: String,
    /** Free text such as "2 cups"; may combine several recipes, such as "1 cup + 2 cups". */
    val amount: String,
    val storeSection: StoreSection = StoreSection.OTHER,
    /** true = "Got it!", false = "Still looking". */
    val gotIt: Boolean = false,
    /** Ids of the recipes this item was added for. Empty for items added by hand. */
    val fromRecipeIds: Set<String> = emptySet(),
)

fun GroceryList.groupedBySection(): Map<StoreSection, List<GroceryItem>> {
    val bySection = items.groupBy { it.storeSection }
    return StoreSection.entries
        .filter { it in bySection }
        .associateWith { bySection.getValue(it) }
}
