package org.hack4impact.risedc.transit.speech

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.hack4impact.risedc.core.common.speech.SpeakResult
import org.hack4impact.risedc.core.common.speech.Speaker
import org.koin.compose.koinInject

/** Temporary demo. */
@Composable
fun ReadAloudDemo(speaker: Speaker = koinInject()) {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("Ready") }
    var job by remember { mutableStateOf<Job?>(null) }
    val speaking by speaker.isSpeaking.collectAsState()
    val scope = rememberCoroutineScope()

    LifecycleResumeEffect(speaker) {
        onPauseOrDispose {
            job?.cancel()
            result = "Stopped"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Read-aloud demo", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Text to read") },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Text to read" },
        )
        Button(
            onClick = {
                job?.cancel()
                result = "Preparing speech"
                // Start speech within the browser's click handler.
                job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    val outcome = speaker.speak(input)
                    result = when (outcome) {
                        SpeakResult.Done -> "Done"
                        SpeakResult.Stopped -> "Stopped"
                        is SpeakResult.Unavailable -> outcome.reason
                    }
                }
            },
            enabled = input.isNotBlank(),
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Speak") }
        OutlinedButton(
            onClick = { speaker.stop() },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Stop") }
        Text(if (speaking) "Speaking" else "Result: $result")
    }
}
