package com.virtualalertslider.app.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.virtualalertslider.app.ui.theme.IosDarkBorder
import com.virtualalertslider.app.ui.theme.IosDarkGlassTint
import com.virtualalertslider.app.ui.theme.IosLightBorder
import com.virtualalertslider.app.ui.theme.IosLightGlassTint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    tintColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 0.5.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val defaultTint = if (isDark) IosDarkGlassTint else IosLightGlassTint
    val defaultBorder = if (isDark) IosDarkBorder else IosLightBorder

    val effectiveTint = tintColor ?: defaultTint
    val effectiveBorder = borderColor ?: defaultBorder

    // On API >= 31, blur is supported natively in hardware. Below API 31, or if hazeState is null,
    // we use a smooth solid translucent backdrop.
    val supportsHardwareBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val fallbackColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.72f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.75f)
    }

    val baseModifier = modifier
        .then(
            if (shadowElevation > 0.dp) {
                Modifier.shadow(elevation = shadowElevation, shape = shape, clip = false)
            } else {
                Modifier
            }
        )
        .clip(shape)

    val styledModifier = if (hazeState != null && supportsHardwareBlur) {
        runCatching {
            baseModifier.hazeChild(
                state = hazeState,
                style = HazeStyle(
                    tint = effectiveTint,
                    blurRadius = 24.dp
                )
            )
        }.getOrElse {
            baseModifier.background(fallbackColor)
        }
    } else {
        baseModifier.background(fallbackColor)
    }

    Box(
        modifier = styledModifier.border(BorderStroke(borderWidth, effectiveBorder), shape),
        content = content
    )
}
