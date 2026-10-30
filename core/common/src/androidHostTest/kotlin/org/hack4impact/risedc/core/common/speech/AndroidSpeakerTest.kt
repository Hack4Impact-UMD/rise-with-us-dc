package org.hack4impact.risedc.core.common.speech

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Tests reuse and cleanup with a fake engine, without playing audio. */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MagicNumber", "TooManyFunctions")
class AndroidSpeakerTest {
    @Test
    fun completedSpeechReusesEngineWithDistinctIds() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines, ready = true)
        assertTrue(engines.isEmpty())
        val first = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("First") }
        val engine = engines.single()
        val firstId = engine.spoken.single().id
        engine.start(firstId)
        assertTrue(speaker.isSpeaking.value)
        engine.finish(firstId)
        assertEquals(SpeakResult.Done, first.await())

        val second = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Second") }
        assertEquals(1, engines.size)
        assertNotEquals(firstId, engine.spoken.last().id)
        engine.finish(engine.spoken.last().id)
        assertEquals(SpeakResult.Done, second.await())
        assertEquals(0, engine.closeCalls)
    }

    @Test
    fun replacementDuringInitializationKeepsConnection() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines)
        val old = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Old") }
        val current = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Current") }
        assertEquals(SpeakResult.Stopped, old.await())
        val engine = engines.single()
        assertEquals(0, engine.closeCalls)
        assertTrue(engine.spoken.isEmpty())

        engine.initialized.complete(true)
        runCurrent()
        assertEquals("Current", engine.spoken.single().text)
        engine.finish(engine.spoken.single().id)
        assertEquals(SpeakResult.Done, current.await())
    }

    @Test
    fun stopAndCancellationKeepEngineForNextRequest() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines)
        val stopped = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Stop") }
        val engine = engines.single()
        speaker.stop()
        assertEquals(SpeakResult.Stopped, stopped.await())
        assertEquals(1, engine.stopCalls)

        val cancelled = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Cancel") }
        engine.initialized.complete(true)
        runCurrent()
        engine.start(engine.spoken.single().id)
        assertTrue(speaker.isSpeaking.value)
        cancelled.cancelAndJoin()
        assertFalse(speaker.isSpeaking.value)
        assertEquals(2, engine.stopCalls)
        assertEquals(0, engine.closeCalls)

        val next = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Next") }
        assertEquals(1, engines.size)
        assertEquals("Next", engine.spoken.last().text)
        engine.finish(engine.spoken.last().id)
        assertEquals(SpeakResult.Done, next.await())
    }

    @Test
    fun oldCallbacksDoNotAffectCurrentSpeech() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines, ready = true)
        val old = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Old") }
        val engine = engines.single()
        val oldId = engine.spoken.single().id
        val current = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Current") }
        assertEquals(SpeakResult.Stopped, old.await())
        assertEquals(1, engine.stopCalls)
        val currentId = engine.spoken.last().id
        // Android can deliver queued callbacks after a request is replaced.
        engine.start(oldId)
        assertFalse(speaker.isSpeaking.value)
        engine.start(currentId)
        engine.finish(oldId, SpeakResult.Stopped)
        assertTrue(speaker.isSpeaking.value)
        assertFalse(current.isCompleted)
        assertEquals(1, engine.stopCalls)
        engine.finish(currentId)
        assertEquals(SpeakResult.Done, current.await())
    }

    @Test
    fun failedInitializationRetriesWithNewEngine() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines)
        val failed = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("First") }
        engines.single().initialized.complete(false)
        assertIs<SpeakResult.Unavailable>(failed.await())
        assertEquals(1, engines.single().closeCalls)

        val retry = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Retry") }
        assertEquals(2, engines.size)
        val engine = engines.last()
        engine.initialized.complete(true)
        runCurrent()
        engine.finish(engine.spoken.single().id)
        assertEquals(SpeakResult.Done, retry.await())
    }

    @Test
    fun initializationTimeoutClosesEngine() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Hello") }
        advanceTimeBy(5_001)
        assertIs<SpeakResult.Unavailable>(pending.await())
        assertEquals(1, engines.single().closeCalls)
        assertTrue(engines.single().spoken.isEmpty())
    }

    @Test
    fun queueFailureDiscardsEngine() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Hello") }
        val engine = engines.single()
        engine.queueSuccess = false
        engine.initialized.complete(true)
        assertIs<SpeakResult.Unavailable>(pending.await())
        assertEquals(1, engine.closeCalls)
    }

    @Test
    fun configurationFailureRetriesWithSameEngine() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines)
        val failed = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Hello") }
        val engine = engines.single()
        engine.configurationError = "No voice"
        engine.initialized.complete(true)
        assertIs<SpeakResult.Unavailable>(failed.await())
        assertEquals(0, engine.closeCalls)
        engine.configurationError = null
        val retry = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Retry") }
        assertEquals(1, engines.size)
        engine.finish(engine.spoken.single().id)
        assertEquals(SpeakResult.Done, retry.await())
    }

    @Test
    fun configurationExceptionRetriesWithNewEngine() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines, ready = true)
        val first = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("First") }
        val engine = engines.single()
        engine.finish(engine.spoken.single().id)
        assertEquals(SpeakResult.Done, first.await())

        engine.configurationException = IllegalStateException("Voice query failed")
        assertIs<SpeakResult.Unavailable>(speaker.speak("Second"))
        assertEquals(1, engine.closeCalls)
        assertEquals(1, engine.spoken.size)

        val retry = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Retry") }
        assertEquals(2, engines.size)
        val replacement = engines.last()
        assertEquals("Retry", replacement.spoken.single().text)
        replacement.finish(replacement.spoken.single().id)
        assertEquals(SpeakResult.Done, retry.await())
    }

    @Test
    fun releaseStopsSpeechAndIgnoresOldEngineCallbacks() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines, ready = true)
        val old = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Old") }
        val previous = engines.single()
        val oldId = previous.spoken.single().id
        speaker.release()
        speaker.release()
        assertEquals(SpeakResult.Stopped, old.await())
        assertEquals(1, previous.closeCalls)

        val next = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Next") }
        val current = engines.last()
        assertEquals(2, engines.size)
        previous.start(oldId)
        assertFalse(speaker.isSpeaking.value)
        current.start(current.spoken.single().id)
        previous.finish(oldId)
        assertTrue(speaker.isSpeaking.value)
        assertFalse(next.isCompleted)
        current.finish(current.spoken.single().id)
        assertEquals(SpeakResult.Done, next.await())
        speaker.release()
        assertEquals(1, current.closeCalls)
    }

    @Test
    fun resumedCallerCanReplaceSpeechBeforeSubmission() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines, ready = true)
        // Unconfined lets "Old" resume inside replacement cleanup and start "Newest".
        val resumed = async(UnconfinedTestDispatcher(testScheduler)) {
            speaker.speak("Old")
            speaker.speak("Newest")
        }
        val replaced = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Replaced") }
        assertEquals(SpeakResult.Stopped, replaced.await())
        val engine = engines.single()
        assertEquals(listOf("Old", "Newest"), engine.spoken.map { it.text })
        engine.finish(engine.spoken.last().id)
        assertEquals(SpeakResult.Done, resumed.await())
    }

    @Test
    fun blankAndOversizedInputSkipInitialization() = runTest {
        val engines = mutableListOf<FakeEngine>()
        val speaker = createSpeaker(engines)
        assertEquals(SpeakResult.Done, speaker.speak(" "))
        assertTrue(engines.isEmpty())
        assertIs<SpeakResult.Unavailable>(speaker.speak("x".repeat(4_001)))
        assertEquals(0, engines.single().initializeCalls)
        assertTrue(engines.single().spoken.isEmpty())
    }
}

