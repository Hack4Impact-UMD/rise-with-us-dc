package org.hack4impact.risedc.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** What the list needs to draw one card. */
data class PictureCardItem(val id: String, val label: String, val painter: Painter? = null)

/** Width at which a window leaves the Compact size class (Material: 600dp). */
private val CompactWidthLimit = 600.dp

/**
 * One column on phones; grid on tablets (width >= 600dp, i.e. not the
 * Compact window size class).
 *
 * Not lazy, so it is safe inside a verticalScroll parent such as
 * [BandSheetScaffold]'s content slot. It does not scroll itself and composes every
 * item, so keep it to dozens of items, not hundreds. When used standalone, wrap it
 * in a verticalScroll yourself.
 *
 * The layout switch uses this list's own measured width, not the window's.
 */
@Composable
fun PictureCardList(
    items: List<PictureCardItem>,
    onItemClick: (PictureCardItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    spacing: Dp = 20.dp,
    minCardWidth: Dp = 280.dp,
) {
    BoxWithConstraints(modifier) {
        val layoutDirection = LocalLayoutDirection.current
        val available = maxWidth -
            contentPadding.calculateStartPadding(layoutDirection) -
            contentPadding.calculateEndPadding(layoutDirection)
        val columns = if (maxWidth < CompactWidthLimit) {
            1
        } else {
            ((available + spacing) / (minCardWidth + spacing)).toInt().coerceAtLeast(1)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    rowItems.forEach { item ->
                        PictureCard(
                            label = item.label,
                            painter = item.painter,
                            onClick = { onItemClick(item) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
