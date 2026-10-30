package com.kosking.app.explorar

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.kosking.app.cliente.ClienteViewModel
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.PanelDuenoEmbedido
import com.kosking.app.dueno.PantallaAgregarCliente
import com.kosking.app.dueno.PantallaAgregarFiado
import com.kosking.app.dueno.PantallaDetalleCliente
import com.kosking.app.dueno.PantallaDetalleMovimiento
import com.kosking.app.dueno.PantallaSeleccionarCliente
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Producto
import com.kosking.app.explorar.estaAbiertoAhora
import com.kosking.app.explorar.textoHorarioHoy

private val tiposLocales = listOf(
    "Todos", "Kiosco", "Almacén", "Maxikiosco", "Ropa", "Corralón", "Farmacia", "Verdulería"
)

private enum class TabExplorar { INICIO, FAVORITOS, MAPA, OFERTAS, MAS }

private sealed class VistaDueno {
    object Panel : VistaDueno()
    object AgregarCliente : VistaDueno()
    data class DetalleCliente(val id: String) : VistaDueno()
    data class AgregarFiado(val clienteId: String, val tipoInicial: String = "cargo", val desdeResumen: Boolean = false) : VistaDueno()
    data class SeleccionarCliente(val tipo: String) : VistaDueno()
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
    val productosOferta by vm.productosOferta.collectAsState()
    val productosFiltrados by vm.productosFiltrados.collectAsState()
    val hayMasProductos by vm.hayMasProductos.collectAsState()
    val cargandoInicio by vm.cargandoInicio.collectAsState()
    val notificacionesNoLeidas by vm.notificacionesNoLeidas.collectAsState()
    val notificaciones by vm.notificaciones.collectAsState()
    val localesGuardados by clienteVm.localesGuardados.collectAsState()
    val nombreLocal by duenoVm.nombreLocal.collectAsState()

    var mostrarNotificaciones by remember { mutableStateOf(false) }
    var mostrarDialogConectarCliente by remember { mutableStateOf(false) }

    var tabActual by rememberSaveable { mutableStateOf(TabExplorar.INICIO) }
    val tabHistory = remember { mutableStateListOf<TabExplorar>() }
    var vistaDueno by remember { mutableStateOf<VistaDueno>(VistaDueno.Panel) }
    var tabPanelDueno by remember { mutableStateOf(0) }
    var mostrarPerfil by remember { mutableStateOf(false) }
    // Controla si el panel se muestra dentro del tab Más.
    // Se activa solo cuando mostrarPanelDueno se vuelve true (navegación explícita).
    // Al presionar "Más" desde otra tab se resetea, mostrando TabMas normal.
    var panelActivoEnMas by remember { mutableStateOf(false) }

    // Al activarse el panel, ir al tab Más, resetear vista y activar panel
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

    // Bottom sheet notificaciones
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

    // Bottom sheet info de cuenta
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

