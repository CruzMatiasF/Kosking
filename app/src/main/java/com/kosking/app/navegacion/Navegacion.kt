package com.kosking.app.navegacion

import androidx.activity.compose.BackHandler
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
import com.kosking.app.feature.kiosco.detalle.PantallaDetalleMovimiento
import com.kosking.app.empleado.EmpleadoViewModel
import com.kosking.app.empleado.PantallaIngresarCodigo
import com.kosking.app.home.PantallaHome
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.datos.StartupUseCase
import com.kosking.app.datos.StartupDestino
import com.kosking.app.feature.kiosco.PanelDuenoEmbedido
import com.kosking.app.feature.kiosco.detalle.PantallaDetalleCliente
import com.kosking.app.autenticacion.PantallaOnboarding
import com.kosking.app.autenticacion.PantallaElegirRubro
import com.kosking.app.autenticacion.PantallaDescargandoRubro
import com.kosking.app.feature.kiosco.tabs.TabClientes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.datos.PreferenciasRepository
import com.kosking.app.scanner.PantallaEscaner
import com.kosking.app.scanner.TipoMovimiento

private sealed class DestinoPostLogin {
    object CrearLocal : DestinoPostLogin()
    object IngresarCodigo : DestinoPostLogin()
}

@Composable
fun KoskingNavHost(
    onTemaChanged: (String) -> Unit = {},
    abrirEscaner: Boolean = false,
    onEscanerConsumido: () -> Unit = {}
) {
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
            val ctx = LocalContext.current
            var startupResuelto by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (startupResuelto) return@LaunchedEffect
                val destino = StartupUseCase.resolver(
                    context = ctx,
                    empleadoTieneSession = empleadoVm.tieneSession.value,
                    duenoLocalIdFlow = duenoVm.localId,
                    duenoCargandoInicialFlow = duenoVm.cargandoInicial
                )
                when (destino) {
                    StartupDestino.PanelEmpleado ->
                        nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
                    StartupDestino.PanelDueno -> mostrarPanelDueno = true
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
                abrirEscaner = abrirEscaner,
                onEscanerConsumido = onEscanerConsumido,
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
                        esDueno.isNotBlank() -> mostrarPanelDueno = true
                        else -> nav.navigate(Rutas.ELEGIR_RUBRO)
                    }
                },
                onSoyEmpleado = {
                    when {
                        !estaLogueado -> {
                            destinoPostLogin = DestinoPostLogin.IngresarCodigo
                            nav.navigate(Rutas.LOGIN)
                        }
                        esEmpleado -> nav.navigate(Rutas.PANEL_EMPLEADO) { popUpTo(Rutas.EXPLORAR) }
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
                onMiLocal = { nav.navigate(Rutas.CONFIGURAR_LOCAL) },
            )
        }

        composable(Rutas.LOGIN) {
            PantallaLogin(
                onLoginExitoso = {
                    nav.popBackStack()
                    val destino = destinoPostLogin
                    destinoPostLogin = null
                    when (destino) {
                        DestinoPostLogin.CrearLocal -> nav.navigate(Rutas.ELEGIR_RUBRO)
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
                        DestinoPostLogin.CrearLocal -> nav.navigate(Rutas.ELEGIR_RUBRO)
                        DestinoPostLogin.IngresarCodigo -> nav.navigate(Rutas.INGRESAR_CODIGO)
                        null -> {}
                    }
                },
                onVolver = { nav.popBackStack() }
            )
        }

        composable(Rutas.ELEGIR_RUBRO) {
            PantallaElegirRubro(
                onRubroElegido = { tipo ->
                    nav.navigate(Rutas.descargandoRubro(tipo))
                }
            )
        }

        composable(
            Rutas.DESCARGANDO_RUBRO,
            arguments = listOf(navArgument("tipo") { type = NavType.StringType })
        ) { back ->
            val tipo = back.arguments?.getString("tipo") ?: "Kiosco"
            PantallaDescargandoRubro(
                tipo = tipo,
                onListo = {
                    nav.navigate(Rutas.CREAR_LOCAL) {
                        popUpTo(Rutas.ELEGIR_RUBRO) { inclusive = true }
                    }
                }
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

            var tabEmpleado by rememberSaveable { mutableStateOf(0) } // 0=panel, 1=escaner, 2=clientes
            var mostrarProductosEmpleado by rememberSaveable { mutableStateOf(false) }
            var escanerModoInicialEmpleado by remember { mutableStateOf(TipoMovimiento.VENTA) }

            BackHandler(enabled = tabEmpleado != 0) {
                tabEmpleado = 0
                escanerModoInicialEmpleado = TipoMovimiento.VENTA
            }

            when (tabEmpleado) {
                1 -> Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = false,
                                onClick = { tabEmpleado = 0 },
                                icon = { Icon(Icons.Default.Store, null) },
                                label = { Text("Panel") }
                            )
                            NavigationBarItem(
                                selected = true,
                                onClick = {},
                                icon = {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                },
                                label = { Text("Escáner") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = { tabEmpleado = 2 },
                                icon = { Icon(Icons.Default.People, null) },
                                label = { Text("Clientes") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        PantallaEscaner(
                            vm = duenoVm,
                            onCerrar = { tabEmpleado = 0; escanerModoInicialEmpleado = TipoMovimiento.VENTA },
                            esEmpleado = true,
                            modoInicial = escanerModoInicialEmpleado
                        )
                    }
                }
                2 -> {
                    val clientes by duenoVm.clientes.collectAsState()
                    val movimientosEmpleado by duenoVm.movimientosLocal.collectAsState()
                    val cargandoInicialEmpleado by duenoVm.cargandoInicial.collectAsState()
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { tabEmpleado = 0 },
                                    icon = { Icon(Icons.Default.Store, null) },
                                    label = { Text("Panel") }
                                )
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { tabEmpleado = 1 },
                                    icon = { Icon(Icons.Default.QrCodeScanner, null) },
                                    label = { Text("Escáner") }
                                )
                                NavigationBarItem(
                                    selected = true,
                                    onClick = {},
                                    icon = { Icon(Icons.Default.People, null) },
                                    label = { Text("Clientes") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(Modifier.padding(innerPadding)) {
                            com.kosking.app.feature.kiosco.tabs.TabClientes(
                                clientes = clientes,
                                movimientos = movimientosEmpleado,
                                cargandoInicial = cargandoInicialEmpleado,
                                onVerCliente = { id -> nav.navigate(Rutas.detalleClienteEmpleado(id)) },
                                onAgregarCliente = {},
                                esEmpleado = true,
                                onVolver = { tabEmpleado = 0 }
                            )
                        }
                    }
                }
                else -> if (mostrarProductosEmpleado) {
                    val productosEmpleado by duenoVm.productos.collectAsState()
                    val cargandoEmpleado by duenoVm.cargando.collectAsState()
                    com.kosking.app.feature.kiosco.tabs.TabProductos(
                        productos = productosEmpleado,
                        cargando = cargandoEmpleado,
                        esEmpleado = true,
                        onGuardar = { _, _, _, _, _, _, _, _, _ -> },
                        onEliminar = {},
                        onVolver = { mostrarProductosEmpleado = false }
                    )
                } else PanelDuenoEmbedido(
                    vm = duenoVm,
                    esEmpleado = true,
                    puedeVerDeuda = puedeVerDeuda,
                    onCerrarSesionEmpleado = {
                        empleadoVm.cerrarSesion()
                        duenoVm.cerrarSesion()
                        nav.navigate(Rutas.EXPLORAR) { popUpTo(0) { inclusive = true } }
                    },
                    onVerClientes = { tabEmpleado = 2 },
                    onAbrirDetalleCliente = { id -> nav.navigate(Rutas.detalleClienteEmpleado(id)) },
                    onAbrirDetalleMovimiento = { id -> nav.navigate(Rutas.detalleMovimiento(id)) },
                    onAbrirProductos = { mostrarProductosEmpleado = true },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = true,
                                onClick = {},
                                icon = { Icon(Icons.Default.Store, null) },
                                label = { Text("Panel") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = { tabEmpleado = 1 },
                                icon = {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                },
                                label = { Text("Escáner") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = { tabEmpleado = 2 },
                                icon = { Icon(Icons.Default.People, null) },
                                label = { Text("Clientes") }
                            )
                        }
                    }
                )
            }
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

    }
}
