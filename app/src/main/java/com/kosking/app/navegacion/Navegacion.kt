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
import com.kosking.app.cliente.ClienteViewModel
import com.kosking.app.cliente.PantallaIngresarPIN
import com.kosking.app.dueno.PantallaCrearLocal
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.PanelDueno
import com.kosking.app.dueno.PantallaDetalleMovimiento
import com.kosking.app.empleado.EmpleadoViewModel
import com.kosking.app.empleado.PantallaIngresarCodigo
import com.kosking.app.empleado.PantallaPanelEmpleado
import com.kosking.app.explorar.ExplorarViewModel
import com.kosking.app.explorar.PantallaExplorar
import com.kosking.app.explorar.PantallaVidreraPublica
import com.kosking.app.explorar.VidreraPublicaViewModel
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.datos.PreferenciasRepository
import com.kosking.app.perfil.PantallaAcercaDe
import com.kosking.app.perfil.PantallaConfigurarPerfil
import com.kosking.app.perfil.PantallaConfiguracion
import com.kosking.app.perfil.PantallaHistorial
import com.kosking.app.perfil.PantallaNotificaciones
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLDecoder
import java.net.URLEncoder

private sealed class DestinoPostLogin {
    object PanelDueno : DestinoPostLogin()
    object IngresarCodigo : DestinoPostLogin()
    data class IngresarPIN(val localId: String, val nombreLocal: String) : DestinoPostLogin()
}

