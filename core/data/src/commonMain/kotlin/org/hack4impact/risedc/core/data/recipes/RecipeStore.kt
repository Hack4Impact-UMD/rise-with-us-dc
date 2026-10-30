package org.hack4impact.risedc.core.data.recipes

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import org.hack4impact.risedc.core.data.model.Recipe
import org.hack4impact.risedc.core.data.storage.LocalStore
import org.hack4impact.risedc.core.data.storage.SettingsJsonStore

/** Saved recipes on this device. The key's version suffix changes if the saved format breaks. */
class RecipeStore(settings: Settings, json: Json) :
    LocalStore<Recipe> by SettingsJsonStore(settings, "recipes.v1", json, Recipe.serializer(), Recipe::id)
