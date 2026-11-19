package com.kosking.app.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.NavDueno
import com.kosking.app.dueno.PanelDuenoEmbedido
import com.kosking.app.dueno.PantallaAgregarCliente
import com.kosking.app.dueno.PantallaAgregarFiado
import com.kosking.app.dueno.detalle.PantallaDetalleCliente
import com.kosking.app.dueno.detalle.PantallaDetalleMovimiento
import com.kosking.app.dueno.tabs.TabEquipo
import com.kosking.app.perfil.PantallaPerfil
import com.kosking.app.perfil.PantallaPrivacidad
import com.kosking.app.perfil.PantallaPoliticaPrivacidad
import com.kosking.app.perfil.PantallaConfigurarPerfil
import com.kosking.app.ajustes.PantallaSeguridad
import com.kosking.app.ajustes.PantallaConfiguracion
import com.kosking.app.ajustes.PantallaAcercaDe
import com.kosking.app.ajustes.PantallaNotificaciones
import com.kosking.app.ajustes.PantallaApariencia
import com.kosking.app.ajustes.PantallaExportar
import com.kosking.app.ajustes.PantallaEstadisticas
import com.kosking.app.ajustes.PantallaSoporte

private sealed class VistaExtra {
    object Ninguna : VistaExtra()
    object Perfil : VistaExtra()
    object Seguridad : VistaExtra()
    object Privacidad : VistaExtra()
    object PoliticaPrivacidad : VistaExtra()
    object ConfigurarPerfil : VistaExtra()
    object Configuracion : VistaExtra()
    object Notificaciones : VistaExtra()
    object Apariencia : VistaExtra()
    object Exportar : VistaExtra()
    object Estadisticas : VistaExtra()
    object Soporte : VistaExtra()
    object AcercaDe : VistaExtra()
}

// Tabs de la barra inferior — siempre los mismos para todos los rubros
private enum class TabNav(val titulo: String) {
    MI_LOCAL("Mi Local"),
    MI_EQUIPO("Mi Equipo"),
    MAS("Más")
}

