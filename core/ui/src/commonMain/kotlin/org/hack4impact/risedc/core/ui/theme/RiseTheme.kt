package org.hack4impact.risedc.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Root theme for every RISE DC app.
 * TODO(first ticket "theme + adaptive scaffold"): high-contrast dark palette, Lexend type,
 * touch-target sizes, window size classes. See the design rules in the wiki.
 */
@Composable
fun RiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
