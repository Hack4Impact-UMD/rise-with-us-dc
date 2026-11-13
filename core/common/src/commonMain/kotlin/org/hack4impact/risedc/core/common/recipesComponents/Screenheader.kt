package org.hack4impact.risedc.core.common.recipesComponents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Optional back button on the left, centered title. Same position on every screen
 * (design rule 1). Pass onBack = null on screens without a back button.
 */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (onBack != null) {
            BackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
            )
        }
        Text(
            text = title,
            // Reserve the button's width on both sides so the title stays truly centered.
            modifier = Modifier.padding(horizontal = 72.dp).semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
