package org.hack4impact.risedc.core.data.sticker

import org.hack4impact.risedc.core.data.model.StoreSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StickerCatalogTest {
    @Test
    fun ingredientsCarryTheirManifestStoreSection() {
        assertEquals(
            IngredientSticker("308", "Whole milk", StoreSection.DAIRY_AND_EGGS),
            StickerCatalog.ingredients["308"],
        )
        assertEquals(StoreSection.PRODUCE, StickerCatalog.ingredients["60b"]?.storeSection)
        assertEquals(StoreSection.PANTRY, StickerCatalog.ingredients["S10"]?.storeSection)
        assertEquals(StoreSection.FROZEN, StickerCatalog.ingredients["F01"]?.storeSection)
    }

    @Test
    fun utensilsComeFromKitchenAndAdaptedTools() {
        assertEquals(Sticker("K11", "Mixing bowls"), StickerCatalog.utensils["K11"])
        assertTrue(StickerCatalog.utensils.keys.all { it.startsWith("K") || it.startsWith("A") })
    }

    @Test
    fun dishesAndStepTilesHaveTheirOwnLists() {
        assertEquals(Sticker("D01", "Toast with jelly"), StickerCatalog.dishes["D01"])
        assertTrue(StickerCatalog.dishes.keys.all { it.startsWith("D") })
        assertEquals(Sticker("STEP02", "Step: Spread"), StickerCatalog.steps["STEP02"])
        assertTrue(StickerCatalog.steps.keys.all { it.startsWith("STEP") })
    }

    @Test
    fun noIdIsInTwoLists() {
        val lists = listOf(
            StickerCatalog.ingredients.keys,
            StickerCatalog.utensils.keys,
            StickerCatalog.dishes.keys,
            StickerCatalog.steps.keys,
        )
        assertEquals(lists.sumOf { it.size }, lists.flatten().toSet().size)
    }

    @Test
    fun householdAndAppIconsAreLeftOut() {
        val all = StickerCatalog.ingredients.keys + StickerCatalog.utensils.keys +
            StickerCatalog.dishes.keys + StickerCatalog.steps.keys
        listOf("H01", "SAFE01", "SEC01", "MEAL01").forEach { id -> assertFalse(id in all, id) }
    }
}
