@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.hack4impact.risedc.core.common.speech

import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsException
import kotlin.js.toList

internal class WebSpeaker(
    private val synthesis: WebSpeechSynthesis? = browserSpeechSynthesis(),
    private val utteranceFactory: (String) -> WebUtterance = ::browserUtterance,
) : Speaker {
    private val session = SpeechSession()
    override val isSpeaking = session.isSpeaking

    init {
        englishVoice()
    }

    @Suppress("ReturnCount") // Blank or replaced requests skip native setup.
    override suspend fun speak(text: String): SpeakResult {
        if (text.isBlank()) return SpeakResult.Done
        var utterance: WebUtterance? = null
        val request = session.begin {
            utterance?.let {
                it.onstart = null
                it.onend = null
                it.onerror = null
            }
            utterance = null
            try {
                synthesis?.cancel()
            } catch (_: JsException) {
                // Finish cleanup even if browser cancellation fails.
            }
        }
        try {
            if (!request.isActive) return request.await()
            val selectedVoice = englishVoice()
            if (synthesis == null || selectedVoice == null) {
                request.finish(
                    SpeakResult.Unavailable("No local English voice is ready. Check installed voices and try again."),
                )
            } else {
                val speech = utteranceFactory(text)
                utterance = speech
                speech.voice = selectedVoice
                speech.lang = selectedVoice.lang
                speech.rate = SPEECH_RATE_FACTOR.toDouble()
                speech.onstart = { request.started() }
                speech.onend = { request.finish(SpeakResult.Done) }
                speech.onerror = { event ->
                    val outcome = when (event.error) {
                        "canceled", "interrupted" -> SpeakResult.Stopped
                        "not-allowed" -> SpeakResult.Unavailable("Speech was blocked. Tap Speak again.")
                        "text-too-long" -> SpeakResult.Unavailable("Text is too long. Try a shorter instruction.")
                        else -> SpeakResult.Unavailable("Speech could not be played.")
                    }
                    request.finish(outcome)
                }
                // Browsers may require speak() to run directly from a click.
                synthesis.speak(speech)
            }
            return request.await()
        } catch (_: JsException) {
            request.finish(SpeakResult.Unavailable("Speech could not be started."))
            return request.await()
        } finally {
            request.finish(SpeakResult.Stopped)
        }
    }

    override fun stop() = session.stop()

    private fun englishVoice(): WebVoice? {
        val voices = try {
            synthesis?.getVoices()?.toList().orEmpty()
        } catch (_: JsException) {
            emptyList()
        }
        val english = voices.filter { it.localService && it.lang.substringBefore('-').equals("en", ignoreCase = true) }
        return english.firstOrNull { it.lang.equals("en-US", ignoreCase = true) } ?: english.firstOrNull()
    }
}

internal external interface WebSpeechSynthesis : JsAny {
    fun getVoices(): JsArray<WebVoice>
    fun speak(utterance: WebUtterance)
    fun cancel()
}

internal external interface WebVoice : JsAny {
    val lang: String
    val localService: Boolean
}

internal external interface WebUtterance : JsAny {
    var voice: WebVoice?
    var lang: String
    var rate: Double
    var onstart: (() -> Unit)?
    var onend: (() -> Unit)?
    var onerror: ((WebSpeechError) -> Unit)?
}

internal external interface WebSpeechError : JsAny {
    val error: String
}

private fun browserSpeechSynthesis(): WebSpeechSynthesis = js(
    """typeof window.speechSynthesis !== 'undefined' && typeof SpeechSynthesisUtterance !== 'undefined'
        ? window.speechSynthesis : null""",
)

@Suppress("UNUSED_PARAMETER") // Used by the JavaScript body.
private fun browserUtterance(text: String): WebUtterance = js("new SpeechSynthesisUtterance(text)")
