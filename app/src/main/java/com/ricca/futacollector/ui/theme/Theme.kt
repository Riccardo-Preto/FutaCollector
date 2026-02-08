package com.ricca.futacollector.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LillaPastelloDark,
    onPrimary = Color(0xFF381E72),

    secondary = Purple200,
    onSecondary = Color(0xFF332D41),

    background = DarkBackground,
    onBackground = Color(0xFFE6E1E5),

    surface = DarkSurface,
    onSurface = Color(0xFFE6E1E5),

    // QUI IL CAMBIO: Il box di ricerca ora ha più "anima" viola
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LillaNeonSoft // Il testo dentro il box ora è un lilla chiarissimo
)
private val LightColorScheme = lightColorScheme(
    // Il bottone ora è color "Glicine": non è fucsia e non è blu
    primary = Purple500,
    onPrimary = Color(0xFF42355B), // Testo viola scuro sul bottone (più elegante del bianco)

    primaryContainer = LillaChiaro,
    onPrimaryContainer = Purple900,

    secondary = LillaScuro,
    onSecondary = Color.White,

    background = Purple50,
    onBackground = Purple900,

    surface = Color.White,
    onSurface = Purple900,

    // La tua barra di ricerca
    surfaceVariant = LillaChiaro,
    onSurfaceVariant = Purple700
)

@Composable
fun FutaCollectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
