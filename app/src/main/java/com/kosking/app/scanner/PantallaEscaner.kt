package com.kosking.app.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.scanner.components.*
import com.kosking.app.util.WhatsAppUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectorModo(
    modo: TipoMovimiento,
    onModo: (TipoMovimiento) -> Unit
) {
    val opciones = listOf(
        TipoMovimiento.CARGO to "Anotar",
        TipoMovimiento.VENTA to "Vender",
        TipoMovimiento.CARGAR_NUEVO to "Cargar nuevo"
    )
    val pagerState = rememberPagerState(
        initialPage = opciones.indexOfFirst { it.first == modo }.coerceAtLeast(0)
    ) { opciones.size }

    // Cuando cambia el tab activo, sincronizar el pager al instante (sin animación)
    LaunchedEffect(modo) {
        val target = opciones.indexOfFirst { it.first == modo }.coerceAtLeast(0)
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }

    // Activar el modo en cuanto el item cruza el centro, sin esperar el snap
    LaunchedEffect(pagerState.currentPage) {
        onModo(opciones[pagerState.currentPage].first)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val itemW = maxWidth / 3

        // Indicador central fijo
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(itemW)
                .fillMaxHeight()
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = itemW),
            pageSize = androidx.compose.foundation.pager.PageSize.Fixed(itemW),
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp
        ) { page ->
            Box(
                modifier = Modifier
                    .width(itemW)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val esActivo = page == pagerState.currentPage
                Text(
                    opciones[page].second,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (esActivo) FontWeight.Bold else FontWeight.Normal,
                    color = if (esActivo) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }
    }
}

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
            .height(40.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        carritos.forEachIndexed { idx, carrito ->
            val esActivo = idx == activoIdx
            val cantItems = carrito.itemsAnotados.sumOf { it.cantidad }
            // Box externo para poder superponer el badge de cantidad
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { onSeleccionar(idx) },
                    shape = RoundedCornerShape(50),
                    color = if (esActivo) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val label = carrito.clienteSeleccionado?.nombre?.split(" ")?.take(2)?.joinToString(" ")
                            ?: "Cliente ${idx + 1}"
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (esActivo) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Zona táctil amplia para la X
                        if (esActivo && carritos.size > 1) {
                            Box(
                                modifier = Modifier.size(36.dp).clickable { onCerrar(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close, null,
                                    Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            Spacer(Modifier.width(12.dp))
                        }
                    }
                }
                // Badge de cantidad — esquina superior derecha, solo si hay items
                if (cantItems > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-4).dp)
                            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$cantItems",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        if (carritos.size < 3) {
            Surface(
                onClick = onAgregar,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add, null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    onCerrar: () -> Unit,
    esEmpleado: Boolean = false
) {
    val context = LocalContext.current
    val escanerVm: EscanerViewModel = viewModel(factory = EscanerViewModel.factory(vm))
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    // ── Pantalla confirmar (pantalla completa superpuesta) ──
    val clienteConfirmar = escanerVm.clienteSeleccionado
    val esVenta = escanerVm.tipoMovimiento == TipoMovimiento.VENTA
    if (escanerVm.mostrarConfirmar && (esVenta || clienteConfirmar != null)) {
        Box(modifier = Modifier.fillMaxSize()) {
            PantallaConfirmar(
                cliente = clienteConfirmar,
                items = escanerVm.itemsAnotados,
                tipo = if (esVenta) "venta" else "cargo",
                cargando = cargandoVm,
                onConfirmar = {
                    if (esVenta) escanerVm.registrarVenta()
                    else escanerVm.confirmarAnotacion(clienteConfirmar!!)
                },
                onCancelar = { escanerVm.mostrarConfirmar = false }
            )
        }
        return
    }

    // ── Snackbar: éxito venta ──
    LaunchedEffect(escanerVm.ventaExitosa) {
        val ex = escanerVm.ventaExitosa ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = "Venta registrada — $${"%.0f".format(ex.second)}",
            duration = SnackbarDuration.Short
        )
        escanerVm.ventaExitosa = null
    }

    // ── Snackbar: éxito anotación ──
    LaunchedEffect(escanerVm.anotacionExitosa) {
        val ex = escanerVm.anotacionExitosa ?: return@LaunchedEffect
        val (cliente, items, tipo) = ex
        val result = snackbarHostState.showSnackbar(
            message = "Anotado para ${cliente.nombre}",
            actionLabel = if (cliente.telefono.isNotBlank()) "Avisar WA" else null,
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed && cliente.telefono.isNotBlank()) {
            val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
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
        }
        escanerVm.anotacionExitosa = null
    }

    // ── Navegar a inicio cuando se confirmó el último carrito ──
    LaunchedEffect(escanerVm.irAInicio) {
        if (escanerVm.irAInicio) onCerrar()
    }

    // ── Pantalla principal ──
    Box(modifier = Modifier.fillMaxSize()) {
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt, null,
                            Modifier.size(44.dp),
                            tint = Color.White.copy(alpha = 0.35f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Se necesita acceso a la cámara",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { launcher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Text("Permitir cámara")
                        }
                    }
                }
                // Gradiente suave en el borde inferior para fundir con el panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))
                        )
                )
                IconButton(onClick = onCerrar, modifier = Modifier.padding(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Row(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.QrCode, null, tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(22.dp))
                    IconoCodigoBarras(color = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(width = 24.dp, height = 20.dp))
                }
            }

            // Panel inferior
            Surface(modifier = Modifier.fillMaxWidth().weight(0.60f), color = MaterialTheme.colorScheme.surface) {
                val onCambiarCantidad: (com.kosking.app.modelo.ItemAnotado, Int) -> Unit = { item, nueva ->
                    escanerVm.itemsAnotados = if (nueva <= 0)
                        escanerVm.itemsAnotados - item
                    else
                        escanerVm.itemsAnotados.map { if (it === item) it.copy(cantidad = nueva) else it }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    SelectorCarrito(
                        carritos = escanerVm.carritos,
                        activoIdx = escanerVm.carritoActivoIdx,
                        onSeleccionar = { escanerVm.seleccionarCarrito(it) },
                        onAgregar = { escanerVm.agregarCarrito() },
                        onCerrar = { escanerVm.cerrarCarrito(it) }
                    )
                    HorizontalDivider()
                    SelectorModo(
                        modo = escanerVm.tipoMovimiento,
                        onModo = { escanerVm.tipoMovimiento = it }
                    )
                    HorizontalDivider()
                    EscanerPanel(
                    itemsAnotados = escanerVm.itemsAnotados,
                    productos = productos,
                    clienteSeleccionado = escanerVm.clienteSeleccionado,
                    busquedaCliente = escanerVm.busquedaCliente,
                    filtradosCliente = filtradosCliente,
                    cargandoAnotacion = escanerVm.cargandoAnotacion,
                    onAgregarSugerencia = { escanerVm.agregarALista(it) },
                    onCambiarCantidad = onCambiarCantidad,
                    tipoMovimiento = escanerVm.tipoMovimiento,
                    onCobrar = { escanerVm.mostrarConfirmar = true },
                    onAnotar = { escanerVm.mostrarConfirmar = true },
                    onCargarNuevo = {
                        escanerVm.productoParaCargar = ProductoDetectado("", 0.0)
                        escanerVm.nombreCarga = ""
                        escanerVm.precioCarga = ""
                    },
                    onClienteSelected = { escanerVm.clienteSeleccionado = it; escanerVm.busquedaCliente = "" },
                    onBusquedaChange = { escanerVm.busquedaCliente = it },
                    onLimpiarCliente = { escanerVm.clienteSeleccionado = null; escanerVm.busquedaCliente = "" },
                    modifier = Modifier.weight(1f)
                )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

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

    // ── Sheet producto desconocido ──
    val productoParaCargar = escanerVm.productoParaCargar
    if (productoParaCargar != null) {
        EscanerDesconocidoSheet(
            producto = productoParaCargar,
            esEmpleado = esEmpleado,
            onAgregar = { nombre, precio, guardar ->
                escanerVm.agregarProductoDesconocido(nombre, precio, guardar)
            },
            onDismiss = {
                escanerVm.productoParaCargar = null
                escanerVm.yaDetecto.set(false)
            }
        )
    }
}
