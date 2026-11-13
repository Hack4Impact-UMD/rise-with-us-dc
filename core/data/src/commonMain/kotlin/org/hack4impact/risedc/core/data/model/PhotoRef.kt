package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.Serializable

/** Points to a photo that comes with the app or is saved on the device. */
@Serializable
data class PhotoRef(val path: String)
