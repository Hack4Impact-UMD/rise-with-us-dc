package org.hack4impact.risedc.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.hack4impact.risedc.core.ui.resources.Res
import org.hack4impact.risedc.core.ui.resources.ic_image_placeholder
import org.jetbrains.compose.resources.painterResource
enum class PictureCardSize { Large, Compact }

@Composable
fun PictureCard(
    label: String,
    painter: Painter?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PictureCardSize = PictureCardSize.Large,
    imageAspectRatio: Float? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val large = size == PictureCardSize.Large
    val ratio = imageAspectRatio ?: if (large) 8f / 5f else 2f

    // The whole card is one button. Pressed = built-in ripple; focused = outline.
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource,
        border = if (focused) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column {
            // Picture on top (null painter = tint + image icon)
            Box(Modifier.fillMaxWidth().aspectRatio(if (large) 8f / 5f else 2f), Alignment.Center) {
                if (painter != null) {
                    Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
                        Icon(
                            painter = painterResource(
                                Res.drawable.ic_image_placeholder,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // Centered label strip below
            Text(
                text = label,
                modifier = Modifier
                    .fillMaxWidth()
                    // added horizontal = 16.dp so wrapped text won't touch card edges at 200% font size
                    .padding(horizontal = 16.dp, vertical = if (large) 20.dp else 16.dp),
                style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
