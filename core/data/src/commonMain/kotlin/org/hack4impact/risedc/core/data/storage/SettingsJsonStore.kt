package org.hack4impact.risedc.core.data.storage

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Saves a list of items as one JSON string under [key]. Shared by recipes and grocery lists. */
internal class SettingsJsonStore<T>(
    private val settings: Settings,
    private val key: String,
    private val json: Json,
    serializer: KSerializer<T>,
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

    // Saved data that can't be read (for example after a model change) starts empty instead of crashing.
    private fun load(): List<T> {
        val raw = settings.getStringOrNull(key) ?: return emptyList()
        return try {
            json.decodeFromString(listSerializer, raw)
        } catch (e: SerializationException) {
            emptyList()
        }
    }
}