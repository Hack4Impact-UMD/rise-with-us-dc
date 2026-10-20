package org.hack4impact.risedc.core.common.location

import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.common.di.platformModule
import org.hack4impact.risedc.core.common.location.LocationResult.Unavailable.Reason
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class WebLocationProviderTest {
    @Test
    fun returnsUnavailableFromTheWebBinding() = runTest {
        val application = koinApplication { modules(platformModule) }
        try {
            assertEquals(
                LocationResult.Unavailable(Reason.NotSupported),
                application.koin.get<LocationProvider>().currentLocation(),
            )
        } finally {
            application.close()
        }
    }
}
