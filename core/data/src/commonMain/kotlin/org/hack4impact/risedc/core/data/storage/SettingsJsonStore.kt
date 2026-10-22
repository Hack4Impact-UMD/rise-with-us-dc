package org.hack4impact.risedc.core.data.storage

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Saves a list of items as one JSON string under [key]. Shared by recipes and grocery lists. */
internal class SettingsJsonStore<T>(
    private val settings: Settings,
    private val key: String,
    private val json: Json,
    serializer: KSerializer<T>,
    private val idOf: (T) -> String,
) {
    private val listSerializer = ListSerializer(serializer)
    private val state =
        MutableStateFlow(
            settings.getStringOrNull(key)?.let {
                json.decodeFromString(listSerializer, it)
            } ?: emptyList(),
        )

    fun observeAll(): StateFlow<List<T>> = state.asStateFlow()

    fun get(id: String): T? = state.value.find { idOf(it) == id }

    fun save(item: T) {
        val updated = state.updateAndGet { list -> list.filterNot { idOf(it) == idOf(item) } + item }
        settings.putString(key, json.encodeToString(listSerializer, updated))
    }
}
