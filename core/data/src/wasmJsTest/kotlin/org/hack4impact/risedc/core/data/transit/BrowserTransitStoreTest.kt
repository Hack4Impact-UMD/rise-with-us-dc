package org.hack4impact.risedc.core.data.transit

import com.russhwolf.settings.Settings
import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.data.model.Maneuver
import org.hack4impact.risedc.core.data.model.Place
import org.hack4impact.risedc.core.data.model.Trip
import org.hack4impact.risedc.core.data.model.TripSource
import org.hack4impact.risedc.core.data.model.TripStep
import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserTransitStoreTest {
    @Test
    fun storesReloadFromLocalStorage() = runTest {
        val settings = Settings()
        val keys = listOf("trips.v1", "places.v1")
        val previous = keys.associateWith { settings.getStringOrNull(it) }
        try {
            keys.forEach { settings.remove(it) }
            val trip = Trip(
                "browser-test",
                "Practice trip",
                listOf(TripStep.Walk(Maneuver.DEPART, 1)),
                TripSource.AD_HOC,
            )
            val place = Place("browser-test", "Public destination", 38.9, -77.0)
            TripStore(settings, RiseJson).save(trip)
            PlaceStore(settings, RiseJson).save(place)
            val reloadedTrips = TripStore(Settings(), RiseJson)
            val reloadedPlaces = PlaceStore(Settings(), RiseJson)
            assertEquals(trip, reloadedTrips.get(trip.id))
            assertEquals(place, reloadedPlaces.get(place.id))
        } finally {
            previous.forEach { (key, value) ->
                if (value == null) settings.remove(key) else settings.putString(key, value)
            }
        }
    }
}
