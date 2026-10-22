package org.hack4impact.risedc.core.data.grocery

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.hack4impact.risedc.core.data.model.GroceryList
import org.hack4impact.risedc.core.data.storage.SettingsJsonStore

/** Saved grocery lists on this device. Uses the same storage as recipes (decision 0007). */
interface GroceryListStore {
    fun observeAll(): Flow<List<GroceryList>>

    suspend fun get(id: String): GroceryList?

    /** Adds the list, or replaces the saved one with the same id. */
    suspend fun save(list: GroceryList)
}

class SettingsGroceryListStore(settings: Settings, json: Json) : GroceryListStore {
    private val store = SettingsJsonStore(settings, KEY, json, GroceryList.serializer(), GroceryList::id)

    override fun observeAll(): Flow<List<GroceryList>> = store.observeAll()

    override suspend fun get(id: String): GroceryList? = store.get(id)

    override suspend fun save(list: GroceryList) = store.save(list)

    private companion object { // companion kind of acts like a static var in java
        const val KEY = "grocery_lists.v1"
    }
}
