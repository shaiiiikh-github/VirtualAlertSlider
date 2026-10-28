package com.virtualalertslider.app.ui.screens.main

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.ui.components.AlertModeCard
import com.virtualalertslider.app.ui.components.HeaderCard
import com.virtualalertslider.app.ui.components.PermissionBanner
import com.virtualalertslider.app.ui.components.VerticalAlertSlider
import com.virtualalertslider.app.ui.theme.RingBlue
import com.virtualalertslider.app.ui.theme.SilentPink
import com.virtualalertslider.app.ui.theme.VibrateAmber

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh DND status when app resumes from background or Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDndStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Card
                HeaderCard(
                    isDndGranted = uiState.isDndGranted,
                    onSettingsClick = onNavigateToSettings
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Permission Warning Banner if missing
                if (!uiState.isDndGranted) {
                    PermissionBanner(
                        onRequestPermissionClick = {
                            context.startActivity(viewModel.getNotificationPolicyIntent())
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Active Mode Showcase Card
                AlertModeCard(currentMode = uiState.currentMode)

                Spacer(modifier = Modifier.height(28.dp))

                // Vertical Alert Slider Component
                VerticalAlertSlider(
                    currentMode = uiState.currentMode,
                    onModeSelected = { mode ->
                        viewModel.selectMode(mode)
                    },
                    hapticEnabled = uiState.settings.hapticFeedbackEnabled
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Quick Mode Selector Filter Chips
                Text(
                    text = "QUICK SWITCH",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AlertMode.entries.forEach { mode ->
                        val isSelected = mode == uiState.currentMode
                        val chipColor = when (mode) {
                            AlertMode.RING -> RingBlue
                            AlertMode.VIBRATE -> VibrateAmber
                            AlertMode.SILENT -> SilentPink
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectMode(mode) },
                            label = { Text(text = "${mode.title}") },
                            leadingIcon = {
                                Text(
                                    text = when (mode) {
                                        AlertMode.RING -> "🔔"
                                        AlertMode.VIBRATE -> "📳"
                                        AlertMode.SILENT -> "🔇"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Error Dialog when permission is required
        if (uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearErrorMessage() },
                title = { Text("Permission Required") },
                text = { Text(uiState.errorMessage ?: "Unknown error") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearErrorMessage()
                            context.startActivity(viewModel.getNotificationPolicyIntent())
                        }
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearErrorMessage() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
