package org.hack4impact.risedc.core.data.sticker

import org.hack4impact.risedc.core.data.model.StoreSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
        assertEquals(UtensilSticker("K11", "Mixing bowls"), StickerCatalog.utensils["K11"])
        assertTrue(StickerCatalog.utensils.keys.all { it.startsWith("K") || it.startsWith("A") })
    }

    @Test
    fun ingredientsAndUtensilsDoNotOverlap() {
        assertTrue(StickerCatalog.ingredients.keys.intersect(StickerCatalog.utensils.keys).isEmpty())
    }

    @Test
    fun householdIconsDishesAndStepTilesAreLeftOut() {
        listOf("H01", "SAFE01", "SEC01", "D01", "STEP01").forEach { id ->
            assertNull(StickerCatalog.ingredients[id], id)
            assertNull(StickerCatalog.utensils[id], id)
        }
    }
}
