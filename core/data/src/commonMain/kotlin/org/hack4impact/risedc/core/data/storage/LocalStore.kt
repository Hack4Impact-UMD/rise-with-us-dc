package org.hack4impact.risedc.core.data.storage

import kotlinx.coroutines.flow.Flow


interface LocalStore<T> {
    fun observeAll(): Flow<List<T>>

    suspend fun get(id: String): T?

    
    suspend fun save(item: T)
}