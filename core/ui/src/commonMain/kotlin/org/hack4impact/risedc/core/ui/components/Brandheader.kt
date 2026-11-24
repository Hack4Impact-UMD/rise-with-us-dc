package org.hack4impact.risedc.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RISE logo above the app name. The logo is passed in so RISE's vector file can live
 * in the app's resources (e.g. painterResource(Res.drawable.rise_logo)).
 * The app name is a parameter because it is still undecided ("APP NAME?" in the mockup).
 */
@Composable
fun BrandHeader(
    logo: Painter,
    appName: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    logoBadgeColor: Color? = null,
    logoHeight: Dp = 64.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (logoBadgeColor != null) {
            Box(
                modifier = Modifier
                    .size(logoHeight + 28.dp)
                    .clip(CircleShape)
                    .background(logoBadgeColor)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = logo,
                    contentDescription = "RISE",
                    modifier = Modifier.height(logoHeight),
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            Image(
                painter = logo,
                contentDescription = "RISE",
                modifier = Modifier.height(logoHeight), // CHANGED: uses logoHeight param (default 64.dp)
                contentScale = ContentScale.Fit, // ADDED: keeps logo aspect ratio intact
            )
        }
        Text(
            text = appName,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}
