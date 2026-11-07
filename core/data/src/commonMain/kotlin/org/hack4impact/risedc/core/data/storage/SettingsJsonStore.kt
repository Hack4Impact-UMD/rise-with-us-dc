package org.hack4impact.risedc.core.data.storage

import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray

/** Saves a list of items as one JSON string under [key]. Shared by recipes and grocery lists. */
internal class SettingsJsonStore<T>(
    private val settings: Settings,
    private val key: String,
    private val json: Json,
    private val serializer: KSerializer<T>,
    private val idOf: (T) -> String,
) : LocalStore<T> {
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<T>>(emptyList())
    private var loaded = false

    // Saved entries this version can't read (for example after a model change). They are written back
    // untouched on every save, so a later version that can read them gets them back.
    private var unreadable: List<JsonElement> = emptyList()

    override fun observeAll(): Flow<List<T>> = flow {
        mutex.withLock { loadIfNeeded() }
        emitAll(state)
    }

    override suspend fun get(id: String): T? = mutex.withLock {
        loadIfNeeded()
        state.value.find { idOf(it) == id }
    }

    override suspend fun save(item: T) = edit(idOf(item)) { items ->
        val index = items.indexOfFirst { idOf(it) == idOf(item) }
        if (index < 0) items + item else items.toMutableList().also { it[index] = item }
    }

    override suspend fun update(id: String, transform: (T) -> T): T? {
        var updated: T? = null
        edit { items -> items.map { if (idOf(it) == id) transform(it).also { new -> updated = new } else it } }
        return updated
    }

    override suspend fun delete(id: String) = edit(id) { items -> items.filterNot { idOf(it) == id } }

    // An unreadable entry with [replacedId] is dropped too: the user has replaced or deleted it.
    private suspend fun edit(replacedId: String? = null, change: (List<T>) -> List<T>) = mutex.withLock {
        loadIfNeeded()
        val updated = change(state.value)
        if (replacedId != null) unreadable = unreadable.filterNot { rawIdOf(it) == replacedId }
        withContext(Dispatchers.Default) {
            val elements = updated.map { json.encodeToJsonElement(serializer, it) } + unreadable
            settings.putString(key, json.encodeToString(JsonElement.serializer(), JsonArray(elements)))
        }
        state.value = updated
    }

    // Both stored models keep their id under "id".
    private fun rawIdOf(element: JsonElement): String? =
        ((element as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull

    // Called with the mutex held. Reading and decoding happen off the caller's (often the UI) thread.
    private suspend fun loadIfNeeded() {
        if (loaded) return
        withContext(Dispatchers.Default) { load() }
        loaded = true
    }

    private fun load() {
        val raw = settings.getStringOrNull(key) ?: return
        val elements =
            try {
                json.parseToJsonElement(raw).jsonArray
            } catch (ignored: IllegalArgumentException) {
                // Not a JSON array at all, so nothing can be kept entry by entry: back up the whole thing.
                backUp(raw)
                return
            }
        val items = mutableListOf<T>()
        val skipped = mutableListOf<JsonElement>()
        for (element in elements) {
            try {
                items += json.decodeFromJsonElement(serializer, element)
            } catch (ignored: IllegalArgumentException) {
                skipped += element
            }
        }
        state.value = items
        unreadable = skipped
    }

    // Each distinct unreadable copy gets its own key, so a later one never replaces an earlier one.
    private fun backUp(raw: String) {
        val backupKey = generateSequence(1) { it + 1 }
            .map { n -> if (n == 1) "$key.unreadable" else "$key.unreadable.$n" }
            .first { settings.getStringOrNull(it).let { existing -> existing == null || existing == raw } }
        settings.putString(backupKey, raw)
    }
}
