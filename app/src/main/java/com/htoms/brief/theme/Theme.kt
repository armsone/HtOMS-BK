package com.htoms.brief.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkColorScheme = darkColorScheme(
    primary = BriefTheme.brandOrange,
    onPrimary = Color(0xFF18181B),
    primaryContainer = BrandPalette.card,
    onPrimaryContainer = Color.White,
    secondary = BrandPalette.mutedText,
    onSecondary = Color.White,
    background = BrandPalette.background,
    onBackground = Color.White,
    surface = BrandPalette.card,
    onSurface = Color.White,
    surfaceVariant = BrandPalette.boardCell,
    onSurfaceVariant = BrandPalette.mutedText,
    error = BriefTheme.negative,
    onError = Color.White
)

/** 전 플랫폼 공통 다크 전용 테마. TV(10-foot)에서는 글자를 1.25배 키운다. */
@Composable
fun HtOMSBriefTheme(
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTelevision =
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    val baseDensity = LocalDensity.current
    val density = if (isTelevision) {
        Density(baseDensity.density, baseDensity.fontScale * 1.25f)
    } else {
        baseDensity
    }

    CompositionLocalProvider(LocalDensity provides density) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}
