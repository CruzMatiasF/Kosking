package com.kosking.app.onboarding

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.NavDueno
import com.kosking.app.dueno.PanelDuenoEmbedido
import com.kosking.app.dueno.PantallaAgregarCliente
import com.kosking.app.dueno.PantallaAgregarFiado
import com.kosking.app.dueno.detalle.PantallaDetalleCliente
import com.kosking.app.dueno.detalle.PantallaDetalleMovimiento

@Composable
fun PantallaHome(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    duenoVm: DuenoViewModel,
    mostrarPanelDueno: Boolean,
    onCerrarPanelDueno: () -> Unit,
    tabInicialDueno: Int,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onIniciarSesion: () -> Unit,
    onCerrarSesionCuenta: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onConfiguracion: () -> Unit = {},
    onAcercaDe: () -> Unit = {},
) {
    var vistaDueno by remember { mutableStateOf<NavDueno>(NavDueno.Panel) }
    var tabPanelDueno by remember { mutableIntStateOf(tabInicialDueno) }

    LaunchedEffect(mostrarPanelDueno) {
        if (mostrarPanelDueno) {
            vistaDueno = NavDueno.Panel
            tabPanelDueno = tabInicialDueno
        }
    }

    BackHandler(enabled = mostrarPanelDueno && vistaDueno !is NavDueno.Panel) {
        vistaDueno = when (val v = vistaDueno) {
            is NavDueno.AgregarFiado -> if (v.volverAPanel) NavDueno.Panel
                                        else NavDueno.DetalleCliente(v.clienteId)
            else -> NavDueno.Panel
        }
    }

    if (mostrarPanelDueno) {
        when (val vista = vistaDueno) {
            is NavDueno.Panel -> PanelDuenoEmbedido(
                vm = duenoVm,
                tabSeleccionado = tabPanelDueno,
                onTabChange = { tabPanelDueno = it },
                onAbrirAgregarCliente = { vistaDueno = NavDueno.AgregarCliente },
                onAbrirDetalleCliente = { vistaDueno = NavDueno.DetalleCliente(it) },
                onAbrirAgregarFiado = { clienteId, tipo ->
                    vistaDueno = NavDueno.AgregarFiado(clienteId, tipo, volverAPanel = tabPanelDueno != 0)
                },
                onAbrirDetalleMovimiento = { vistaDueno = NavDueno.DetalleMovimiento(it) }
            )
            is NavDueno.AgregarCliente -> PantallaAgregarCliente(
                onVolver = { vistaDueno = NavDueno.Panel },
                vm = duenoVm
            )
            is NavDueno.DetalleCliente -> PantallaDetalleCliente(
                clienteId = vista.id,
                onVolver = { vistaDueno = NavDueno.Panel },
                onVerMovimiento = { movId ->
                    vistaDueno = NavDueno.DetalleMovimiento(movId, clienteId = vista.id)
                },
                onAnotarFiado = { clienteId -> vistaDueno = NavDueno.AgregarFiado(clienteId, "cargo") },
                onCobrar = { clienteId -> vistaDueno = NavDueno.AgregarFiado(clienteId, "pago") },
                vm = duenoVm
            )
            is NavDueno.AgregarFiado -> PantallaAgregarFiado(
                clienteId = vista.clienteId,
                onGuardado = {
                    vistaDueno = if (vista.volverAPanel) NavDueno.Panel
                                 else NavDueno.DetalleCliente(vista.clienteId)
                },
                onVolver = {
                    vistaDueno = if (vista.volverAPanel) NavDueno.Panel
                                 else NavDueno.DetalleCliente(vista.clienteId)
                },
                vm = duenoVm,
                tipoInicial = vista.tipoInicial
            )
            is NavDueno.DetalleMovimiento -> PantallaDetalleMovimiento(
                movimientoId = vista.id,
                onVolver = {
                    vistaDueno = vista.clienteId?.let { NavDueno.DetalleCliente(it) } ?: NavDueno.Panel
                },
                vm = duenoVm
            )
        }
    } else {
        AppPublica(
            estaLogueado = estaLogueado,
            nombreUsuario = nombreUsuario,
            esDueno = esDueno,
            esEmpleado = esEmpleado,
            onSoyDueno = onSoyDueno,
            onSoyEmpleado = onSoyEmpleado,
            onIniciarSesion = onIniciarSesion,
            onCerrarSesion = onCerrarSesionCuenta,
            onConfigurarPerfil = onConfigurarPerfil,
            onConfiguracion = onConfiguracion,
            onAcercaDe = onAcercaDe,
        )
    }
}

