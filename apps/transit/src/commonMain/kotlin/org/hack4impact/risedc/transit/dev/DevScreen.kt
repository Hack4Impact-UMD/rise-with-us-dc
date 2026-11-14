package org.hack4impact.risedc.transit.dev

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.hack4impact.risedc.core.common.location.LocationProvider
import org.hack4impact.risedc.core.common.location.LocationResult
import org.koin.compose.koinInject
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun DevScreen(onBack: () -> Unit) {
    val locationProvider = koinInject<LocationProvider>()
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    @Suppress("TooGenericExceptionCaught")
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

    Column(modifier = Modifier.safeContentPadding()) {
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
            },
        )
        Button(onClick = {
            run { message }
        }) {
            Text("Submit")
        }

        Button(onClick = {
            run { locationProvider.currentLocation().describe() }
        }) {
            Text("Get Location")
        }

        Text(result)
    }
}

private fun LocationResult.describe(): String = when (this) {
    is LocationResult.Success -> "Lat $lat, Lng $lng (±${accuracyMeters}m)"
    LocationResult.PermissionDenied -> "Permission denied (on Android this also means never asked)"
    LocationResult.ApproximateOnly -> "Approximate location only"
    is LocationResult.Unavailable -> "Unavailable: $reason${detail?.let { " ($it)" } ?: ""}"
}
