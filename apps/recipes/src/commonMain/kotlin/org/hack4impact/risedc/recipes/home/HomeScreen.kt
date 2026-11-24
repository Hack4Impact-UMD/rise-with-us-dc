package org.hack4impact.risedc.recipes.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.hack4impact.risedc.core.ui.components.BrandHeader
import org.hack4impact.risedc.core.ui.components.PictureCard
import org.hack4impact.risedc.core.ui.components.PictureCardSize
import org.hack4impact.risedc.core.ui.components.RiseButton
import org.hack4impact.risedc.core.ui.components.RiseButtonStyle
import org.hack4impact.risedc.core.ui.resources.Res
import org.hack4impact.risedc.core.ui.resources.ic_add_circle
import org.hack4impact.risedc.core.ui.resources.ic_settings
import org.hack4impact.risedc.core.ui.resources.rise_logo
import org.jetbrains.compose.resources.painterResource

private const val APP_NAME = "RISE Recipes"
private const val TABLET_BAND_FRACTION = 0.33f
private const val PHONE_CARD_ASPECT_RATIO = 2.1f
private const val TABLET_HERO_CARD_ASPECT_RATIO = 2.6f
private const val TABLET_PAIR_CARD_ASPECT_RATIO = 1.35f
private const val LARGE_FONT_SCALE_THRESHOLD = 1.5f

private val TabletWidthBreakpoint = 600.dp
private val MinTabletBandWidth = 220.dp
private val MaxTabletBandWidth = 340.dp
private val MinSideBySideWidthAtLargeFont = 400.dp
private val HomeButtonShape = RoundedCornerShape(8.dp)

@Composable
fun HomeScreen(
    onCreateRecipeClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onGroceriesClick: () -> Unit,
    onAddRecipeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logo = painterResource(Res.drawable.rise_logo)
    val addIcon = painterResource(Res.drawable.ic_add_circle)
    val settingsIcon = painterResource(Res.drawable.ic_settings)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth < TabletWidthBreakpoint) {
                HomePhoneLayout(
                    logo = logo,
                    addIcon = addIcon,
                    settingsIcon = settingsIcon,
                    onCreateRecipeClick = onCreateRecipeClick,
                    onRecipesClick = onRecipesClick,
                    onGroceriesClick = onGroceriesClick,
                    onAddRecipeClick = onAddRecipeClick,
                    onSettingsClick = onSettingsClick,
                    minHeight = maxHeight,
                )
            } else {
                val bandWidth = (maxWidth * TABLET_BAND_FRACTION).coerceIn(
                    MinTabletBandWidth,
                    MaxTabletBandWidth,
                )
                HomeTabletLayout(
                    logo = logo,
                    onCreateRecipeClick = onCreateRecipeClick,
                    onRecipesClick = onRecipesClick,
                    onGroceriesClick = onGroceriesClick,
                    onAddRecipeClick = onAddRecipeClick,
                    onSettingsClick = onSettingsClick,
                    bandWidth = bandWidth,
                    minHeight = maxHeight,
                )
            }
        }
    }
}

@Composable
private fun HomePhoneLayout(
    logo: Painter,
    addIcon: Painter,
    settingsIcon: Painter,
    onCreateRecipeClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onGroceriesClick: () -> Unit,
    onAddRecipeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    minHeight: Dp,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .heightIn(min = minHeight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BrandHeader(
            logo = logo,
            appName = APP_NAME,
            contentColor = colors.primary,
            logoHeight = 60.dp,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PictureCard(
                label = "Create Recipe",
                painter = null,
                onClick = onCreateRecipeClick,
                modifier = Modifier.fillMaxWidth(),
                size = PictureCardSize.Compact,
                imageAspectRatio = PHONE_CARD_ASPECT_RATIO,
            )
            PictureCard(
                label = "Recipes",
                painter = null,
                onClick = onRecipesClick,
                modifier = Modifier.fillMaxWidth(),
                size = PictureCardSize.Compact,
                imageAspectRatio = PHONE_CARD_ASPECT_RATIO,
            )
            PictureCard(
                label = "Groceries",
                painter = null,
                onClick = onGroceriesClick,
                modifier = Modifier.fillMaxWidth(),
                size = PictureCardSize.Compact,
                imageAspectRatio = PHONE_CARD_ASPECT_RATIO,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RiseButton(
                text = "Add Recipe",
                onClick = onAddRecipeClick,
                modifier = Modifier.fillMaxWidth(),
                shape = HomeButtonShape,
                leadingIcon = addIcon,
            )
            RiseButton(
                text = "Settings",
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = HomeButtonShape,
                leadingIcon = settingsIcon,
            )
        }
    }
}

@Composable
private fun HomeTabletLayout(
    logo: Painter,
    onCreateRecipeClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onGroceriesClick: () -> Unit,
    onAddRecipeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    bandWidth: Dp,
    minHeight: Dp,
) {
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale

    Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(bandWidth)
                .fillMaxHeight()
                .background(colors.primary)
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
                )
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            BrandHeader(
                logo = logo,
                appName = APP_NAME,
                contentColor = colors.onPrimary,
                logoBadgeColor = colors.surface,
                logoHeight = 64.dp,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RiseButton(
                    text = "Add Recipe",
                    onClick = onAddRecipeClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = HomeButtonShape,
                    style = RiseButtonStyle.OnColor,
                )
                RiseButton(
                    text = "Settings",
                    onClick = onSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = HomeButtonShape,
                    style = RiseButtonStyle.OnColor,
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            val stackBottomCards = maxWidth < MinSideBySideWidthAtLargeFont &&
                fontScale >= LARGE_FONT_SCALE_THRESHOLD

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
                    )
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                PictureCard(
                    label = "Create Recipe",
                    painter = null,
                    onClick = onCreateRecipeClick,
                    modifier = Modifier.fillMaxWidth(),
                    size = PictureCardSize.Large,
                    imageAspectRatio = if (stackBottomCards) {
                        PHONE_CARD_ASPECT_RATIO
                    } else {
                        TABLET_HERO_CARD_ASPECT_RATIO
                    },
                )

                if (stackBottomCards) {
                    PictureCard(
                        label = "Recipes",
                        painter = null,
                        onClick = onRecipesClick,
                        modifier = Modifier.fillMaxWidth(),
                        size = PictureCardSize.Large,
                        imageAspectRatio = PHONE_CARD_ASPECT_RATIO,
                    )
                    PictureCard(
                        label = "Groceries",
                        painter = null,
                        onClick = onGroceriesClick,
                        modifier = Modifier.fillMaxWidth(),
                        size = PictureCardSize.Large,
                        imageAspectRatio = PHONE_CARD_ASPECT_RATIO,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        PictureCard(
                            label = "Recipes",
                            painter = null,
                            onClick = onRecipesClick,
                            modifier = Modifier.weight(1f),
                            size = PictureCardSize.Large,
                            imageAspectRatio = TABLET_PAIR_CARD_ASPECT_RATIO,
                        )
                        PictureCard(
                            label = "Groceries",
                            painter = null,
                            onClick = onGroceriesClick,
                            modifier = Modifier.weight(1f),
                            size = PictureCardSize.Large,
                            imageAspectRatio = TABLET_PAIR_CARD_ASPECT_RATIO,
                        )
                    }
                }
            }
        }
    }
}
