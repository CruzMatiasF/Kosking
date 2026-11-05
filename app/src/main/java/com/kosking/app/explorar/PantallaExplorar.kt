package com.kosking.app.explorar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.cliente.ClienteViewModel
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.PanelDuenoEmbedido
import com.kosking.app.dueno.PantallaAgregarCliente
import com.kosking.app.dueno.PantallaAgregarFiado
import com.kosking.app.dueno.detalle.PantallaDetalleCliente
import com.kosking.app.dueno.detalle.PantallaDetalleMovimiento

private enum class TabExplorar { INICIO, TIENDAS, MAPA, MAS }

private sealed class VistaDueno {
    object Panel : VistaDueno()
    object AgregarCliente : VistaDueno()
    data class DetalleCliente(val id: String) : VistaDueno()
    data class AgregarFiado(val clienteId: String, val tipoInicial: String = "cargo", val volverAPanel: Boolean = false) : VistaDueno()
    data class DetalleMovimiento(val id: String, val clienteId: String? = null) : VistaDueno()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaExplorar(
    onVerLocal: (localId: String) -> Unit,
    onConfigurarPerfil: () -> Unit,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onVerMisLocales: () -> Unit,
    onIniciarSesion: () -> Unit,
    onCerrarSesionCuenta: () -> Unit,
    onConfiguracion: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    onAcercaDe: () -> Unit = {},
    onHistorial: () -> Unit = {},
    estaLogueado: Boolean,
    nombreUsuario: String,
    emailUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    clienteVm: ClienteViewModel,
    duenoVm: DuenoViewModel,
    mostrarPanelDueno: Boolean,
    onCerrarPanelDueno: () -> Unit,
    tabInicialDueno: Int = 0,
    vm: ExplorarViewModel = viewModel()
) {
    val locales by vm.locales.collectAsState()
    val todosLocales by vm.todosLocales.collectAsState()
    val busqueda by vm.busqueda.collectAsState()
    val productosFiltrados by vm.productosFiltrados.collectAsState()
    val todosProductos by vm.todosProductos.collectAsState()
    val hayMasProductos by vm.hayMasProductos.collectAsState()
    val cargandoInicio by vm.cargandoInicio.collectAsState()
    val refrescando by vm.refrescando.collectAsState()
    val notificacionesNoLeidas by vm.notificacionesNoLeidas.collectAsState()
    val notificaciones by vm.notificaciones.collectAsState()
    val localesGuardados by clienteVm.localesGuardados.collectAsState()

    var mostrarNotificaciones by remember { mutableStateOf(false) }
    var mostrarDialogConectarCliente by remember { mutableStateOf(false) }

    var tabActual by rememberSaveable { mutableStateOf(TabExplorar.INICIO) }
    val tabHistory = remember { mutableStateListOf<TabExplorar>() }
    var vistaDueno by remember { mutableStateOf<VistaDueno>(VistaDueno.Panel) }
    var tabPanelDueno by remember { mutableIntStateOf(0) }
    var mostrarPerfil by remember { mutableStateOf(false) }
    var panelActivoEnMas by remember { mutableStateOf(false) }

    LaunchedEffect(mostrarPanelDueno) {
        if (mostrarPanelDueno) {
            if (tabActual != TabExplorar.MAS) {
                tabHistory.add(tabActual)
                tabActual = TabExplorar.MAS
            }
            vistaDueno = VistaDueno.Panel
            tabPanelDueno = tabInicialDueno
            panelActivoEnMas = true
        } else {
            panelActivoEnMas = false
        }
    }

    if (mostrarNotificaciones) {
        ModalBottomSheet(onDismissRequest = { mostrarNotificaciones = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notificaciones", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (notificaciones.isNotEmpty()) {
                        TextButton(onClick = { vm.marcarTodasLeidas() }) {
                            Text("Marcar leídas", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (notificaciones.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Notifications, null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("Sin notificaciones",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    notificaciones.forEach { notif ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { vm.marcarLeida(notif.id) }
                                .background(
                                    if (!notif.leida) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(
                                        if (!notif.leida) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.titulo, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (!notif.leida) FontWeight.SemiBold else FontWeight.Normal)
                                if (notif.cuerpo.isNotBlank()) {
                                    Text(notif.cuerpo, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (mostrarPerfil) {
        ModalBottomSheet(onDismissRequest = { mostrarPerfil = false }) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(
                            if (estaLogueado) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (estaLogueado && nombreUsuario.isNotBlank()) {
                        Text(nombreUsuario.first().uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (estaLogueado) {
                    Text(nombreUsuario.ifBlank { "Mi cuenta" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    if (emailUsuario.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(emailUsuario, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Sin cuenta iniciada",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Andá a 'Más' para iniciar sesión",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(24.dp))
                if (estaLogueado) {
                    Button(onClick = { mostrarPerfil = false; onConfigurarPerfil() },
                        modifier = Modifier.fillMaxWidth()) { Text("Configurar perfil") }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(onClick = { mostrarPerfil = false },
                    modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
            }
        }
    }

    BackHandler(enabled = busqueda.isNotBlank() || panelActivoEnMas || tabHistory.isNotEmpty() || tabActual != TabExplorar.INICIO) {
        when {
            busqueda.isNotBlank() -> {
                vm.busqueda.value = ""
            }
            panelActivoEnMas && vistaDueno !is VistaDueno.Panel -> {
                vistaDueno = when (val v = vistaDueno) {
                    is VistaDueno.AgregarFiado -> if (v.volverAPanel) VistaDueno.Panel
                                                  else VistaDueno.DetalleCliente(v.clienteId)
                    else -> VistaDueno.Panel
                }
            }
            panelActivoEnMas -> {
                panelActivoEnMas = false
                onCerrarPanelDueno()
            }
            tabHistory.isNotEmpty() -> {
                tabActual = tabHistory.removeAt(tabHistory.lastIndex)
            }
            tabActual != TabExplorar.INICIO -> {
                tabActual = TabExplorar.INICIO
            }
        }
    }

    Scaffold(
        topBar = {
            when (tabActual) {
                TabExplorar.INICIO -> TopAppBar(
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("K", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    title = {
                        val interactionSource = remember { MutableInteractionSource() }
                        BasicTextField(
                            value = busqueda,
                            onValueChange = { vm.busqueda.value = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            decorationBox = { innerTextField ->
                                OutlinedTextFieldDefaults.DecorationBox(
                                    value = busqueda,
                                    innerTextField = innerTextField,
                                    enabled = true,
                                    singleLine = true,
                                    visualTransformation = VisualTransformation.None,
                                    interactionSource = interactionSource,
                                    placeholder = {
                                        Text("Buscar productos o kioscos...",
                                            style = MaterialTheme.typography.bodySmall)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, null,
                                            modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = if (busqueda.isNotBlank()) {
                                        {
                                            IconButton(onClick = { vm.busqueda.value = "" }) {
                                                Icon(Icons.Default.Close, null,
                                                    modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    } else null,
                                    contentPadding = OutlinedTextFieldDefaults.contentPadding(
                                        top = 0.dp, bottom = 0.dp, start = 4.dp, end = 4.dp
                                    ),
                                    container = {
                                        OutlinedTextFieldDefaults.Container(
                                            enabled = true,
                                            isError = false,
                                            interactionSource = interactionSource,
                                            shape = RoundedCornerShape(24.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    }
                                )
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            mostrarNotificaciones = true
                            vm.marcarTodasLeidas()
                        }) {
                            BadgedBox(badge = {
                                if (notificacionesNoLeidas > 0) {
                                    Badge {
                                        Text(
                                            if (notificacionesNoLeidas > 9) "9+" else "$notificacionesNoLeidas"
                                        )
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Notifications, "Notificaciones")
                            }
                        }
                        IconButton(onClick = { mostrarPerfil = true }) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .background(
                                        if (estaLogueado) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (estaLogueado && nombreUsuario.isNotBlank()) {
                                    Text(
                                        nombreUsuario.first().uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Text("?", style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                )
                TabExplorar.TIENDAS -> TopAppBar(title = { Text("Tiendas") })
                TabExplorar.MAPA -> TopAppBar(title = { Text("Mapa") })
                TabExplorar.MAS -> if (panelActivoEnMas) {
                    val vistaActual = vistaDueno
                    when {
                        vistaActual !is VistaDueno.Panel && tabPanelDueno != 0 -> {
                            // Pantallas con su propio TopAppBar
                        }
                        tabPanelDueno != 0 -> {
                            TopAppBar(
                                title = {
                                    Text(when (tabPanelDueno) {
                                        1 -> "Clientes"; 2 -> "Productos"; 3 -> "Equipo"; else -> "Mi local"
                                    })
                                },
                                navigationIcon = {
                                    IconButton(onClick = { tabPanelDueno = 0 }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                                    }
                                }
                            )
                        }
                        else -> {
                            TopAppBar(
                                title = { Text("Panel del dueño") }
                            )
                        }
                    }
                } else {
                    TopAppBar(title = { Text("Más") })
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tabActual == TabExplorar.INICIO,
                    onClick = {
                        if (tabActual == TabExplorar.INICIO) {
                            vm.busqueda.value = ""
                        } else {
                            tabHistory.add(tabActual)
                            tabActual = TabExplorar.INICIO
                        }
                    },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = tabActual == TabExplorar.TIENDAS,
                    onClick = {
                        if (tabActual != TabExplorar.TIENDAS) {
                            tabHistory.add(tabActual)
                            tabActual = TabExplorar.TIENDAS
                        }
                    },
                    icon = { Icon(Icons.Default.Store, null) },
                    label = { Text("Tiendas") }
                )
                NavigationBarItem(
                    selected = tabActual == TabExplorar.MAPA,
                    onClick = {
                        if (tabActual != TabExplorar.MAPA) {
                            tabHistory.add(tabActual)
                            tabActual = TabExplorar.MAPA
                        }
                    },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Mapa") }
                )
                NavigationBarItem(
                    selected = tabActual == TabExplorar.MAS,
                    onClick = {
                        when {
                            tabActual == TabExplorar.MAS && panelActivoEnMas -> {
                                when (vistaDueno) {
                                    is VistaDueno.Panel -> onCerrarPanelDueno()
                                    is VistaDueno.AgregarFiado -> vistaDueno = VistaDueno.DetalleCliente((vistaDueno as VistaDueno.AgregarFiado).clienteId)
                                    else -> vistaDueno = VistaDueno.Panel
                                }
                            }
                            tabActual == TabExplorar.MAS -> { /* ya en Más */ }
                            else -> {
                                tabHistory.add(tabActual)
                                tabActual = TabExplorar.MAS
                                panelActivoEnMas = false
                                onCerrarPanelDueno()
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Más") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tabActual) {
                TabExplorar.INICIO -> TabInicio(
                    locales = locales,
                    todosLocales = todosLocales,
                    productosFiltrados = productosFiltrados,
                    busqueda = busqueda,
                    localesGuardados = localesGuardados.isNotEmpty(),
                    hayMasProductos = hayMasProductos,
                    cargando = cargandoInicio,
                    onVerLocal = onVerLocal,
                    onVerMisLocales = onVerMisLocales,
                    onCargarMas = { vm.cargarMasProductos() },
                    isRefrescando = refrescando,
                    onRefrescar = { vm.refrescar() }
                )
                TabExplorar.TIENDAS -> TabTiendas(
                    todosLocales = todosLocales,
                    todosProductos = todosProductos,
                    onVerLocal = onVerLocal,
                    isRefrescando = refrescando,
                    onRefrescar = { vm.refrescar() }
                )
                TabExplorar.MAPA -> TabMapa(locales = todosLocales, onVerLocal = onVerLocal)
                TabExplorar.MAS -> if (panelActivoEnMas) {
                    when (val vista = vistaDueno) {
                        is VistaDueno.Panel -> PanelDuenoEmbedido(
                            vm = duenoVm,
                            tabSeleccionado = tabPanelDueno,
                            onTabChange = { tabPanelDueno = it },
                            onAbrirAgregarCliente = { vistaDueno = VistaDueno.AgregarCliente },
                            onAbrirDetalleCliente = { vistaDueno = VistaDueno.DetalleCliente(it) },
                            onAbrirAgregarFiado = { clienteId, tipo -> vistaDueno = VistaDueno.AgregarFiado(clienteId, tipo, volverAPanel = tabPanelDueno != 0) },
                            onAbrirDetalleMovimiento = { vistaDueno = VistaDueno.DetalleMovimiento(it) }
                        )
                        is VistaDueno.AgregarCliente -> PantallaAgregarCliente(
                            onVolver = { vistaDueno = VistaDueno.Panel },
                            vm = duenoVm
                        )
                        is VistaDueno.DetalleCliente -> PantallaDetalleCliente(
                            clienteId = vista.id,
                            onVolver = { vistaDueno = VistaDueno.Panel },
                            onVerMovimiento = { movId ->
                                vistaDueno = VistaDueno.DetalleMovimiento(movId, clienteId = vista.id)
                            },
                            onAnotarFiado = { clienteId -> vistaDueno = VistaDueno.AgregarFiado(clienteId, "cargo") },
                            onCobrar = { clienteId -> vistaDueno = VistaDueno.AgregarFiado(clienteId, "pago") },
                            vm = duenoVm
                        )
                        is VistaDueno.AgregarFiado -> PantallaAgregarFiado(
                            clienteId = vista.clienteId,
                            onGuardado = {
                                vistaDueno = if (vista.volverAPanel) VistaDueno.Panel
                                             else VistaDueno.DetalleCliente(vista.clienteId)
                            },
                            onVolver = {
                                vistaDueno = if (vista.volverAPanel) VistaDueno.Panel
                                             else VistaDueno.DetalleCliente(vista.clienteId)
                            },
                            vm = duenoVm,
                            tipoInicial = vista.tipoInicial
                        )
                        is VistaDueno.DetalleMovimiento -> PantallaDetalleMovimiento(
                            movimientoId = vista.id,
                            onVolver = {
                                vistaDueno = vista.clienteId?.let { VistaDueno.DetalleCliente(it) }
                                    ?: VistaDueno.Panel
                            },
                            vm = duenoVm
                        )
                    }
                } else {
                    TabMas(
                        estaLogueado = estaLogueado,
                        nombreUsuario = nombreUsuario,
                        esDueno = esDueno,
                        esEmpleado = esEmpleado,
                        tieneLocalesFiado = localesGuardados.isNotEmpty(),
                        onIniciarSesion = onIniciarSesion,
                        onCerrarSesionCuenta = onCerrarSesionCuenta,
                        onSoyDueno = onSoyDueno,
                        onSoyEmpleado = onSoyEmpleado,
                        onVerMiFiado = onVerMisLocales,
                        onConfigurarPerfil = onConfigurarPerfil,
                        onConectarmeComoCliente = { mostrarDialogConectarCliente = true },
                        onConfiguracion = onConfiguracion,
                        onNotificaciones = onNotificaciones,
                        onAcercaDe = onAcercaDe,
                        onHistorial = onHistorial
                    )
                }
            }
        }
    }

    if (mostrarDialogConectarCliente) {
        DialogConectarComoCliente(
            clienteVm = clienteVm,
            onExito = { mostrarDialogConectarCliente = false },
            onDismiss = { mostrarDialogConectarCliente = false }
        )
    }
}
