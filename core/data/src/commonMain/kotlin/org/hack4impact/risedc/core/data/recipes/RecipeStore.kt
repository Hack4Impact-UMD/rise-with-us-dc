package org.hack4impact.risedc.core.data.recipes

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.hack4impact.risedc.core.data.model.Recipe
import org.hack4impact.risedc.core.data.storage.SettingsJsonStore

/** Saved recipes on this device. Works offline. */
interface RecipeStore {
    /** All saved recipes; emits again whenever one is saved or deleted. */
    fun observeAll(): Flow<List<Recipe>>

    suspend fun get(id: String): Recipe?

    /** Adds the recipe, or replaces the saved one with the same id. */
    suspend fun save(recipe: Recipe)
}

/** [RecipeStore] backed by multiplatform-settings. See wiki decision 0007. */
class SettingsRecipeStore(settings: Settings, json: Json) : RecipeStore {
    private val store = SettingsJsonStore(settings, KEY, json, Recipe.serializer(), Recipe::id)

    override fun observeAll(): Flow<List<Recipe>> = store.observeAll()

    override suspend fun get(id: String): Recipe? = store.get(id)

    override suspend fun save(recipe: Recipe) = store.save(recipe)

    private companion object {
        const val KEY = "recipes.v1"
    }
}
