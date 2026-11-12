package org.hack4impact.risedc.core.common.recipesComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** What the list needs to draw one card. */
data class PictureCardItem(
    val id: String,
    val label: String,
    val painter: Painter? = null,
)

/** Width at which a window leaves the Compact size class (Material: 600dp). */
private val CompactWidthLimit = 600.dp

/**
 * One column on phones; adaptive grid on tablets (width >= 600dp, i.e. not the
 * Compact window size class). Vertical scroll only.
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
        if (maxWidth < CompactWidthLimit) {
            LazyColumn(
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                items(items, key = { it.id }) { item ->
                    PictureCard(
                        label = item.label,
                        painter = item.painter,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minCardWidth),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                items(items, key = { it.id }) { item ->
                    PictureCard(
                        label = item.label,
                        painter = item.painter,
                        onClick = { onItemClick(item) },
                    )
                }
            }
        }
    }
}