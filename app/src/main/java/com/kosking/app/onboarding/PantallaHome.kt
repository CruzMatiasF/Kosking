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
    onRegistrarse: () -> Unit = {},
    onCerrarSesionCuenta: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onAbrirPerfil: () -> Unit = {},
    onConfiguracion: () -> Unit = {},
    onAcercaDe: () -> Unit = {},
) {
    var vistaDueno by remember { mutableStateOf<NavDueno>(NavDueno.Panel) }
    var tabPanelDueno by remember { mutableIntStateOf(tabInicialDueno) }
    // 0 = Panel/Inicio, 1 = Más
    var tabNav by remember { mutableIntStateOf(0) }

    LaunchedEffect(mostrarPanelDueno) {
        if (mostrarPanelDueno) {
            vistaDueno = NavDueno.Panel
            tabPanelDueno = tabInicialDueno
        }
    }

    BackHandler(enabled = vistaDueno !is NavDueno.Panel) {
        vistaDueno = when (val v = vistaDueno) {
            is NavDueno.AgregarFiado -> if (v.volverAPanel) NavDueno.Panel
                                        else NavDueno.DetalleCliente(v.clienteId)
            else -> NavDueno.Panel
        }
    }

    // Si hay una sub-pantalla del panel abierta, ocultar la nav bar
    val enSubPantalla = vistaDueno !is NavDueno.Panel

    Scaffold(
        bottomBar = {
            if (!enSubPantalla) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tabNav == 1,
                        onClick = { tabNav = 1 },
                        icon = { Icon(Icons.Default.Menu, null) },
                        label = { Text("Más") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = androidx.compose.ui.Modifier.padding(padding)) {
            when {
                enSubPantalla || tabNav == 0 -> when (val vista = vistaDueno) {
                    is NavDueno.Panel -> if (mostrarPanelDueno) {
                        PanelDuenoEmbedido(
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
                    } else {
                        TabMas(
                            estaLogueado = estaLogueado,
                            nombreUsuario = nombreUsuario,
                            esDueno = esDueno,
                            esEmpleado = esEmpleado,
                            onSoyDueno = onSoyDueno,
                            onSoyEmpleado = onSoyEmpleado,
                            onIniciarSesion = onIniciarSesion,
                            onRegistrarse = onRegistrarse,
                            onCerrarSesion = onCerrarSesionCuenta,
                            onConfigurarPerfil = onConfigurarPerfil,
                            onAbrirPerfil = onAbrirPerfil,
                            onConfiguracion = onConfiguracion,
                            onAcercaDe = onAcercaDe,
                        )
                    }
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
                else -> TabMas(
                    estaLogueado = estaLogueado,
                    nombreUsuario = nombreUsuario,
                    esDueno = esDueno,
                    esEmpleado = esEmpleado,
                    onSoyDueno = { onSoyDueno(); tabNav = 0 },
                    onSoyEmpleado = onSoyEmpleado,
                    onIniciarSesion = onIniciarSesion,
                    onRegistrarse = onRegistrarse,
                    onCerrarSesion = onCerrarSesionCuenta,
                    onConfigurarPerfil = onConfigurarPerfil,
                    onAbrirPerfil = onAbrirPerfil,
                    onConfiguracion = onConfiguracion,
                    onAcercaDe = onAcercaDe,
                )
            }
        }
    }
}

@Composable
internal fun TabMas(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onIniciarSesion: () -> Unit,
    onRegistrarse: () -> Unit = {},
    onCerrarSesion: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onAbrirPerfil: () -> Unit = {},
    onConfiguracion: () -> Unit,
    onAcercaDe: () -> Unit,
) {
    val context = LocalContext.current
    var mostrarDialogRol by remember { mutableStateOf(false) }

    if (mostrarDialogRol) {
        AlertDialog(
            onDismissRequest = { mostrarDialogRol = false },
            title = { Text("¿Cómo querés usar Kosking?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        onClick = { mostrarDialogRol = false; onSoyDueno() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏪", style = MaterialTheme.typography.headlineSmall)
                            Column {
                                Text("Soy el dueño", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Creo y administro mi local",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                            }
                        }
                    }
                    Card(
                        onClick = { mostrarDialogRol = false; onSoyEmpleado() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👥", style = MaterialTheme.typography.headlineSmall)
                            Column {
                                Text("Soy empleado", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Me uno con el código del dueño",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarDialogRol = false }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── CUENTA ──────────────────────────────────────────────────
        if (!estaLogueado) {
            // Sin sesión: invitar a crear cuenta o iniciar sesión
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Crear una cuenta es gratis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Con tu cuenta podés crear o unirte a un local.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onRegistrarse,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Crear cuenta")
            }
            OutlinedButton(
                onClick = onIniciarSesion,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Iniciar sesión")
            }
        } else {
            // Con sesión: avatar + nombre + rol (clickeable → perfil)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAbrirPerfil)
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        nombreUsuario.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        nombreUsuario.ifBlank { "Mi cuenta" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            esDueno -> "Dueño de local"
                            esEmpleado -> "Empleado"
                            else -> "Cuenta personal"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight, null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // ── PANEL (si tiene rol) ─────────────────────────────────────
        if (esDueno) {
            Button(
                onClick = onSoyDueno,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Store, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir mi panel")
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
                Text("Ir a mi panel")
            }
        }

        // ── LOCAL (si no tiene rol y está logueado) ──────────────────
        if (estaLogueado && !esDueno && !esEmpleado) {
            Button(
                onClick = { mostrarDialogRol = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Store, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Configurar mi local")
            }
        }

        // ── CONFIGURACIÓN (si está logueado) ─────────────────────────
        if (estaLogueado) {
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Configuración") },
                leadingContent = { Icon(Icons.Default.Settings, null, Modifier.size(20.dp)) },
                modifier = Modifier.clickable(onClick = onConfiguracion).fillMaxWidth()
            )
        }

        // ── APP ───────────────────────────────────────────────────────
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
            modifier = Modifier.clickable(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT,
                        "¡Usá Kosking para manejar los fiaditos de tu local! Es gratis para empezar. Buscalo como \"Kosking\" en Play Store 📱")
                }
                context.startActivity(Intent.createChooser(intent, "Compartir Kosking"))
            }).fillMaxWidth()
        )

        // ── CERRAR SESIÓN ─────────────────────────────────────────────
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

