package com.kosking.app.dueno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnotarRapidoSheet(
    clientes: List<Cliente>,
    cargando: Boolean,
    clienteInicial: Cliente? = null,
    productos: List<Producto> = emptyList(),
    tipoInicial: String = "cargo",
    onBuscarGlobal: suspend (String) -> List<String> = { emptyList() },
    onGuardar: (clienteId: String, items: List<Pair<String, Double>>, tipo: String) -> Unit,
    onDismiss: () -> Unit
) {
    // ── Estado ───────────────────────────────────────────────────────────────
    val clientesOrdenados = remember(clientes) { clientes.sortedByDescending { it.deudaTotal } }
    var clienteSeleccionado by remember(clienteInicial) {
        mutableStateOf(clienteInicial ?: clientesOrdenados.firstOrNull())
    }
    var esCargo by remember { mutableStateOf(tipoInicial == "cargo") }
    var pedirConfirmCambio by remember { mutableStateOf(false) }
    val carrito = remember { mutableStateListOf<ItemCarrito>() }
    var busquedaCliente by remember { mutableStateOf("") }
    var mostrarListaClientes by remember { mutableStateOf(false) }
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
    val clienteFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (clienteInicial == null) try { clienteFocusRequester.requestFocus() } catch (_: Exception) {}
    }
    var descripcionOtro by remember { mutableStateOf("") }
    var montoOtroTexto by remember { mutableStateOf("") }
    var mostrarOtro by remember { mutableStateOf(productos.isEmpty()) }
    var montoTexto by remember { mutableStateOf("") }
    var sugerenciasOtro by remember { mutableStateOf(emptyList<String>()) }
    var pedirConfirmSinPrecio by remember { mutableStateOf(false) }

    LaunchedEffect(descripcionOtro) {
        if (descripcionOtro.length < 2) { sugerenciasOtro = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(300)
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
    val montoUnicoDouble = montoTexto.toDoubleOrNull() ?: 0.0
    fun buildItems(): List<Pair<String, Double>> = buildList {
        carrito.forEach { item -> add(Pair(item.producto.nombre, item.producto.ultimoPrecio * item.cantidad)) }
        if (mostrarOtro && montoOtroDouble > 0) add(Pair(descripcionOtro.trim(), montoOtroDouble))
        if (!esCargo) add(Pair("", montoUnicoDouble))
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
            title = { Text("¿Cambiar a Pago?") },
            text = { Text("Vas a perder los items que ya cargaste. ¿Continuás?") },
            confirmButton = {
                TextButton(onClick = { limpiarCarrito(); esCargo = false; pedirConfirmCambio = false }) {
                    Text("Cambiar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pedirConfirmCambio = false }) { Text("Cancelar") } }
        )
    }

    // ── Sheet ────────────────────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    if (mostrarListaClientes) { mostrarListaClientes = false; busquedaCliente = "" }
                }
        ) {
            ZonaCliente(
                clienteInicial = clienteInicial,
                clienteSeleccionado = clienteSeleccionado,
                mostrarListaClientes = mostrarListaClientes,
                busquedaCliente = busquedaCliente,
                clientesFiltrados = clientesFiltrados,
                clienteFocusRequester = clienteFocusRequester,
                esCargo = esCargo,
                deudaActual = deudaActual,
                deudaFinal = deudaFinal,
                totalConOtro = totalConOtro,
                hayItemsEnCarrito = carrito.isNotEmpty() || (mostrarOtro && montoOtroTexto.isNotBlank()),
                montoOtroTexto = montoOtroTexto,
                onClienteSeleccionado = { clienteSeleccionado = it },
                onMostrarListaClientes = { mostrarListaClientes = it },
                onBusquedaClienteChange = { busquedaCliente = it },
                onEsCargoChange = { esCargo = it },
                onPedirConfirmCambio = { pedirConfirmCambio = true },
                limpiarCarrito = ::limpiarCarrito
            )
            HorizontalDivider()
            ZonaProductos(
                esCargo = esCargo,
                productos = productos,
                busquedaProducto = busquedaProducto,
                productosFiltrados = productosFiltrados,
                carrito = carrito,
                mostrarOtro = mostrarOtro,
                montoTexto = montoTexto,
                onBusquedaChange = { busquedaProducto = it },
                onToggleMostrarOtro = { mostrarOtro = !mostrarOtro },
                onToggleCarrito = ::toggleCarrito,
                onQuitarUno = ::quitarUno,
                onSetCantidad = ::setCantidad,
                onMontoTextoChange = { montoTexto = it.filter { c -> c.isDigit() || c == '.' } }
            )
            HorizontalDivider()
            ZonaResumen(
                esCargo = esCargo,
                mostrarOtro = mostrarOtro,
                productos = productos,
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
                montoUnicoDouble = montoUnicoDouble,
                clienteSeleccionado = clienteSeleccionado,
                onDescripcionOtroChange = { descripcionOtro = it; sugerenciasOtro = emptyList() },
                onMontoOtroTextoChange = { montoOtroTexto = it.filter { c -> c.isDigit() || c == '.' } },
                onSugerenciaSelected = { descripcionOtro = it; sugerenciasOtro = emptyList() },
                onOcultarOtro = { mostrarOtro = false; descripcionOtro = ""; montoOtroTexto = ""; sugerenciasOtro = emptyList() },
                onPedirConfirmSinPrecioChange = { pedirConfirmSinPrecio = it },
                buildItems = ::buildItems,
                onGuardar = onGuardar
            )
        }
    }
}
