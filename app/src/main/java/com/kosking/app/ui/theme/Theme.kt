package com.kosking.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = Rojo,
    onPrimary = Blanco,
    primaryContainer = RojoClaro,
    onPrimaryContainer = RojoOscuro,
    secondary = Naranja,
    onSecondary = Blanco,
    secondaryContainer = NaranjaClaro,
    // Azul para el panel del dueño
    tertiary = Azul,
    onTertiary = Blanco,
    tertiaryContainer = AzulClaro,
    onTertiaryContainer = AzulOscuro,
    background = Gris100,
    onBackground = Gris900,
    surface = Blanco,
    onSurface = Gris900,
    surfaceVariant = Gris100,
    onSurfaceVariant = Gris700,
    outline = Gris200,
    error = Rojo,
)

private val EsquemaOscuro = darkColorScheme(
    primary = Rojo,
    onPrimary = Blanco,
    primaryContainer = RojoOscuro,
    onPrimaryContainer = RojoClaro,
    secondary = Naranja,
    onSecondary = Blanco,
    // Azul para el panel del dueño
    tertiary = Azul,
    onTertiary = Blanco,
    tertiaryContainer = AzulOscuro,
    onTertiaryContainer = AzulClaro,
    background = FondoDark,
    onBackground = Blanco,
    surface = SuperficieDark,
    onSurface = Blanco,
    surfaceVariant = SuperficieDark,
    onSurfaceVariant = Gris500,
    outline = BordeDark,
    error = Rojo,
)

@Composable
fun KoskingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}