package com.kosking.app.navegacion

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kosking.app.autenticacion.PantallaLogin
import com.kosking.app.autenticacion.PantallaRegistro
import com.kosking.app.autenticacion.AuthViewModel
import com.kosking.app.dueno.local.PantallaCrearLocal
import com.kosking.app.dueno.local.PantallaConfigurarLocal
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.detalle.PantallaDetalleMovimiento
import com.kosking.app.empleado.EmpleadoViewModel
import com.kosking.app.empleado.PantallaIngresarCodigo
import com.kosking.app.home.PantallaHome
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.datos.StartupUseCase
import com.kosking.app.datos.StartupDestino
import com.kosking.app.ajustes.PantallaAcercaDe
import com.kosking.app.ajustes.PantallaApariencia
import com.kosking.app.ajustes.PantallaConfiguracion
import com.kosking.app.ajustes.PantallaSeguridad
import com.kosking.app.ajustes.PantallaNotificaciones
import com.kosking.app.ajustes.PantallaSoporte
import com.kosking.app.perfil.PantallaConfigurarPerfil
import com.kosking.app.perfil.PantallaPerfil
import com.kosking.app.dueno.PanelDuenoEmbedido
import com.kosking.app.dueno.detalle.PantallaDetalleCliente
import com.kosking.app.autenticacion.PantallaOnboarding
import com.kosking.app.perfil.PantallaPrivacidad
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kosking.app.datos.PreferenciasRepository
import kotlinx.coroutines.launch

private sealed class DestinoPostLogin {
    object CrearLocal : DestinoPostLogin()
    object IngresarCodigo : DestinoPostLogin()
}

