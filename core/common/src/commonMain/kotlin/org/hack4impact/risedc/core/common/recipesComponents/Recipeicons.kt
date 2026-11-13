package org.hack4impact.risedc.core.common.recipesComponents

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private const val ICON_SIZE = 24f

private fun icon(name: String, path: String): ImageVector {
    val builder = ImageVector.Builder(name, ICON_SIZE.dp, ICON_SIZE.dp, ICON_SIZE, ICON_SIZE)
    return builder.addPath(
        PathParser().parsePathString(path).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

internal val BackArrowIcon: ImageVector by lazy {
    icon("BackArrow", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")
}

internal val CheckIcon: ImageVector by lazy {
    icon("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")
}

internal val PlaceholderIcon: ImageVector by lazy {
    icon(
        "Image",
        "M19,5v14H5V5h14m0,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5" +
            "c0,-1.1 -0.9,-2 -2,-2zM14.14,11.86l-3,3.87L9,13.14 6,17h12l-3.86,-5.14z",
    )
}