@Composable
fun KoskingNavHost() {
    val nav = rememberNavController()
    val duenoVm: DuenoViewModel = viewModel()
    val empleadoVm: EmpleadoViewModel = viewModel()
    val clienteVm: ClienteViewModel = viewModel()
    val explorarVm: ExplorarViewModel = viewModel()

    var mostrarPanelDueno by remember { mutableStateOf(false) }
    var tabInicialDueno by remember { mutableStateOf(0) }
    var tabInicialEmpleado by remember { mutableStateOf(0) }
    var destinoPostLogin by remember { mutableStateOf<DestinoPostLogin?>(null) }

    // Auth state reactivo
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

    // Cargar nombre real del usuario desde Firestore
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

    // EXPLORAR siempre es la raíz — se redirige desde ahí si hay sesión activa
    NavHost(navController = nav, startDestination = Rutas.EXPLORAR) {

        // ── Explorar (raíz) ───────────────────────────────────────────────────

        composable(Rutas.EXPLORAR) {
            val empleadoTieneSession by empleadoVm.tieneSession.collectAsState()
            val localesGuardados by clienteVm.localesGuardados.collectAsState()
            val context = LocalContext.current

            // Redirigir según la pantalla de inicio configurada por el usuario
            LaunchedEffect(Unit) {
                if (clienteVm.startupRedirectDone) return@LaunchedEffect
                clienteVm.startupRedirectDone = true
                delay(600L)
                val pantalla = PreferenciasRepository.getPantallaInicio(context)
                when (pantalla) {
                    PreferenciasRepository.PANTALLA_EMPLEADO -> {
                        if (empleadoVm.tieneSession.value) {
                            nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                        }
                    }
                    PreferenciasRepository.PANTALLA_DUENO -> {
                        // localId se carga asincrónicamente; esperamos hasta 3s a que llegue
                        val localId = withTimeoutOrNull(3000L) {
                            duenoVm.localId.filter { it.isNotBlank() }.first()
                        }
                        if (localId != null && FirebaseAuth.getInstance().currentUser != null) {
                            mostrarPanelDueno = true
                        }
                    }
                    PreferenciasRepository.PANTALLA_PRODUCTOS -> {
                        if (empleadoVm.tieneSession.value) {
                            tabInicialEmpleado = 1
                            nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                        } else {
                            val localId = withTimeoutOrNull(3000L) {
                                duenoVm.localId.filter { it.isNotBlank() }.first()
                            }
                            if (localId != null && FirebaseAuth.getInstance().currentUser != null) {
                                tabInicialDueno = 2
                                mostrarPanelDueno = true
                            }
                        }
                    }
                    PreferenciasRepository.PANTALLA_FIADO -> {
                        if (clienteVm.localesGuardados.value.isNotEmpty()) {
                            nav.navigate(Rutas.VER_FIADO) { popUpTo(Rutas.EXPLORAR) }
                        }
                    }
                    else -> {
                        // PANTALLA_EXPLORAR o cualquier default: no redirigir
                        // pero si el empleado tiene sesión activa, igual redirigir (comportamiento anterior)
                        if (empleadoVm.tieneSession.value) {
                            nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                        }
                    }
                }
            }

            PantallaExplorar(
                onVerLocal = { localId -> nav.navigate(Rutas.vidreraPublica(localId)) },
                onConfigurarPerfil = { nav.navigate(Rutas.CONFIGURAR_PERFIL) },
                onSoyDueno = {
                    when {
                        !estaLogueado -> {
                            destinoPostLogin = DestinoPostLogin.PanelDueno
                            nav.navigate(Rutas.LOGIN)
                        }
                        duenoVm.tieneSession -> mostrarPanelDueno = true
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
                onVerMisLocales = { nav.navigate(Rutas.VER_FIADO) },
                onConfiguracion = { nav.navigate(Rutas.CONFIGURACION) },
                onNotificaciones = { nav.navigate(Rutas.NOTIFICACIONES) },
                onAcercaDe = { nav.navigate(Rutas.ACERCA_DE) },
                onHistorial = { nav.navigate(Rutas.HISTORIAL) },
                onIniciarSesion = {
                    destinoPostLogin = null
                    nav.navigate(Rutas.LOGIN)
                },
                onCerrarSesionCuenta = {
                    empleadoVm.cerrarSesion()
                    duenoVm.cerrarSesion()
                    mostrarPanelDueno = false
                },
                estaLogueado = estaLogueado,
                nombreUsuario = nombreUsuario,
                emailUsuario = emailUsuario,
                esDueno = esDueno.isNotBlank(),
                esEmpleado = esEmpleado,
                clienteVm = clienteVm,
                duenoVm = duenoVm,
                mostrarPanelDueno = mostrarPanelDueno,
                onCerrarPanelDueno = { mostrarPanelDueno = false },
                tabInicialDueno = tabInicialDueno,
                vm = explorarVm
            )
        }

        composable(
            Rutas.VIDRIERA_PUBLICA,
            arguments = listOf(navArgument("localId") { type = NavType.StringType })
        ) { back ->
            val localId = back.arguments?.getString("localId") ?: return@composable
            val vm: VidreraPublicaViewModel = viewModel()
            PantallaVidreraPublica(
                localId = localId,
                onVolver = { nav.popBackStack() },
                vm = vm
            )
        }

        // ── Autenticación ─────────────────────────────────────────────────────

        composable(Rutas.LOGIN) {
            PantallaLogin(
                onLoginExitoso = {
                    nav.popBackStack()
                    when (val dest = destinoPostLogin) {
                        DestinoPostLogin.IngresarCodigo -> {
                            destinoPostLogin = null
                            nav.navigate(Rutas.INGRESAR_CODIGO)
                        }
                        is DestinoPostLogin.IngresarPIN -> {
                            destinoPostLogin = null
                            nav.navigate(Rutas.ingresarPIN(dest.localId, dest.nombreLocal))
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
                    when (val dest = destinoPostLogin) {
                        DestinoPostLogin.IngresarCodigo -> {
                            destinoPostLogin = null
                            nav.navigate(Rutas.INGRESAR_CODIGO) { popUpTo(Rutas.EXPLORAR) }
                        }
                        is DestinoPostLogin.IngresarPIN -> {
                            destinoPostLogin = null
                            nav.navigate(Rutas.ingresarPIN(dest.localId, dest.nombreLocal)) {
                                popUpTo(Rutas.EXPLORAR)
                            }
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

        composable(Rutas.PANEL_DUENO) {
            PanelDueno(
                onCerrarSesion = {
                    nav.navigate(Rutas.EXPLORAR) { popUpTo(0) { inclusive = true } }
                },
                onVolver = { nav.popBackStack() },
                vm = duenoVm
            )
        }

        // ── Empleado ──────────────────────────────────────────────────────────

        composable(Rutas.INGRESAR_CODIGO) {
            PantallaIngresarCodigo(
                onEmpleadoVerificado = {
                    nav.navigate(Rutas.PANEL_EMPLEADO) {
                        popUpTo(Rutas.EXPLORAR)
                    }
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
                onGuardado = { nav.popBackStack() },
                onVolver = { nav.popBackStack() },
                cargando = cargando,
                error = error,
                productos = productos,
                deudaActual = cliente?.deudaTotal ?: 0.0,
                limiteCredito = cliente?.limiteCredito ?: 0.0,
                nombreCliente = cliente?.nombre ?: "",
                nombreEmpleado = nombreEmpleado,
                onAgregar = { producto, cantidad, monto, tipo ->
                    empleadoVm.agregarMovimiento(clienteId, producto, cantidad, monto, tipo) {
                        nav.popBackStack()
                    }
                }
            )
        }

        // ── Cliente ───────────────────────────────────────────────────────────

        composable(
            Rutas.INGRESAR_PIN,
            arguments = listOf(
                navArgument("localId") { type = NavType.StringType },
                navArgument("nombreLocal") { type = NavType.StringType }
            )
        ) { back ->
            val localId = back.arguments?.getString("localId") ?: return@composable
            val nombreLocal = URLDecoder.decode(
                back.arguments?.getString("nombreLocal") ?: "", "UTF-8"
            )
            PantallaIngresarPIN(
                localId = localId,
                nombreLocal = nombreLocal,
                onClienteVerificado = {
                    nav.navigate(Rutas.VER_FIADO) {
                        popUpTo(Rutas.EXPLORAR)
                    }
                },
                onVolver = { nav.popBackStack() },
                vm = clienteVm
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

        composable(Rutas.VER_FIADO) {
            com.kosking.app.cliente.PantallaVerFiado(
                onVolver = { nav.popBackStack() },
                vm = clienteVm
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
            val localesGuardadosVal by clienteVm.localesGuardados.collectAsState()
            PantallaConfiguracion(
                esDueno = esDuenoVal.isNotBlank(),
                esEmpleado = esEmpleadoVal,
                tieneLocalesFiado = localesGuardadosVal.isNotEmpty(),
                onVolver = { nav.popBackStack() }
            )
        }

        composable(Rutas.NOTIFICACIONES) {
            PantallaNotificaciones(onVolver = { nav.popBackStack() })
        }

        composable(Rutas.ACERCA_DE) {
            PantallaAcercaDe(onVolver = { nav.popBackStack() })
        }

        composable(Rutas.HISTORIAL) {
            PantallaHistorial(
                onVolver = { nav.popBackStack() },
                onVerLocal = { localId -> nav.navigate(Rutas.vidreraPublica(localId)) }
            )
        }
    }
}
