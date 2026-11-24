package org.hack4impact.risedc.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class RiseButtonStyle {
    Primary,
    OnColor,
}

@Composable
fun RiseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    style: RiseButtonStyle = RiseButtonStyle.Primary,
    leadingIcon: Painter? = null,
) {
    val colors = MaterialTheme.colorScheme
    val buttonColors = when (style) {
        RiseButtonStyle.Primary -> ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        )
        RiseButtonStyle.OnColor -> ButtonDefaults.buttonColors(
            containerColor = colors.surface,
            contentColor = colors.onSurface,
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = buttonColors,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}
