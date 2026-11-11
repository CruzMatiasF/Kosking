package com.kosking.app.navegacion

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.activity.compose.BackHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kosking.app.autenticacion.PantallaLogin
import com.kosking.app.autenticacion.PantallaRegistro
import com.kosking.app.autenticacion.AuthViewModel
import com.kosking.app.dueno.local.PantallaCrearLocal
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.detalle.PantallaDetalleMovimiento
import com.kosking.app.empleado.EmpleadoViewModel
import com.kosking.app.empleado.PantallaIngresarCodigo
import com.kosking.app.empleado.PantallaPanelEmpleado
import com.kosking.app.onboarding.PantallaHome
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.datos.StartupUseCase
import com.kosking.app.datos.StartupDestino
import com.kosking.app.perfil.PantallaAcercaDe
import com.kosking.app.perfil.PantallaConfigurarPerfil
import com.kosking.app.perfil.PantallaConfiguracion
import com.kosking.app.onboarding.PantallaOnboarding
import com.kosking.app.perfil.PantallaPrivacidad
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

private sealed class DestinoPostLogin {
    object PanelDueno : DestinoPostLogin()
    object IngresarCodigo : DestinoPostLogin()
}

@Composable
fun KoskingNavHost() {
    val nav = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val duenoVm: DuenoViewModel = viewModel()
    val empleadoVm: EmpleadoViewModel = viewModel()
    var mostrarPanelDueno by remember { mutableStateOf(false) }
    var tabInicialDueno by remember { mutableStateOf(0) }
    var tabInicialEmpleado by remember { mutableStateOf(0) }
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
    LaunchedEffect(authUser?.uid) {
        val uid = authUser?.uid ?: run { nombreUsuario = ""; return@LaunchedEffect }
        try {
            nombreUsuario = FiadoRepository.obtenerUsuario(uid)?.nombre
                ?: authUser?.displayName ?: ""
        } catch (_: Exception) {}
    }

    val esDueno by duenoVm.localId.collectAsState()
    val esEmpleado by empleadoVm.tieneSession.collectAsState()

    val context = LocalContext.current

    val onboardingVisto = remember {
        context.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE)
            .getBoolean("onboarding_visto", false)
    }

    NavHost(
        navController = nav,
        startDestination = if (onboardingVisto) Rutas.EXPLORAR else Rutas.ONBOARDING
    ) {

        composable(Rutas.ONBOARDING) {
            PantallaOnboarding(
                onTerminar = {
                    context.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("onboarding_visto", true).apply()
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
            var startupResuelto by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (startupResuelto) return@LaunchedEffect
                val destino = StartupUseCase.resolver(
                    context = ctx,
                    empleadoTieneSession = empleadoVm.tieneSession.value,
                    duenoLocalIdFlow = duenoVm.localId,
                    localesGuardados = emptyList()
                )
                when (destino) {
                    StartupDestino.PanelEmpleado ->
                        nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                    is StartupDestino.PanelEmpleadoConTab -> {
                        tabInicialEmpleado = destino.tab
                        nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                    }
                    StartupDestino.PanelDueno -> mostrarPanelDueno = true
                    is StartupDestino.PanelDuenoConTab -> {
                        tabInicialDueno = destino.tab
                        mostrarPanelDueno = true
                    }
                    else -> {}
                }
                startupResuelto = true
            }

            if (!startupResuelto) return@composable

            PantallaHome(
                estaLogueado = estaLogueado,
                nombreUsuario = nombreUsuario,
                esDueno = esDueno.isNotBlank(),
                esEmpleado = esEmpleado,
                duenoVm = duenoVm,
                mostrarPanelDueno = mostrarPanelDueno,
                onCerrarPanelDueno = { mostrarPanelDueno = false },
                tabInicialDueno = tabInicialDueno,
                onSoyDueno = {
                    when {
                        !estaLogueado -> {
                            destinoPostLogin = DestinoPostLogin.PanelDueno
                            nav.navigate(Rutas.LOGIN)
                        }
                        duenoTieneSession -> mostrarPanelDueno = true
                        else -> nav.navigate(Rutas.CREAR_LOCAL)
                    }
                },
                onSoyEmpleado = {
                    if (!estaLogueado) {
                        destinoPostLogin = DestinoPostLogin.IngresarCodigo
                        nav.navigate(Rutas.LOGIN)
                    } else if (empleadoTieneSession) {
                        nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                    } else {
                        nav.navigate(Rutas.INGRESAR_CODIGO)
                    }
                },
                onIniciarSesion = {
                    destinoPostLogin = null
                    nav.navigate(Rutas.LOGIN)
                },
                onCerrarSesionCuenta = {
                    empleadoVm.cerrarSesion()
                    duenoVm.cerrarSesion()
                    mostrarPanelDueno = false
                    nav.navigate(Rutas.EXPLORAR) { popUpTo(0) { inclusive = true } }
                },
                onConfigurarPerfil = { nav.navigate(Rutas.CONFIGURAR_PERFIL) },
                onConfiguracion = { nav.navigate(Rutas.CONFIGURACION) },
                onAcercaDe = { nav.navigate(Rutas.ACERCA_DE) },
            )
        }

        composable(Rutas.LOGIN) {
            PantallaLogin(
                onLoginExitoso = {
                    nav.popBackStack()
                    when (destinoPostLogin) {
                        DestinoPostLogin.IngresarCodigo -> {
                            destinoPostLogin = null
                            nav.navigate(Rutas.INGRESAR_CODIGO)
                        }
                        else -> {
                            destinoPostLogin = null
                            mostrarPanelDueno = true
                        }
                    }
                },
                onIrARegistro = { nav.navigate(Rutas.REGISTRO) }
            )
        }

        composable(Rutas.REGISTRO) {
            PantallaRegistro(
                onRegistroExitoso = {
                    when (destinoPostLogin) {
                        DestinoPostLogin.IngresarCodigo -> {
                            destinoPostLogin = null
                            nav.navigate(Rutas.INGRESAR_CODIGO) { popUpTo(Rutas.EXPLORAR) }
                        }
                        else -> {
                            destinoPostLogin = null
                            nav.navigate(Rutas.CREAR_LOCAL) { popUpTo(Rutas.EXPLORAR) }
                        }
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
            PantallaPanelEmpleado(
                onVerCliente = { id -> nav.navigate(Rutas.agregarFiadoEmpleado(id)) },
                onCerrarSesion = {
                    empleadoVm.cerrarSesion()
                    nav.navigate(Rutas.EXPLORAR) { popUpTo(0) { inclusive = true } }
                },
                onVolver = { nav.popBackStack() },
                tabInicial = tabInicialEmpleado,
                vm = empleadoVm
            )
        }

        composable(
            Rutas.AGREGAR_FIADO_EMPLEADO,
            arguments = listOf(navArgument("clienteId") { type = NavType.StringType })
        ) { back ->
            val clienteId = back.arguments?.getString("clienteId") ?: return@composable
            val cargando by empleadoVm.cargando.collectAsState()
            val error by empleadoVm.error.collectAsState()
            val productos by empleadoVm.productos.collectAsState()
            val nombreEmpleado by empleadoVm.nombreEmpleado.collectAsState()
            val clientes by empleadoVm.clientesHabilitados.collectAsState()
            val cliente = remember(clientes, clienteId) { clientes.find { it.id == clienteId } }
            com.kosking.app.dueno.PantallaAgregarFiadoConLambda(
                onVolver = { nav.popBackStack() },
                cargando = cargando,
                error = error,
                productos = productos,
                deudaActual = cliente?.deudaTotal ?: 0.0,
                limiteCredito = cliente?.limiteCredito ?: 0.0,
                nombreCliente = cliente?.nombre ?: "",
                nombreEmpleado = nombreEmpleado,
                visitantesConocidos = cliente?.visitantesConocidos ?: emptyList(),
                onConfirmar = { items, tipo, visitante ->
                    empleadoVm.agregarMovimientos(clienteId, items, tipo, visitante) { nav.popBackStack() }
                }
            )
        }

        composable(
            Rutas.DETALLE_MOVIMIENTO,
            arguments = listOf(navArgument("movimientoId") { type = NavType.StringType })
        ) { back ->
            val movimientoId = back.arguments?.getString("movimientoId") ?: return@composable
            PantallaDetalleMovimiento(
                movimientoId = movimientoId,
                onVolver = { nav.popBackStack() },
                vm = duenoVm
            )
        }

        composable(Rutas.CONFIGURAR_PERFIL) {
            PantallaConfigurarPerfil(
                nombreActual = nombreUsuario,
                emailActual = emailUsuario,
                onVolver = { nav.popBackStack() }
            )
        }

        composable(Rutas.CONFIGURACION) {
            val esDuenoVal by duenoVm.localId.collectAsState()
            val esEmpleadoVal by empleadoVm.tieneSession.collectAsState()
            PantallaConfiguracion(
                esDueno = esDuenoVal.isNotBlank(),
                esEmpleado = esEmpleadoVal,
                tieneLocalesFiado = false,
                onVolver = { nav.popBackStack() }
            )
        }

        composable(Rutas.ACERCA_DE) {
            PantallaAcercaDe(
                onVolver = { nav.popBackStack() },
                onPrivacidad = { nav.navigate(Rutas.PRIVACIDAD) }
            )
        }

        composable(Rutas.PRIVACIDAD) {
            com.kosking.app.perfil.PantallaPrivacidad(onVolver = { nav.popBackStack() })
        }
    }
}
