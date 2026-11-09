package org.hack4impact.risedc.core.data.transit

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import org.hack4impact.risedc.core.data.model.Trip
import org.hack4impact.risedc.core.data.storage.LocalStore
import org.hack4impact.risedc.core.data.storage.SettingsJsonStore

/** Stores trips on this device. */
class TripStore(settings: Settings, json: Json) :
    LocalStore<Trip> by SettingsJsonStore(settings, "trips.v1", json, Trip.serializer(), Trip::id)
