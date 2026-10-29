package com.virtualalertslider.app.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.SliderSettings
import com.virtualalertslider.app.domain.model.ThemePreference
import com.virtualalertslider.app.ui.components.GroupedSettingsSection
import com.virtualalertslider.app.ui.components.SettingsDivider
import com.virtualalertslider.app.ui.components.SettingsRow
import com.virtualalertslider.app.ui.components.SettingsSwitchRow
import com.virtualalertslider.app.ui.theme.AccessGrantedGreen
import com.virtualalertslider.app.ui.theme.AccessRequiredRed
import com.virtualalertslider.app.ui.theme.IosDarkBackground
import com.virtualalertslider.app.ui.theme.IosLightBackground
import com.virtualalertslider.app.ui.theme.RingBlue
import com.virtualalertslider.app.ui.theme.VibrateAmber
import com.virtualalertslider.app.ui.theme.VirtualAlertSliderTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
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

    SettingsScreenContent(
        settings = settings,
        onBackClick = onBackClick,
        onRequestDndPermission = {
            context.startActivity(viewModel.getNotificationPolicyIntent())
        },
        onHapticChange = { viewModel.setHapticFeedbackEnabled(it) },
        onBypassChange = { viewModel.setBypassStuckSlider(it) },
        onThemeChange = { viewModel.setThemePreference(it) },
        onDefaultModeChange = { viewModel.setDefaultMode(it) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    settings: SliderSettings,
    onBackClick: () -> Unit,
    onRequestDndPermission: () -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onBypassChange: (Boolean) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onDefaultModeChange: (AlertMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var themeExpanded by remember { mutableStateOf(false) }
    var defaultModeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = if (isDark) Color.White else Color.Black
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = RingBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) IosDarkBackground else IosLightBackground
                )
            )
        },
        containerColor = if (isDark) IosDarkBackground else IosLightBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Section 1: System & Permissions
            GroupedSettingsSection(title = "System & Permissions") {
                SettingsRow(
                    title = "Do Not Disturb Access",
                    subtitle = if (settings.isNotificationPolicyAccessGranted) {
                        "Granted • Full audio control enabled"
                    } else {
                        "Required • Tap to open system settings"
                    },
                    icon = if (settings.isNotificationPolicyAccessGranted) Icons.Default.CheckCircle else Icons.Default.Lock,
                    iconTint = if (settings.isNotificationPolicyAccessGranted) AccessGrantedGreen else AccessRequiredRed,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = onRequestDndPermission
                )
            }

            // Section 2: Preferences
            GroupedSettingsSection(title = "Preferences") {
                SettingsSwitchRow(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate on slider snap and mode changes",
                    checked = settings.hapticFeedbackEnabled,
                    onCheckedChange = onHapticChange,
                    icon = Icons.Default.TouchApp,
                    iconTint = VibrateAmber
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Bypass Stuck Hardware Slider",
                    subtitle = "Unmute audio if physical switch is stuck",
                    checked = settings.bypassStuckSlider,
                    onCheckedChange = onBypassChange,
                    icon = Icons.Default.Build,
                    iconTint = RingBlue
                )

                SettingsDivider()

                // Theme Row with Dropdown
                Box {
                    SettingsRow(
                        title = "Appearance",
                        subtitle = "Select app color scheme",
                        icon = Icons.Default.ColorLens,
                        iconTint = RingBlue,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = settings.themePreference.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF8E8E93)
                                    )
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        onClick = { themeExpanded = true }
                    )

                    DropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        ThemePreference.entries.forEach { preference ->
                            DropdownMenuItem(
                                text = { Text(preference.title) },
                                onClick = {
                                    onThemeChange(preference)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }

                SettingsDivider()

                // Default Startup Mode with Dropdown
                Box {
                    SettingsRow(
                        title = "Default Startup Mode",
                        subtitle = "Applied on device boot",
                        icon = Icons.Default.Notifications,
                        iconTint = RingBlue,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = settings.defaultMode.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF8E8E93)
                                    )
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        onClick = { defaultModeExpanded = true }
                    )

                    DropdownMenu(
                        expanded = defaultModeExpanded,
                        onDismissRequest = { defaultModeExpanded = false }
                    ) {
                        AlertMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.title) },
                                onClick = {
                                    onDefaultModeChange(mode)
                                    defaultModeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (settings.bypassStuckSlider) {
                Text(
                    text = "When enabled, Virtual Alert Slider forces audio stream unmute and overrides physically jammed silent switches on OnePlus and vendor Android devices.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6C6C70)
                    ),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }

            // Section 3: Integrations & Shortcuts
            GroupedSettingsSection(title = "Integrations & Shortcuts") {
                SettingsRow(
                    title = "Quick Settings Tile",
                    subtitle = "1-tap notification shade toggle",
                    icon = Icons.Default.Smartphone,
                    iconTint = RingBlue
                )

                SettingsDivider()

                SettingsRow(
                    title = "Home Screen Widget",
                    subtitle = "Dedicated 3-stage desktop control",
                    icon = Icons.Default.Widgets,
                    iconTint = RingBlue
                )
            }

            // Section 4: About
            GroupedSettingsSection(title = "About") {
                SettingsRow(
                    title = "Virtual Alert Slider",
                    subtitle = "Version 1.2.0",
                    icon = Icons.Default.Info,
                    iconTint = RingBlue,
                    trailingContent = {
                        Text(
                            text = "Release",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF8E8E93)
                            )
                        )
                    }
                )
            }

            Text(
                text = "Operates independently using standard Android AudioManager and Notification Policy APIs. Designed as an accessible, tactile digital alternative for hardware sound switches.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6C6C70)
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
            )
        }
    }
}

