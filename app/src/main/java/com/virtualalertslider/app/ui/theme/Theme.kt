package com.virtualalertslider.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.virtualalertslider.app.domain.model.ThemePreference

private val DarkColorScheme = darkColorScheme(
    primary = RingBlue,
    secondary = VibrateAmber,
    tertiary = SilentRed,
    background = IosDarkBackground,
    surface = IosDarkSurface,
    surfaceVariant = IosDarkSurfaceElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = IosDarkTextPrimary,
    onSurface = IosDarkTextPrimary,
    onSurfaceVariant = IosDarkTextSecondary,
    outline = IosDarkBorder,
    outlineVariant = IosDarkSeparator
)

private val LightColorScheme = lightColorScheme(
    primary = RingBlue,
    secondary = VibrateAmber,
    tertiary = SilentRed,
    background = IosLightBackground,
    surface = IosLightSurface,
    surfaceVariant = IosLightSurfaceElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = IosLightTextPrimary,
    onSurface = IosLightTextPrimary,
    onSurfaceVariant = IosLightTextSecondary,
    outline = IosLightBorder,
    outlineVariant = IosLightSeparator
)

@Composable
fun VirtualAlertSliderTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    darkTheme: Boolean = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            runCatching {
                var ctx = view.context
                while (ctx is android.content.ContextWrapper) {
                    if (ctx is Activity) break
                    ctx = ctx.baseContext
                }
                val window = (ctx as? Activity)?.window ?: return@SideEffect
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
