package dev.anicli.android.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Floating bottom navigation built on real Haze backdrop sampling: the bar's capsule is a
 * [hazeEffect] surface over a [HazeState] source tree, so scrolling content — posters
 * included — is actually blurred and tinted behind the glass. On devices where Haze falls
 * back to lower-cost rendering, the same tinted translucent capsule is preserved.
 */
@Composable
fun GlassBottomNavigation(
    hazeState: HazeState,
    selected: String,
    onHome: () -> Unit,
    onManga: () -> Unit,
    onSettings: () -> Unit
) {
    val capsule = RoundedCornerShape(50)
    val onDark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    val glassStyle = if (onDark) {
        HazeStyle(
            blurRadius = 20.dp,
            backgroundColor = Color(0x40101010),
            tints = listOf(HazeTint(Color(0x63301F2A))),
            fallbackTint = HazeTint(Color(0xE0201B26))
        )
    } else {
        HazeStyle(
            blurRadius = 20.dp,
            backgroundColor = Color(0x6BFFFFFF),
            tints = listOf(HazeTint(Color(0x70FFF6F6))),
            fallbackTint = HazeTint(Color(0xE6F7F2F2))
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            .shadow(16.dp, capsule)
            .clip(capsule)
            .hazeEffect(state = hazeState, style = glassStyle)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(onDark, selected, onHome, onManga, onSettings)
    }
}

private data class GlassNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@Composable
private fun androidx.compose.foundation.layout.RowScope.items(
    onDark: Boolean,
    selected: String,
    onHome: () -> Unit,
    onManga: () -> Unit,
    onSettings: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val navItems = listOf(
        GlassNavItem("home", "الرئيسية", Icons.Rounded.Home, onHome),
        GlassNavItem("manga", "المانجا", Icons.Rounded.MenuBook, onManga),
        GlassNavItem("settings", "الإعدادات", Icons.Rounded.Settings, onSettings)
    )
    navItems.forEach { item ->
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (pressed) 0.94f else 1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "glass-nav-press-${item.id}"
        )
        val selectedColor by animateColorAsState(
            targetValue = if (selected == item.id) {
                if (onDark) Color(0xFFFFB3BB) else MaterialTheme.colorScheme.primary
            } else {
                if (onDark) Color(0xFFB8B4C8) else MaterialTheme.colorScheme.onSurfaceVariant
            },
            animationSpec = tween(150),
            label = "glass-nav-tint-${item.id}"
        )
        val pillColor by animateColorAsState(
            targetValue = if (selected == item.id) {
                (if (onDark) Color.White else Color.Black).copy(alpha = 0.10f)
            } else Color.Transparent,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "glass-nav-pill-${item.id}"
        )
        Row(
            Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(RoundedCornerShape(50))
                .background(pillColor)
                .clickable(interactionSource = interaction, indication = null) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    item.action()
                }
                .padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, contentDescription = item.label, tint = selectedColor, modifier = Modifier.size(21.dp))
            AnimatedVisibility(
                visible = selected == item.id,
                enter = fadeIn(tween(130)) + expandHorizontally(),
                exit = fadeOut(tween(90)) + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(6.dp))
                    Text(item.label, color = selectedColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
