package org.hack4impact.risedc.core.common.location

import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.common.di.platformModule
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class WebLocationProviderTest {
    @Test
    fun returnsUnavailableFromTheWebBinding() = runTest {
        val application = koinApplication { modules(platformModule) }
        try {
            assertEquals(
                LocationResult.Unavailable("not supported on web"),
                application.koin.get<LocationProvider>().currentLocation(),
            )
        } finally {
            application.close()
        }
    }
}