private fun createSpeaker(engines: MutableList<FakeEngine>, ready: Boolean = false) =
    AndroidSpeaker { started, finished ->
        FakeEngine(started, finished).also {
            engines += it
            if (ready) it.initialized.complete(true)
        }
    }

private data class Utterance(val text: String, val id: String)

private class FakeEngine(
    private val onStarted: (String?) -> Unit,
    private val onFinished: (String?, SpeakResult) -> Unit,
) : AndroidTtsEngine {
    override val initialized = CompletableDeferred<Boolean>()
    override val maxInputLength = 4_000
    val spoken = mutableListOf<Utterance>()
    var initializeCalls = 0
    var stopCalls = 0
    var closeCalls = 0
    var queueSuccess = true
    var configurationError: String? = null
    var configurationException: RuntimeException? = null

    override fun initialize() {
        initializeCalls++
    }

    override fun configure(utteranceId: String): String? {
        configurationException?.let { throw it }
        return configurationError
    }

    override fun speak(text: String, utteranceId: String): Boolean {
        spoken += Utterance(text, utteranceId)
        return queueSuccess
    }

    override fun stop() {
        stopCalls++
    }

    override fun close() {
        closeCalls++
    }

    fun start(id: String) = onStarted(id)

    fun finish(id: String, result: SpeakResult = SpeakResult.Done) = onFinished(id, result)
}
