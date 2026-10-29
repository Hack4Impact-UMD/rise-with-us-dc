package org.hack4impact.risedc.core.data.model

import org.hack4impact.risedc.core.data.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import org.hack4impact.risedc.core.data.network.RiseJson

class RecipeSerializationTest {
    private val json = RiseJson

    @Test
    fun fullRecipeRoundTripsUnchanged() {
        val recipe = TestData.recipe()
        val decoded = json.decodeFromString(Recipe.serializer(), json.encodeToString(Recipe.serializer(), recipe))
        assertEquals(recipe, decoded)
    }

    @Test
    fun minimalRecipeRoundTripsUnchanged() {
        val recipe = Recipe(id = "r", title = "Toast", minutes = 2, source = RecipeSource.GENERATED)
        val decoded = json.decodeFromString(Recipe.serializer(), json.encodeToString(Recipe.serializer(), recipe))
        assertEquals(recipe, decoded)
    }
}
