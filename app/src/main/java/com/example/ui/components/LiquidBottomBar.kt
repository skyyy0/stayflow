package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.viewmodel.HomeTab

private data class TabItemSpec(
    val tab: HomeTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val tabSpecs = listOf(
    TabItemSpec(HomeTab.EXPLORE, Icons.Rounded.Explore, Icons.Outlined.Explore),
    TabItemSpec(HomeTab.SAVED, Icons.Rounded.Bookmarks, Icons.Outlined.Bookmarks),
    TabItemSpec(HomeTab.AI, Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    TabItemSpec(HomeTab.SETTINGS, Icons.Rounded.Shield, Icons.Outlined.Shield),
)

/**
 * Floating iOS Liquid Glass Tab Bar for handheld portrait screens.
 */
@Composable
fun LiquidBottomTabBar(
    activeTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val surface = MaterialTheme.colorScheme.surface
    val isDark = surface.luminance() < 0.5f

    val borderBrush = remember(isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (isDark) 0.26f else 0.90f),
                Color.White.copy(alpha = if (isDark) 0.08f else 0.35f),
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = surface.copy(alpha = if (isDark) 0.86f else 0.92f),
            border = BorderStroke(1.dp, borderBrush),
            shadowElevation = 12.dp,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabSpecs.forEach { spec ->
                    val selected = activeTab == spec.tab
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_pill_bg",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_content_color",
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.10f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                        label = "tab_icon_scale",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(pillColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTab(spec.tab)
                            }
                            .testTag("nav_tab_${spec.tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = if (selected) spec.selectedIcon else spec.unselectedIcon,
                                contentDescription = spec.tab.title,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = spec.tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Adaptive Liquid Glass Navigation Rail for tablet / wide screens.
 */
@Composable
fun LiquidNavigationRail(
    activeTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val surface = MaterialTheme.colorScheme.surface
    val isDark = surface.luminance() < 0.5f

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = surface.copy(alpha = if (isDark) 0.84f else 0.90f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (isDark) 0.16f else 0.70f)),
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
            .width(88.dp)
            .fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            tabSpecs.forEach { spec ->
                val selected = activeTab == spec.tab
                val pillColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                    label = "rail_pill",
                )
                val tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillColor)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTab(spec.tab)
                        }
                        .padding(vertical = 10.dp)
                        .testTag("rail_tab_${spec.tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = if (selected) spec.selectedIcon else spec.unselectedIcon,
                        contentDescription = spec.tab.title,
                        tint = tint,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = spec.tab.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
