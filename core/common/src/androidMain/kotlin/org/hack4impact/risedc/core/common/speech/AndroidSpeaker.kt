package org.hack4impact.risedc.core.common.speech

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private const val INITIALIZATION_TIMEOUT_MILLIS = 5_000L

internal class AndroidSpeaker(context: Context) : Speaker {
    private val context = context.applicationContext
    private val session = SpeechSession()

    override val isSpeaking = session.isSpeaking

    override suspend fun speak(text: String): SpeakResult {
        if (text.isBlank()) return SpeakResult.Done

        val speech = AndroidSpeechEngine(context)
        val request = session.begin(speech::close)
        speech.request = request
        return try {
            if (request.isActive && text.length > TextToSpeech.getMaxSpeechInputLength()) {
                request.finish(SpeakResult.Unavailable("Text exceeds the speech engine's input limit."))
            } else if (request.isActive) {
                speech.initialize()
                val initialized = withTimeoutOrNull(INITIALIZATION_TIMEOUT_MILLIS.milliseconds) {
                    select {
                        speech.initialized.onAwait { it == TextToSpeech.SUCCESS }
                        request.result.onAwait { false }
                    }
                }
                if (request.isActive) {
                    val failure = when (initialized) {
                        null -> "Speech engine initialization timed out."
                        false -> "Speech engine initialization failed."
                        true -> speech.configure()
                    }
                    if (failure != null) {
                        request.finish(SpeakResult.Unavailable(failure))
                    } else if (request.isActive) {
                        speech.speak(text)
                    }
                }
            }
            request.await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: RuntimeException) {
            request.finish(SpeakResult.Unavailable("Speech engine setup or playback failed."))
            request.await()
        } finally {
            request.finish(SpeakResult.Stopped)
        }
    }

    override fun stop() = session.stop()
}

private class AndroidSpeechEngine(private val context: Context) {
    lateinit var request: SpeechSession.SpeechRequest
    val initialized = CompletableDeferred<Int>()
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var engine: TextToSpeech? = null
    private var closed = false
    private var receiverRegistered = false
    private var focusGranted = false
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            handler.post { request.finish(SpeakResult.Stopped) }
        }
    }
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                request.finish(SpeakResult.Stopped)
            }
        }
    }

    fun initialize() {
        engine = TextToSpeech(context) { status ->
            // The constructor can call back before engine is assigned.
            handler.post {
                if (closed) {
                    releaseEngine()
                } else {
                    initialized.complete(status)
                }
            }
        }
    }

    @Suppress("ReturnCount") // Return the specific setup error.
    fun configure(): String? {
        val tts = engine ?: return "Speech engine is unavailable."
        val voices = tts.voices.orEmpty().filter { voice ->
            voice.locale?.language == Locale.ENGLISH.language &&
                !voice.isNetworkConnectionRequired &&
                voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }
        val voice = voices.firstOrNull { it.locale == Locale.US } ?: voices.firstOrNull()
            ?: return "An installed local English voice is unavailable."
        if (tts.setVoice(voice) != TextToSpeech.SUCCESS) return "Speech engine configuration failed."
        tts.setOnUtteranceProgressListener(progressListener())
        val attributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .build()
        tts.setAudioAttributes(attributes)
        val manager = audioManager ?: return "Speech audio is unavailable."
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(noisyReceiver, filter)
        }
        receiverRegistered = true
        focusGranted = requestFocus(manager, attributes) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return if (focusGranted) null else "Speech audio focus is unavailable."
    }

    fun speak(text: String) {
        if (engine?.speak(text, TextToSpeech.QUEUE_ADD, null, "speech") != TextToSpeech.SUCCESS) {
            request.finish(SpeakResult.Unavailable("Speech could not be queued."))
        }
    }

    fun close() {
        closed = true
        releaseEngine()
        if (receiverRegistered) {
            receiverRegistered = false
            runCatching { context.unregisterReceiver(noisyReceiver) }
        }
        if (focusGranted) {
            focusGranted = false
            runCatching { abandonFocus() }
        }
    }

    private fun releaseEngine() {
        // Initialization may finish after shutdown, so cleanup can run again.
        runCatching { engine?.shutdown() }
    }

    // Each engine's callbacks belong to one request.
    private fun progressListener() = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = progress(request::started)

        override fun onDone(utteranceId: String?) = progress { request.finish(SpeakResult.Done) }

        @Deprecated("Android forwards the current error callback to this method.")
        override fun onError(utteranceId: String?) = progress {
            request.finish(SpeakResult.Unavailable("Speech synthesis failed."))
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) = progress {
            request.finish(SpeakResult.Stopped)
        }
    }

    private fun progress(update: () -> Unit) {
        handler.post { update() }
    }

    @Suppress("DEPRECATION") // Required for API 24–25.
    private fun requestFocus(manager: AudioManager, attributes: AudioAttributes): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(focusListener, handler)
                .build()
            focusRequest = focus
            manager.requestAudioFocus(focus)
        } else {
            manager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }

    @Suppress("DEPRECATION") // Match the focus API used by requestFocus.
    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            audioManager?.abandonAudioFocus(focusListener)
        }
    }
}
