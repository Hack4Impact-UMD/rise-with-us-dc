@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.hack4impact.risedc.core.common.speech

import kotlinx.cinterop.ObjCSignatureOverride
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionModeVoicePrompt
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOldDeviceUnavailable
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesisVoiceTraitIsNoveltyVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.setActive
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS has no general speech-error callback. Silent failures can leave the call waiting
 * until Stop, cancellation, replacement or interruption.
 */
internal class IosSpeaker : Speaker {
    private val session = SpeechSession()
    private val speechDelegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didStartSpeechUtterance: AVSpeechUtterance) {
            withPlayback(didStartSpeechUtterance) { it.request.started() }
        }

        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
            withPlayback(didFinishSpeechUtterance) { it.request.finish(SpeakResult.Done) }
        }

        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) {
            withPlayback(didCancelSpeechUtterance) { it.request.finish(SpeakResult.Stopped) }
        }
    }
    private val synthesizer = AVSpeechSynthesizer().apply { delegate = speechDelegate }
    private val audioSession = AVAudioSession.sharedInstance()
    private val notifications = NSNotificationCenter.defaultCenter
    private var playback: Playback? = null

    override val isSpeaking = session.isSpeaking

    // Check input and voices before audio setup.
    @Suppress("ReturnCount")
    override suspend fun speak(text: String): SpeakResult {
        if (text.isBlank()) return SpeakResult.Done

        val observers = mutableListOf<NSObjectProtocol>()
        var audioActive = false
        val request = session.begin {
            playback = null
            observers.forEach { notifications.removeObserver(it) }
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
            if (audioActive) {
                audioSession.setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
            }
        }
        if (!request.isActive) return request.await()
        val voice = englishVoice()
        if (voice == null) {
            request.finish(SpeakResult.Unavailable("No local English voice is available."))
            return request.await()
        }

        observers += notifications.addObserverForName(
            AVAudioSessionInterruptionNotification,
            audioSession,
            NSOperationQueue.mainQueue,
        ) { notification ->
            val type = notification?.userInfo?.get(AVAudioSessionInterruptionTypeKey) as? NSNumber
            if (type?.unsignedLongLongValue == AVAudioSessionInterruptionTypeBegan) {
                request.finish(SpeakResult.Stopped)
            }
        }
        observers += notifications.addObserverForName(
            AVAudioSessionRouteChangeNotification,
            audioSession,
            NSOperationQueue.mainQueue,
        ) { notification ->
            val reason = notification?.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber
            if (reason?.unsignedLongLongValue == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
                request.finish(SpeakResult.Stopped)
            }
        }

        val configured = audioSession.setCategory(
            AVAudioSessionCategoryPlayback,
            AVAudioSessionModeVoicePrompt,
            0uL,
            null,
        )
        audioActive = configured && audioSession.setActive(true, null)
        if (!audioActive) {
            request.finish(SpeakResult.Unavailable("Audio playback is unavailable."))
        } else if (request.isActive) {
            val utterance = AVSpeechUtterance(string = text).apply {
                this.voice = voice
                rate = AVSpeechUtteranceDefaultSpeechRate * SPEECH_RATE_FACTOR
            }
            playback = Playback(utterance, request)
            synthesizer.speakUtterance(utterance)
        }
        return request.await()
    }

    override fun stop() {
        session.stop()
    }

    private fun withPlayback(utterance: AVSpeechUtterance, block: (Playback) -> Unit) {
        val callback: () -> Unit = { playback?.takeIf { it.utterance == utterance }?.let(block) }
        if (NSThread.isMainThread) callback() else dispatch_async(dispatch_get_main_queue(), callback)
    }

    private fun englishVoice(): AVSpeechSynthesisVoice? {
        val voices = AVSpeechSynthesisVoice.speechVoices().filterIsInstance<AVSpeechSynthesisVoice>()
            .filter { it.voiceTraits and AVSpeechSynthesisVoiceTraitIsNoveltyVoice == 0uL }
        return voices.firstOrNull { it.language == "en-US" }
            ?: voices.firstOrNull { it.language == "en" || it.language.startsWith("en-") }
    }

    private class Playback(val utterance: AVSpeechUtterance, val request: SpeechSession.SpeechRequest)
}
