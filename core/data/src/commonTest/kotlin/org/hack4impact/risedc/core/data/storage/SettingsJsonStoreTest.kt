package org.hack4impact.risedc.core.data.storage

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.data.TestData.recipe
import org.hack4impact.risedc.core.data.network.RiseJson
import org.hack4impact.risedc.core.data.recipes.RecipeStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SettingsJsonStoreTest {
    private val backing = MapSettings()
    private var failWrites = false
    private var afterWrite: () -> Unit = {}
    private val settings = object : Settings by backing {
        override fun putString(key: String, value: String) {
            check(!failWrites) { "Storage unavailable" }
            backing.putString(key, value)
            afterWrite()
        }
    }

    @Test
    fun failedWritesKeepUnreadableEntries() = runTest {
        val unreadable = """{"id":"reserved"}"""
        listOf(false, true).forEach { delete ->
            backing.putString("recipes.v1", "[$unreadable]")
            val store = RecipeStore(settings, RiseJson)
            failWrites = true
            assertFailsWith<IllegalStateException> {
                if (delete) store.delete("reserved") else store.save(recipe(id = "reserved"))
            }
            failWrites = false
            store.save(recipe(id = "other"))
            assertTrue(unreadable in backing.getString("recipes.v1", ""))
        }
    }

    @Test
    fun failedUpdatesLeaveDataUnchanged() = runTest {
        val store = RecipeStore(settings, RiseJson)
        val expected = recipe().copy(utensils = listOf("Spoon"))
        store.save(expected.copy(utensils = mutableListOf("Spoon")))
        failWrites = true
        assertFailsWith<IllegalStateException> {
            store.update(expected.id) { current ->
                (current.utensils as MutableList<String>).clear()
                current.copy(title = "Unsaved")
            }
        }
        assertEquals(expected, store.get(expected.id))
        assertEquals(expected, RecipeStore(backing, RiseJson).get(expected.id))
    }

    @Test
    fun updateReturnsIndependentCopies() = runTest {
        val store = RecipeStore(settings, RiseJson)
        val expected = recipe().copy(utensils = listOf("Spoon"), title = "Updated")
        store.save(expected.copy(title = "Original", utensils = mutableListOf("Spoon")))
        val returned = requireNotNull(store.update(expected.id) { it.copy(title = "Updated") })
        (returned.utensils as MutableList<String>).clear()
        assertEquals(expected, store.get(expected.id))
        assertEquals(expected, RecipeStore(backing, RiseJson).get(expected.id))
    }

    @Test
    fun cancellationAfterWriteKeepsData() = runTest {
        val store = RecipeStore(settings, RiseJson)
        store.save(recipe(id = "first"))
        val other = recipe(id = "other")
        val writer: Job = launch { store.save(other) }
        afterWrite = { writer.cancel() }
        writer.join()
        afterWrite = {}
        assertTrue(writer.isCancelled)
        assertEquals(other, store.get(other.id))
        store.save(recipe(id = "third"))
        assertEquals(other, RecipeStore(backing, RiseJson).get(other.id))
    }
}
