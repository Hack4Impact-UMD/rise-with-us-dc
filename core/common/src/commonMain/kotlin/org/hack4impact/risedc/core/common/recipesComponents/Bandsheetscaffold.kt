package org.hack4impact.risedc.core.common.recipesComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Primary-color band across the top (drawn under the status bar; [bandContent] can
 * hold the recipe photo later) with BackButton(OnColor) on it. A white sheet with
 * rounded top corners overlaps the band and holds the title, optional subtitle and a
 * scrolling content slot.
 *
 * Needs edge-to-edge so the band reaches behind the status bar.
 * [content] sits inside a verticalScroll Column, so don't put a LazyColumn in it.
 */
@Composable
fun BandSheetScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    bandHeight: Dp = 200.dp,     // band height below the status bar
    sheetOverlap: Dp = 24.dp,    // how far the sheet rises over the band
    bandContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxSize().background(colors.surface)) {
        // Band: background first so it also covers the status bar area.
        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.primary)
                .statusBarsPadding()
                .height(bandHeight),
            content = bandContent,
        )

        // Sheet
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(Modifier.height(bandHeight - sheetOverlap))
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = colors.surface,
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(top = 36.dp).semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        content = content,
                    )
                }
            }
        }

        // Back button on the band (same place as ScreenHeader's)
        BackButton(
            onClick = onBack,
            style = BackButtonStyle.OnColor,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .align(Alignment.TopStart),
        )
    }
}