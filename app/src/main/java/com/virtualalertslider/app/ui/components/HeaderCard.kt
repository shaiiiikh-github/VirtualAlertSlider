package com.virtualalertslider.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.virtualalertslider.app.ui.theme.AccessGrantedGreen
import com.virtualalertslider.app.ui.theme.AccessRequiredRed

@Composable
fun HeaderCard(
    isDndGranted: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Virtual Alert Slider",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your physical alert slider, available digitally.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // DND Access Status Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDndGranted) AccessGrantedGreen.copy(alpha = 0.15f) else AccessRequiredRed.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDndGranted) AccessGrantedGreen.copy(alpha = 0.4f) else AccessRequiredRed.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDndGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "DND Status",
                    tint = if (isDndGranted) AccessGrantedGreen else AccessRequiredRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDndGranted) "DND Policy Access Granted" else "DND Access Required for Silent Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDndGranted) AccessGrantedGreen else AccessRequiredRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
