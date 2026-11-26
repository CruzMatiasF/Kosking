package com.kosking.app.feature.kiosco

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.ItemAnotado
import com.kosking.app.modelo.Producto
import com.kosking.app.scanner.PantallaConfirmar
import com.kosking.app.scanner.components.MovimientoExitosoScreen
import kotlinx.coroutines.launch

enum class ModoManual { VENDER, ANOTAR }

/** Estado inmutable de un carrito en la pantalla manual. */
private data class CarritoManual(
    val id: Int,
    val items: List<ItemAnotado> = emptyList(),
    val clienteSeleccionado: Cliente? = null,
    val busquedaCliente: String = "",
    val mostrarSugerencias: Boolean = false,
    val categoriaSeleccionada: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaManual(
    modo: ModoManual,  // valor inicial; el usuario puede cambiarlo dentro
    productos: List<Producto>,
    clientes: List<Cliente>,
    cargando: Boolean,
    onConfirmarVenta: (items: List<ItemAnotado>, onExito: () -> Unit) -> Unit,
    onConfirmarAnotacion: (clienteId: String, items: List<ItemAnotado>, onExito: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var modoActual by remember { mutableStateOf(modo) }
    var carritos by remember { mutableStateOf(listOf(CarritoManual(id = 1))) }
    var carritoActivoIdx by remember { mutableStateOf(0) }
    var mostrarConfirmar by remember { mutableStateOf(false) }
    var exitoItems by remember { mutableStateOf<List<ItemAnotado>>(emptyList()) }
    var exitoCliente by remember { mutableStateOf<Cliente?>(null) }
    var mostrarExito by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val carrito = carritos[carritoActivoIdx]

    fun updateCarrito(block: CarritoManual.() -> CarritoManual) {
        carritos = carritos.mapIndexed { i, c -> if (i == carritoActivoIdx) c.block() else c }
    }

    val categorias = remember(productos) {
        productos.map { it.categoria.ifBlank { "Otros" } }.distinct().sorted()
    }
    val productosFiltrados = remember(productos, carrito.categoriaSeleccionada) {
        productos
            .filter { carrito.categoriaSeleccionada == null || it.categoria.ifBlank { "Otros" } == carrito.categoriaSeleccionada }
            .sortedByDescending { it.vecesUsado }
    }
    val clientesFiltrados = remember(clientes, carrito.busquedaCliente) {
        if (carrito.busquedaCliente.isBlank()) emptyList()
        else clientes.filter { it.nombre.contains(carrito.busquedaCliente, ignoreCase = true) }.take(5)
    }

    fun agregar(prod: Producto) {
        val idx = carrito.items.indexOfFirst { it.productoId == prod.id }
        updateCarrito {
            if (idx >= 0)
                copy(items = items.mapIndexed { i, it -> if (i == idx) it.copy(cantidad = it.cantidad + 1) else it })
            else
                copy(items = items + ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id, 1))
        }
    }

    // ── Pantalla éxito ──
    if (mostrarExito) {
        MovimientoExitosoScreen(
            titulo = if (modoActual == ModoManual.VENDER) "Venta registrada"
                     else "Anotado para ${exitoCliente?.nombre ?: ""}",
            items = exitoItems,
            onInicio = onDismiss,
            onCerrar = onDismiss
        )
        return
    }

    // ── Pantalla confirmar ──
    if (mostrarConfirmar) {
        PantallaConfirmar(
            cliente = if (modoActual == ModoManual.ANOTAR) carrito.clienteSeleccionado else null,
            items = carrito.items,
            tipo = if (modoActual == ModoManual.VENDER) "venta" else "cargo",
            cargando = cargando,
            onConfirmar = {
                if (modoActual == ModoManual.VENDER) {
                    val itemsConfirmados = carrito.items
                    onConfirmarVenta(itemsConfirmados) {
                        mostrarConfirmar = false
                        if (carritos.size > 1) {
                            val idx = carritoActivoIdx
                            carritos = carritos.filterIndexed { i, _ -> i != idx }
                            carritoActivoIdx = minOf(idx, carritos.size - 1)
                            scope.launch { snackbarHostState.showSnackbar("Venta registrada", duration = SnackbarDuration.Short) }
                        } else {
                            exitoItems = itemsConfirmados
                            mostrarExito = true
                        }
                    }
                } else {
                    val cliente = carrito.clienteSeleccionado ?: return@PantallaConfirmar
                    val itemsConfirmados = carrito.items
                    onConfirmarAnotacion(cliente.id, itemsConfirmados) {
                        mostrarConfirmar = false
                        if (carritos.size > 1) {
                            val idx = carritoActivoIdx
                            carritos = carritos.filterIndexed { i, _ -> i != idx }
                            carritoActivoIdx = minOf(idx, carritos.size - 1)
                            scope.launch { snackbarHostState.showSnackbar("Anotado para ${cliente.nombre}", duration = SnackbarDuration.Short) }
                        } else {
                            exitoItems = itemsConfirmados
                            exitoCliente = cliente
                            mostrarExito = true
                        }
                    }
                }
            },
            onCancelar = { mostrarConfirmar = false }
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(if (modoActual == ModoManual.VENDER) "Vender" else "Anotar", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    actions = {
                        // Toggle Vender / Anotar
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            listOf(ModoManual.VENDER to "Vender", ModoManual.ANOTAR to "Anotar").forEach { (m, label) ->
                                val activo = modoActual == m
                                Surface(
                                    onClick = {
                                        if (modoActual != m) {
                                            modoActual = m
                                            // Al cambiar a VENDER limpiar el cliente seleccionado (irrelevante)
                                            if (m == ModoManual.VENDER) {
                                                updateCarrito { copy(clienteSeleccionado = null, busquedaCliente = "") }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = if (activo) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
                                            color = if (activo) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (carrito.items.isNotEmpty()) {
                            TextButton(onClick = { updateCarrito { copy(items = emptyList()) } }) {
                                Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                val total = carrito.items.sumOf { it.precio * it.cantidad }
                val puedeConfirmar = carrito.items.isNotEmpty() &&
                    (modoActual == ModoManual.VENDER || carrito.clienteSeleccionado != null)
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$${"%.0f".format(total)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (total > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { mostrarConfirmar = true },
                            enabled = puedeConfirmar,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                if (modoActual == ModoManual.VENDER) "Cobrar" else "Anotar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {

                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Selector de carritos ──
                    SelectorCarritoManual(
                        carritos = carritos,
                        activoIdx = carritoActivoIdx,
                        onSeleccionar = { carritoActivoIdx = it },
                        onAgregar = {
                            if (carritos.size < 3) {
                                val newId = (carritos.maxOf { it.id }) + 1
                                carritos = carritos + CarritoManual(id = newId)
                                carritoActivoIdx = carritos.size - 1
                            }
                        },
                        onCerrar = { idx ->
                            if (carritos.size > 1) {
                                carritos = carritos.filterIndexed { i, _ -> i != idx }
                                carritoActivoIdx = minOf(carritoActivoIdx, carritos.size - 1)
                            }
                        }
                    )

                    HorizontalDivider()

                    // ── Buscador de cliente (solo Anotar) ──
                    if (modoActual == ModoManual.ANOTAR) {
                        if (carrito.clienteSeleccionado == null) {
                            OutlinedTextField(
                                value = carrito.busquedaCliente,
                                onValueChange = { q ->
                                    updateCarrito { copy(busquedaCliente = q, mostrarSugerencias = q.isNotBlank()) }
                                },
                                placeholder = { Text("Buscar cliente…") },
                                leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (carrito.busquedaCliente.isNotBlank())
                                        IconButton(onClick = { updateCarrito { copy(busquedaCliente = "", mostrarSugerencias = false) } }) {
                                            Icon(Icons.Default.Clear, null, Modifier.size(16.dp))
                                        }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = MaterialTheme.shapes.medium
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    carrito.clienteSeleccionado!!.nombre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { updateCarrito { copy(clienteSeleccionado = null, busquedaCliente = "") } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    // ── Mitad superior: catálogo ──
                    Column(modifier = Modifier.weight(1f)) {

                        if (carrito.categoriaSeleccionada == null) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(categorias) { cat ->
                                    Card(
                                        onClick = { updateCarrito { copy(categoriaSeleccionada = cat) } },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                        modifier = Modifier.aspectRatio(1f)
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                cat,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputChip(
                                    selected = true,
                                    onClick = { updateCarrito { copy(categoriaSeleccionada = null) } },
                                    label = { Text(carrito.categoriaSeleccionada!!, style = MaterialTheme.typography.labelMedium) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                                )
                            }
                            androidx.compose.foundation.lazy.LazyRow(
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(productosFiltrados, key = { it.id }) { prod ->
                                    val cant = carrito.items.firstOrNull { it.productoId == prod.id }?.cantidad ?: 0
                                    ProductoManualCard(prod = prod, cantEnCarrito = cant, onClick = { agregar(prod) })
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // ── Mitad inferior: carrito (remito) ──
                    Column(modifier = Modifier.weight(1f)) {
                        val LINEAS_REMITO = 6
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Producto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text("Cant.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                            HorizontalDivider()

                            val totalLineas = maxOf(LINEAS_REMITO, carrito.items.size)
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(totalLineas) { i ->
                                    val item = carrito.items.getOrNull(i)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (item != null) {
                                            Text(
                                                item.nombre,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.width(36.dp),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                val itemIdx = carrito.items.indexOf(item)
                                                IconButton(
                                                    onClick = {
                                                        updateCarrito {
                                                            if (item.cantidad <= 1)
                                                                copy(items = items.filterIndexed { j, _ -> j != itemIdx })
                                                            else
                                                                copy(items = items.mapIndexed { j, it -> if (j == itemIdx) it.copy(cantidad = it.cantidad - 1) else it })
                                                        }
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        if (item.cantidad <= 1) Icons.Default.Delete else Icons.Default.Remove,
                                                        null, Modifier.size(12.dp),
                                                        tint = if (item.cantidad <= 1) MaterialTheme.colorScheme.error
                                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text("${item.cantidad}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 2.dp))
                                                IconButton(
                                                    onClick = {
                                                        val itemIdx2 = carrito.items.indexOf(item)
                                                        updateCarrito {
                                                            copy(items = items.mapIndexed { j, it -> if (j == itemIdx2) it.copy(cantidad = it.cantidad + 1) else it })
                                                        }
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) { Icon(Icons.Default.Add, null, Modifier.size(12.dp)) }
                                            }
                                            Text(
                                                "$${"%.0f".format(item.precio * item.cantidad)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.width(56.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }

                // ── Dropdown flotante de clientes ──
                if (carrito.mostrarSugerencias && clientesFiltrados.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = if (modoActual == ModoManual.ANOTAR) 68.dp else 0.dp)
                            .zIndex(10f)
                            .shadow(8.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 4.dp
                    ) {
                        Column {
                            clientesFiltrados.forEach { cliente ->
                                ListItem(
                                    headlineContent = { Text(cliente.nombre, fontWeight = FontWeight.Medium) },
                                    supportingContent = {
                                        if (cliente.deudaTotal > 0)
                                            Text("Deuda: $${"%.0f".format(cliente.deudaTotal)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    },
                                    modifier = Modifier.clickable {
                                        updateCarrito { copy(clienteSeleccionado = cliente, busquedaCliente = "", mostrarSugerencias = false) }
                                    }
                                )
                                if (cliente != clientesFiltrados.last()) HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorCarritoManual(
    carritos: List<CarritoManual>,
    activoIdx: Int,
    onSeleccionar: (Int) -> Unit,
    onAgregar: () -> Unit,
    onCerrar: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Siempre 3 slots fijos
        repeat(3) { idx ->
            Box(modifier = Modifier.weight(1f)) {
                when {
                    idx < carritos.size -> {
                        val c = carritos[idx]
                        val esActivo = idx == activoIdx
                        val cantItems = c.items.sumOf { it.cantidad }
                        Surface(
                            onClick = { onSeleccionar(idx) },
                            shape = RoundedCornerShape(50),
                            color = if (esActivo) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val label = c.clienteSeleccionado?.nombre?.split(" ")?.take(2)?.joinToString(" ")
                                    ?: "Cliente ${idx + 1}"
                                Text(label, style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (esActivo) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f))
                                if (cantItems > 0) {
                                    Text("·$cantItems", style = MaterialTheme.typography.labelSmall,
                                        color = if (esActivo) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                                if (esActivo && carritos.size > 1) {
                                    Icon(Icons.Default.Close, null,
                                        Modifier.size(12.dp).clickable { onCerrar(idx) },
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                    idx == carritos.size -> {
                        Surface(
                            onClick = onAgregar,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    else -> Spacer(modifier = Modifier.fillMaxWidth().height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductoManualCard(
    prod: Producto,
    cantEnCarrito: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cantEnCarrito > 0) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.width(110.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (prod.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(prod.imagenUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.ShoppingCart, null,
                        Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                if (cantEnCarrito > 0) {
                    Badge(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        Text("$cantEnCarrito")
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    prod.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (prod.ultimoPrecio > 0) "$${"%.0f".format(prod.ultimoPrecio)}" else "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (cantEnCarrito > 0) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
