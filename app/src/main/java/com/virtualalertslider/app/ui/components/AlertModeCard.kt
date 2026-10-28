package com.virtualalertslider.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.ui.theme.RingBlue
import com.virtualalertslider.app.ui.theme.SilentPink
import com.virtualalertslider.app.ui.theme.VibrateAmber

@Composable
fun AlertModeCard(
    currentMode: AlertMode,
    modifier: Modifier = Modifier
) {
    val accentColor = when (currentMode) {
        AlertMode.RING -> RingBlue
        AlertMode.VIBRATE -> VibrateAmber
        AlertMode.SILENT -> SilentPink
    }

    val icon: ImageVector = when (currentMode) {
        AlertMode.RING -> Icons.Default.Notifications
        AlertMode.VIBRATE -> Icons.Default.Vibration
        AlertMode.SILENT -> Icons.Default.NotificationsOff
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.12f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Circle Badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = currentMode.title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Animated Mode Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ACTIVE MODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = accentColor,
                        modifier = Modifier.size(6.dp)
                    ) {}
                }

                Spacer(modifier = Modifier.height(2.dp))

                AnimatedContent(
                    targetState = currentMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ModeTitleText"
                ) { mode ->
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                AnimatedContent(
                    targetState = currentMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ModeSubtitleText"
                ) { mode ->
                    Text(
                        text = mode.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
