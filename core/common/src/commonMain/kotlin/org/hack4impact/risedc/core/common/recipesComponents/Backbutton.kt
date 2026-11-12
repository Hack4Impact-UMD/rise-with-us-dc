package org.hack4impact.risedc.core.common.recipesComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class BackButtonStyle {
    /** Outlined disc, for use on white. */
    OnLight,

    /** White disc, for use on the primary-color band. */
    OnColor,
}

/** Round back arrow: drawn at 32dp inside a 48dp touch target. Reads "Back". */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BackButtonStyle = BackButtonStyle.OnLight,
    label: String = "Back",
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp) // minimum touch target
            .semantics { contentDescription = label },
    ) {
        val discModifier = when (style) {
            BackButtonStyle.OnLight ->
                Modifier.background(colors.surface, CircleShape).border(1.dp, colors.outlineVariant, CircleShape)
            BackButtonStyle.OnColor ->
                Modifier.background(Color.White, CircleShape)
        }
        Box(
            modifier = Modifier.size(32.dp).then(discModifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = BackArrowIcon,
                contentDescription = null, // described on the button
                modifier = Modifier.size(18.dp),
                tint = colors.onSurface,
            )
        }
    }
}
