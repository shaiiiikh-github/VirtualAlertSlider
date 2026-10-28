package com.virtualalertslider.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.ui.theme.RingBlue
import com.virtualalertslider.app.ui.theme.SilentPink
import com.virtualalertslider.app.ui.theme.VibrateAmber
import kotlin.math.roundToInt

@Composable
fun VerticalAlertSlider(
    currentMode: AlertMode,
    onModeSelected: (AlertMode) -> Unit,
    hapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val modes = listOf(AlertMode.RING, AlertMode.VIBRATE, AlertMode.SILENT)

    // Animated position ratio: 0.0f (Top - RING), 0.5f (Middle - VIBRATE), 1.0f (Bottom - SILENT)
    val targetFraction = when (currentMode) {
        AlertMode.RING -> 0.0f
        AlertMode.VIBRATE -> 0.5f
        AlertMode.SILENT -> 1.0f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SliderFraction"
    )

    val activeColor by animateColorAsState(
        targetValue = when (currentMode) {
            AlertMode.RING -> RingBlue
            AlertMode.VIBRATE -> VibrateAmber
            AlertMode.SILENT -> SilentPink
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SliderColor"
    )

    fun triggerHaptics() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    Surface(
        modifier = modifier
            .width(280.dp)
            .height(380.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Slider Track & Knob
            var trackHeightPx by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .onGloballyPositioned { coordinates ->
                        trackHeightPx = coordinates.size.height.toFloat()
                    }
                    .pointerInput(trackHeightPx) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (trackHeightPx > 0f) {
                                    val currentY = animatedFraction * trackHeightPx + dragAmount.y
                                    val clampedY = currentY.coerceIn(0f, trackHeightPx)
                                    val fraction = clampedY / trackHeightPx
                                    val newMode = when {
                                        fraction < 0.33f -> AlertMode.RING
                                        fraction < 0.66f -> AlertMode.VIBRATE
                                        else -> AlertMode.SILENT
                                    }
                                    if (newMode != currentMode) {
                                        triggerHaptics()
                                        onModeSelected(newMode)
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // Background Track Line
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    RingBlue.copy(alpha = 0.4f),
                                    VibrateAmber.copy(alpha = 0.4f),
                                    SilentPink.copy(alpha = 0.4f)
                                )
                            )
                        )
                )

                // Mode Snap Nodes on Track
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    modes.forEach { mode ->
                        val isSelected = mode == currentMode
                        val nodeColor = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 18.dp else 12.dp)
                                .clip(CircleShape)
                                .background(nodeColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        triggerHaptics()
                                        onModeSelected(mode)
                                    }
                                }
                        )
                    }
                }

                // Sliding Metallic Thumb / Knob
                val density = androidx.compose.ui.platform.LocalDensity.current
                val knobSizePx = with(density) { 56.dp.toPx() }
                val knobOffsetPx = (animatedFraction * (trackHeightPx - knobSizePx)).coerceAtLeast(0f)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, knobOffsetPx.roundToInt()) }
                        .size(56.dp)
                        .shadow(16.dp, CircleShape, spotColor = activeColor)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    activeColor,
                                    activeColor.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val icon: ImageVector = when (currentMode) {
                        AlertMode.RING -> Icons.Default.Notifications
                        AlertMode.VIBRATE -> Icons.Default.Vibration
                        AlertMode.SILENT -> Icons.Default.NotificationsOff
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = currentMode.title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Column: Mode Labels & Tap Targets
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                modes.forEach { mode ->
                    val isSelected = mode == currentMode
                    val textColor = if (isSelected) {
                        when (mode) {
                            AlertMode.RING -> RingBlue
                            AlertMode.VIBRATE -> VibrateAmber
                            AlertMode.SILENT -> SilentPink
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) textColor.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable {
                                if (!isSelected) {
                                    triggerHaptics()
                                    onModeSelected(mode)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (mode) {
                                        AlertMode.RING -> "🔔"
                                        AlertMode.VIBRATE -> "📳"
                                        AlertMode.SILENT -> "🔇"
                                    },
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                            Text(
                                text = when (mode) {
                                    AlertMode.RING -> "Full Audio"
                                    AlertMode.VIBRATE -> "Haptics Only"
                                    AlertMode.SILENT -> "Muted DND"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = if (isSelected) 0.9f else 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
