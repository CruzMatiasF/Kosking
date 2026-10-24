package com.kosking.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    icono: @Composable (() -> Unit)? = null,
    iconoFinal: @Composable (() -> Unit)? = null,
    transformacion: VisualTransformation = VisualTransformation.None,
    opciones: KeyboardOptions = KeyboardOptions.Default,
    error: Boolean = false,
    habilitado: Boolean = true,
    lineaUnica: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta, color = AzulOscuro) },
        leadingIcon = icono,
        trailingIcon = iconoFinal,
        visualTransformation = transformacion,
        keyboardOptions = opciones,
        isError = error,
        enabled = habilitado,
        singleLine = lineaUnica,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AzulNavy,
            unfocusedTextColor = AzulNavy,
            disabledTextColor = Gris500,
            focusedContainerColor = Blanco,
            unfocusedContainerColor = Blanco,
            focusedBorderColor = Azul,
            unfocusedBorderColor = AzulOscuro.copy(alpha = 0.4f),
            focusedLabelColor = Azul,
            cursorColor = Azul,
        ),
    )
}

@Composable
fun BotonPrincipal(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    containerColor: androidx.compose.ui.graphics.Color = Rojo,
    contentColor: androidx.compose.ui.graphics.Color = Blanco,
) {
    Button(
        onClick = alTocar,
        enabled = habilitado && !cargando,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        if (cargando) {
            CircularProgressIndicator(color = contentColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(texto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun MensajeError(texto: String) {
    if (texto.isNotEmpty()) {
        Text(
            text = texto,
            color = Rojo,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// ── Barra de navegación inferior compartida ───────────────────────────────────

@Composable
fun KoskingBottomBar(navController: NavController, tabSeleccionado: Int) {
    Column {
        HorizontalDivider(color = Gris200, thickness = 1.dp)
        NavigationBar(containerColor = AzulNavy, tonalElevation = 0.dp) {
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = Blanco,
                selectedTextColor = Blanco,
                indicatorColor = Rojo.copy(alpha = 0.35f),
                unselectedIconColor = Blanco.copy(alpha = 0.5f),
                unselectedTextColor = Blanco.copy(alpha = 0.5f),
            )
            NavigationBarItem(
                selected = tabSeleccionado == 0,
                onClick = { navController.navigate(Rutas.HOME) { launchSingleTop = true } },
                icon = { Icon(Icons.Default.Home, null) },
                label = { Text("Inicio") },
                colors = navColors,
            )
            NavigationBarItem(
                selected = tabSeleccionado == 1,
                onClick = { navController.navigate(Rutas.FAVORITOS) { launchSingleTop = true } },
                icon = { Icon(Icons.Default.Favorite, null) },
                label = { Text("Favoritos") },
                colors = navColors,
            )
            NavigationBarItem(
                selected = tabSeleccionado == 2,
                onClick = { navController.navigate(Rutas.MAPA_SIMPLE) { launchSingleTop = true } },
                icon = { Icon(Icons.Default.LocationOn, null) },
                label = { Text("Mapa") },
                colors = navColors,
            )
            NavigationBarItem(
                selected = tabSeleccionado == 3,
                onClick = { navController.navigate(Rutas.OFERTAS) { launchSingleTop = true } },
                icon = { Icon(Icons.Default.LocalOffer, null) },
                label = { Text("Ofertas") },
                colors = navColors,
            )
            NavigationBarItem(
                selected = tabSeleccionado == 4,
                onClick = { navController.navigate(Rutas.MAS) { launchSingleTop = true } },
                icon = { Icon(Icons.Default.MoreHoriz, null) },
                label = { Text("Más") },
                colors = navColors,
            )
        }
    }
}

// ── Componentes de sección compartidos (Más, Configuración, etc.) ─────────────

@Composable
fun SeccionLabel(texto: String) {
    Text(
        texto,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = Gris500,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
fun SeccionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(content = content)
    }
}
