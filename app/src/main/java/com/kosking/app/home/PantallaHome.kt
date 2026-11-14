package com.kosking.app.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.NavDueno
import com.kosking.app.dueno.PanelDuenoEmbedido
import com.kosking.app.dueno.PantallaAgregarCliente
import com.kosking.app.dueno.PantallaAgregarFiado
import com.kosking.app.dueno.detalle.PantallaDetalleCliente
import com.kosking.app.dueno.detalle.PantallaDetalleMovimiento
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
    onTemaChanged: (String) -> Unit = {},
) {
    var vistaDueno by remember { mutableStateOf<NavDueno>(NavDueno.Panel) }
    var tabPanelDueno by remember { mutableIntStateOf(tabInicialDueno) }
    // 0 = Panel/Inicio, 1 = Más
    var tabNav by remember { mutableIntStateOf(0) }
    var vistaExtra by remember { mutableStateOf<VistaExtra>(VistaExtra.Ninguna) }

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

    BackHandler(enabled = vistaExtra !is VistaExtra.Ninguna) {
        vistaExtra = when (vistaExtra) {
            is VistaExtra.Seguridad, is VistaExtra.Privacidad, is VistaExtra.ConfigurarPerfil -> VistaExtra.Perfil
            is VistaExtra.PoliticaPrivacidad -> VistaExtra.Privacidad
            else -> VistaExtra.Ninguna
        }
    }

    val esDuenoState = esDueno
    val esEmpleadoState = esEmpleado

    // Si hay una sub-pantalla del panel abierta, ocultar la nav bar
    val enSubPantalla = vistaDueno !is NavDueno.Panel || vistaExtra !is VistaExtra.Ninguna

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
                            onConfigurarPerfil = { vistaExtra = VistaExtra.ConfigurarPerfil },
                            onAbrirPerfil = { vistaExtra = VistaExtra.Perfil },
                            onConfiguracion = { vistaExtra = VistaExtra.Configuracion },
                            onNotificaciones = { vistaExtra = VistaExtra.Notificaciones },
                            onApariencia = { vistaExtra = VistaExtra.Apariencia },
                            onExportar = { vistaExtra = VistaExtra.Exportar },
                            onEstadisticas = { vistaExtra = VistaExtra.Estadisticas },
                            onSoporte = { vistaExtra = VistaExtra.Soporte },
                            onAcercaDe = { vistaExtra = VistaExtra.AcercaDe },
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
                    onConfigurarPerfil = { vistaExtra = VistaExtra.ConfigurarPerfil },
                    onAbrirPerfil = { vistaExtra = VistaExtra.Perfil },
                    onConfiguracion = { vistaExtra = VistaExtra.Configuracion },
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
                    esDueno = esDuenoState,
                    esEmpleado = esEmpleadoState,
                    onVolver = { vistaExtra = VistaExtra.Ninguna },
                    onMiLocal = onConfiguracion,
                    onCerrarSesion = onCerrarSesionCuenta
                )
                is VistaExtra.Notificaciones -> PantallaNotificaciones(
                    esDueno = esDuenoState,
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

