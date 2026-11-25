package com.kosking.app.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.feature.kiosco.NavDueno
import com.kosking.app.feature.kiosco.PanelDuenoEmbedido
import com.kosking.app.feature.kiosco.PantallaAgregarCliente
import com.kosking.app.feature.kiosco.PantallaAgregarFiado
import com.kosking.app.feature.kiosco.detalle.PantallaDetalleCliente
import com.kosking.app.feature.kiosco.detalle.PantallaDetalleMovimiento
import com.kosking.app.feature.kiosco.tabs.TabClientes
import com.kosking.app.feature.kiosco.tabs.TabEquipo
import com.kosking.app.feature.kiosco.tabs.TabProductos
import com.kosking.app.scanner.PantallaEscaner
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
    object MiEquipo : VistaExtra()
    object Soporte : VistaExtra()
    object AcercaDe : VistaExtra()
    object Alertas : VistaExtra()
}

// Tabs de la barra inferior — siempre los mismos para todos los rubros
private enum class TabNav(val titulo: String) {
    MI_LOCAL("Local"),
    CLIENTES("Clientes"),
    ESCANER(""),
    HISTORIAL("Cuaderno"),
    MAS("Más")
}

@Composable
fun PantallaHome(
    abrirEscaner: Boolean = false,
    onEscanerConsumido: () -> Unit = {},
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
    onMiLocal: () -> Unit = {},
    onTemaChanged: (String) -> Unit = {},
) {
    var vistaDueno by remember { mutableStateOf<NavDueno>(NavDueno.Panel) }
    var tabNav by rememberSaveable { mutableStateOf(TabNav.MI_LOCAL) }
    var vistaExtra by remember { mutableStateOf<VistaExtra>(VistaExtra.Ninguna) }
    LaunchedEffect(abrirEscaner) {
        if (abrirEscaner) {
            tabNav = TabNav.ESCANER
            onEscanerConsumido()
        }
    }

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
    val cargandoInicial by duenoVm.cargandoInicial.collectAsState()

    var mostrarPerfilSheet by remember { mutableStateOf(false) }

    if (mostrarPerfilSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(onDismissRequest = { mostrarPerfilSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (estaLogueado && nombreUsuario.isNotBlank()) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                nombreUsuario.first().uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(nombreUsuario, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            esDueno -> "Dueño de local"
                            esEmpleado -> "Empleado"
                            else -> "Sin local asignado"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No iniciaste sesión", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                Button(
                    onClick = { mostrarPerfilSheet = false; vistaExtra = VistaExtra.Perfil },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Configurar perfil")
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (!enSubPantalla && esDueno && tabNav != TabNav.ESCANER) {
                NavigationBar {
                    TabNav.entries.forEach { tab ->
                        if (tab == TabNav.ESCANER) {
                            NavigationBarItem(
                                selected = false,
                                onClick = { tabNav = TabNav.ESCANER },
                                icon = {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
                                        }
                                    }
                                },
                                label = { Text("") }
                            )
                        } else {
                            NavigationBarItem(
                                selected = tabNav == tab,
                                onClick = { tabNav = tab },
                                icon = {
                                    Icon(
                                        when (tab) {
                                            TabNav.MI_LOCAL  -> Icons.Default.Store
                                            TabNav.CLIENTES  -> Icons.Default.People
                                            TabNav.HISTORIAL -> Icons.Default.Receipt
                                            TabNav.MAS       -> Icons.Default.Menu
                                            TabNav.ESCANER   -> Icons.Default.QrCodeScanner
                                        },
                                        null
                                    )
                                },
                                label = { Text(tab.titulo) }
                            )
                        }
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
                    is NavDueno.Clientes -> {
                        TabClientes(
                            clientes = clientes,
                            movimientos = movimientos,
                            cargandoInicial = cargandoInicial,
                            onVerCliente = { vistaDueno = NavDueno.DetalleCliente(it) },
                            onAgregarCliente = { vistaDueno = NavDueno.AgregarCliente },
                            vm = duenoVm,
                            onAnotarRapido = { cliente, tipo ->
                                vistaDueno = NavDueno.AgregarFiado(cliente.id, tipo, volverAPanel = false)
                            },
                            onVolver = { vistaDueno = NavDueno.Panel }
                        )
                    }
                    is NavDueno.Productos -> {
                        val productos by duenoVm.productos.collectAsState()
                        val cargando by duenoVm.cargando.collectAsState()
                        TabProductos(
                            productos = productos,
                            cargando = cargando,
                            vm = duenoVm,
                            onBuscarGlobal = { query -> duenoVm.buscarProductosGlobales(query) },
                            onGuardar = { nombre, precio, _, productoId, uri, presentacion, codigoBarras, onExito ->
                                duenoVm.guardarProducto(nombre, precio, productoId, "", uri, codigoBarras = codigoBarras, presentacion = presentacion, onExito = onExito)
                            },
                            onEliminar = { duenoVm.eliminarProducto(it) },
                            onVolver = { vistaDueno = NavDueno.Panel }
                        )
                    }
                }

                // Tab Mi Local
                tabNav == TabNav.MI_LOCAL -> {
                    when {
                        mostrarPanelDueno -> PanelDuenoEmbedido(
                            vm = duenoVm,
                            onVerClientes = { vistaDueno = NavDueno.Clientes },
                            onAbrirAgregarCliente = { vistaDueno = NavDueno.AgregarCliente },
                            onAbrirDetalleCliente = { vistaDueno = NavDueno.DetalleCliente(it) },
                            onAbrirDetalleMovimiento = { vistaDueno = NavDueno.DetalleMovimiento(it) },
                            onAbrirProductos = { vistaDueno = NavDueno.Productos },
                            onAbrirPerfil = { mostrarPerfilSheet = true },
                            onAbrirNotificaciones = { vistaExtra = VistaExtra.Alertas }
                        )
                        esEmpleado -> {
                            // Empleado logueado — su panel se carga via PANEL_EMPLEADO en Navegacion
                            // Este estado no debería ocurrir normalmente, pero por si acaso:
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Text("👋", style = MaterialTheme.typography.displayMedium)
                                    Text(
                                        "Sos empleado",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Tu panel de trabajo está en la pestaña Más.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                        else -> {
                            // Sin local — CTA para registrar negocio
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
                                        "Registrá tu negocio para empezar.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = onSoyDueno, modifier = Modifier.fillMaxWidth()) {
                                        Text("Registrar mi negocio")
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
                }

                // Tab Clientes
                tabNav == TabNav.CLIENTES -> {
                    TabClientes(
                        clientes = clientes,
                        movimientos = movimientos,
                        cargandoInicial = cargandoInicial,
                        onVerCliente = { vistaDueno = NavDueno.DetalleCliente(it) },
                        onAgregarCliente = { vistaDueno = NavDueno.AgregarCliente },
                        vm = duenoVm,
                        onAnotarRapido = { cliente, tipo ->
                            vistaDueno = NavDueno.AgregarFiado(cliente.id, tipo, volverAPanel = false)
                        }
                    )
                }

                // Tab Escáner
                tabNav == TabNav.ESCANER -> {
                    PantallaEscaner(
                        vm = duenoVm,
                        onCerrar = { tabNav = TabNav.MI_LOCAL }
                    )
                }

                // Tab Historial
                tabNav == TabNav.HISTORIAL -> {
                    TabHistorial(
                        vm = duenoVm,
                        clientes = clientes,
                        onVerCliente = { vistaDueno = NavDueno.DetalleCliente(it) }
                    )
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
                    onMiEquipo = { vistaExtra = VistaExtra.MiEquipo },
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
                        esEmpleado -> "Empleado"
                        else -> "Cuenta personal"
                    },
                    esDueno = esDueno,
                    onVolver = { vistaExtra = VistaExtra.Ninguna },
                    onInformacionPersonal = { vistaExtra = VistaExtra.ConfigurarPerfil },
                    onSeguridad = { vistaExtra = VistaExtra.Seguridad },
                    onPrivacidad = { vistaExtra = VistaExtra.Privacidad },
                    onNotificaciones = { vistaExtra = VistaExtra.Notificaciones }
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
                is VistaExtra.Alertas -> PantallaAlertas(
                    clientes = clientes,
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
                is VistaExtra.MiEquipo -> Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Mi Equipo", fontWeight = FontWeight.SemiBold) },
                            navigationIcon = {
                                IconButton(onClick = { vistaExtra = VistaExtra.Ninguna }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                                }
                            }
                        )
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
