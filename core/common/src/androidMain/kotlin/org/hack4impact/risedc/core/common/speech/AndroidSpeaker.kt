package org.hack4impact.risedc.core.common.speech

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private const val INITIALIZATION_TIMEOUT_MILLIS = 5_000L

/** Manages speech requests and reuses one Android TTS engine across the requests. */
internal class AndroidSpeaker(
    // Creates an engine that reports utterance start and completion events.
    private val createEngine: (
        onStarted: (String?) -> Unit,
        onFinished: (String?, SpeakResult) -> Unit,
    ) -> AndroidTtsEngine,
) : Speaker {
    constructor(context: Context) : this({ started, finished ->
        AndroidSpeechEngine(context.applicationContext, started, finished)
    }) {
        // Release the engine when the app's single Activity stops.
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStopped(activity: Activity) = release()
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    private val session = SpeechSession()
    private var engine: AndroidTtsEngine? = null
    private var playback: Playback? = null
    private var nextUtteranceId = 0L

    override val isSpeaking = session.isSpeaking

    override suspend fun speak(text: String): SpeakResult {
        if (text.isBlank()) return SpeakResult.Done

        val id = (++nextUtteranceId).toString()
        var requestEngine: AndroidTtsEngine? = null
        val request = session.begin {
            if (playback?.id == id) {
                playback = null
                requestEngine?.stop()
            }
        }
        return try {
            if (request.isActive) {
                playback = Playback(id, request)
                val speech = engine ?: createEngine(
                    { utteranceId -> playback?.takeIf { it.id == utteranceId }?.request?.started() },
                    { utteranceId, result -> playback?.takeIf { it.id == utteranceId }?.request?.finish(result) },
                ).also { engine = it }
                requestEngine = speech
                val failure = prepare(speech, request, text, id)
                if (failure != null) {
                    request.finish(SpeakResult.Unavailable(failure))
                } else if (request.isActive && !speech.speak(text, id)) {
                    discard(speech)
                    request.finish(SpeakResult.Unavailable("Speech could not be queued."))
                }
            }
            request.await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: RuntimeException) {
            requestEngine?.let(::discard)
            request.finish(SpeakResult.Unavailable("Speech engine setup or playback failed."))
            request.await()
        } finally {
            request.finish(SpeakResult.Stopped)
        }
    }

    override fun stop() = session.stop()

    /** Stops speech and shuts down the engine. Later requests can create a new one. */
    fun release() {
        val previous = engine
        engine = null
        session.stop()
        previous?.close()
    }

    private suspend fun prepare(
        speech: AndroidTtsEngine,
        request: SpeechSession.SpeechRequest,
        text: String,
        id: String,
    ): String? {
        if (text.length > speech.maxInputLength) return "Text exceeds the speech engine's input limit."
        speech.initialize()
        val initialized = withTimeoutOrNull(INITIALIZATION_TIMEOUT_MILLIS.milliseconds) {
            select {
                speech.initialized.onAwait { it }
                request.result.onAwait { false }
            }
        }
        return when {
            !request.isActive -> null
            initialized == true -> speech.configure(id)
            else -> {
                discard(speech)
                if (initialized == null) {
                    "Speech engine initialization timed out."
                } else {
                    "Speech engine initialization failed."
                }
            }
        }
    }

    private fun discard(speech: AndroidTtsEngine) {
        if (engine === speech) engine = null
        speech.close()
    }

    private class Playback(val id: String, val request: SpeechSession.SpeechRequest)
}

/** Retained TTS connection; [stop] releases only the current request's resources. */
internal interface AndroidTtsEngine {
    val initialized: Deferred<Boolean>
    val maxInputLength: Int
    fun initialize()
    fun configure(utteranceId: String): String?
    fun speak(text: String, utteranceId: String): Boolean
    fun stop()
    fun close()
}

private class AndroidSpeechEngine(
    private val context: Context,
    private val onStarted: (String?) -> Unit,
    private val onFinished: (String?, SpeakResult) -> Unit,
) : AndroidTtsEngine {
    override val initialized = CompletableDeferred<Boolean>()
    override val maxInputLength get() = TextToSpeech.getMaxSpeechInputLength()
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var engine: TextToSpeech? = null
    private var ready = false
    private var closed = false
    private var noisyReceiver: BroadcastReceiver? = null
    private var focusGranted = false
    private var focusRequest: AudioFocusRequest? = null
    private var focusListener: AudioManager.OnAudioFocusChangeListener? = null

    override fun initialize() {
        if (engine != null) return
        engine = TextToSpeech(context) { status ->
            // The constructor can call back before engine is assigned.
            handler.post {
                if (closed) {
                    releaseEngine()
                } else {
                    ready = status == TextToSpeech.SUCCESS
                    if (ready) engine?.setOnUtteranceProgressListener(progressListener())
                    initialized.complete(ready)
                }
            }
        }
    }

    @Suppress("ReturnCount")
    override fun configure(utteranceId: String): String? {
        val tts = engine ?: return "Speech engine is unavailable."
        // A failed query must discard the connection so the next request can retry.
        val voices = checkNotNull(tts.voices) { "Speech engine voice query failed." }.filter { voice ->
            voice.locale?.language == Locale.ENGLISH.language &&
                !voice.isNetworkConnectionRequired &&
                voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }
        val voice = voices.firstOrNull { it.locale == Locale.US } ?: voices.firstOrNull()
            ?: return "An installed local English voice is unavailable."
        if (tts.setVoice(voice) != TextToSpeech.SUCCESS) return "Speech engine configuration failed."
        val attributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .build()
        tts.setAudioAttributes(attributes)
        val manager = audioManager ?: return "Speech audio is unavailable."
        // Capture this request's ID so queued interruptions cannot stop its replacement.
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                finish(utteranceId, SpeakResult.Stopped)
            }
        }
        focusListener = listener
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    finish(utteranceId, SpeakResult.Stopped)
                }
            }
        }
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        noisyReceiver = receiver
        focusGranted = requestFocus(manager, attributes, listener) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return if (focusGranted) null else "Speech audio focus is unavailable."
    }

    override fun speak(text: String, utteranceId: String): Boolean =
        engine?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId) == TextToSpeech.SUCCESS

    override fun stop() {
        if (ready) runCatching { engine?.stop() }
        noisyReceiver?.let { receiver ->
            noisyReceiver = null
            runCatching { context.unregisterReceiver(receiver) }
        }
        if (focusGranted) {
            focusGranted = false
            runCatching { abandonFocus() }
        }
    }

    override fun close() {
        closed = true
        ready = false
        releaseEngine()
        stop()
    }

    private fun releaseEngine() {
        // Initialization may finish after shutdown, so cleanup can run again.
        runCatching { engine?.shutdown() }
    }

    private fun progressListener() = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            handler.post { onStarted(utteranceId) }
        }

        override fun onDone(utteranceId: String?) = finish(utteranceId, SpeakResult.Done)

        @Deprecated("Android forwards the current error callback to this method.")
        override fun onError(utteranceId: String?) = finish(
            utteranceId,
            SpeakResult.Unavailable("Speech synthesis failed."),
        )

        override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId, SpeakResult.Stopped)
    }

    private fun finish(utteranceId: String?, outcome: SpeakResult) {
        handler.post { onFinished(utteranceId, outcome) }
    }

    @Suppress("DEPRECATION") // Required for API 24–25.
    private fun requestFocus(
        manager: AudioManager,
        attributes: AudioAttributes,
        listener: AudioManager.OnAudioFocusChangeListener,
    ): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(listener, handler)
            .build()
        focusRequest = focus
        manager.requestAudioFocus(focus)
    } else {
        manager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
    }

    @Suppress("DEPRECATION") // Match the focus API used by requestFocus.
    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            focusListener?.let { audioManager?.abandonAudioFocus(it) }
        }
    }
}