@Composable
fun KoskingNavHost(onTemaChanged: (String) -> Unit = {}) {
    val nav = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val duenoVm: DuenoViewModel = viewModel()
    val empleadoVm: EmpleadoViewModel = viewModel()
    var mostrarPanelDueno by remember { mutableStateOf(false) }
    var destinoPostLogin by remember { mutableStateOf<DestinoPostLogin?>(null) }

    val authUser by produceState<FirebaseUser?>(
        initialValue = FirebaseAuth.getInstance().currentUser?.takeIf { !it.isAnonymous }
    ) {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            value = fa.currentUser?.takeIf { !it.isAnonymous }
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        awaitDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }

    val estaLogueado = authUser != null
    val emailUsuario = authUser?.email ?: ""

    var nombreUsuario by remember { mutableStateOf("") }
    var usuarioCompleto by remember { mutableStateOf<com.kosking.app.modelo.Usuario?>(null) }
    LaunchedEffect(authUser?.uid) {
        val uid = authUser?.uid ?: run { nombreUsuario = ""; usuarioCompleto = null; return@LaunchedEffect }
        try {
            val u = FiadoRepository.obtenerUsuario(uid)
            usuarioCompleto = u
            nombreUsuario = u?.nombre ?: authUser?.displayName ?: ""
        } catch (_: Exception) {}
    }

    val esDueno by duenoVm.localId.collectAsState()
    val esEmpleado by empleadoVm.tieneSession.collectAsState()

    // Si el timeout de startup disparó antes de que Firebase cargara, corregir aquí
    LaunchedEffect(esDueno) {
        if (esDueno.isNotBlank() && !mostrarPanelDueno) {
            mostrarPanelDueno = true
        }
    }

    val context = LocalContext.current

    val onboardingVisto = remember { PreferenciasRepository.isOnboardingVisto(context) }

    NavHost(
        navController = nav,
        startDestination = if (onboardingVisto) Rutas.EXPLORAR else Rutas.ONBOARDING
    ) {

        composable(Rutas.ONBOARDING) {
            PantallaOnboarding(
                onTerminar = {
                    PreferenciasRepository.marcarOnboardingVisto(context)
                    nav.navigate(Rutas.EXPLORAR) {
                        popUpTo(Rutas.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.EXPLORAR) {
            val empleadoTieneSession by empleadoVm.tieneSession.collectAsState()
            val duenoTieneSession by duenoVm.tieneSession.collectAsState()
            val ctx = LocalContext.current
            var startupResuelto by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (startupResuelto) return@LaunchedEffect
                val destino = StartupUseCase.resolver(
                    context = ctx,
                    empleadoTieneSession = empleadoVm.tieneSession.value,
                    duenoLocalIdFlow = duenoVm.localId
                )
                when (destino) {
                    StartupDestino.PanelEmpleado ->
                        nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                    StartupDestino.PanelDueno -> mostrarPanelDueno = true
                    is StartupDestino.PanelDuenoConTab -> mostrarPanelDueno = true
                    else -> {}
                }
                startupResuelto = true
            }

            if (!startupResuelto) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@composable
            }

            PantallaHome(
                estaLogueado = estaLogueado,
                nombreUsuario = nombreUsuario,
                esDueno = esDueno.isNotBlank(),
                esEmpleado = esEmpleado,
                duenoVm = duenoVm,
                mostrarPanelDueno = mostrarPanelDueno,
                onCerrarPanelDueno = { mostrarPanelDueno = false },
                onSoyDueno = {
                    when {
                        !estaLogueado -> {
                            destinoPostLogin = DestinoPostLogin.CrearLocal
                            nav.navigate(Rutas.LOGIN)
                        }
                        duenoTieneSession -> mostrarPanelDueno = true
                        else -> nav.navigate(Rutas.CREAR_LOCAL)
                    }
                },
                onSoyEmpleado = {
                    when {
                        !estaLogueado -> {
                            destinoPostLogin = DestinoPostLogin.IngresarCodigo
                            nav.navigate(Rutas.LOGIN)
                        }
                        empleadoTieneSession -> nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                        else -> nav.navigate(Rutas.INGRESAR_CODIGO)
                    }
                },
                onTemaChanged = onTemaChanged,
                onIniciarSesion = { nav.navigate(Rutas.LOGIN) },
                onRegistrarse = { nav.navigate(Rutas.REGISTRO) },
                onCerrarSesionCuenta = {
                    empleadoVm.cerrarSesion()
                    duenoVm.cerrarSesion()
                    mostrarPanelDueno = false
                    nav.navigate(Rutas.EXPLORAR) { popUpTo(0) { inclusive = true } }
                },
                onConfigurarPerfil = { nav.navigate(Rutas.CONFIGURAR_PERFIL) },
                onAbrirPerfil = { nav.navigate(Rutas.PERFIL) },
                onConfiguracion = { nav.navigate(Rutas.CONFIGURACION) },
                onMiLocal = { nav.navigate(Rutas.CONFIGURAR_LOCAL) },
                onAcercaDe = { nav.navigate(Rutas.ACERCA_DE) },
            )
        }

        composable(Rutas.LOGIN) {
            PantallaLogin(
                onLoginExitoso = {
                    nav.popBackStack()
                    val destino = destinoPostLogin
                    destinoPostLogin = null
                    when (destino) {
                        DestinoPostLogin.CrearLocal -> nav.navigate(Rutas.CREAR_LOCAL)
                        DestinoPostLogin.IngresarCodigo -> nav.navigate(Rutas.INGRESAR_CODIGO)
                        null -> {}
                    }
                },
                onIrARegistro = { nav.navigate(Rutas.REGISTRO) },
                onVolver = { nav.popBackStack() }
            )
        }

        composable(Rutas.REGISTRO) {
            PantallaRegistro(
                onRegistroExitoso = {
                    nav.popBackStack(Rutas.EXPLORAR, inclusive = false)
                    val destino = destinoPostLogin
                    destinoPostLogin = null
                    when (destino) {
                        DestinoPostLogin.CrearLocal -> nav.navigate(Rutas.CREAR_LOCAL)
                        DestinoPostLogin.IngresarCodigo -> nav.navigate(Rutas.INGRESAR_CODIGO)
                        null -> {}
                    }
                },
                onVolver = { nav.popBackStack() }
            )
        }

        composable(Rutas.CREAR_LOCAL) {
            PantallaCrearLocal(
                onLocalCreado = {
                    nav.popBackStack(Rutas.EXPLORAR, inclusive = false)
                    mostrarPanelDueno = true
                },
                onVolver = { nav.popBackStack(Rutas.EXPLORAR, inclusive = false) },
                vm = duenoVm
            )
        }

        composable(Rutas.INGRESAR_CODIGO) {
            PantallaIngresarCodigo(
                onEmpleadoVerificado = {
                    nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                },
                onVolver = { nav.popBackStack() },
                vm = empleadoVm
            )
        }

        composable(Rutas.PANEL_EMPLEADO) {
            val empleadoLocalId by empleadoVm.localId.collectAsState()
            val puedeVerDeuda by empleadoVm.puedeVerDeuda.collectAsState()
            val nombreEmpleado by empleadoVm.nombreEmpleado.collectAsState()
            val nombreLocalEmpleado by empleadoVm.nombreLocal.collectAsState()
            LaunchedEffect(empleadoLocalId, nombreEmpleado) {
                if (empleadoLocalId.isNotBlank()) duenoVm.inicializarDesdeEmpleado(empleadoLocalId, nombreEmpleado)
            }
            PanelDuenoEmbedido(
                vm = duenoVm,
                esEmpleado = true,
                puedeVerDeuda = puedeVerDeuda,
                onCerrarSesionEmpleado = {
                    empleadoVm.cerrarSesion()
                    duenoVm.cerrarSesion()
                    nav.navigate(Rutas.EXPLORAR) { popUpTo(0) { inclusive = true } }
                },
                onAbrirDetalleCliente = { id -> nav.navigate(Rutas.detalleClienteEmpleado(id)) },
                onAbrirDetalleMovimiento = { id -> nav.navigate(Rutas.detalleMovimiento(id)) }
            )
        }

        composable(
            Rutas.DETALLE_CLIENTE_EMPLEADO,
            arguments = listOf(navArgument("clienteId") { type = NavType.StringType })
        ) { back ->
            val clienteId = back.arguments?.getString("clienteId") ?: return@composable
            val puedeVerDeuda by empleadoVm.puedeVerDeuda.collectAsState()
            PantallaDetalleCliente(
                clienteId = clienteId,
                onVolver = { nav.popBackStack() },
                onVerMovimiento = { id -> nav.navigate(Rutas.detalleMovimiento(id)) },
                vm = duenoVm,
                esEmpleado = true,
                puedeVerDeuda = puedeVerDeuda
            )
        }

        composable(
            Rutas.DETALLE_MOVIMIENTO,
            arguments = listOf(navArgument("movimientoId") { type = NavType.StringType })
        ) { back ->
            val movimientoId = back.arguments?.getString("movimientoId") ?: return@composable
            val esEmpleadoVal by empleadoVm.tieneSession.collectAsState()
            PantallaDetalleMovimiento(
                movimientoId = movimientoId,
                onVolver = { nav.popBackStack() },
                vm = duenoVm,
                esEmpleado = esEmpleadoVal
            )
        }

        composable(Rutas.PERFIL) {
            val esDuenoVal by duenoVm.localId.collectAsState()
            val esEmpleadoVal by empleadoVm.tieneSession.collectAsState()
            val nombreLocal by empleadoVm.nombreLocal.collectAsState()
            PantallaPerfil(
                nombreUsuario = nombreUsuario,
                rol = when {
                    esDuenoVal.isNotBlank() -> "Dueño de local"
                    esEmpleadoVal -> if (nombreLocal.isNotBlank()) "Empleado en $nombreLocal" else "Empleado"
                    else -> "Cuenta personal"
                },
                esDueno = esDuenoVal.isNotBlank(),
                onVolver = { nav.popBackStack() },
                onInformacionPersonal = { nav.navigate(Rutas.CONFIGURAR_PERFIL) },
                onSeguridad = { nav.navigate(Rutas.SEGURIDAD) },
                onPrivacidad = { nav.navigate(Rutas.PRIVACIDAD) },
                onNotificaciones = { nav.navigate(Rutas.NOTIFICACIONES) }
            )
        }

        composable(Rutas.SEGURIDAD) {
            PantallaSeguridad(onVolver = { nav.popBackStack() })
        }

        composable(Rutas.CONFIGURAR_PERFIL) {
            PantallaConfigurarPerfil(
                nombreActual = nombreUsuario,
                emailActual = emailUsuario,
                usuario = usuarioCompleto,
                onVolver = { nav.popBackStack() }
            )
        }

        composable(Rutas.CONFIGURAR_LOCAL) {
            val localData by duenoVm.localData.collectAsState()
            val cargando by duenoVm.cargando.collectAsState()
            val local = localData ?: return@composable
            PantallaConfigurarLocal(
                localData = local,
                cargando = cargando,
                onGuardar = { nombre, direccion, telefono, horarios ->
                    duenoVm.actualizarLocalCompleto(nombre, direccion, telefono, horarios) {
                        nav.popBackStack()
                    }
                },
                onVolver = { nav.popBackStack() }
            )
        }

        composable(Rutas.CONFIGURACION) {
            val esDuenoVal by duenoVm.localId.collectAsState()
            val esEmpleadoVal by empleadoVm.tieneSession.collectAsState()
            PantallaConfiguracion(
                esDueno = esDuenoVal.isNotBlank(),
                esEmpleado = esEmpleadoVal,
                onVolver = { nav.popBackStack() },
                onApariencia = { nav.navigate(Rutas.APARIENCIA) },
                onSoporte = { nav.navigate(Rutas.SOPORTE) },
                onMiLocal = {
                    when {
                        esDuenoVal.isNotBlank() -> nav.navigate(Rutas.CONFIGURAR_LOCAL)
                        esEmpleadoVal -> nav.navigate(Rutas.INGRESAR_CODIGO)
                        else -> nav.navigate(Rutas.CREAR_LOCAL)
                    }
                },
                onCerrarSesion = {
                    empleadoVm.cerrarSesion()
                    duenoVm.cerrarSesion()
                    mostrarPanelDueno = false
                    nav.navigate(Rutas.EXPLORAR) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Rutas.ACERCA_DE) {
            PantallaAcercaDe(
                onVolver = { nav.popBackStack() },
                onPrivacidad = { nav.navigate(Rutas.POLITICA_PRIVACIDAD) }
            )
        }

        composable(Rutas.APARIENCIA) {
            PantallaApariencia(
                onVolver = { nav.popBackStack() },
                onTemaChanged = onTemaChanged
            )
        }

        composable(Rutas.SOPORTE) {
            PantallaSoporte(onVolver = { nav.popBackStack() })
        }

        composable(Rutas.PRIVACIDAD) {
            com.kosking.app.perfil.PantallaPrivacidad(
                onVolver = { nav.popBackStack() },
                onPoliticaPrivacidad = { nav.navigate(Rutas.POLITICA_PRIVACIDAD) },
                onCuentaEliminada = {
                    nav.navigate(Rutas.EXPLORAR) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Rutas.POLITICA_PRIVACIDAD) {
            com.kosking.app.perfil.PantallaPoliticaPrivacidad(onVolver = { nav.popBackStack() })
        }

        composable(Rutas.NOTIFICACIONES) {
            val esDuenoVal by duenoVm.localId.collectAsState()
            PantallaNotificaciones(
                esDueno = esDuenoVal.isNotBlank(),
                onVolver = { nav.popBackStack() }
            )
        }
    }
}
