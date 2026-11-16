@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.hack4impact.risedc.core.common.speech

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.js.JsArray
import kotlin.js.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebSpeakerTest {
    @Test
    fun wholeTextStartsWithoutSuspendingAndFinishesOnNativeCompletion() = runTest {
        val engine = fakeSynthesis()
        val speaker = WebSpeaker(engine, ::fakeUtterance)
        val text = " Warning.\nInstruction. "
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            speaker.speak(text)
        }
        assertEquals(1, engine.spoken.length)
        assertEquals(text, engine.spoken[0]?.text)
        assertEquals(1.0, engine.spoken[0]?.rate)
        assertFalse(pending.isCompleted)
        engine.start(0)
        assertTrue(speaker.isSpeaking.value)
        engine.end(0)
        assertEquals(SpeakResult.Done, pending.await())
        assertFalse(speaker.isSpeaking.value)
        assertEquals(1, engine.cancelCount)
    }

    @Test
    fun missingApiOrLocalVoiceReturnsUnavailableWithoutSpeaking() = runTest {
        assertIs<SpeakResult.Unavailable>(WebSpeaker(null, ::fakeUtterance).speak("Hello"))
        val engine = fakeSynthesis()
        engine.voices = fakeVoices(local = false)
        val speaker = WebSpeaker(engine, ::fakeUtterance)
        assertIs<SpeakResult.Unavailable>(speaker.speak("Hello"))
        assertEquals(0, engine.spoken.length)
        engine.voices = fakeVoices(local = true)
        assertEquals(0, engine.spoken.length)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Hello") }
        engine.end(0)
        assertEquals(SpeakResult.Done, pending.await())
        engine.voices = emptyVoices()
        assertIs<SpeakResult.Unavailable>(speaker.speak("Again"))
        assertEquals(1, engine.spoken.length)
    }

    @Test
    fun emptyVoicesDoNotWaitAndBlankInputDoesNotInterrupt() = runTest {
        val engine = fakeSynthesis()
        engine.voices = emptyVoices()
        val speaker = WebSpeaker(engine, ::fakeUtterance)
        assertIs<SpeakResult.Unavailable>(speaker.speak("Hello"))
        engine.voices = fakeVoices(local = true)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Hello") }
        val cancellations = engine.cancelCount
        assertEquals(SpeakResult.Done, speaker.speak(""))
        assertEquals(SpeakResult.Done, speaker.speak(" \n "))
        assertEquals(cancellations, engine.cancelCount)
        engine.end(0)
        assertEquals(SpeakResult.Done, pending.await())
    }

    @Test
    fun nativeErrorAndNativeThrowReturnUnavailable() = runTest {
        val engine = fakeSynthesis()
        val speaker = WebSpeaker(engine, ::fakeUtterance)
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            speaker.speak("Warning. Instruction.")
        }
        engine.fail(0, "synthesis-failed")
        assertIs<SpeakResult.Unavailable>(pending.await())
        assertEquals(1, engine.spoken.length)
        engine.throwOnSpeak = true
        assertIs<SpeakResult.Unavailable>(speaker.speak("Hello"))
        assertFalse(speaker.isSpeaking.value)
    }

    @Test
    fun replacementAndCancellationIgnoreOldCallbacks() = runTest {
        val engine = fakeSynthesis()
        val speaker = WebSpeaker(engine, ::fakeUtterance)
        val old = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Old") }
        val lateStart = engine.spoken[0]?.onstart
        val lateEnd = engine.spoken[0]?.onend
        val current = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Current") }
        assertEquals(SpeakResult.Stopped, old.await())
        lateStart?.invoke()
        assertFalse(speaker.isSpeaking.value)
        lateEnd?.invoke()
        engine.start(1)
        assertTrue(speaker.isSpeaking.value)
        old.cancelAndJoin()
        assertFalse(current.isCompleted)
        current.cancelAndJoin()
        assertFalse(speaker.isSpeaking.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun resumedCallerCanReplaceSpeechBeforeItIsSubmitted() = runTest {
        val engine = fakeSynthesis()
        val speaker = WebSpeaker(engine, ::fakeUtterance)
        val resumed = async(UnconfinedTestDispatcher(testScheduler)) {
            speaker.speak("Old")
            speaker.speak("Newest")
        }
        val replaced = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Replaced") }
        assertEquals(SpeakResult.Stopped, replaced.await())
        assertEquals(2, engine.spoken.length)
        assertEquals("Newest", engine.spoken[1]?.text)
        engine.start(1)
        assertTrue(speaker.isSpeaking.value)
        engine.end(1)
        assertEquals(SpeakResult.Done, resumed.await())
    }

    @Test
    fun explicitStopAndNativeCancellationReturnStopped() = runTest {
        val engine = fakeSynthesis()
        val speaker = WebSpeaker(engine, ::fakeUtterance)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Hello") }
        speaker.stop()
        assertEquals(SpeakResult.Stopped, pending.await())
        val next = async(start = CoroutineStart.UNDISPATCHED) { speaker.speak("Again") }
        engine.fail(1, "interrupted")
        assertEquals(SpeakResult.Stopped, next.await())
    }
}

private external interface FakeSynthesis : WebSpeechSynthesis {
    var voices: JsArray<WebVoice>
    val spoken: JsArray<FakeUtterance>
    val cancelCount: Int
    var throwOnSpeak: Boolean
    fun start(index: Int)
    fun end(index: Int)
    fun fail(index: Int, code: String)
}

private external interface FakeUtterance : WebUtterance {
    val text: String
    val rate: Double
}

private fun fakeSynthesis(): FakeSynthesis = js(
    """({
        voices: [{ lang: 'en-US', localService: true }], spoken: [], cancelCount: 0, throwOnSpeak: false,
        getVoices() { return this.voices; },
        speak(utterance) {
            if (this.throwOnSpeak) throw new Error('Engine unavailable');
            this.spoken.push(utterance);
        },
        cancel() { this.cancelCount++; },
        start(index) { this.spoken[index].onstart?.(); },
        end(index) { this.spoken[index].onend?.(); },
        fail(index, code) { this.spoken[index].onerror?.({ error: code }); }
    })""",
)

@Suppress("UNUSED_PARAMETER") // Used by the JavaScript body.
private fun fakeUtterance(text: String): FakeUtterance = js(
    "({ text, voice: null, lang: '', rate: 1, onstart: null, onend: null, onerror: null })",
)

@Suppress("UNUSED_PARAMETER") // Used by the JavaScript body.
private fun fakeVoices(local: Boolean): JsArray<WebVoice> = js("[{ lang: 'en-US', localService: local }]")

private fun emptyVoices(): JsArray<WebVoice> = js("[]")
