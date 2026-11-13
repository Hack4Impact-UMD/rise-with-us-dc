package org.hack4impact.risedc.core.data.grocery

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import org.hack4impact.risedc.core.data.model.GroceryList
import org.hack4impact.risedc.core.data.storage.LocalStore
import org.hack4impact.risedc.core.data.storage.SettingsJsonStore

class GroceryListStore(settings: Settings, json: Json) :
    LocalStore<GroceryList> by SettingsJsonStore(
        settings,
        "grocery_lists.v1",
        json,
        GroceryList.serializer(),
        GroceryList::id,
    )
