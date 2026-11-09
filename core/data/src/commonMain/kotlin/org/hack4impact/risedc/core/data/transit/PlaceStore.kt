package org.hack4impact.risedc.core.data.transit

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import org.hack4impact.risedc.core.data.model.Place
import org.hack4impact.risedc.core.data.storage.LocalStore
import org.hack4impact.risedc.core.data.storage.SettingsJsonStore

/** Stores places on this device. */
class PlaceStore(settings: Settings, json: Json) :
    LocalStore<Place> by SettingsJsonStore(settings, "places.v1", json, Place.serializer(), Place::id)
