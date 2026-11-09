package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Unknown strings decode to [UNKNOWN]. */
@Serializable(with = ManeuverSerializer::class)
enum class Maneuver {
    DEPART,
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    TURN_SLIGHT_LEFT,
    TURN_SLIGHT_RIGHT,
    TURN_SHARP_LEFT,
    TURN_SHARP_RIGHT,
    UTURN_LEFT,
    UTURN_RIGHT,
    UNKNOWN,
}

internal object ManeuverSerializer : KSerializer<Maneuver> {
    override val descriptor = PrimitiveSerialDescriptor("Maneuver", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Maneuver {
        val name = decoder.decodeString()
        return Maneuver.entries.firstOrNull { it.name == name } ?: Maneuver.UNKNOWN
    }

    override fun serialize(encoder: Encoder, value: Maneuver) = encoder.encodeString(value.name)
}
