package org.hack4impact.risedc.core.data.transit

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.data.model.BusStop
import org.hack4impact.risedc.core.data.model.Station
import org.hack4impact.risedc.core.data.model.Trip
import org.hack4impact.risedc.core.data.model.TripSource
import org.hack4impact.risedc.core.data.model.TripStep
import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TransitSnapshotTest {
    private val settings = MapSettings()
    private val store = TripStore(settings, RiseJson)
    private val station = Station("A01", "Metro Center")
    private val stop = BusStop("00123", "Practice stop")
    private val expected = Trip(
        "ride",
        "Practice ride",
        listOf(TripStep.RideRail(listOf(station), station), TripStep.RideBus(listOf(stop), stop)),
        TripSource.AD_HOC,
    )
    private val other = Trip("other", "Other practice trip", emptyList(), TripSource.SAVED)

    @Test
    fun savedTripsIgnoreInputListChanges() = runTest {
        val railStops = mutableListOf(station)
        val busStops = mutableListOf(stop)
        val steps = mutableListOf<TripStep>(
            TripStep.RideRail(railStops, station),
            TripStep.RideBus(busStops, stop),
        )
        store.save(expected.copy(steps = steps))
        railStops.clear()
        busStops.clear()
        steps.clear()
        assertEquals(expected, store.get(expected.id))
        assertEquals(listOf(expected), store.observeAll().first())
        store.save(other)
        assertEquals(listOf(expected, other), TripStore(settings, RiseJson).observeAll().first())
    }

    @Test
    fun getReturnsIndependentCopies() = runTest {
        store.save(RiseJson.decodeFromString<Trip>(RiseJson.encodeToString(expected)))
        val returned = requireNotNull(store.get(expected.id))
        clearRideLists(returned)
        clearIfMutable(returned.steps)
        assertEquals(expected, store.get(expected.id))
        assertEquals(listOf(expected), store.observeAll().first())
        store.save(other)
        assertEquals(listOf(expected, other), TripStore(settings, RiseJson).observeAll().first())
    }

    @Test
    fun collectorsReceiveIndependentCopies() = runTest {
        store.save(RiseJson.decodeFromString<Trip>(RiseJson.encodeToString(expected)))
        val snapshots = store.observeAll()
        val initial = CompletableDeferred<List<Trip>>()
        val collected = async { snapshots.onEach { initial.complete(it) }.take(2).toList() }
        val first = initial.await()
        val second = snapshots.first()
        clearRideLists(first.single())
        clearIfMutable(first.single().steps)
        clearIfMutable(first)
        assertEquals(listOf(expected), second)
        assertEquals(listOf(expected), store.observeAll().first())
        assertEquals(expected, store.get(expected.id))
        store.save(other)
        val emissions = collected.await()
        assertEquals(2, emissions.size)
        val later = emissions.last()
        assertEquals(listOf(expected, other), later)
        clearRideLists(later.first())
        clearIfMutable(later)
        assertEquals(listOf(expected, other), snapshots.first())
        assertEquals(listOf(expected, other), TripStore(settings, RiseJson).observeAll().first())
    }

    @Test
    fun invalidTripsLeaveStoredDataUnchanged() = runTest {
        store.save(other)
        val savedJson = settings.getString("trips.v1", "")
        val stops = mutableListOf(station)
        val invalid = expected.copy(steps = listOf(TripStep.RideRail(stops, station)))
        stops.clear()
        assertFailsWith<IllegalArgumentException> { store.save(invalid) }
        assertEquals(savedJson, settings.getString("trips.v1", ""))
        assertEquals(listOf(other), store.observeAll().first())
        assertEquals(listOf(other), TripStore(settings, RiseJson).observeAll().first())
    }

    private fun clearRideLists(trip: Trip) {
        clearIfMutable((trip.steps[0] as TripStep.RideRail).stops)
        clearIfMutable((trip.steps[1] as TripStep.RideBus).stops)
    }

    private fun <T> clearIfMutable(list: List<T>) {
        try {
            (list as? MutableList<T>)?.clear()
        } catch (_: UnsupportedOperationException) {
            // Unmodifiable lists are safe too.
        }
    }
}
