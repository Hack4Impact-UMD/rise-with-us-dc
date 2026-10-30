package org.hack4impact.risedc.core.data.storage

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

/** Saves a list of items as one JSON string under [key]. Shared by recipes and grocery lists. */
internal class SettingsJsonStore<T>(
    private val settings: Settings,
    private val key: String,
    private val json: Json,
    private val serializer: KSerializer<T>,
    private val idOf: (T) -> String,
) : LocalStore<T> {
    private val listSerializer = ListSerializer(serializer)
    private val mutex = Mutex()
    private val state = MutableStateFlow(load())

    override fun observeAll(): StateFlow<List<T>> = state.asStateFlow()

    override suspend fun get(id: String): T? = state.value.find { idOf(it) == id }

    override suspend fun save(item: T) {
        mutex.withLock {
            val updated = state.value.filterNot { idOf(it) == idOf(item) } + item
            settings.putString(key, json.encodeToString(listSerializer, updated))
            state.value = updated
        }
    }

    // Items that can't be read (for example after a model change) are skipped instead of crashing or
    // hiding the rest. If anything is skipped, the original is kept under a backup key so save() can't destroy it.
    private fun load(): List<T> {
        val raw = settings.getStringOrNull(key) ?: return emptyList()
        val elements =
            try {
                json.parseToJsonElement(raw).jsonArray
            } catch (ignored: IllegalArgumentException) {
                null
            }
        val items =
            elements.orEmpty().mapNotNull { element ->
                try {
                    json.decodeFromJsonElement(serializer, element)
                } catch (ignored: IllegalArgumentException) {
                    null
                }
            }
        if (elements == null || items.size < elements.size) backUp(raw)
        return items
    }

    // Keeps only the first unreadable copy: it is the one most likely to hold the user's real data.
    private fun backUp(raw: String) {
        val backupKey = "$key.unreadable"
        if (settings.getStringOrNull(backupKey) == null) settings.putString(backupKey, raw)
    }
}
