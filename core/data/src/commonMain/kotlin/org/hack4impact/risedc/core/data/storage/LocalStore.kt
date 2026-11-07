package org.hack4impact.risedc.core.data.storage

import kotlinx.coroutines.flow.Flow

interface LocalStore<T> {
    fun observeAll(): Flow<List<T>>

    suspend fun get(id: String): T?

    /** Adds [item], or replaces the item with the same id in place. */
    suspend fun save(item: T)

    /** Changes the item with [id] in one step, so concurrent edits can't overwrite each other. Null if missing. */
    suspend fun update(id: String, transform: (T) -> T): T?

    suspend fun delete(id: String)
}
