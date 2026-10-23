package com.kosking.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A))
)

@Composable
fun PantallaMas(navController: NavController, vm: HomeViewModel = viewModel()) {
    val kioscos by vm.kioscos.collectAsStateWithLifecycle()
    val rolUsuario by vm.rolUsuario.collectAsStateWithLifecycle()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: ""
    val nombre = user?.displayName ?: user?.email?.substringBefore("@") ?: "Usuario"
    val email = user?.email ?: ""
    val tieneTienda = kioscos.any { it.duenioUid == uid }
    val tieneRolNegocio = tieneTienda || rolUsuario == RolUsuario.EMPLEADO

    var mostrarConfirmLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AzulFondo)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header con perfil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BG)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar con inicial
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Rojo.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Blanco,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombre, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Blanco)
                    Text(email, fontSize = 12.sp, color = Blanco.copy(alpha = 0.6f))
                }
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.Close, null, tint = Blanco.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sección Mi actividad
        SeccionLabel("Mi actividad")
        SeccionCard {
            ItemMas(Icons.Default.History, "Historial de búsquedas", Azul) {}
            HorizontalDivider(color = Gris200, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
            ItemMas(Icons.Default.Favorite, "Favoritos", Rojo) {}
            HorizontalDivider(color = Gris200, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
            ItemMas(Icons.Default.Notifications, "Notificaciones", Naranja) {}
        }

        Spacer(Modifier.height(12.dp))

        // Sección Mi negocio
        SeccionLabel("Mi negocio")
        SeccionCard {
            if (tieneTienda) {
                ItemMas(Icons.Default.Store, "Panel del dueño", Azul) {
                    navController.navigate(Rutas.PANEL_DUENO)
                }
                HorizontalDivider(color = Gris200, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                ItemMas(Icons.Default.Settings, "Configuración del local", Gris500) {
                    navController.navigate(Rutas.CONFIG_LOCAL)
                }
            } else if (rolUsuario == RolUsuario.EMPLEADO) {
                ItemMas(Icons.Default.Badge, "Panel del empleado", Azul) {
                    navController.navigate(Rutas.PANEL_EMPLEADO)
                }
            } else {
                ItemMas(Icons.Default.AddBusiness, "Crear mi local", Rojo) {
                    navController.navigate(Rutas.CREAR_LOCAL)
                }
                HorizontalDivider(color = Gris200, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                ItemMas(Icons.Default.GroupAdd, "Unirme como empleado", Azul) {
                    navController.navigate(Rutas.INGRESAR_CODIGO)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Sección Cuenta
        SeccionLabel("Cuenta")
        SeccionCard {
            ItemMas(Icons.Default.Person, "Mi perfil", Gris500) {}
            HorizontalDivider(color = Gris200, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
            ItemMas(Icons.Default.Settings, "Configuración", Gris500) {}
            HorizontalDivider(color = Gris200, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
            ItemMas(Icons.Default.Logout, "Cerrar sesión", Rojo) {
                mostrarConfirmLogout = true
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (mostrarConfirmLogout) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmLogout = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Seguro que querés cerrar sesión?") },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Rutas.LOGIN) { popUpTo(0) { inclusive = true } }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rojo),
                ) { Text("Sí, cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmLogout = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SeccionLabel(texto: String) {
    Text(
        texto,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = Gris500,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun SeccionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun ItemMas(icono: ImageVector, etiqueta: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(etiqueta, fontSize = 15.sp, color = AzulNavy, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Gris500, modifier = Modifier.size(18.dp))
    }
}