@Composable
fun PantallaHome(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    duenoVm: DuenoViewModel,
    mostrarPanelDueno: Boolean,
    onCerrarPanelDueno: () -> Unit,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onIniciarSesion: () -> Unit,
    onRegistrarse: () -> Unit = {},
    onCerrarSesionCuenta: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onAbrirPerfil: () -> Unit = {},
    onConfiguracion: () -> Unit = {},
    onMiLocal: () -> Unit = {},
    onAcercaDe: () -> Unit = {},
    onTemaChanged: (String) -> Unit = {},
) {
    var vistaDueno by remember { mutableStateOf<NavDueno>(NavDueno.Panel) }
    var tabNav by rememberSaveable { mutableStateOf(TabNav.MI_LOCAL) }
    var vistaExtra by remember { mutableStateOf<VistaExtra>(VistaExtra.Ninguna) }

    LaunchedEffect(mostrarPanelDueno) {
        if (mostrarPanelDueno) vistaDueno = NavDueno.Panel
    }

    BackHandler(enabled = vistaDueno !is NavDueno.Panel) {
        vistaDueno = when (val v = vistaDueno) {
            is NavDueno.AgregarFiado -> if (v.volverAPanel) NavDueno.Panel
                                        else NavDueno.DetalleCliente(v.clienteId)
            else -> NavDueno.Panel
        }
    }

    BackHandler(enabled = vistaExtra !is VistaExtra.Ninguna) {
        vistaExtra = when (vistaExtra) {
            is VistaExtra.Seguridad, is VistaExtra.Privacidad, is VistaExtra.ConfigurarPerfil -> VistaExtra.Perfil
            is VistaExtra.PoliticaPrivacidad -> VistaExtra.Privacidad
            else -> VistaExtra.Ninguna
        }
    }

    val enSubPantalla = vistaDueno !is NavDueno.Panel || vistaExtra !is VistaExtra.Ninguna

    // Empleados y clientes para Mi Equipo
    val empleados by duenoVm.empleados.collectAsState()
    val movimientos by duenoVm.movimientosLocal.collectAsState()
    val clientes by duenoVm.clientes.collectAsState()

    Scaffold(
        bottomBar = {
            if (!enSubPantalla && esDueno) {
                NavigationBar {
                    TabNav.entries.forEach { tab ->
                        // Mi Equipo solo para dueños (ya filtrado por esDueno arriba)
                        NavigationBarItem(
                            selected = tabNav == tab,
                            onClick = { tabNav = tab },
                            icon = {
                                Icon(
                                    when (tab) {
                                        TabNav.MI_LOCAL  -> Icons.Default.Store
                                        TabNav.MI_EQUIPO -> Icons.Default.Groups
                                        TabNav.MAS       -> Icons.Default.Menu
                                    },
                                    null
                                )
                            },
                            label = { Text(tab.titulo) }
                        )
                    }
                }
            } else if (!enSubPantalla && !esDueno) {
                // Sin sesión o empleado: solo tab Más
                NavigationBar {
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = { Icon(Icons.Default.Menu, null) },
                        label = { Text("Más") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                // Sub-pantallas de navegación (DetalleCiente, AgregarFiado, etc.)
                enSubPantalla -> when (val vista = vistaDueno) {
                    is NavDueno.Panel -> {
                        // vistaExtra está activa
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

                // Tab Mi Local
                tabNav == TabNav.MI_LOCAL -> {
                    if (mostrarPanelDueno) {
                        PanelDuenoEmbedido(
                            vm = duenoVm,
                            onAbrirAgregarCliente = { vistaDueno = NavDueno.AgregarCliente },
                            onAbrirDetalleCliente = { vistaDueno = NavDueno.DetalleCliente(it) },
                            onAbrirDetalleMovimiento = { vistaDueno = NavDueno.DetalleMovimiento(it) }
                        )
                    } else {
                        // Sin local creado — CTA para registrar negocio
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text("🏪", style = MaterialTheme.typography.displayMedium)
                                Text(
                                    "Tu panel de negocio",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Registrá tu negocio o iniciá sesión para ver tu panel.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = onSoyDueno, modifier = Modifier.fillMaxWidth()) {
                                    Text("Registrar mi negocio")
                                }
                                OutlinedButton(onClick = onSoyEmpleado, modifier = Modifier.fillMaxWidth()) {
                                    Text("Soy empleado")
                                }
                                if (!estaLogueado) {
                                    TextButton(onClick = onIniciarSesion) {
                                        Text("Ya tengo cuenta — Iniciar sesión")
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab Mi Equipo (solo dueños)
                tabNav == TabNav.MI_EQUIPO -> {
                    Scaffold(
                        topBar = {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(title = { Text("Mi Equipo", fontWeight = FontWeight.SemiBold) })
                        }
                    ) { innerPadding ->
                        Box(Modifier.padding(innerPadding)) {
                        TabEquipo(
                            empleados = empleados,
                            movimientos = movimientos,
                            clientes = clientes,
                            onEliminarEmpleado = { duenoVm.eliminarEmpleado(it) },
                            vm = duenoVm
                        )
                        }
                    }
                }

                // Tab Más
                else -> TabMas(
                    estaLogueado = estaLogueado,
                    nombreUsuario = nombreUsuario,
                    esDueno = esDueno,
                    esEmpleado = esEmpleado,
                    onSoyDueno = { onSoyDueno(); tabNav = TabNav.MI_LOCAL },
                    onSoyEmpleado = onSoyEmpleado,
                    onIniciarSesion = onIniciarSesion,
                    onRegistrarse = onRegistrarse,
                    onCerrarSesion = onCerrarSesionCuenta,
                    onConfigurarPerfil = { vistaExtra = VistaExtra.ConfigurarPerfil },
                    onAbrirPerfil = { vistaExtra = VistaExtra.Perfil },
                    onConfiguracion = { if (esDueno) onMiLocal() else vistaExtra = VistaExtra.Configuracion },
                    onNotificaciones = { vistaExtra = VistaExtra.Notificaciones },
                    onApariencia = { vistaExtra = VistaExtra.Apariencia },
                    onExportar = { vistaExtra = VistaExtra.Exportar },
                    onEstadisticas = { vistaExtra = VistaExtra.Estadisticas },
                    onSoporte = { vistaExtra = VistaExtra.Soporte },
                    onAcercaDe = { vistaExtra = VistaExtra.AcercaDe },
                )
            }

            // Sub-pantallas de perfil (encima de todo)
            when (vistaExtra) {
                is VistaExtra.Perfil -> PantallaPerfil(
                    nombreUsuario = nombreUsuario,
                    rol = when {
                        esDueno -> "Dueño de local"
                        else -> "Cuenta personal"
                    },
                    esDueno = esDueno,
                    onVolver = { vistaExtra = VistaExtra.Ninguna },
                    onInformacionPersonal = { vistaExtra = VistaExtra.ConfigurarPerfil },
                    onSeguridad = { vistaExtra = VistaExtra.Seguridad },
                    onPrivacidad = { vistaExtra = VistaExtra.Privacidad }
                )
                is VistaExtra.ConfigurarPerfil -> PantallaConfigurarPerfil(
                    nombreActual = nombreUsuario,
                    emailActual = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "",
                    onVolver = { vistaExtra = VistaExtra.Perfil }
                )
                is VistaExtra.Seguridad -> PantallaSeguridad(
                    onVolver = { vistaExtra = VistaExtra.Perfil }
                )
                is VistaExtra.Privacidad -> PantallaPrivacidad(
                    onVolver = { vistaExtra = VistaExtra.Perfil },
                    onPoliticaPrivacidad = { vistaExtra = VistaExtra.PoliticaPrivacidad },
                    onCuentaEliminada = onCerrarSesionCuenta
                )
                is VistaExtra.PoliticaPrivacidad -> PantallaPoliticaPrivacidad(
                    onVolver = { vistaExtra = VistaExtra.Privacidad }
                )
                is VistaExtra.Configuracion -> PantallaConfiguracion(
                    esDueno = esDueno,
                    esEmpleado = esEmpleado,
                    onVolver = { vistaExtra = VistaExtra.Ninguna },
                    onMiLocal = onMiLocal,
                    onApariencia = { vistaExtra = VistaExtra.Apariencia },
                    onSoporte = { vistaExtra = VistaExtra.Soporte },
                    onCerrarSesion = onCerrarSesionCuenta
                )
                is VistaExtra.Notificaciones -> PantallaNotificaciones(
                    esDueno = esDueno,
                    onVolver = { vistaExtra = VistaExtra.Ninguna }
                )
                is VistaExtra.Apariencia -> PantallaApariencia(
                    onVolver = { vistaExtra = VistaExtra.Ninguna },
                    onTemaChanged = onTemaChanged
                )
                is VistaExtra.Exportar -> PantallaExportar(
                    vm = duenoVm,
                    onVolver = { vistaExtra = VistaExtra.Ninguna }
                )
                is VistaExtra.Estadisticas -> PantallaEstadisticas(
                    vm = duenoVm,
                    onVolver = { vistaExtra = VistaExtra.Ninguna }
                )
                is VistaExtra.Soporte -> PantallaSoporte(
                    onVolver = { vistaExtra = VistaExtra.Ninguna }
                )
                is VistaExtra.AcercaDe -> PantallaAcercaDe(
                    onVolver = { vistaExtra = VistaExtra.Ninguna },
                    onPrivacidad = { vistaExtra = VistaExtra.PoliticaPrivacidad }
                )
                else -> {}
            }
        }
    }
}
