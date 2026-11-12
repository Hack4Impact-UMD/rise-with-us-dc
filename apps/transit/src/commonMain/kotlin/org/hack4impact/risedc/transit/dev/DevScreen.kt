package org.hack4impact.risedc.transit.dev

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.ui.Modifier

@Composable
fun DevScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun run(block: suspend () -> String) {
        scope.launch {
            result = "Running…"
            result = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    Column (modifier = Modifier.safeContentPadding()){
        Button(onClick = onBack) {
            Text("Back")
        }

        Button(onClick = {
            run { "Pong" }
        }) {
            Text("Send Ping")
        }

        OutlinedTextField(
            value = message,
            onValueChange = {
                message = it
            },
            label = {
                Text("Enter a message")
            }
        )
        Button(onClick = {
            run { message }
        }) {
            Text("Submit")
        }

        Text(result)
    }
}
