package com.virtualalertslider.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.ui.theme.RingBlue
import com.virtualalertslider.app.ui.theme.SilentRed
import com.virtualalertslider.app.ui.theme.VibrateAmber

@Composable
fun AlertModeCard(
    currentMode: AlertMode,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val accentColor by animateColorAsState(
        targetValue = when (currentMode) {
            AlertMode.RING -> RingBlue
            AlertMode.VIBRATE -> VibrateAmber
            AlertMode.SILENT -> SilentRed
        },
        animationSpec = tween(durationMillis = 250),
        label = "AlertModeAccent"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedContent(
                targetState = currentMode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "ModeTitle"
            ) { mode ->
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color.Black,
                        letterSpacing = 0.3.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = currentMode,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "ModeSubtitle"
        ) { mode ->
            Text(
                text = mode.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6C6C70)
                )
            )
        }
    }
}
