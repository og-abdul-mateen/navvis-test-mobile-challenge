package com.navvistest.mobilechallenge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = BlueForText,
    onPrimary = Color.White,
    secondary = NavVisGreen,
    onSecondary = Color.Black,
    secondaryContainer = NavVisGreen,
    onSecondaryContainer = Color.Black,
    tertiary = NavVisGreen,
    onTertiary = Color.Black,
    background = NavVisPageGrey,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = DividerLight,
    onSurfaceVariant = Slate,
    // Menus and other raised surfaces
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = DividerLight,
    outlineVariant = DividerLight,
    error = NavVisRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = BlueOnDark,
    onPrimary = OnBlueOnDark,
    secondary = NavVisGreen,
    onSecondary = Color.Black,
    secondaryContainer = NavVisGreen,
    onSecondaryContainer = Color.Black,
    tertiary = NavVisGreen,
    onTertiary = Color.Black,
    background = NearBlack,
    onBackground = NavVisPageGrey,
    surface = NearBlack,
    onSurface = NavVisPageGrey,
    surfaceVariant = DividerDark,
    onSurfaceVariant = SlateOnDark,
    surfaceContainerLowest = RaisedOnDark,
    surfaceContainerLow = RaisedOnDark,
    surfaceContainer = RaisedOnDark,
    surfaceContainerHigh = RaisedOnDark,
    surfaceContainerHighest = RaisedOnDark,
    outline = DividerDark,
    outlineVariant = DividerDark,
    error = RedOnDark,
    onError = Color.Black,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

// navvis.com has square corners on everything, so the app uses similar style
private val SquareShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

/**
 * The NavVis theme in light and dark modes.
 */
@Composable
fun MobileChallengeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = SquareShapes,
        content = content,
    )
}
