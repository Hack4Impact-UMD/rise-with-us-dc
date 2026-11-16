package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.Serializable

/** WMATA station code and display name. */
@Serializable
data class Station(val code: String, val name: String)
