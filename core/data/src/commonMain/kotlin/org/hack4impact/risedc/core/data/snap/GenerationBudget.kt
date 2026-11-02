package org.hack4impact.risedc.core.data.snap

import com.russhwolf.settings.Settings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.time.Clock

/** Tracks API attempts per device, resetting on a new date in DC's timezone. */
internal class GenerationBudget(
    private val settings: Settings,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.of("America/New_York")) },
) {
    /** Persists one reservation before sending a request. False if capped or stored data is invalid. */
    suspend fun reserve(): Boolean = lock.withLock {
        val date = today().toString()
        val saved = settings.getStringOrNull(KEY)?.let { storedJson ->
            try {
                // Decode the stored JSON into a DailyCallUsage object
                RiseJson.decodeFromString<DailyCallUsage>(storedJson)
            } catch (_: SerializationException) {
                return@withLock false
            }
        }

        // Retrieve the number of calls made today
        val callsUsed = saved?.takeIf { it.date == date }?.callsUsed ?: 0

        // Check if the limit has been reached
        if (callsUsed >= LIMIT) return@withLock false

        // Saves today's date and the incremented call count
        settings.putString(KEY, RiseJson.encodeToString(DailyCallUsage(date, callsUsed + 1)))
        true
    }

    @Serializable
    private data class DailyCallUsage(val date: String, val callsUsed: Int)

    // Contains a shared lock across instances so every reservation updates the same
    // per-device daily count one at a time
    private companion object {
        val lock = Mutex()
        const val KEY = "snap.dailyCallUsage"
        const val LIMIT = 50
    }
}
