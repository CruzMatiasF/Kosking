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
import com.kosking.app.scanner.PantallaConfirmarAnotacion
import com.kosking.app.scanner.components.MovimientoExitosoScreen

enum class ModoManual { VENDER, ANOTAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaManual(
    modo: ModoManual,
    productos: List<Producto>,
    clientes: List<Cliente>,
    cargando: Boolean,
    onConfirmarVenta: (items: List<ItemAnotado>, onExito: () -> Unit) -> Unit,
    onConfirmarAnotacion: (clienteId: String, items: List<ItemAnotado>, onExito: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val carrito = remember { mutableStateListOf<ItemAnotado>() }
    var clienteSeleccionado by remember { mutableStateOf<Cliente?>(null) }
    var busquedaCliente by remember { mutableStateOf("") }
    var mostrarSugerenciasCliente by remember { mutableStateOf(false) }
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }
    var mostrarConfirmar by remember { mutableStateOf(false) }
    var exitoItems by remember { mutableStateOf<List<ItemAnotado>>(emptyList()) }
    var exitoCliente by remember { mutableStateOf<Cliente?>(null) }
    var mostrarExito by remember { mutableStateOf(false) }

    val categorias = remember(productos) {
        productos.map { it.categoria.ifBlank { "Otros" } }.distinct().sorted()
    }
    val productosFiltrados = remember(productos, categoriaSeleccionada) {
        productos
            .filter { categoriaSeleccionada == null || it.categoria.ifBlank { "Otros" } == categoriaSeleccionada }
            .sortedByDescending { it.vecesUsado }
    }
    val clientesFiltrados = remember(clientes, busquedaCliente) {
        if (busquedaCliente.isBlank()) emptyList()
        else clientes.filter { it.nombre.contains(busquedaCliente, ignoreCase = true) }.take(5)
    }

    fun agregar(prod: Producto) {
        val idx = carrito.indexOfFirst { it.productoId == prod.id }
        if (idx >= 0) carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad + 1)
        else carrito.add(ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id, 1))
    }

    // ── Pantalla éxito ──
    if (mostrarExito) {
        MovimientoExitosoScreen(
            titulo = if (modo == ModoManual.VENDER) "Venta registrada"
                     else "Anotado para ${exitoCliente?.nombre ?: ""}",
            items = exitoItems,
            onInicio = onDismiss,
            onCerrar = onDismiss
        )
        return
    }

    // ── Pantalla confirmar ──
    if (mostrarConfirmar) {
        PantallaConfirmarAnotacion(
            cliente = if (modo == ModoManual.ANOTAR) clienteSeleccionado else null,
            items = carrito.toList(),
            tipo = if (modo == ModoManual.VENDER) "venta" else "cargo",
            cargando = cargando,
            onConfirmar = {
                if (modo == ModoManual.VENDER) {
                    val itemsConfirmados = carrito.toList()
                    onConfirmarVenta(itemsConfirmados) {
                        exitoItems = itemsConfirmados
                        mostrarConfirmar = false
                        mostrarExito = true
                    }
                } else {
                    val cliente = clienteSeleccionado ?: return@PantallaConfirmarAnotacion
                    val itemsConfirmados = carrito.toList()
                    onConfirmarAnotacion(cliente.id, itemsConfirmados) {
                        exitoItems = itemsConfirmados
                        exitoCliente = cliente
                        mostrarConfirmar = false
                        mostrarExito = true
                    }
                }
            },
            onVolver = { mostrarConfirmar = false }
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                TopAppBar(
                    title = { Text(if (modo == ModoManual.VENDER) "Vender" else "Anotar", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    actions = {
                        if (carrito.isNotEmpty()) {
                            TextButton(onClick = { carrito.clear() }) {
                                Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                val total = carrito.sumOf { it.precio * it.cantidad }
                val puedeConfirmar = carrito.isNotEmpty() &&
                    (modo == ModoManual.VENDER || clienteSeleccionado != null)
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
                                if (modo == ModoManual.VENDER) "Cobrar" else "Anotar",
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

                    // ── Buscador de cliente (solo Anotar) ──
                    if (modo == ModoManual.ANOTAR) {
                        if (clienteSeleccionado == null) {
                            OutlinedTextField(
                                value = busquedaCliente,
                                onValueChange = {
                                    busquedaCliente = it
                                    mostrarSugerenciasCliente = it.isNotBlank()
                                },
                                placeholder = { Text("Buscar cliente…") },
                                leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (busquedaCliente.isNotBlank()) IconButton(onClick = { busquedaCliente = ""; mostrarSugerenciasCliente = false }) {
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
                                    clienteSeleccionado!!.nombre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { clienteSeleccionado = null; busquedaCliente = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    // ── Mitad superior: catálogo ──
                    Column(modifier = Modifier.weight(1f)) {

                        if (categoriaSeleccionada == null) {
                            // Cards grandes de categoría
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(categorias) { cat ->
                                    Card(
                                        onClick = { categoriaSeleccionada = cat },
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
                            // Chip compacto con la categoría activa + X para volver
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputChip(
                                    selected = true,
                                    onClick = { categoriaSeleccionada = null },
                                    label = { Text(categoriaSeleccionada!!, style = MaterialTheme.typography.labelMedium) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                                )
                            }
                            androidx.compose.foundation.lazy.LazyRow(
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(productosFiltrados, key = { it.id }) { prod ->
                                    val cant = carrito.firstOrNull { it.productoId == prod.id }?.cantidad ?: 0
                                    ProductoManualCard(prod = prod, cantEnCarrito = cant, onClick = { agregar(prod) })
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // ── Mitad inferior: carrito ──
                    Column(modifier = Modifier.weight(1f)) {
                        // Remito — siempre muestra N líneas, los productos las van llenando
                        val LINEAS_REMITO = 6
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Encabezado
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

                            val totalLineas = maxOf(LINEAS_REMITO, carrito.size)
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(totalLineas) { i ->
                                    val item = carrito.getOrNull(i)
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
                                                IconButton(
                                                    onClick = {
                                                        val idx = carrito.indexOf(item)
                                                        if (item.cantidad <= 1) carrito.removeAt(idx)
                                                        else carrito[idx] = item.copy(cantidad = item.cantidad - 1)
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) { Icon(if (item.cantidad <= 1) Icons.Default.Delete else Icons.Default.Remove, null, Modifier.size(12.dp), tint = if (item.cantidad <= 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                                                Text("${item.cantidad}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 2.dp))
                                                IconButton(
                                                    onClick = { val idx = carrito.indexOf(item); carrito[idx] = item.copy(cantidad = item.cantidad + 1) },
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
                                        // línea vacía — no muestra nada, solo el espacio y el divider
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }

                // ── Dropdown flotante de clientes (zIndex: flota sobre todo) ──
                if (mostrarSugerenciasCliente && clientesFiltrados.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = if (modo == ModoManual.ANOTAR) 68.dp else 0.dp)
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
                                        clienteSeleccionado = cliente
                                        busquedaCliente = ""
                                        mostrarSugerenciasCliente = false
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
