package com.example.sqlite.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val FuiColorScheme = darkColorScheme(
    primary = FuiPrimary,
    onPrimary = FuiOnPrimary,
    primaryContainer = FuiPrimaryContainer,
    onPrimaryContainer = FuiOnPrimaryContainer,
    secondary = FuiSecondary,
    onSecondary = FuiOnSecondary,
    secondaryContainer = FuiSecondaryContainer,
    onSecondaryContainer = FuiOnSecondaryContainer,
    tertiary = FuiTertiary,
    onTertiary = FuiOnTertiary,
    background = FuiBackground,
    onBackground = FuiOnBackground,
    surface = FuiSurface,
    onSurface = FuiOnSurface,
    surfaceVariant = FuiSurfaceVariant,
    onSurfaceVariant = FuiOnBackground,
    error = FuiError,
    onError = FuiOnError,
    outline = FuiOutline
)

val FuiShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

@Composable
fun SqliteTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = FuiColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FuiShapes,
        content = content
    )
}
