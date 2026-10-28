package com.virtualalertslider.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.virtualalertslider.app.ui.screens.main.MainScreen
import com.virtualalertslider.app.ui.screens.main.MainViewModel
import com.virtualalertslider.app.ui.screens.settings.SettingsScreen
import com.virtualalertslider.app.ui.screens.settings.SettingsViewModel
import com.virtualalertslider.app.ui.theme.VirtualAlertSliderTheme

sealed class Screen {
    data object Main : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainUiState by mainViewModel.uiState.collectAsState()
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

            VirtualAlertSliderTheme(themePreference = mainUiState.settings.themePreference) {
                when (currentScreen) {
                    is Screen.Main -> {
                        MainScreen(
                            viewModel = mainViewModel,
                            onNavigateToSettings = { currentScreen = Screen.Settings }
                        )
                    }
                    is Screen.Settings -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBackClick = { currentScreen = Screen.Main }
                        )
                    }
                }
            }
        }
    }
}
