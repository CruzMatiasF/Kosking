package com.kosking.app.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import kotlin.math.abs
import kotlinx.coroutines.flow.drop
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.scanner.components.*
import com.kosking.app.util.WhatsAppUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun SelectorCarrito(
    carritos: List<CarritoState>,
    activoIdx: Int,
    onSeleccionar: (Int) -> Unit,
    onAgregar: () -> Unit,
    onCerrar: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        carritos.forEachIndexed { idx, carrito ->
            val esActivo = idx == activoIdx
            val cantItems = carrito.itemsAnotados.sumOf { it.cantidad }
            Surface(
                onClick = { onSeleccionar(idx) },
                shape = RoundedCornerShape(50),
                color = if (esActivo) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(30.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${idx + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (esActivo) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (cantItems > 0) {
                        Text(
                            "·$cantItems",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (esActivo) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (carritos.size > 1) {
                        Icon(
                            Icons.Default.Close, null,
                            Modifier
                                .size(12.dp)
                                .clickable { onCerrar(idx) },
                            tint = if (esActivo) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        if (carritos.size < 3) {
            IconButton(
                onClick = onAgregar,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.Add, null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val modos = listOf(ModoEscaner.ANOTAR, ModoEscaner.VENDER, ModoEscaner.CARGAR_NUEVO)
private val modoLabel = mapOf(
    ModoEscaner.VENDER to "Vender",
    ModoEscaner.ANOTAR to "Anotar",
    ModoEscaner.CARGAR_NUEVO to "Cargar nuevo"
)

@Composable
private fun SelectorModo(
    modo: ModoEscaner,
    onCambiar: (ModoEscaner) -> Unit
) {
    val idx = modos.indexOf(modo)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = idx)
    val snapBehavior = rememberSnapFlingBehavior(state)

    val haptic = LocalHapticFeedback.current

    fun centeredIdx(): Int {
        val info = state.layoutInfo
        if (info.visibleItemsInfo.isEmpty()) return -1
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
        return info.visibleItemsInfo
            .minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }?.index ?: -1
    }

    // Calculado sincrónicamente desde el layout — sin coroutine, sin delay de frame
    val visualIdx by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - center) }
                ?.index ?: state.firstVisibleItemIndex
        }
    }

    // Haptic + cambio de modo en cuanto el ítem cruza el centro — sin esperar que pare el scroll
    LaunchedEffect(state) {
        snapshotFlow { visualIdx }
            .drop(1)
            .collect { newIdx ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                modos.getOrNull(newIdx)?.let { onCambiar(it) }
            }
    }

    // Snapea al ítem correcto cuando el modo cambia desde afuera o cuando un diálogo rechaza el cambio
    LaunchedEffect(idx, state.isScrollInProgress) {
        if (!state.isScrollInProgress && centeredIdx() != idx) {
            state.animateScrollToItem(idx)
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
    ) {
        val itemWidth = maxWidth / modos.size
        val pill = RoundedCornerShape(50)

        // Indicador fijo en el centro — nunca se mueve
        Box(
            Modifier
                .padding(2.dp)
                .width(itemWidth - 4.dp)
                .height(32.dp)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.primary, pill)
        )

        // Labels que se deslizan por encima del indicador
        LazyRow(
            state = state,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(horizontal = itemWidth),
            modifier = Modifier.fillMaxWidth().clip(pill)
        ) {
            itemsIndexed(modos) { i, m ->
                Box(
                    Modifier.width(itemWidth).height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modoLabel[m] ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (i == visualIdx) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (i == visualIdx) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEscaner(
    vm: DuenoViewModel,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val escanerVm: EscanerViewModel = viewModel(factory = EscanerViewModel.factory(vm))

    val productos by vm.productos.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val movimientos by vm.movimientosLocal.collectAsState()
    val cargandoVm by vm.cargando.collectAsState()
    val nombreLocal by vm.nombreLocal.collectAsState()

    val clientesOrdenados = remember(clientes, movimientos) {
        clientes.sortedByDescending { c ->
            movimientos.filter { it.clienteId == c.id }.maxOfOrNull { it.fecha.seconds } ?: 0L
        }
    }
    val busquedaClienteActiva = escanerVm.carritoActivo.busquedaCliente
    val filtradosCliente = remember(busquedaClienteActiva, clientesOrdenados) {
        if (busquedaClienteActiva.isBlank()) clientesOrdenados
        else clientesOrdenados.filter { it.nombre.contains(busquedaClienteActiva, ignoreCase = true) }
    }
    var permiso by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permiso = granted }

    LaunchedEffect(Unit) {
        permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!permiso) launcher.launch(Manifest.permission.CAMERA)
        escanerVm.resetear()
    }

    // ── Pantalla confirmar ──
    val clienteConfirmar = escanerVm.clienteSeleccionado
    val esVender = escanerVm.modo == ModoEscaner.VENDER
    if (escanerVm.mostrarConfirmar && (esVender || clienteConfirmar != null)) {
        PantallaConfirmarAnotacion(
            cliente = clienteConfirmar,
            items = escanerVm.itemsAnotados,
            tipo = if (esVender) "venta" else "cargo",
            cargando = cargandoVm,
            onConfirmar = {
                if (esVender) escanerVm.registrarVenta()
                else escanerVm.confirmarAnotacion(clienteConfirmar!!)
            },
            onVolver = { escanerVm.mostrarConfirmar = false }
        )
        return
    }

    // ── Pantalla éxito venta ──
    escanerVm.ventaExitosa?.let { (items, _) ->
        MovimientoExitosoScreen(
            titulo = "Venta registrada",
            items = items,
            onInicio = { escanerVm.ventaExitosa = null; escanerVm.itemsAnotados = emptyList(); onCerrar() },
            onCerrar = { escanerVm.ventaExitosa = null; escanerVm.itemsAnotados = emptyList(); onCerrar() }
        )
        return
    }

    // ── Pantalla éxito anotación ──
    escanerVm.anotacionExitosa?.let { (cliente, items, tipo) ->
        val hora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
        MovimientoExitosoScreen(
            titulo = "Anotado para ${cliente.nombre}",
            items = items,
            onInicio = { escanerVm.anotacionExitosa = null; onCerrar() },
            onCerrar = { escanerVm.anotacionExitosa = null; onCerrar() },
            onWhatsApp = if (cliente.telefono.isNotBlank()) ({
                WhatsAppUtil.abrirWhatsApp(
                    context, cliente.telefono,
                    WhatsAppUtil.ticketVisita(
                        nombreCliente = cliente.nombre,
                        nombreLocal = nombreLocal,
                        items = items.map { Triple(it.nombre, it.precio, it.cantidad) },
                        tipo = tipo,
                        hora = hora
                    )
                )
                escanerVm.anotacionExitosa = null
                onCerrar()
            }) else null
        )
        return
    }

    // ── Pantalla principal ──
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Área de cámara
        Box(modifier = Modifier.fillMaxWidth().weight(0.40f)) {
            if (permiso) {
                CamaraEscaner(
                    onCodigo = { escanerVm.onCodigo(it) },
                    isYaDetecto = { escanerVm.yaDetecto.get() },
                    buscandoProducto = escanerVm.buscandoProducto,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MarcaEscaner(escaneoExitoso = escanerVm.escaneoExitoso, modifier = Modifier.size(220.dp))
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Dar permiso de cámara")
                    }
                }
            }
            IconButton(onClick = onCerrar, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            val iconColor = Color.White.copy(alpha = 0.65f)
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.QrCode, null, tint = iconColor, modifier = Modifier.size(22.dp))
                IconoCodigoBarras(color = iconColor, modifier = Modifier.size(width = 24.dp, height = 20.dp))
            }
            // Cliente ahora vive en el panel inferior (EscanerAnotarPanel)
        }

        // Panel inferior
        Surface(modifier = Modifier.fillMaxWidth().weight(0.60f), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Selector de carritos simultáneos
                SelectorCarrito(
                    carritos = escanerVm.carritos,
                    activoIdx = escanerVm.carritoActivoIdx,
                    onSeleccionar = { escanerVm.seleccionarCarrito(it) },
                    onAgregar = { escanerVm.agregarCarrito() },
                    onCerrar = { escanerVm.cerrarCarrito(it) }
                )

                HorizontalDivider()

                // Selector de modo — swipe horizontal para cambiar
                SelectorModo(
                    modo = escanerVm.modo,
                    onCambiar = { nuevo ->
                        val tieneItems = escanerVm.itemsAnotados.isNotEmpty()
                        val aCargar = nuevo == ModoEscaner.CARGAR_NUEVO
                        if (aCargar && tieneItems) escanerVm.mostrarDialogoCambioModo = true
                        else { escanerVm.modo = nuevo; escanerVm.yaDetecto.set(false) }
                    }
                )

                HorizontalDivider()

                // Vender y Anotar siempre en composición (object-pool style).
                // requiredSize(0.dp) = ocupan 0px y no se dibujan cuando no son el modo activo,
                // pero su estado interno (scroll, categoría, etc.) se preserva entre switches.
                val onCambiarCantidad: (com.kosking.app.modelo.ItemAnotado, Int) -> Unit = { item, nueva ->
                    escanerVm.itemsAnotados = if (nueva <= 0)
                        escanerVm.itemsAnotados - item
                    else
                        escanerVm.itemsAnotados.map { if (it === item) it.copy(cantidad = nueva) else it }
                }
                Box(modifier = Modifier.weight(1f)) {
                    EscanerVenderPanel(
                        itemsVenta = escanerVm.itemsAnotados,
                        productos = productos,
                        cargando = escanerVm.cargandoAnotacion,
                        onAgregarProducto = { escanerVm.agregarALista(it) },
                        onCambiarCantidad = onCambiarCantidad,
                        onCobrar = { escanerVm.mostrarConfirmar = true },
                        modifier = if (escanerVm.modo == ModoEscaner.VENDER) Modifier.fillMaxSize()
                                   else Modifier.requiredSize(0.dp)
                    )
                    EscanerAnotarPanel(
                        itemsAnotados = escanerVm.itemsAnotados,
                        productos = productos,
                        clienteSeleccionado = escanerVm.clienteSeleccionado,
                        busquedaCliente = escanerVm.busquedaCliente,
                        filtradosCliente = filtradosCliente,
                        cargandoAnotacion = escanerVm.cargandoAnotacion,
                        onAgregarSugerencia = { escanerVm.agregarALista(it) },
                        onCambiarCantidad = onCambiarCantidad,
                        onConfirmar = { escanerVm.mostrarConfirmar = true },
                        onClienteSelected = { escanerVm.clienteSeleccionado = it; escanerVm.busquedaCliente = "" },
                        onBusquedaChange = { escanerVm.busquedaCliente = it },
                        onLimpiarCliente = { escanerVm.clienteSeleccionado = null; escanerVm.busquedaCliente = "" },
                        onNuevoCliente = {
                            escanerVm.nombreNuevoCliente = it
                            escanerVm.telefonoNuevoCliente = ""
                            escanerVm.busquedaCliente = ""
                            escanerVm.mostrarRegistroCliente = true
                        },
                        modifier = if (escanerVm.modo == ModoEscaner.ANOTAR) Modifier.fillMaxSize()
                                   else Modifier.requiredSize(0.dp)
                    )
                    if (escanerVm.modo == ModoEscaner.CARGAR_NUEVO) {
                        EscanerCargarPanel(
                            productoParaCargar = escanerVm.productoParaCargar,
                            nombreCarga = escanerVm.nombreCarga,
                            precioCarga = escanerVm.precioCarga,
                            cargandoVm = cargandoVm,
                            onNombreChange = { escanerVm.nombreCarga = it },
                            onPrecioChange = { escanerVm.precioCarga = it.filter { c -> c.isDigit() || c == '.' } },
                            onGuardar = { escanerVm.guardarProducto() },
                            onEscanearOtro = {
                                escanerVm.productoParaCargar = null
                                escanerVm.yaDetecto.set(false)
                                escanerVm.precioCarga = ""
                                escanerVm.nombreCarga = ""
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    } // Column escaner

    // ── Sheet registro de nuevo cliente ──
    if (escanerVm.mostrarRegistroCliente) {
        RegistroClienteSheet(
            nombreInicial = escanerVm.nombreNuevoCliente,
            telefonoInicial = escanerVm.telefonoNuevoCliente,
            onNombreChange = { escanerVm.nombreNuevoCliente = it },
            onTelefonoChange = { escanerVm.telefonoNuevoCliente = it },
            cargando = false,
            onCrear = {
                escanerVm.agregarClienteYObtener(
                    nombre = escanerVm.nombreNuevoCliente.trim(),
                    telefono = escanerVm.telefonoNuevoCliente.trim()
                ) { nuevo ->
                    escanerVm.clienteSeleccionado = nuevo
                    escanerVm.mostrarRegistroCliente = false
                }
            },
            onDismiss = { escanerVm.mostrarRegistroCliente = false }
        )
    }

    // ── Diálogo cambio de modo ──
    if (escanerVm.mostrarDialogoCambioModo) {
        AlertDialog(
            onDismissRequest = { escanerVm.mostrarDialogoCambioModo = false },
            title = { Text("¿Cambiar de modo?") },
            text = {
                val n = escanerVm.itemsAnotados.size
                Text("Tenés $n producto${if (n != 1) "s" else ""} en la lista. Si cambiás a Cargar nuevo los perdés.")
            },
            confirmButton = {
                TextButton(onClick = {
                    escanerVm.mostrarDialogoCambioModo = false
                    escanerVm.itemsAnotados = emptyList()
                    escanerVm.modo = ModoEscaner.CARGAR_NUEVO
                    escanerVm.yaDetecto.set(false)
                }) { Text("Sí, cambiar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { escanerVm.mostrarDialogoCambioModo = false }) { Text("Cancelar") }
            }
        )
    }
}
