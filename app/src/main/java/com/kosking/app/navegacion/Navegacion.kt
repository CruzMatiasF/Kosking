package com.kosking.app.navegacion

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kosking.app.dueno.local.PantallaConfigurarLocal
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
import com.kosking.app.perfil.PantallaPerfil
import com.kosking.app.dueno.PantallaColaboradores
import com.kosking.app.onboarding.PantallaOnboarding
import com.kosking.app.perfil.PantallaPrivacidad
import androidx.compose.ui.platform.LocalContext
import com.kosking.app.datos.PreferenciasRepository
import kotlinx.coroutines.launch

private sealed class DestinoPostLogin {
    object CrearLocal : DestinoPostLogin()
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
            PantallaPanelEmpleado(
                onVerCliente = { id -> nav.navigate(Rutas.agregarFiadoEmpleado(id)) },
                onCerrarSesion = {
                    empleadoVm.cerrarSesion()
                    nav.navigate(Rutas.EXPLORAR) { popUpTo(0) { inclusive = true } }
                },
                onVolver = { nav.popBackStack() },
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
            val nombreEmpleado by empleadoVm.nombreEmpleado.collectAsState()
            val clientes by empleadoVm.clientesHabilitados.collectAsState()
            val productos by empleadoVm.productos.collectAsState()
            val cliente = remember(clientes, clienteId) { clientes.find { it.id == clienteId } }
            com.kosking.app.dueno.PantallaAgregarFiadoConLambda(
                onVolver = { nav.popBackStack() },
                cargando = cargando,
                error = error,
                deudaActual = cliente?.deudaTotal ?: 0.0,
                limiteCredito = cliente?.limiteCredito ?: 0.0,
                nombreCliente = cliente?.nombre ?: "",
                nombreEmpleado = nombreEmpleado,
                productos = productos,
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

        composable(Rutas.PERFIL) {
            val localData by duenoVm.localData.collectAsState()
            val esDuenoVal by duenoVm.localId.collectAsState()
            PantallaPerfil(
                nombreUsuario = nombreUsuario,
                rol = when {
                    esDuenoVal.isNotBlank() -> "Dueño de local"
                    else -> "Cuenta personal"
                },
                esDueno = esDuenoVal.isNotBlank(),
                onVolver = { nav.popBackStack() },
                onInformacionPersonal = { nav.navigate(Rutas.CONFIGURAR_PERFIL) },
                onPrivacidad = { nav.navigate(Rutas.PRIVACIDAD) }
            )
        }

        composable(Rutas.COLABORADORES) {
            val empleados by duenoVm.empleados.collectAsState()
            val movimientos by duenoVm.movimientosLocal.collectAsState()
            val clientes by duenoVm.clientes.collectAsState()
            PantallaColaboradores(
                empleados = empleados,
                movimientos = movimientos,
                clientes = clientes,
                onEliminarEmpleado = { duenoVm.eliminarEmpleado(it) },
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
                onColaboradores = { nav.navigate(Rutas.COLABORADORES) },
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
                onPrivacidad = { nav.navigate(Rutas.PRIVACIDAD) }
            )
        }

        composable(Rutas.PRIVACIDAD) {
            com.kosking.app.perfil.PantallaPrivacidad(onVolver = { nav.popBackStack() })
        }
    }
}
