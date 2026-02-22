package nl.teunk.currere.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CurrereDarkColorScheme = darkColorScheme(
    primary = LimeGreen,
    onPrimary = DarkBackground,
    primaryContainer = LimeGreenDark,
    onPrimaryContainer = TextPrimary,
    secondary = LimeGreen,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorRed,
    onError = TextPrimary,
)

@Composable
fun CurrereTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CurrereDarkColorScheme,
        typography = CurrereTypography,
        content = content,
    )
}
