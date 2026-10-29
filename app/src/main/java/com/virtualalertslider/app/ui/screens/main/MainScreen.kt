package com.virtualalertslider.app.ui.screens.main

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.SliderSettings
import com.virtualalertslider.app.ui.components.AlertModeCard
import com.virtualalertslider.app.ui.components.GlassSurface
import com.virtualalertslider.app.ui.components.HeaderCard
import com.virtualalertslider.app.ui.components.PermissionBanner
import com.virtualalertslider.app.ui.components.SegmentedControl
import com.virtualalertslider.app.ui.components.VerticalAlertSlider
import com.virtualalertslider.app.ui.theme.IosDarkBackground
import com.virtualalertslider.app.ui.theme.IosLightBackground
import com.virtualalertslider.app.ui.theme.RingBlue
import com.virtualalertslider.app.ui.theme.SilentRed
import com.virtualalertslider.app.ui.theme.VibrateAmber
import com.virtualalertslider.app.ui.theme.VirtualAlertSliderTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    MainScreenContent(
        uiState = uiState,
        onModeSelected = { viewModel.selectMode(it) },
        onNavigateToSettings = onNavigateToSettings,
        onRequestDndPermission = {
            context.startActivity(viewModel.getNotificationPolicyIntent())
        },
        onClearError = { viewModel.clearErrorMessage() },
        modifier = modifier
    )
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onModeSelected: (AlertMode) -> Unit,
    onNavigateToSettings: () -> Unit,
    onRequestDndPermission: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val hazeState = remember { HazeState() }
    val snackbarHostState = remember { SnackbarHostState() }
    var showPermissionBanner by remember { mutableStateOf(true) }

    // Dynamic ambient color transition for the soft backdrop glow
    val ambientColor by animateColorAsState(
        targetValue = when (uiState.currentMode) {
            AlertMode.RING -> RingBlue
            AlertMode.VIBRATE -> VibrateAmber
            AlertMode.SILENT -> SilentRed
        },
        animationSpec = tween(durationMillis = 300),
        label = "AmbientGlowColor"
    )

    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) IosDarkBackground else IosLightBackground)
    ) {
        // Soft ambient background glow for glass refraction
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(320.dp)
                .clip(CircleShape)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ambientColor.copy(alpha = if (isDark) 0.18f else 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main content container with Haze sampling
        Column(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
                .padding(safePadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Minimal Header
            HeaderCard(
                onSettingsClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slim dismissible permission banner (shown only when DND is missing)
            if (!uiState.isDndGranted && showPermissionBanner) {
                PermissionBanner(
                    onRequestPermissionClick = onRequestDndPermission,
                    onDismissClick = {
                        showPermissionBanner = false
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Hero Glass Slider Panel
            GlassSurface(
                hazeState = hazeState,
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlertModeCard(currentMode = uiState.currentMode)

                    Spacer(modifier = Modifier.height(28.dp))

                    VerticalAlertSlider(
                        currentMode = uiState.currentMode,
                        onModeSelected = onModeSelected,
                        hapticEnabled = uiState.settings.hapticFeedbackEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // iOS-style Segmented Control (Replacing outdated FilterChips)
            SegmentedControl(
                currentMode = uiState.currentMode,
                onModeSelected = onModeSelected,
                hapticEnabled = uiState.settings.hapticFeedbackEnabled
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = safePadding.calculateBottomPadding() + 16.dp)
        )

        // Error Dialog
        if (uiState.errorMessage != null) {
            val isPermissionError = uiState.errorMessage.contains("permission", ignoreCase = true) ||
                    uiState.errorMessage.contains("Do Not Disturb", ignoreCase = true)
            AlertDialog(
                onDismissRequest = onClearError,
                title = {
                    Text(
                        text = if (isPermissionError) "Permission Required" else "Notice",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = { Text(uiState.errorMessage) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val wasPermission = isPermissionError
                            onClearError()
                            if (wasPermission) {
                                onRequestDndPermission()
                            }
                        }
                    ) {
                        Text(
                            text = if (isPermissionError) "Settings" else "OK",
                            fontWeight = FontWeight.SemiBold,
                            color = RingBlue
                        )
                    }
                },
                dismissButton = {
                    if (isPermissionError) {
                        TextButton(onClick = onClearError) {
                            Text("Cancel", color = Color(0xFF8E8E93))
                        }
                    }
                },
                containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// Previews: Light and Dark, Large Phone (411x891) and Small Phone (360x640)

@Preview(
    name = "Dark Theme - Large Phone",
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MainScreenPreviewDarkLarge() {
    VirtualAlertSliderTheme(darkTheme = true) {
        MainScreenContent(
            uiState = MainUiState(
                currentMode = AlertMode.RING,
                settings = SliderSettings(hapticFeedbackEnabled = true),
                isDndGranted = true
            ),
            onModeSelected = {},
            onNavigateToSettings = {},
            onRequestDndPermission = {},
            onClearError = {}
        )
    }
}

@Preview(
    name = "Light Theme - Large Phone",
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MainScreenPreviewLightLarge() {
    VirtualAlertSliderTheme(darkTheme = false) {
        MainScreenContent(
            uiState = MainUiState(
                currentMode = AlertMode.VIBRATE,
                settings = SliderSettings(hapticFeedbackEnabled = true),
                isDndGranted = false
            ),
            onModeSelected = {},
            onNavigateToSettings = {},
            onRequestDndPermission = {},
            onClearError = {}
        )
    }
}

@Preview(
    name = "Dark Theme - Small Phone",
    showBackground = true,
    device = "spec:width=360dp,height=640dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MainScreenPreviewDarkSmall() {
    VirtualAlertSliderTheme(darkTheme = true) {
        MainScreenContent(
            uiState = MainUiState(
                currentMode = AlertMode.SILENT,
                settings = SliderSettings(hapticFeedbackEnabled = true),
                isDndGranted = true
            ),
            onModeSelected = {},
            onNavigateToSettings = {},
            onRequestDndPermission = {},
            onClearError = {}
        )
    }
}

@Preview(
    name = "Light Theme - Small Phone",
    showBackground = true,
    device = "spec:width=360dp,height=640dp",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MainScreenPreviewLightSmall() {
    VirtualAlertSliderTheme(darkTheme = false) {
        MainScreenContent(
            uiState = MainUiState(
                currentMode = AlertMode.RING,
                settings = SliderSettings(hapticFeedbackEnabled = true),
                isDndGranted = true
            ),
            onModeSelected = {},
            onNavigateToSettings = {},
            onRequestDndPermission = {},
            onClearError = {}
        )
    }
}