// Previews: Light and Dark, Large Phone (411x891) and Small Phone (360x640)

@Preview(
    name = "Settings - Dark Theme Large Phone",
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun SettingsScreenPreviewDarkLarge() {
    VirtualAlertSliderTheme(darkTheme = true) {
        SettingsScreenContent(
            settings = SliderSettings(
                hapticFeedbackEnabled = true,
                bypassStuckSlider = true,
                isNotificationPolicyAccessGranted = true
            ),
            onBackClick = {},
            onRequestDndPermission = {},
            onHapticChange = {},
            onBypassChange = {},
            onThemeChange = {},
            onDefaultModeChange = {}
        )
    }
}

@Preview(
    name = "Settings - Light Theme Large Phone",
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun SettingsScreenPreviewLightLarge() {
    VirtualAlertSliderTheme(darkTheme = false) {
        SettingsScreenContent(
            settings = SliderSettings(
                hapticFeedbackEnabled = false,
                bypassStuckSlider = false,
                isNotificationPolicyAccessGranted = false
            ),
            onBackClick = {},
            onRequestDndPermission = {},
            onHapticChange = {},
            onBypassChange = {},
            onThemeChange = {},
            onDefaultModeChange = {}
        )
    }
}

@Preview(
    name = "Settings - Dark Theme Small Phone",
    showBackground = true,
    device = "spec:width=360dp,height=640dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun SettingsScreenPreviewDarkSmall() {
    VirtualAlertSliderTheme(darkTheme = true) {
        SettingsScreenContent(
            settings = SliderSettings(
                hapticFeedbackEnabled = true,
                bypassStuckSlider = false,
                isNotificationPolicyAccessGranted = true
            ),
            onBackClick = {},
            onRequestDndPermission = {},
            onHapticChange = {},
            onBypassChange = {},
            onThemeChange = {},
            onDefaultModeChange = {}
        )
    }
}

@Preview(
    name = "Settings - Light Theme Small Phone",
    showBackground = true,
    device = "spec:width=360dp,height=640dp",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun SettingsScreenPreviewLightSmall() {
    VirtualAlertSliderTheme(darkTheme = false) {
        SettingsScreenContent(
            settings = SliderSettings(
                hapticFeedbackEnabled = true,
                bypassStuckSlider = true,
                isNotificationPolicyAccessGranted = false
            ),
            onBackClick = {},
            onRequestDndPermission = {},
            onHapticChange = {},
            onBypassChange = {},
            onThemeChange = {},
            onDefaultModeChange = {}
        )
    }
}
