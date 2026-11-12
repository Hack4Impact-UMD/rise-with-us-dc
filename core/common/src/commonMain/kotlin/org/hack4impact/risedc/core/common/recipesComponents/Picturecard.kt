package org.hack4impact.risedc.core.common.recipesComponents

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class PictureCardSize { Large, Compact }

@Composable
fun PictureCard(
    label: String,
    painter: Painter?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PictureCardSize = PictureCardSize.Large,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val large = size == PictureCardSize.Large

    // The whole card is one button. Pressed = built-in ripple; focused = outline.
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource,
        border = if (focused) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column {
            // Picture on top (null painter = tint + image icon)
            Box(Modifier.fillMaxWidth().aspectRatio(if (large) 8f / 5f else 2f), Alignment.Center) {
                if (painter != null) {
                    Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
                        Icon(PlaceholderIcon, null, Modifier.size(40.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Centered label strip below
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth().padding(vertical = if (large) 22.dp else 18.dp),
                style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val PlaceholderIcon: ImageVector by lazy {
    ImageVector.Builder("Image", 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString("M19,5v14H5V5h14m0,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM14.14,11.86l-3,3.87L9,13.14 6,17h12l-3.86,-5.14z").toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}