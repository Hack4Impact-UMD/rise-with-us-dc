package org.hack4impact.risedc.core.data

import kotlinx.serialization.Serializable
import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.test.Test
import kotlin.test.assertEquals

class RiseJsonTest {
    @Serializable
    private data class Sample(val name: String)

    @Test
    fun ignoresUnknownKeysFromTheBackend() {
        assertEquals(Sample("a"), RiseJson.decodeFromString<Sample>("""{"name":"a","extra":1}"""))
    }
}
