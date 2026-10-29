package com.virtualalertslider.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.ui.theme.IosDarkBorder
import com.virtualalertslider.app.ui.theme.IosLightBorder
import com.virtualalertslider.app.ui.theme.RingBlue
import com.virtualalertslider.app.ui.theme.SilentRed
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
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current

    val currentModeState by rememberUpdatedState(currentMode)
    val onModeSelectedState by rememberUpdatedState(onModeSelected)

    // Animated target position fraction: 0.0f (RING), 0.5f (VIBRATE), 1.0f (SILENT)
    val targetFraction = when (currentMode) {
        AlertMode.RING -> 0.0f
        AlertMode.VIBRATE -> 0.5f
        AlertMode.SILENT -> 1.0f
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(targetFraction) }

    // Spring animation for smooth, refined Apple-like motion
    val animatedFraction by animateFloatAsState(
        targetValue = if (isDragging) dragFraction else targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AlertSliderFraction"
    )

    // Subtle 250ms accent cross-fade
    val accentColor by animateColorAsState(
        targetValue = when (currentMode) {
            AlertMode.RING -> RingBlue
            AlertMode.VIBRATE -> VibrateAmber
            AlertMode.SILENT -> SilentRed
        },
        animationSpec = tween(durationMillis = 250),
        label = "AlertSliderAccent"
    )

    fun triggerHaptic() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    val knobSize = 58.dp
    val trackPadding = 5.dp
    val trackWidth = 72.dp

    BoxWithConstraints(
        modifier = modifier
            .width(trackWidth)
            .heightIn(min = 220.dp, max = 340.dp)
            .semantics {
                this.contentDescription = "Virtual Alert Slider"
                this.stateDescription = currentMode.title
                this.customActions = listOf(
                    CustomAccessibilityAction("Switch to Ring") {
                        if (currentModeState != AlertMode.RING) {
                            triggerHaptic()
                            onModeSelectedState(AlertMode.RING)
                        }
                        true
                    },
                    CustomAccessibilityAction("Switch to Vibrate") {
                        if (currentModeState != AlertMode.VIBRATE) {
                            triggerHaptic()
                            onModeSelectedState(AlertMode.VIBRATE)
                        }
                        true
                    },
                    CustomAccessibilityAction("Switch to Silent") {
                        if (currentModeState != AlertMode.SILENT) {
                            triggerHaptic()
                            onModeSelectedState(AlertMode.SILENT)
                        }
                        true
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val knobSizePx = with(density) { knobSize.toPx() }
        val paddingPx = with(density) { trackPadding.toPx() }

        val travelPx = (totalHeightPx - 2 * paddingPx - knobSizePx).coerceAtLeast(1f)

        fun fractionFromY(y: Float): Float {
            val knobTopTarget = y - (knobSizePx / 2f) - paddingPx
            return (knobTopTarget / travelPx).coerceIn(0f, 1f)
        }

        fun modeFromFraction(f: Float): AlertMode {
            return when {
                f < 0.28f -> AlertMode.RING
                f > 0.72f -> AlertMode.SILENT
                else -> AlertMode.VIBRATE
            }
        }

        val trackBg = if (isDark) Color(0x33767680) else Color(0x1F767680)
        val trackBorder = if (isDark) IosDarkBorder else IosLightBorder

        // Glass Capsule Outer Track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(trackBg)
                .border(BorderStroke(0.5.dp, trackBorder), CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val tappedFraction = fractionFromY(offset.y)
                        val newMode = modeFromFraction(tappedFraction)
                        if (newMode != currentModeState) {
                            triggerHaptic()
                            onModeSelectedState(newMode)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = fractionFromY(offset.y)
                        },
                        onDragEnd = {
                            val targetMode = modeFromFraction(dragFraction)
                            isDragging = false
                            if (targetMode != currentModeState) {
                                triggerHaptic()
                                onModeSelectedState(targetMode)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val newFraction = fractionFromY(change.position.y)
                            val prevMode = modeFromFraction(dragFraction)
                            dragFraction = newFraction
                            val newMode = modeFromFraction(newFraction)
                            if (prevMode != newMode) {
                                triggerHaptic()
                            }
                        }
                    )
                }
        ) {
            // 3 Snap Stop Tick Dots along the centerline
            val dotColor = if (isDark) Color(0x4DFFFFFF) else Color(0x3D000000)

            // Exact center offsets for the stops:
            // Stop 0 (RING): paddingPx + knobSizePx / 2
            // Stop 1 (VIBRATE): totalHeightPx / 2
            // Stop 2 (SILENT): totalHeightPx - paddingPx - knobSizePx / 2
            val ringStopY = with(density) { (paddingPx + knobSizePx / 2f).toDp() }
            val vibrateStopY = with(density) { (totalHeightPx / 2f).toDp() }
            val silentStopY = with(density) { (totalHeightPx - paddingPx - knobSizePx / 2f).toDp() }

            listOf(ringStopY, vibrateStopY, silentStopY).forEach { stopY ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = 0,
                                y = with(density) { (stopY - 3.dp).roundToPx() }
                            )
                        }
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .align(Alignment.TopCenter)
                )
            }

            // iOS Control Center-style Floating Circular White Knob
            val knobTopPx = paddingPx + (animatedFraction * travelPx)

            val knobBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFFFFFFFF)
            val knobIcon: ImageVector = when (currentMode) {
                AlertMode.RING -> Icons.Default.Notifications
                AlertMode.VIBRATE -> Icons.Default.Vibration
                AlertMode.SILENT -> Icons.Default.NotificationsOff
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = knobTopPx.roundToInt()) }
                    .size(knobSize)
                    .align(Alignment.TopCenter)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.3f),
                        ambientColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(CircleShape)
                    .background(knobBg)
                    .border(BorderStroke(0.5.dp, Color(0x1F000000)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = knobIcon,
                    contentDescription = currentMode.title,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
