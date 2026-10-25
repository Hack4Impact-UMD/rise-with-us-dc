package org.hack4impact.risedc.transit.dev

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*

@Composable
fun DevScreen() {
    var result by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column {
        Button(onClick = {
            result = "Pong"
        }) {
            Text("Send Ping")
        }

        Text(result)

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Enter a message") }
        )

        Button(onClick = {
            result = message
        }) {
            Text("Submit")
        }

        Text(result)
    }
}