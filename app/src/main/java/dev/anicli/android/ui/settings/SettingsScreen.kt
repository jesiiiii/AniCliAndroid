package dev.anicli.android.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import dev.anicli.android.util.UchihaSettings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

private val CARD_SHAPE = RoundedCornerShape(18.dp)

/** Arabic title for a stored quality id. */
internal fun qualityLabel(quality: String): String = when (quality) {
    UchihaSettings.QUALITY_AUTO -> "تلقائي"
    UchihaSettings.QUALITY_1080P -> "1080p"
    UchihaSettings.QUALITY_720P -> "720p"
    UchihaSettings.QUALITY_480P -> "480p"
    UchihaSettings.QUALITY_360P -> "360p"
    else -> "تلقائي"
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    var qualityDialogOpen by remember { mutableStateOf(false) }
    var oledDark by remember {
        mutableStateOf(UchihaSettings.oledDark(context))
    }
    var autoNext by remember {
        mutableStateOf(UchihaSettings.autoNextEpisode(context))
    }
    var preferredQuality by remember {
        mutableStateOf(UchihaSettings.preferredQuality(context))
    }
    BackHandler(enabled = qualityDialogOpen) { qualityDialogOpen = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .hazeSource(state = hazeState),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Text(
                    "الإعدادات",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 22.dp).padding(top = 22.dp, bottom = 10.dp)
                )
            }
            item {
                SettingsGroup(
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    QualityRow(
                        current = preferredQuality,
                        onClick = { qualityDialogOpen = true }
                    )
                    GroupSeparator()
                    SettingsToggleRow(
                        icon = { Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = iconTint(oledDark), modifier = Modifier.size(20.dp)) },
                        title = "الوضع المظلم OLED",
                        subtitle = "خلفية سوداء حقيقية لحبّ خلايا شاشة OLED",
                        checked = oledDark,
                        onCheckedChange = { enabled ->
                            UchihaSettings.setOledDark(context, enabled)
                            oledDark = enabled
                        }
                    )
                    GroupSeparator()
                    SettingsToggleRow(
                        icon = { Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = iconTint(autoNext), modifier = Modifier.size(20.dp)) },
                        title = "تشغيل الحلقة التالية تلقائيًا",
                        subtitle = "الانتقال للحلقة التالية عند انتهاء الحالية",
                        checked = autoNext,
                        onCheckedChange = { enabled ->
                            UchihaSettings.setAutoNextEpisode(context, enabled)
                            autoNext = enabled
                        }
                    )
                }
            }
            item {
                Text(
                    "تؤثر إعدادات الجودة والحلقة التالية على التشغيل مباشرةً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = qualityDialogOpen,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(140))
        ) {
            QualityPopover(
                hazeState = hazeState,
                selected = preferredQuality,
                onSelect = { choice ->
                    UchihaSettings.setPreferredQuality(context, choice)
                    preferredQuality = choice
                    qualityDialogOpen = false
                },
                onDismiss = { qualityDialogOpen = false }
            )
        }
    }
}

/** Glass-free surface group: restrained #151515 cards hold the rows, iOS-style. */
@Composable
private fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CARD_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun GroupSeparator() {
    Box(
        Modifier
            .padding(start = 52.dp, top = 2.dp, bottom = 2.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

private fun iconTint(active: Boolean): Color =
    if (active) Color(0xFFE2435C) else Color(0xFF7A7488)

@Composable
private fun QualityRow(current: String, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color(0xFFE2435C), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                "دقة التشغيل",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "تُشغّل الحلقة بأقرب جودة متاحة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedContent(
            targetState = qualityLabel(current),
            label = "quality-value",
            transitionSpec = {
                (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f)) togetherWith
                    (fadeOut(tween(90)) + scaleOut(targetScale = 0.92f))
            }
        ) { label ->
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Rounded.KeyboardArrowLeft,
            contentDescription = "تغيير الجودة",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedSettingsSwitch(checked = checked, contentDescription = title)
    }
}

/** Spring-driven Androidx-free switch: one thumb, one track, all state animated. */
@Composable
private fun AnimatedSettingsSwitch(checked: Boolean, contentDescription: String) {
    val thumbAlignment: Alignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFFE2435C) else Color(0xFF33303B),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "oled-switch-track"
    )
    val thumbScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "oled-switch-scale"
    )
    Box(
        Modifier
            .width(52.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = thumbAlignment
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(25.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * Floating quality-selection popover inside the same window, so the settings screen itself
 * is genuinely sampled through [hazeState]: dimmed, blurred, tinted scrim behind a springy
 * rounded card listing the optional maximum quality. Dismissing via scrim tap or Back is
 * handled by the caller.
 */
@Composable
private fun QualityPopover(
    hazeState: HazeState,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        blurRadius = 26.dp,
                        backgroundColor = Color(0x33000000),
                        tints = listOf(HazeTint(Color(0x66000000))),
                        fallbackTint = HazeTint(Color(0x88000000))
                    )
                )
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }
        )
        AnimatedContent(
            targetState = selected,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 46.dp),
            label = "quality-popover",
            transitionSpec = {
                (fadeIn(tween(120)) + scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))) togetherWith
                    (fadeOut(tween(100)) + scaleOut(targetScale = 0.94f))
            }
        ) { current ->
            Column(
                Modifier
                    .shadow(24.dp, CARD_SHAPE)
                    .clip(CARD_SHAPE)
                    .background(Color(0xFF151515))
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "دقة التشغيل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                options().forEach { option ->
                    val isSel = option == current
                    val rowTint by animateColorAsState(
                        targetValue = if (isSel) Color(0x14FFFFFF) else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "quality-row-$option"
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(option)
                            }
                            .background(rowTint)
                            .padding(horizontal = 20.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(qualityLabel(option), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFFF0EFFB))
                            Text(
                                if (option == UchihaSettings.QUALITY_AUTO) "أعلى جودة متاحة من المصدر" else "حد أقصى مفضل — تُشغّل أقرب جودة متاحة",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB8B4C8)
                            )
                        }
                        AnimatedVisibility(visible = isSel, enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.6f), exit = fadeOut(tween(90))) {
                            Icon(Icons.Rounded.Check, contentDescription = "محدد", tint = Color(0xFFE2435C), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun options(): List<String> = listOf(
    UchihaSettings.QUALITY_AUTO,
    UchihaSettings.QUALITY_1080P,
    UchihaSettings.QUALITY_720P,
    UchihaSettings.QUALITY_480P,
    UchihaSettings.QUALITY_360P
)