@Composable
private fun AppPublica(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onIniciarSesion: () -> Unit,
    onCerrarSesion: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onConfiguracion: () -> Unit,
    onAcercaDe: () -> Unit,
) {
    var tabSeleccionado by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navColors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 0,
                    onClick = { tabSeleccionado = 0 },
                    icon = { Icon(Icons.Default.Explore, null) },
                    label = { Text("Explorar") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 1,
                    onClick = { tabSeleccionado = 1 },
                    icon = {
                        if (estaLogueado) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (tabSeleccionado == 1) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    nombreUsuario.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tabSeleccionado == 1) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else {
                            Icon(Icons.Default.Person, null)
                        }
                    },
                    label = { Text("Más") },
                    colors = navColors
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tabSeleccionado) {
                1 -> TabMas(
                    estaLogueado = estaLogueado,
                    nombreUsuario = nombreUsuario,
                    esDueno = esDueno,
                    esEmpleado = esEmpleado,
                    onSoyDueno = onSoyDueno,
                    onSoyEmpleado = onSoyEmpleado,
                    onIniciarSesion = onIniciarSesion,
                    onCerrarSesion = onCerrarSesion,
                    onConfigurarPerfil = onConfigurarPerfil,
                    onConfiguracion = onConfiguracion,
                    onAcercaDe = onAcercaDe,
                )
                else -> TabExplorar()
            }
        }
    }
}

@Composable
private fun TabExplorar() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🗺️", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Próximamente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Vas a poder encontrar locales\ncerca tuyo en el mapa",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TabMas(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onIniciarSesion: () -> Unit,
    onCerrarSesion: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onConfiguracion: () -> Unit,
    onAcercaDe: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: avatar + nombre o "Invitado"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (estaLogueado) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (estaLogueado) {
                    Text(
                        nombreUsuario.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column {
                Text(
                    if (estaLogueado) nombreUsuario.ifBlank { "Mi cuenta" } else "Invitado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        esDueno -> "Dueño de local"
                        esEmpleado -> "Empleado"
                        estaLogueado -> "Sin rol configurado"
                        else -> "No iniciaste sesión"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Acceso al panel (si ya tiene rol)
        if (esDueno) {
            Button(
                onClick = onSoyDueno,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Store, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir mi panel de dueño")
            }
        }
        if (esEmpleado) {
            Button(
                onClick = onSoyEmpleado,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Badge, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ir a mi panel de empleado")
            }
        }

        // Cards de rol (si no tiene ninguno)
        if (!esDueno && !esEmpleado) {
            Text(
                if (estaLogueado) "¿Qué querés hacer?" else "¿Querés usar Kosking?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RolCard(
                emoji = "🏪",
                titulo = "Quiero ser dueño",
                subtitulo = "Registrá tu local y manejá los fiaditos",
                onClick = onSoyDueno,
                destacada = true
            )
            RolCard(
                emoji = "👤",
                titulo = "Soy empleado de un local",
                subtitulo = "Entrá con el código que te dio el dueño",
                onClick = onSoyEmpleado
            )
        }

        // Login/Registro si no está logueado
        if (!estaLogueado) {
            HorizontalDivider()
            Text(
                "¿Ya tenés cuenta?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onIniciarSesion,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Iniciar sesión")
            }
        }

        // Opciones de cuenta (si está logueado)
        if (estaLogueado) {
            HorizontalDivider()
            Text(
                "Cuenta",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ListItem(
                headlineContent = { Text("Configurar perfil") },
                leadingContent = { Icon(Icons.Default.Edit, null, Modifier.size(20.dp)) },
                modifier = Modifier.clickable(onClick = onConfigurarPerfil).fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            ListItem(
                headlineContent = { Text("Configuración") },
                leadingContent = { Icon(Icons.Default.Settings, null, Modifier.size(20.dp)) },
                modifier = Modifier.clickable(onClick = onConfiguracion).fillMaxWidth()
            )
        }

        // Opciones generales
        HorizontalDivider()
        Text(
            "App",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ListItem(
            headlineContent = { Text("Acerca de Kosking") },
            leadingContent = { Icon(Icons.Default.Info, null, Modifier.size(20.dp)) },
            modifier = Modifier.clickable(onClick = onAcercaDe).fillMaxWidth()
        )
        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
        ListItem(
            headlineContent = { Text("Compartir con un amigo") },
            leadingContent = { Icon(Icons.Default.Share, null, Modifier.size(20.dp)) },
            modifier = Modifier.clickable(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "¡Usá Kosking para manejar los fiaditos de tu local! Es gratis para empezar. Buscalo como \"Kosking\" en Play Store 📱"
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir Kosking"))
                }
            ).fillMaxWidth()
        )

        // Cerrar sesión
        if (estaLogueado) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cerrar sesión")
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RolCard(
    emoji: String,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    destacada: Boolean = false
) {
    val containerColor = if (destacada) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (destacada) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = contentColor)
                Text(subtitulo, style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f))
            }
        }
    }
}
