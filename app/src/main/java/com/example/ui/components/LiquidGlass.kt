package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lightweight GPU-accelerated ambient backdrop with subtle iOS Liquid Glass aura orbs.
 * Uses pure radial Canvas gradients so scrolling stays locked at 60/120fps on all devices.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val isDark = bg.luminance() < 0.5f

    Box(
        modifier = modifier
            .background(bg)
            .drawBehind {
                val topOrbRadius = size.width * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = if (isDark) 0.16f else 0.10f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.06f),
                        radius = topOrbRadius,
                    ),
                    radius = topOrbRadius,
                    center = Offset(size.width * 0.15f, size.height * 0.06f),
                )

                val midOrbRadius = size.width * 0.78f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = if (isDark) 0.14f else 0.09f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.90f, size.height * 0.36f),
                        radius = midOrbRadius,
                    ),
                    radius = midOrbRadius,
                    center = Offset(size.width * 0.90f, size.height * 0.36f),
                )
            },
        content = content,
    )
}

/**
 * iOS-inspired Liquid Glass panel with translucent frosted fill and specular edge highlight.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val isDark = surface.luminance() < 0.5f

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "glass_press_scale",
    )

    val glassColor = if (isDark) {
        surface.copy(alpha = if (highlighted) 0.82f else 0.72f)
    } else {
        surface.copy(alpha = if (highlighted) 0.92f else 0.84f)
    }

    val borderBrush = remember(isDark, highlighted, primary) {
        Brush.linearGradient(
            colors = if (highlighted) {
                listOf(
                    primary.copy(alpha = if (isDark) 0.55f else 0.45f),
                    Color.White.copy(alpha = if (isDark) 0.18f else 0.70f),
                    primary.copy(alpha = 0.22f),
                )
            } else {
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.24f else 0.85f),
                    Color.White.copy(alpha = if (isDark) 0.06f else 0.25f),
                    primary.copy(alpha = if (isDark) 0.14f else 0.10f),
                )
            },
        )
    }

    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
            )
    } else {
        Modifier.clip(shape)
    }

    Surface(
        shape = shape,
        color = glassColor,
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = if (isDark) 0.dp else 2.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(clickableModifier),
    ) {
        Box(
            modifier = Modifier.drawBehind {
                // Subtle top specular inner sheen
                drawLine(
                    color = Color.White.copy(alpha = if (isDark) 0.12f else 0.65f),
                    start = Offset(24f, 1.5f),
                    end = Offset(size.width - 24f, 1.5f),
                    strokeWidth = 1.5f,
                )
            },
            content = content,
        )
    }
}

/**
 * Spring-physics tactile scale modifier for buttons & interactive pills.
 */
fun Modifier.liquidPressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "liquid_press_scale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Custom iOS Liquid Glass inspired animated toggle switch with smooth spring physics and haptic feedback.
 */
@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val thumbOffset: Dp by animateDpAsState(
        targetValue = if (checked) 23.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "liquid_switch_offset",
    )

    val thumbWidth: Dp by animateDpAsState(
        targetValue = if (isPressed) 29.dp else 25.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "liquid_switch_width",
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) primary else surfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "liquid_switch_track",
    )

    val trackBrush = remember(checked, trackColor, secondary) {
        if (checked) {
            Brush.horizontalGradient(listOf(primary, secondary.copy(alpha = 0.85f)))
        } else {
            Brush.horizontalGradient(listOf(trackColor, trackColor))
        }
    }

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 31.dp)
            .clip(CircleShape)
            .background(trackBrush)
            .border(
                width = 1.dp,
                color = if (checked) {
                    Color.White.copy(alpha = 0.35f)
                } else {
                    Color.White.copy(alpha = if (isDark) 0.14f else 0.55f)
                },
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(!checked)
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val effectiveOffset = if (isPressed && checked) thumbOffset - 4.dp else thumbOffset
        Box(
            modifier = Modifier
                .offset(x = effectiveOffset)
                .height(25.dp)
                .width(thumbWidth)
                .clip(CircleShape)
                .background(Color.White)
                .border(
                    width = 0.5.dp,
                    color = Color.Black.copy(alpha = 0.08f),
                    shape = CircleShape,
                )
                .padding(2.dp),
        )
    }
}
