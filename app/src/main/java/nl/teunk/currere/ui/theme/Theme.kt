package nl.teunk.currere.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = CurrereBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = CurrereBlueLight,
    secondary = CurrereOrange,
)

private val DarkColorScheme = darkColorScheme(
    primary = CurrereBlueLight,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = CurrereBlueDark,
    secondary = CurrereOrange,
)

@Composable
fun CurrereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CurrereTypography,
        content = content,
    )
}
