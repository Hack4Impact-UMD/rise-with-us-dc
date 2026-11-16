package org.hack4impact.risedc.core.common.speech

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechSessionTest {
    @Test
    fun playbackStateAndResultFollowNativeEvents() = runTest {
        val session = SpeechSession()
        var released = 0
        val request = session.begin { released++ }
        assertFalse(session.isSpeaking.value)
        assertFalse(request.result.isCompleted)
        request.started()
        assertTrue(session.isSpeaking.value)
        request.finish(SpeakResult.Done)
        assertEquals(SpeakResult.Done, request.await())
        assertFalse(session.isSpeaking.value)
        assertEquals(1, released)
    }

    @Test
    fun failureKeepsItsResultAndReleasesOnlyOnceDespiteLateCallbacks() = runTest {
        val session = SpeechSession()
        var released = 0
        val request = session.begin { released++ }
        request.started()
        request.finish(SpeakResult.Unavailable("Voice failed"))
        request.finish(SpeakResult.Done)
        request.started()
        assertEquals(SpeakResult.Unavailable("Voice failed"), request.await())
        assertFalse(session.isSpeaking.value)
        assertEquals(1, released)
    }

    @Test
    fun replacementStopsOldSpeechAndIgnoresItsLateCallbacks() = runTest {
        val session = SpeechSession()
        var oldReleased = 0
        var newReleased = 0
        val old = session.begin { oldReleased++ }
        old.started()
        val current = session.begin { newReleased++ }
        old.started()
        assertFalse(session.isSpeaking.value)
        current.started()
        old.finish(SpeakResult.Done)
        old.finish(SpeakResult.Unavailable("Old error"))
        old.finish(SpeakResult.Stopped)
        assertEquals(SpeakResult.Stopped, old.await())
        assertTrue(current.isActive)
        assertTrue(session.isSpeaking.value)
        assertEquals(1, oldReleased)
        assertEquals(0, newReleased)
        current.finish(SpeakResult.Done)
        assertEquals(SpeakResult.Done, current.await())
    }

    @Test
    fun stopDuringPreparationIgnoresLateStartAndReleasesOnce() = runTest {
        val session = SpeechSession()
        var released = 0
        val request = session.begin { released++ }
        session.stop()
        session.stop()
        request.started()
        assertEquals(SpeakResult.Stopped, request.await())
        assertFalse(session.isSpeaking.value)
        assertEquals(1, released)
    }

    @Test
    fun callerCancellationPropagatesAndReleasesItsRequest() = runTest {
        val session = SpeechSession()
        var released = false
        val request = session.begin { released = true }
        val pending = async(start = CoroutineStart.UNDISPATCHED) { request.await() }
        pending.cancelAndJoin()
        assertFailsWith<CancellationException> { pending.await() }
        assertTrue(released)
        assertFalse(request.isActive)
    }

    @Test
    fun cancelledOldCallerCannotStopItsReplacement() = runTest {
        val session = SpeechSession()
        val old = session.begin { }
        val pending = async(start = CoroutineStart.UNDISPATCHED) { old.await() }
        val current = session.begin { }
        pending.cancelAndJoin()
        assertTrue(current.isActive)
        session.stop()
        assertEquals(SpeakResult.Stopped, current.await())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun completionCleansUpBeforeAnObserverStartsNewSpeech() = runTest {
        val session = SpeechSession()
        var nativeSpeech = "Old"
        val old = session.begin { nativeSpeech = "" }
        old.started()
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            session.isSpeaking.drop(1).collect { speaking ->
                if (!speaking) {
                    session.begin { nativeSpeech = "" }
                    nativeSpeech = "New"
                }
            }
        }
        old.finish(SpeakResult.Done)
        assertEquals("New", nativeSpeech)
        observer.cancelAndJoin()
        session.stop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun replacementCleansUpBeforeAnObserverStartsNewSpeech() = runTest {
        val session = SpeechSession()
        var nativeSpeech = "Old"
        val old = session.begin { nativeSpeech = "" }
        old.started()
        var newest: SpeechSession.SpeechRequest? = null
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            session.isSpeaking.drop(1).collect { speaking ->
                if (!speaking) {
                    newest = session.begin { nativeSpeech = "" }
                    nativeSpeech = "Newest"
                    newest.started()
                }
            }
        }
        val replaced = session.begin { nativeSpeech = "" }
        assertEquals(SpeakResult.Stopped, replaced.await())
        assertEquals("Newest", nativeSpeech)
        assertEquals(true, newest?.isActive)
        assertTrue(session.isSpeaking.value)
        observer.cancelAndJoin()
        session.stop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aResumedOldCallerCannotLeaveAnOverwrittenRequestRunning() = runTest {
        val session = SpeechSession()
        val old = session.begin { }
        var newest: SpeechSession.SpeechRequest? = null
        val resumed = async(UnconfinedTestDispatcher(testScheduler)) {
            old.await()
            newest = session.begin { }
            newest.started()
        }
        val replaced = session.begin { }
        resumed.await()
        assertEquals(SpeakResult.Stopped, replaced.await())
        assertEquals(true, newest?.isActive)
        assertTrue(session.isSpeaking.value)
        session.stop()
    }
}
