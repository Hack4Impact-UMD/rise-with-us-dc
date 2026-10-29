package org.hack4impact.risedc.core.common.speech

import kotlinx.coroutines.flow.StateFlow

/** Local English speech. Call [speak] and [stop] from the main thread. */
interface Speaker {
    /**
     * Speaks [text], replacing current speech. Blank input does nothing.
     * Returns when playback ends or fails; cancellation stops only this request.
     * Platform input limits return [SpeakResult.Unavailable].
     */
    suspend fun speak(text: String): SpeakResult

    /** Stops the current request, including initialization. */
    fun stop()

    /** True during playback; false during initialization. */
    val isSpeaking: StateFlow<Boolean>
}

sealed interface SpeakResult {
    data object Done : SpeakResult
    data object Stopped : SpeakResult
    data class Unavailable(val reason: String) : SpeakResult
}
