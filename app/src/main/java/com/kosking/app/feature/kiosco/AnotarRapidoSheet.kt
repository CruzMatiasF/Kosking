package com.kosking.app.feature.kiosco

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import kotlinx.coroutines.delay

@Composable
fun AnotarRapidoSheet(
    clientes: List<Cliente>,
    movimientos: List<Movimiento> = emptyList(),
    cargando: Boolean,
    clienteInicial: Cliente? = null,
    productos: List<Producto> = emptyList(),
    tipoInicial: String = "cargo",
    onBuscarGlobal: suspend (String) -> List<String> = { emptyList() },
    onGuardarProducto: (nombre: String, precio: Double, onExito: () -> Unit) -> Unit = { _, _, cb -> cb() },
    onGuardar: (clienteId: String, items: List<Triple<String, Double, Int>>, tipo: String) -> Unit,
    onDismiss: () -> Unit,
    onAgregarCliente: (() -> Unit)? = null,
    onAbrirProductos: (() -> Unit)? = null
) {
    // ── Estado ───────────────────────────────────────────────────────────────
    val clientesOrdenados = remember(clientes) { clientes.sortedByDescending { it.deudaTotal } }
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }
    val recientes = remember(movimientos, clientes) {
        movimientos
            .sortedByDescending { it.fecha.toDate().time }
            .map { it.clienteId }
            .distinct()
            .take(4)
            .mapNotNull { clienteMap[it] }
    }
    var clienteSeleccionado by remember(clienteInicial) {
        mutableStateOf(clienteInicial ?: clientesOrdenados.firstOrNull())
    }
    var paso by remember { mutableIntStateOf(if (clienteInicial != null) 2 else 1) }
    var esCargo by remember { mutableStateOf(tipoInicial == "cargo") }
    var pedirConfirmCambio by remember { mutableStateOf(false) }
    var clienteConAlerta by remember { mutableStateOf<Cliente?>(null) }
    val carrito = remember { mutableStateListOf<ItemCarrito>() }
    var busquedaCliente by remember { mutableStateOf("") }
    val clientesFiltrados = remember(clientesOrdenados, busquedaCliente) {
        if (busquedaCliente.isBlank()) clientesOrdenados
        else clientesOrdenados.filter { it.nombre.contains(busquedaCliente, ignoreCase = true) }
    }
    var busquedaProducto by remember { mutableStateOf("") }
    val productosFiltrados = remember(productos, busquedaProducto) {
        val base = productos.sortedByDescending { it.vecesUsado }
        if (busquedaProducto.isBlank()) base
        else {
            val q = busquedaProducto.trim().lowercase()
            base.filter { prod -> prod.nombre.lowercase().split(" ").any { it.startsWith(q) } }
        }
    }
    var productosGlobales by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(busquedaProducto, productosFiltrados) {
        if (busquedaProducto.length < 2) { productosGlobales = emptyList(); return@LaunchedEffect }
        delay(400)
        val nombresLocales = productosFiltrados.map { it.nombre.lowercase() }.toSet()
        productosGlobales = try {
            onBuscarGlobal(busquedaProducto).filter { it.lowercase() !in nombresLocales }
        } catch (_: Exception) { emptyList() }
    }
    val clienteFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (clienteInicial == null) try { clienteFocusRequester.requestFocus() } catch (_: Exception) {}
    }
    var descripcionOtro by remember { mutableStateOf("") }
    var montoOtroTexto by remember { mutableStateOf("") }
    var mostrarOtro by remember { mutableStateOf(productos.isEmpty()) }
    var montoUnicoTexto by remember { mutableStateOf("") }
    var sugerenciasOtro by remember { mutableStateOf(emptyList<String>()) }
    var pedirConfirmSinPrecio by remember { mutableStateOf(false) }
    var productoGlobalPendiente by remember { mutableStateOf<String?>(null) }
    var precioGlobalTexto by remember { mutableStateOf("") }

    LaunchedEffect(descripcionOtro) {
        if (descripcionOtro.length < 2) { sugerenciasOtro = emptyList(); return@LaunchedEffect }
        delay(300)
        sugerenciasOtro = try { onBuscarGlobal(descripcionOtro) } catch (_: Exception) { emptyList() }
    }

    // ── Funciones locales ────────────────────────────────────────────────────
    fun limpiarCarrito() {
        carrito.clear(); mostrarOtro = false; descripcionOtro = ""
        montoOtroTexto = ""; sugerenciasOtro = emptyList()
    }
    fun toggleCarrito(prod: Producto) {
        val idx = carrito.indexOfFirst { it.producto.id == prod.id }
        if (idx >= 0) carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad + 1)
        else carrito.add(ItemCarrito(prod, 1))
    }
    fun quitarUno(prod: Producto) {
        val idx = carrito.indexOfFirst { it.producto.id == prod.id }
        if (idx < 0) return
        if (carrito[idx].cantidad > 1) carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad - 1)
        else carrito.removeAt(idx)
    }
    fun setCantidad(prod: Producto, n: Int) {
        val idx = carrito.indexOfFirst { it.producto.id == prod.id }
        if (n <= 0) { if (idx >= 0) carrito.removeAt(idx); return }
        if (idx >= 0) carrito[idx] = carrito[idx].copy(cantidad = n)
        else carrito.add(ItemCarrito(prod, n))
    }

    // ── Derivados ────────────────────────────────────────────────────────────
    val montoOtroDouble = montoOtroTexto.toDoubleOrNull() ?: 0.0
    val montoUnicoDouble = montoUnicoTexto.toDoubleOrNull() ?: 0.0
    fun armarItems(): List<Triple<String, Double, Int>> = buildList {
        carrito.forEach { item -> add(Triple(item.producto.nombre, item.producto.ultimoPrecio * item.cantidad, item.cantidad)) }
        if (mostrarOtro && montoOtroDouble > 0) add(Triple(descripcionOtro.trim(), montoOtroDouble, 1))
        if (!esCargo) add(Triple("", montoUnicoDouble, 1))
    }
    val totalCarrito = carrito.sumOf { it.producto.ultimoPrecio * it.cantidad }
    val totalConOtro = totalCarrito + (if (mostrarOtro && montoOtroDouble > 0) montoOtroDouble else 0.0)
    val deudaActual = clienteSeleccionado?.deudaTotal ?: 0.0
    val limiteCredito = clienteSeleccionado?.limiteCredito ?: 0.0
    val deudaFinal = when {
        esCargo -> deudaActual + totalConOtro
        else -> (deudaActual - montoUnicoDouble).coerceAtLeast(0.0)
    }
    val superaLimite = esCargo && limiteCredito > 0 && deudaFinal > limiteCredito
    val puedeGuardar = clienteSeleccionado != null && !cargando && !superaLimite && when {
        esCargo -> totalConOtro > 0
        else -> montoUnicoDouble > 0
    }

    // ── Diálogo cambio de tipo ───────────────────────────────────────────────
    if (pedirConfirmCambio) {
        AlertDialog(
            onDismissRequest = { pedirConfirmCambio = false },
            title = { Text("¿Cambiar a Cobrar?") },
            text = { Text("Vas a perder los items que ya cargaste. ¿Continuás?") },
            confirmButton = {
                TextButton(onClick = { limpiarCarrito(); esCargo = false; pedirConfirmCambio = false }) {
                    Text("Cambiar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pedirConfirmCambio = false }) { Text("Cancelar") } }
        )
    }

    // ── Dialog cliente bloqueado / con alerta ───────────────────────────────
    clienteConAlerta?.let { cliente ->
        val bloqueado = !cliente.habilitado
        AlertDialog(
            onDismissRequest = { clienteConAlerta = null },
            title = {
                Text(if (bloqueado) "Cliente bloqueado" else "Atención: ${cliente.nombre}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (bloqueado) "${cliente.nombre} está bloqueado por el dueño."
                        else "El dueño pidió que lo avises antes de anotar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (cliente.notaInterna.isNotBlank()) {
                        Text(
                            "\"${cliente.notaInterna}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clienteSeleccionado = cliente
                    busquedaCliente = ""
                    paso = 2
                    clienteConAlerta = null
                }) {
                    Text("Anotar igual", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { clienteConAlerta = null }) { Text("Cancelar") }
            }
        )
    }

    // ── Dialog precio producto global ────────────────────────────────────────
    productoGlobalPendiente?.let { nombre ->
        val precioDouble = precioGlobalTexto.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { productoGlobalPendiente = null; precioGlobalTexto = "" },
            title = { Text(nombre) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿A qué precio lo vendés?", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = precioGlobalTexto,
                        onValueChange = { precioGlobalTexto = it.filter { c -> c.isDigit() || c == '.' } },
                        prefix = { Text("$") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "El producto se guarda en tu catálogo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onGuardarProducto(nombre, precioDouble) {
                        val prod = Producto(id = "global_$nombre", nombre = nombre, ultimoPrecio = precioDouble)
                        toggleCarrito(prod)
                        busquedaProducto = ""
                        productosGlobales = emptyList()
                    }
                    productoGlobalPendiente = null
                    precioGlobalTexto = ""
                }) { Text("Guardar y agregar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onGuardarProducto(nombre, 0.0) {}
                    val prod = Producto(id = "global_$nombre", nombre = nombre, ultimoPrecio = 0.0)
                    toggleCarrito(prod)
                    busquedaProducto = ""
                    productosGlobales = emptyList()
                    productoGlobalPendiente = null
                    precioGlobalTexto = ""
                }) { Text("Sin precio por ahora") }
            }
        )
    }

    // ── Sheet ────────────────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(contentWindowInsets = WindowInsets.systemBars) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = paso,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    else
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                },
                label = "paso"
            ) { pasoActual ->
                if (pasoActual == 1) {
                    Paso1ElegirCliente(
                        clientesFiltrados = clientesFiltrados,
                        recientes = recientes,
                        busquedaCliente = busquedaCliente,
                        esCargo = esCargo,
                        focusRequester = clienteFocusRequester,
                        onBusquedaChange = { busquedaCliente = it },
                        onClienteSeleccionado = { cliente ->
                            if (!cliente.habilitado || cliente.alertaEmpleado) {
                                clienteConAlerta = cliente
                            } else {
                                clienteSeleccionado = cliente
                                busquedaCliente = ""
                                paso = 2
                            }
                        },
                        onDismiss = onDismiss,
                        onEsCargoChange = { esCargo = it },
                        onLimpiarCarrito = ::limpiarCarrito,
                        onAgregarCliente = onAgregarCliente?.let { cb -> { onDismiss(); cb() } }
                    )
                } else {
                    // ── Paso 2: cargar productos / monto ───────────────────────
                    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (clienteInicial == null) {
                                IconButton(onClick = { paso = 1; busquedaCliente = "" }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                                }
                            } else {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar")
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    clienteSeleccionado?.nombre ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (deudaActual > 0) {
                                    Text(
                                        "Deuda: $${"%.0f".format(deudaActual)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(end = 12.dp)) {
                                SegmentedButton(
                                    selected = esCargo,
                                    onClick = { esCargo = true; limpiarCarrito() },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    label = { Text("Anotar") }
                                )
                                SegmentedButton(
                                    selected = !esCargo,
                                    onClick = {
                                        if (carrito.isNotEmpty() || (mostrarOtro && montoOtroTexto.isNotBlank()))
                                            pedirConfirmCambio = true
                                        else { montoUnicoTexto = ""; esCargo = false }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    label = { Text("Cobrar") }
                                )
                            }
                        }
                        HorizontalDivider()

                        if (!esCargo) {
                            ZonaPago(
                                deudaActual = deudaActual,
                                montoUnicoTexto = montoUnicoTexto,
                                montoUnicoDouble = montoUnicoDouble,
                                deudaFinal = deudaFinal,
                                cargando = cargando,
                                puedeGuardar = puedeGuardar,
                                onMontoChange = { montoUnicoTexto = it },
                                onGuardar = { clienteSeleccionado?.let { onGuardar(it.id, armarItems(), "pago") } }
                            )
                        } else {
                            if (productos.isEmpty() && onAbrirProductos != null) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, null, Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Sin productos cargados",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            Text("Cargá los productos de tu local para anotar más rápido.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                                        }
                                        TextButton(
                                            onClick = { onDismiss(); onAbrirProductos() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Ir", style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                }
                            }
                            ZonaProductos(
                                productos = productos,
                                busquedaProducto = busquedaProducto,
                                productosFiltrados = productosFiltrados,
                                carrito = carrito,
                                mostrarOtro = mostrarOtro,
                                productosGlobales = productosGlobales,
                                onBusquedaChange = { busquedaProducto = it; productosGlobales = emptyList() },
                                onToggleMostrarOtro = { mostrarOtro = !mostrarOtro },
                                onToggleCarrito = ::toggleCarrito,
                                onQuitarUno = ::quitarUno,
                                onSetCantidad = ::setCantidad,
                                onUsarProductoGlobal = { prod -> productoGlobalPendiente = prod.nombre; precioGlobalTexto = "" }
                            )
                            HorizontalDivider()
                            ZonaResumen(
                                mostrarOtro = mostrarOtro,
                                descripcionOtro = descripcionOtro,
                                montoOtroTexto = montoOtroTexto,
                                sugerenciasOtro = sugerenciasOtro,
                                superaLimite = superaLimite,
                                limiteCredito = limiteCredito,
                                carrito = carrito,
                                montoOtroDouble = montoOtroDouble,
                                totalConOtro = totalConOtro,
                                pedirConfirmSinPrecio = pedirConfirmSinPrecio,
                                cargando = cargando,
                                puedeGuardar = puedeGuardar,
                                clienteSeleccionado = clienteSeleccionado,
                                onDescripcionOtroChange = { descripcionOtro = it; sugerenciasOtro = emptyList() },
                                onMontoOtroTextoChange = { montoOtroTexto = it.filter { c -> c.isDigit() || c == '.' } },
                                onSugerenciaSelected = { descripcionOtro = it; sugerenciasOtro = emptyList() },
                                onOcultarOtro = { mostrarOtro = false; descripcionOtro = ""; montoOtroTexto = ""; sugerenciasOtro = emptyList() },
                                onPedirConfirmSinPrecioChange = { pedirConfirmSinPrecio = it },
                                buildItems = ::armarItems,
                                onGuardar = onGuardar
                            )
                        }
                    }
                }
            }
        }
        } // Scaffold
    }
}
