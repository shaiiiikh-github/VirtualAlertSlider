package com.virtualalertslider.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.ui.theme.IosDarkBorder
import com.virtualalertslider.app.ui.theme.IosLightBorder
import com.virtualalertslider.app.ui.theme.RingBlue
import com.virtualalertslider.app.ui.theme.SilentRed
import com.virtualalertslider.app.ui.theme.VibrateAmber

@Composable
fun SegmentedControl(
    currentMode: AlertMode,
    onModeSelected: (AlertMode) -> Unit,
    hapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val modes = AlertMode.entries

    val selectedIndex = modes.indexOf(currentMode).coerceAtLeast(0)

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SegmentedControlIndicator"
    )

    val containerBg = if (isDark) Color(0x33767680) else Color(0x1F767680)
    val containerBorder = if (isDark) IosDarkBorder else IosLightBorder

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(containerBg)
            .border(BorderStroke(0.5.dp, containerBorder), CircleShape)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / modes.size

        // Sliding indicator thumb
        val thumbOffset = segmentWidth * animatedIndex
        val thumbBg = if (isDark) Color(0xFF2C2C2E) else Color.White
        val thumbBorder = if (isDark) Color(0x33FFFFFF) else Color(0x0A000000)

        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbOffset.roundToPx(), y = 0) }
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.15f)
                )
                .clip(CircleShape)
                .background(thumbBg)
                .border(BorderStroke(0.5.dp, thumbBorder), CircleShape)
        )

        // Segment items
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            modes.forEach { mode ->
                val isSelected = mode == currentMode
                val accentColor = when (mode) {
                    AlertMode.RING -> RingBlue
                    AlertMode.VIBRATE -> VibrateAmber
                    AlertMode.SILENT -> SilentRed
                }

                val textColor = if (isSelected) {
                    accentColor
                } else {
                    if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            this.role = Role.Tab
                            this.selected = isSelected
                            this.stateDescription = "${mode.title} ${if (isSelected) "selected" else ""}"
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                }
                                onModeSelected(mode)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}

