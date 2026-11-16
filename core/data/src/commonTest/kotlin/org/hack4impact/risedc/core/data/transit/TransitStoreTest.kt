package org.hack4impact.risedc.core.data.transit

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.data.di.dataModule
import org.hack4impact.risedc.core.data.model.Maneuver
import org.hack4impact.risedc.core.data.model.Place
import org.hack4impact.risedc.core.data.model.Trip
import org.hack4impact.risedc.core.data.model.TripSource
import org.hack4impact.risedc.core.data.model.TripStep
import org.hack4impact.risedc.core.data.network.RiseJson
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TransitStoreTest {
    private val trip = Trip("shared-id", "Practice trip", listOf(TripStep.Walk(Maneuver.DEPART, 1)), TripSource.AD_HOC)
    private val place = Place("shared-id", "Public destination", 38.9, -77.0)

    @Test
    fun koinStoresAreSingletonsWithSeparateKeys() = runTest {
        val settings = MapSettings()
        settings.putString("recipes.v1", "untouched")
        val application = koinApplication {
            modules(dataModule, module { single<Settings> { settings } })
        }
        try {
            val trips = application.koin.get<TripStore>()
            val places = application.koin.get<PlaceStore>()
            assertSame(trips, application.koin.get<TripStore>())
            assertSame(places, application.koin.get<PlaceStore>())
            trips.save(trip)
            places.save(place)
            assertEquals(trip, TripStore(settings, RiseJson).get(trip.id))
            assertEquals(place, PlaceStore(settings, RiseJson).get(place.id))
            assertEquals("untouched", settings.getString("recipes.v1", ""))
        } finally {
            application.close()
        }
    }

    @Test
    fun tripsWithUnknownStepsAreKeptUntilDeleted() = runTest {
        val settings = MapSettings()
        val unknown = """{"id":"future","name":"Future trip","steps":[{"type":"futureStep"}],"source":"SAVED"}"""
        settings.putString("trips.v1", "[$unknown]")
        val store = TripStore(settings, RiseJson)
        assertEquals(emptyList(), store.observeAll().first())
        store.save(trip)
        assertEquals(trip, TripStore(settings, RiseJson).get(trip.id))
        assertTrue(unknown in settings.getString("trips.v1", ""))
        store.delete("future")
        assertEquals(listOf(trip), TripStore(settings, RiseJson).observeAll().first())
        assertTrue(unknown !in settings.getString("trips.v1", ""))
    }
}
