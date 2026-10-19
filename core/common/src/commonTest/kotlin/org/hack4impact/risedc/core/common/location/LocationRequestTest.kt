package org.hack4impact.risedc.core.common.location

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.hack4impact.risedc.core.common.location.LocationResult.Unavailable.Reason
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LocationRequestTest {
    private val location = LocationResult.Success(lat = 0.0, lng = 0.0, accuracyMeters = 5.0, timestampMillis = 1_000)

    @Test
    fun returnsValidLocationsAtCoordinateBoundaries() {
        assertEquals(location, location.validated())
        val boundary = location.copy(lat = -90.0, lng = 180.0, accuracyMeters = 0.0)
        assertEquals(boundary, boundary.validated())
        val oppositeBoundary = location.copy(lat = 90.0, lng = -180.0)
        assertEquals(oppositeBoundary, oppositeBoundary.validated())
    }

    @Test
    fun rejectsInvalidCoordinatesAndAccuracy() {
        val invalidLocations = listOf(
            location.copy(lat = Double.NaN),
            location.copy(lat = 90.1),
            location.copy(lat = -90.1),
            location.copy(lng = Double.POSITIVE_INFINITY),
            location.copy(lng = 180.1),
            location.copy(lng = -180.1),
            location.copy(accuracyMeters = -1.0),
            location.copy(accuracyMeters = Double.NaN),
            location.copy(accuracyMeters = Double.POSITIVE_INFINITY),
        )
        invalidLocations.forEach {
            assertEquals(LocationResult.Unavailable(Reason.NoFix, "invalid location"), it.validated())
        }
    }

    @Test
    fun doesNotValidateTimestamps() {
        listOf(Long.MIN_VALUE, 0L, Long.MAX_VALUE).forEach { timestampMillis ->
            val timestampedLocation = location.copy(timestampMillis = timestampMillis)
            assertEquals(timestampedLocation, timestampedLocation.validated())
        }
    }

    @Test
    fun returnsCompletedResult() = runTest {
        assertEquals(location, withLocationTimeout { location })
        val unavailable = LocationResult.Unavailable(Reason.NoFix, "location request failed")
        assertEquals(unavailable, withLocationTimeout { unavailable })
    }

    @Test
    fun timesOutAfterThirtySecondsAndCancelsTheRequest() = runTest {
        var cleanedUp = false
        val startedAt = testScheduler.currentTime
        val result = withLocationTimeout {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { cleanedUp = true }
            }
        }
        assertEquals(LocationResult.Unavailable(Reason.TimedOut), result)
        assertEquals(30_000L, testScheduler.currentTime - startedAt)
        assertTrue(cleanedUp)
    }

    @Test
    fun propagatesCallerCancellationAndRunsCleanup() = runTest {
        val started = CompletableDeferred<Unit>()
        var cleanedUp = false
        val pending = async {
            withLocationTimeout {
                try {
                    started.complete(Unit)
                    awaitCancellation()
                } finally {
                    cleanedUp = true
                }
            }
        }
        started.await()
        pending.cancelAndJoin()
        assertFailsWith<CancellationException> { pending.await() }
        assertTrue(cleanedUp)
    }

    @Test
    fun propagatesShorterCallerTimeout() = runTest {
        assertFailsWith<CancellationException> {
            withTimeout(1) { withLocationTimeout { awaitCancellation() } }
        }
    }
}
