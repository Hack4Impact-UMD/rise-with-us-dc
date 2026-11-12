package org.hack4impact.risedc.core.common.speech

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Coordinates requests, with one active at a time. Main thread only. */
internal class SpeechSession {
    private var active: SpeechRequest? = null
    private val speaking = MutableStateFlow(false)
    val isSpeaking = speaking.asStateFlow()

    /** Replaces the current request. [release] cleans up the new one. */
    fun begin(release: () -> Unit): SpeechRequest {
        val previous = active
        val request = SpeechRequest(release)
        // Set active first. Ending the old request can start newer speech.
        active = request
        previous?.finish(SpeakResult.Stopped)
        if (request.isActive) speaking.value = false
        return request
    }

    fun stop() {
        active?.finish(SpeakResult.Stopped)
    }

    /** Tracks one speech call's playback, result, and cleanup. */
    inner class SpeechRequest(private val release: () -> Unit) {
        val result = CompletableDeferred<SpeakResult>()
        val isActive: Boolean get() = active === this
        private var finished = false
        fun started() {
            if (isActive) speaking.value = true
        }

        /** Runs [release] once before updating [isSpeaking] or completing [result]. */
        fun finish(outcome: SpeakResult) {
            if (finished) return
            finished = true
            if (isActive) active = null
            try {
                release()
            } finally {
                if (active == null) speaking.value = false
                result.complete(outcome)
            }
        }

        /** Waits for [result]; cancellation stops only this request. */
        suspend fun await(): SpeakResult = try {
            result.await()
        } finally {
            finish(SpeakResult.Stopped)
        }
    }
}