    // Back físico: limpia buscador → sub-vista → panel → tab anterior → inicio → cierra app
    BackHandler(enabled = busqueda.isNotBlank() || panelActivoEnMas || tabHistory.isNotEmpty() || tabActual != TabExplorar.INICIO) {
        when {
            busqueda.isNotBlank() -> {
                vm.busqueda.value = ""
            }
            panelActivoEnMas && vistaDueno !is VistaDueno.Panel -> {
                vistaDueno = when (val v = vistaDueno) {
                    is VistaDueno.AgregarFiado -> if (v.desdeResumen) VistaDueno.SeleccionarCliente(v.tipoInicial)
                                                  else VistaDueno.DetalleCliente(v.clienteId)
                    is VistaDueno.SeleccionarCliente -> VistaDueno.Panel
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
                        // Espacio para logo futuro
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
                        // Campana de notificaciones con badge
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
                        // Ícono de perfil
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
                TabExplorar.FAVORITOS -> TopAppBar(title = { Text("Favoritos") })
                TabExplorar.MAPA -> TopAppBar(title = { Text("Mapa") })
                TabExplorar.OFERTAS -> TopAppBar(title = { Text("Ofertas") })
                TabExplorar.MAS -> if (panelActivoEnMas) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(nombreLocal.ifBlank { "Mi local" })
                                Text("Panel del dueño", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
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
                            // Ya en inicio: limpiar buscador si hay texto
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
                    selected = tabActual == TabExplorar.FAVORITOS,
                    onClick = {
                        if (tabActual != TabExplorar.FAVORITOS) {
                            tabHistory.add(tabActual)
                            tabActual = TabExplorar.FAVORITOS
                        }
                    },
                    icon = { Icon(Icons.Default.Favorite, null) },
                    label = { Text("Favoritos") }
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
                    selected = tabActual == TabExplorar.OFERTAS,
                    onClick = {
                        if (tabActual != TabExplorar.OFERTAS) {
                            tabHistory.add(tabActual)
                            tabActual = TabExplorar.OFERTAS
                        }
                    },
                    icon = { Icon(Icons.Default.LocalOffer, null) },
                    label = { Text("Ofertas") }
                )
                NavigationBarItem(
                    selected = tabActual == TabExplorar.MAS,
                    onClick = {
                        when {
                            tabActual == TabExplorar.MAS && panelActivoEnMas -> {
                                // Ya estoy en Más con el panel activo: navegar hacia atrás en el panel
                                when (vistaDueno) {
                                    is VistaDueno.Panel -> onCerrarPanelDueno()
                                    is VistaDueno.AgregarFiado -> vistaDueno = VistaDueno.DetalleCliente((vistaDueno as VistaDueno.AgregarFiado).clienteId)
                                    else -> vistaDueno = VistaDueno.Panel
                                }
                            }
                            tabActual == TabExplorar.MAS -> { /* ya estoy en Más, no hacer nada */ }
                            else -> {
                                // Vengo de otra tab: ir a Más sin mostrar el panel.
                                // Reseteamos también el estado global para que al volver a
                                // "Administrar mi local" el LaunchedEffect se dispare de nuevo.
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
                    productosDestacados = productosOferta,
                    busqueda = busqueda,
                    localesGuardados = localesGuardados.isNotEmpty(),
                    hayMasProductos = hayMasProductos,
                    cargando = cargandoInicio,
                    onVerLocal = onVerLocal,
                    onVerMisLocales = onVerMisLocales,
                    onVerOfertas = { tabActual = TabExplorar.OFERTAS },
                    onCargarMas = { vm.cargarMasProductos() }
                )
                TabExplorar.FAVORITOS -> TabFavoritos(
                    localesGuardados = localesGuardados.map { lg ->
                        todosLocales.find { it.id == lg.localId }
                    }.filterNotNull(),
                    onVerLocal = onVerLocal,
                    onVerMisFiados = onVerMisLocales
                )
                TabExplorar.MAPA -> TabMapa(locales = todosLocales, onVerLocal = onVerLocal)
                TabExplorar.OFERTAS -> TabOfertas(
                    productos = productosOferta,
                    locales = todosLocales,
                    onVerLocal = onVerLocal
                )
                TabExplorar.MAS -> if (panelActivoEnMas) {
                    when (val vista = vistaDueno) {
                        is VistaDueno.Panel -> PanelDuenoEmbedido(
                            vm = duenoVm,
                            tabSeleccionado = tabPanelDueno,
                            onTabChange = { tabPanelDueno = it },
                            onAbrirAgregarCliente = { vistaDueno = VistaDueno.AgregarCliente },
                            onAbrirDetalleCliente = { vistaDueno = VistaDueno.DetalleCliente(it) },
                            onAbrirAgregarFiado = { clienteId, tipo -> vistaDueno = VistaDueno.AgregarFiado(clienteId, tipo) },
                            onAbrirDetalleMovimiento = { vistaDueno = VistaDueno.DetalleMovimiento(it) },
                            onAbrirSeleccionarCliente = { tipo -> vistaDueno = VistaDueno.SeleccionarCliente(tipo) }
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
                            vm = duenoVm
                        )
                        is VistaDueno.AgregarFiado -> PantallaAgregarFiado(
                            clienteId = vista.clienteId,
                            onGuardado = {
                                vistaDueno = if (vista.desdeResumen) VistaDueno.Panel
                                             else VistaDueno.DetalleCliente(vista.clienteId)
                            },
                            onVolver = {
                                vistaDueno = if (vista.desdeResumen) VistaDueno.SeleccionarCliente(vista.tipoInicial)
                                             else VistaDueno.DetalleCliente(vista.clienteId)
                            },
                            vm = duenoVm,
                            tipoInicial = vista.tipoInicial
                        )
                        is VistaDueno.SeleccionarCliente -> PantallaSeleccionarCliente(
                            tipo = vista.tipo,
                            vm = duenoVm,
                            onSeleccionado = { clienteId ->
                                vistaDueno = VistaDueno.AgregarFiado(clienteId, vista.tipo, desdeResumen = true)
                            },
                            onVolver = { vistaDueno = VistaDueno.Panel }
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
            onExito = {
                mostrarDialogConectarCliente = false
            },
            onDismiss = { mostrarDialogConectarCliente = false }
        )
    }
}

@Composable
fun DialogConectarComoCliente(
    clienteVm: ClienteViewModel,
    onExito: () -> Unit,
    onDismiss: () -> Unit
) {
    val cargando by clienteVm.cargando.collectAsState()
    val error by clienteVm.error.collectAsState()
    var codigo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { clienteVm.error.value = null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conectar con un local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "El dueño te tiene que dar tu código personal. Lo encontrás en tu perfil dentro del local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.uppercase().take(8) },
                    label = { Text("Código de acceso") },
                    placeholder = { Text("KSK-XXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { clienteVm.verificarPorCodigoAcceso(codigo.trim()) { onExito() } },
                enabled = codigo.length >= 6 && !cargando
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Conectar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Tab Inicio ────────────────────────────────────────────────────────────────

@Composable
private fun TabInicio(
    locales: List<Local>,
    todosLocales: List<Local>,
    productosFiltrados: List<Producto>,
    productosDestacados: List<Producto>,
    busqueda: String,
    localesGuardados: Boolean,
    hayMasProductos: Boolean = false,
    cargando: Boolean = false,
    onVerLocal: (String) -> Unit,
    onVerMisLocales: () -> Unit,
    onVerOfertas: () -> Unit = {},
    onCargarMas: () -> Unit = {}
) {
    var tipoFiltro by remember { mutableStateOf("Todos") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Detectar cuando el usuario llega cerca del final → cargar más productos
    LaunchedEffect(listState) {
        var ultimoEstado = false
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 4 && info.totalItemsCount > 0
        }.collect { cercaDelFinal ->
            if (cercaDelFinal && !ultimoEstado) onCargarMas()
            ultimoEstado = cercaDelFinal
        }
    }

    // Al empezar a buscar, resetear el filtro de tipo
    LaunchedEffect(busqueda) {
        if (busqueda.isNotBlank()) tipoFiltro = "Todos"
    }

    val localMap = remember(todosLocales) { todosLocales.associateBy { it.id } }

    val localesFiltrados = remember(locales, tipoFiltro) {
        if (tipoFiltro == "Todos") locales else locales.filter { it.tipo == tipoFiltro }
    }

    val productosPorTipo = remember(productosFiltrados, tipoFiltro, todosLocales) {
        val base = if (busqueda.isBlank()) productosFiltrados else productosFiltrados
        if (tipoFiltro == "Todos") base
        else base.filter { prod -> localMap[prod.localId]?.tipo == tipoFiltro }
    }

    if (cargando) {
        SkeletonInicio()
        return
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

        // Banner mis locales
        if (localesGuardados) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    onClick = onVerMisLocales
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ver mi fiado", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text("Tocá para ver lo que debés", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }

        // Categorías — solo visibles en modo normal (no durante búsqueda)
        if (busqueda.isBlank()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tiposLocales) { tipo ->
                        val sel = tipo == tipoFiltro
                        val icono = if (tipo == "Todos") Icons.Default.Apps else iconParaTipo(tipo)
                        FilterChip(
                            selected = sel,
                            onClick = { tipoFiltro = tipo },
                            label = { Text(tipo) },
                            leadingIcon = {
                                Icon(icono, null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }

        if (busqueda.isNotBlank()) {
            // ── Modo búsqueda ──────────────────────────────────────────────

            if (locales.isNotEmpty()) {
                item {
                    SeccionLabel(
                        texto = "Locales",
                        conteo = "${locales.size} encontrado${if (locales.size != 1) "s" else ""}"
                    )
                }
                items(locales, key = { it.id }) { local ->
                    FilaLocalPlana(local = local, onClick = { onVerLocal(local.id) })
                }
            }

            if (productosFiltrados.isNotEmpty()) {
                item {
                    SeccionLabel(
                        texto = "Productos",
                        conteo = "${productosFiltrados.size} resultado${if (productosFiltrados.size != 1) "s" else ""}"
                    )
                }
                val paresBusqueda = productosFiltrados.chunked(2)
                items(paresBusqueda) { par ->
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        par.forEach { prod ->
                            TarjetaProducto(
                                producto = prod,
                                nombreLocal = localMap[prod.localId]?.nombre ?: "",
                                onClick = { localMap[prod.localId]?.id?.let { onVerLocal(it) } },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        if (par.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (productosFiltrados.isEmpty() && locales.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("Sin resultados para \"$busqueda\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

        } else {
            // ── Modo normal: ofertas → locales → productos ─────────────────

            // Banner de ofertas
            if (productosDestacados.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface),
                        onClick = onVerOfertas
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Ofertas cerca tuyo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.surface
                                )
                                Text(
                                    "Descuentos en kioscos de tu barrio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        "Ver ${productosDestacados.size} oferta${if (productosDestacados.size != 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.LocalOffer, null,
                                modifier = Modifier.size(48.dp).padding(start = 8.dp),
                                tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // Locales en scroll horizontal
            if (localesFiltrados.isNotEmpty()) {
                item {
                    SeccionLabel(
                        texto = "Locales",
                        conteo = "${localesFiltrados.size} registrado${if (localesFiltrados.size != 1) "s" else ""}"
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(localesFiltrados, key = { it.id }) { local ->
                            TarjetaLocalH(local = local, onClick = { onVerLocal(local.id) })
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Productos en grilla 2 columnas
            if (productosPorTipo.isNotEmpty()) {
                item {
                    SeccionLabel(
                        texto = "Productos",
                        conteo = "${productosPorTipo.size} publicado${if (productosPorTipo.size != 1) "s" else ""}"
                    )
                }
                val paresProductos = productosPorTipo.chunked(2)
                items(paresProductos) { par ->
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        par.forEach { prod ->
                            TarjetaProducto(
                                producto = prod,
                                nombreLocal = localMap[prod.localId]?.nombre ?: "",
                                onClick = { localMap[prod.localId]?.id?.let { onVerLocal(it) } },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        if (par.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            // Indicador de "cargar más" al final de productos
            if (hayMasProductos && productosPorTipo.isNotEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }

            if (localesFiltrados.isEmpty() && productosPorTipo.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.StoreMallDirectory, null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(12.dp))
                            Text("No hay publicaciones todavía",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ── Skeleton de carga ─────────────────────────────────────────────────────────

@Composable
private fun SkeletonInicio() {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    @Composable
    fun SkBox(modifier: Modifier) {
        Box(modifier.clip(RoundedCornerShape(8.dp)).background(skeletonColor))
    }

    Column(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
        // Chips skeleton
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { SkBox(Modifier.width(72.dp).height(32.dp)) }
        }
        // Locales skeleton header
        SkBox(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp).width(80.dp).height(12.dp))
        // Locales row skeleton
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { SkBox(Modifier.width(130.dp).height(130.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        // Productos skeleton header
        SkBox(Modifier.padding(start = 16.dp, bottom = 8.dp).width(100.dp).height(12.dp))
        // Product grid skeleton
        repeat(2) {
            Row(modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) {
                    SkBox(Modifier.weight(1f).height(160.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// Label de sección estilo ALL CAPS + contador
@Composable
private fun SeccionLabel(texto: String, conteo: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            texto.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (conteo.isNotBlank()) {
            Text(conteo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Card para ofertas: grilla 2 columnas con badge −%
@Composable
private fun CardOferta(
    producto: Producto,
    nombreLocal: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val descuento = if (producto.precioAnterior > 0)
        (((producto.precioAnterior - producto.precio) / producto.precioAnterior) * 100).toInt()
    else 0

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                if (producto.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = producto.imagenUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingBag, null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (descuento > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart)
                            .padding(7.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("−$descuento%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                if (nombreLocal.isNotBlank()) {
                    Text(nombreLocal,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(producto.nombre,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$${"%.0f".format(producto.precio)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold)
                    if (producto.precioAnterior > 0) {
                        Text("$${"%.0f".format(producto.precioAnterior)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough)
                    }
                }
            }
        }
    }
}

// Tarjeta de producto para grilla 2 columnas
@Composable
private fun TarjetaProducto(
    producto: Producto,
    nombreLocal: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (producto.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = producto.imagenUrl,
                        contentDescription = producto.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                } else {
                    Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (producto.esOferta && producto.precioAnterior > 0) {
                    val descuento = (((producto.precioAnterior - producto.precio) / producto.precioAnterior) * 100).toInt()
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("−$descuento%", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                if (nombreLocal.isNotBlank()) {
                    Text(nombreLocal, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(producto.nombre, style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$${"%.0f".format(producto.precio)}",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
                    if (producto.esOferta && producto.precioAnterior > 0) {
                        Text("$${"%.0f".format(producto.precioAnterior)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough)
                    }
                }
            }
        }
    }
}


// Tarjeta compacta para scroll horizontal de locales
@Composable
private fun TarjetaLocalH(local: Local, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(130.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (local.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = local.imagenUrl,
                        contentDescription = local.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(iconParaTipo(local.tipo), null, modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(local.nombre, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(local.tipo, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .then(if (local.abierto) Modifier.background(MaterialTheme.colorScheme.onSurface)
                          else Modifier.background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(if (local.abierto) "Abierto" else "Cerrado",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (local.abierto) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Fila plana para locales (con separador)
@Composable
private fun FilaLocalPlana(local: Local, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconParaTipo(local.tipo), null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(local.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(local.tipo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (local.abierto)
                            Modifier.background(MaterialTheme.colorScheme.onSurface)
                        else
                            Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    if (local.abierto) "Abierto" else "Cerrado",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (local.abierto) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
    }
}

// ── Tab Favoritos ─────────────────────────────────────────────────────────────

@Composable
private fun TabFavoritos(
    localesGuardados: List<Local>,
    onVerLocal: (String) -> Unit,
    onVerMisFiados: () -> Unit
) {
    if (localesGuardados.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FavoriteBorder, null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant)
                Text("Todavía no tenés locales guardados",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Buscá un local y pedile tu PIN al dueño",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = onVerMisFiados
            ) {
                Row(modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Ver todos mis fiados",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("${localesGuardados.size} local${if (localesGuardados.size != 1) "es" else ""} guardado${if (localesGuardados.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        item {
            Text("Mis locales", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }

        items(localesGuardados, key = { it.id }) { local ->
            Card(modifier = Modifier.fillMaxWidth(),
                onClick = { onVerLocal(local.id) }) {
                ListItem(
                    headlineContent = { Text(local.nombre, fontWeight = FontWeight.Medium) },
                    supportingContent = {
                        if (local.direccion.isNotBlank()) Text(local.direccion)
                    },
                    leadingContent = {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center) {
                            Icon(iconParaTipo(local.tipo), null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                        }
                    },
                    trailingContent = {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (local.abierto) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(if (local.abierto) "Abierto" else "Cerrado",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (local.abierto) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        }
    }
}

// ── Tab Mapa ──────────────────────────────────────────────────────────────────

@Composable
private fun TabMapa(locales: List<Local>, onVerLocal: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localesConCoordenadas = remember(locales) { locales.filter { it.lat != 0.0 && it.lng != 0.0 } }
    var localSeleccionado by remember { mutableStateOf<Local?>(null) }
    var soloAbiertos by remember { mutableStateOf(false) }
    var ubicacionUsuario by remember { mutableStateOf<LatLng?>(null) }
    var permisoUbicacionDenegado by remember { mutableStateOf(false) }

    val localesFiltrados = remember(localesConCoordenadas, soloAbiertos) {
        if (soloAbiertos) localesConCoordenadas.filter { estaAbiertoAhora(it) }
        else localesConCoordenadas
    }

    val camaraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-34.6037, -58.3816), 11f)
    }

    // Pedir ubicación al entrar al tab y centrar cámara
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val ok = permisos[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                 permisos[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            try {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) ubicacionUsuario = LatLng(loc.latitude, loc.longitude)
                    }
            } catch (_: SecurityException) {}
        } else {
            permisoUbicacionDenegado = true
        }
    }

    LaunchedEffect(Unit) {
        permLauncher.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    // Centrar cámara cuando llega la ubicación
    LaunchedEffect(ubicacionUsuario) {
        ubicacionUsuario?.let { pos ->
            camaraState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pos, 13f)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camaraState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            properties = MapProperties(isMyLocationEnabled = ubicacionUsuario != null)
        ) {
            localesFiltrados.forEach { local ->
                val abierto = estaAbiertoAhora(local)
                val markerState = remember(local.lat, local.lng) { MarkerState(position = LatLng(local.lat, local.lng)) }
                Marker(
                    state = markerState,
                    title = local.nombre,
                    snippet = buildString {
                        append(local.tipo)
                        val horarioTexto = textoHorarioHoy(local)
                        if (horarioTexto.isNotBlank()) append(" · $horarioTexto")
                    },
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (abierto) BitmapDescriptorFactory.HUE_GREEN
                        else BitmapDescriptorFactory.HUE_RED
                    ),
                    onClick = { localSeleccionado = local; false }
                )
            }
        }

        // Chip filtro "Solo abiertos" arriba
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = soloAbiertos,
                onClick = { soloAbiertos = !soloAbiertos; localSeleccionado = null },
                label = { Text("Solo abiertos") },
                leadingIcon = if (soloAbiertos) ({
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                }) else null
            )
        }

        // Botón "Mi ubicación" abajo a la derecha
        FloatingActionButton(
            onClick = {
                val pos = ubicacionUsuario
                if (pos != null) {
                    scope.launch {
                        camaraState.animate(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pos, 14f)
                        )
                    }
                } else {
                    permLauncher.launch(arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (localSeleccionado != null) 160.dp else 16.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.MyLocation, "Mi ubicación")
        }

        // Panel inferior cuando se selecciona un marcador
        localSeleccionado?.let { local ->
            val abierto = estaAbiertoAhora(local)
            val horarioTexto = textoHorarioHoy(local)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 72.dp, bottom = 16.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(local.nombre, fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Column {
                                    Text(buildString {
                                        append(local.tipo)
                                        if (local.direccion.isNotBlank()) append(" · ${local.direccion}")
                                    })
                                    if (horarioTexto.isNotBlank()) {
                                        Text(horarioTexto,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (abierto) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                        .background(
                                            if (abierto) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(iconParaTipo(local.tipo), null,
                                        tint = if (abierto) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp))
                                }
                            },
                            trailingContent = {
                                Badge(
                                    containerColor = if (abierto) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(if (abierto) "Abierto" else "Cerrado",
                                        color = if (abierto) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                        // Botones: Ver vidriera + Cómo llegar
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onVerLocal(local.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Store, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ver local")
                            }
                            if (local.lat != 0.0 && local.lng != 0.0) {
                                Button(
                                    onClick = {
                                        val uri = Uri.parse("geo:${local.lat},${local.lng}?q=${local.lat},${local.lng}(${Uri.encode(local.nombre)})")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Directions, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cómo llegar")
                                }
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { localSeleccionado = null },
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                ) {
                    Icon(Icons.Default.Close, "Cerrar",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Aviso si no hay locales con coordenadas
        if (localesConCoordenadas.isEmpty() && locales.isNotEmpty()) {
            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp, start = 12.dp, end = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ningún local cargó su ubicación todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Tab Ofertas ───────────────────────────────────────────────────────────────

@Composable
private fun TabOfertas(
    productos: List<Producto>,
    locales: List<Local>,
    onVerLocal: (String) -> Unit
) {
    val localMap = remember(locales) { locales.associateBy { it.id } }

    if (productos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocalOffer, null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant)
                Text("Sin ofertas por el momento",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Cuando un kiosco publique ofertas\naparecen acá",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)) {

        item {
            SeccionLabel(
                texto = "Ofertas",
                conteo = "${productos.size} disponible${if (productos.size != 1) "s" else ""}"
            )
        }

        val pares = productos.chunked(2)
        items(pares) { par ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                par.forEach { prod ->
                    CardOferta(
                        producto = prod,
                        nombreLocal = localMap[prod.localId]?.nombre ?: "",
                        onClick = { localMap[prod.localId]?.id?.let { onVerLocal(it) } },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                if (par.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ── Tab Más ───────────────────────────────────────────────────────────────────

@Composable
private fun TabMas(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    tieneLocalesFiado: Boolean,
    onIniciarSesion: () -> Unit,
    onCerrarSesionCuenta: () -> Unit,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onVerMiFiado: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onConectarmeComoCliente: () -> Unit = {},
    onConfiguracion: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    onAcercaDe: () -> Unit = {},
    onHistorial: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        if (!estaLogueado) {
            // ── Sin cuenta ──────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person, null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Ingresar a Kosking",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Registrate para administrar tu local, entrar como empleado o llevar el control de tu fiado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onIniciarSesion, modifier = Modifier.fillMaxWidth()) {
                            Text("Iniciar sesión / Crear cuenta")
                        }
                    }
                }
            }

            // Lo que pueden hacer sin cuenta
            item {
                Text(
                    "Sin cuenta podés",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Explorar locales", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Buscá kioscos, almacenes y más cerca tuyo") },
                            leadingContent = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Ver ofertas", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Mirá los precios y promociones de cada negocio") },
                            leadingContent = { Icon(Icons.Default.LocalOffer, null, tint = MaterialTheme.colorScheme.secondary) }
                        )
                    }
                }
            }

        } else {
            // ── Con cuenta ──────────────────────────────────────────────────

            // Tarjeta de perfil
            item {
                Card(modifier = Modifier.fillMaxWidth(), onClick = onConfigurarPerfil) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (nombreUsuario.firstOrNull() ?: "?").toString().uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                nombreUsuario.ifBlank { "Mi cuenta" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val rolesActivos = buildList {
                                if (esDueno) add("Dueño")
                                if (esEmpleado) add("Empleado")
                                if (tieneLocalesFiado) add("Cliente")
                            }
                            Text(
                                if (rolesActivos.isEmpty()) "Cuenta activa"
                                else rolesActivos.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Fiado: una sola entrada que adapta su acción según si ya tiene locales
            item {
                Text(
                    "Mi fiado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Ver mi fiado", fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(
                                if (tieneLocalesFiado) "Revisá lo que debés en tus locales guardados"
                                else "Conectate a un local para ver tu fiado"
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable(
                            onClick = if (tieneLocalesFiado) onVerMiFiado else onConectarmeComoCliente
                        )
                    )
                }
            }

            // Rol activo: empleado
            if (esEmpleado) {
                item {
                    Text(
                        "Trabajo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(modifier = Modifier.fillMaxWidth(), onClick = onSoyEmpleado) {
                        ListItem(
                            headlineContent = { Text("Panel de empleado", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Registrá fiados y pagos de los clientes") },
                            leadingContent = {
                                Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.secondary)
                            },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                        )
                    }
                }
            }

            // Rol activo: dueño
            if (esDueno) {
                item {
                    Text(
                        "Mi negocio",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(modifier = Modifier.fillMaxWidth(), onClick = onSoyDueno) {
                        ListItem(
                            headlineContent = { Text("Administrar mi local", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Clientes, productos, empleados y actividad") },
                            leadingContent = {
                                Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                        )
                    }
                }
            }

            // Opciones para agregar roles que no tienen
            val sinRoles = !esDueno && !esEmpleado && !tieneLocalesFiado
            if (sinRoles || (!esDueno && !esEmpleado)) {
                item {
                    Text(
                        "Unirse como",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            if (!esDueno) {
                                ListItem(
                                    headlineContent = { Text("Dueño de un local", fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text("Creá tu local y empezá a gestionar fiados") },
                                    leadingContent = {
                                        Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                    modifier = Modifier.clickable(onClick = onSoyDueno)
                                )
                            }
                            if (!esDueno && !esEmpleado) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            if (!esEmpleado) {
                                ListItem(
                                    headlineContent = { Text("Empleado", fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text("Ingresá el código que te dio el dueño") },
                                    leadingContent = {
                                        Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.secondary)
                                    },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                    modifier = Modifier.clickable(onClick = onSoyEmpleado)
                                )
                            }
                        }
                    }
                }
            }

            // Sección: Actividad
            item {
                Text(
                    "Actividad",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Notificaciones", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Avisos de tus locales y novedades") },
                            leadingContent = { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable(onClick = onNotificaciones)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Historial", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Lo que viste y visitaste últimamente") },
                            leadingContent = { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable(onClick = onHistorial)
                        )
                    }
                }
            }

            // Sección: Descubrir
            item {
                Text(
                    "Descubrir",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Ofertazos", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Las mejores promociones del momento") },
                            leadingContent = { Icon(Icons.Default.LocalOffer, null, tint = MaterialTheme.colorScheme.secondary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable { /* TODO */ }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Categorías", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Explorá por tipo de local o producto") },
                            leadingContent = { Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.secondary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable { /* TODO */ }
                        )
                    }
                }
            }

            // Cerrar sesión
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onCerrarSesionCuenta,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout, null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Sección: Kosking (siempre visible, con o sin cuenta)
        item {
            Text(
                "Kosking",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Tutoriales", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Aprendé a usar Kosking paso a paso") },
                        leadingContent = { Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Configuración", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Preferencias de la aplicación") },
                        leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable(onClick = onConfiguracion)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Acerca de Kosking", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Versión, información y contacto") },
                        leadingContent = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable(onClick = onAcercaDe)
                    )
                }
            }
        }

        item {
            Text(
                "Kosking v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}


fun iconParaTipo(tipo: String): ImageVector = when (tipo) {
    "Almacén" -> Icons.Default.ShoppingBasket
    "Maxikiosco" -> Icons.Default.ShoppingCart
    "Ropa" -> Icons.Default.Checkroom
    "Corralón" -> Icons.Default.Foundation
    "Farmacia" -> Icons.Default.LocalPharmacy
    "Verdulería" -> Icons.Default.Eco
    else -> Icons.Default.Store
}
