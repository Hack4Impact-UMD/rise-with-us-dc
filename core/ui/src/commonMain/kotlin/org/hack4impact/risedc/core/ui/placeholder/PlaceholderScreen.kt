package org.hack4impact.risedc.core.ui.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Stand-in for a screen that hasn't been built yet. It shows the route name and buttons to the
 * screens it leads to, so the whole nav graph is clickable from day one. Replace each use with the
 * real screen.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    links: List<Pair<String, () -> Unit>> = emptyList(),
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeContentPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text("Placeholder", style = MaterialTheme.typography.bodyMedium)
            links.forEach { (label, onClick) -> Button(onClick = onClick) { Text(label) } }
            if (onBack != null) OutlinedButton(onClick = onBack) { Text("Back") }
        }
    }
}
