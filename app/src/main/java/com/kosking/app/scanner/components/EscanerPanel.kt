package com.kosking.app.scanner.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.ItemAnotado
import com.kosking.app.modelo.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EscanerPanel(
    itemsAnotados: List<ItemAnotado>,
    productos: List<Producto>,
    clienteSeleccionado: Cliente?,
    busquedaCliente: String,
    filtradosCliente: List<Cliente>,
    cargandoAnotacion: Boolean,
    carritoIdx: Int = 0,
    onAgregarSugerencia: (ItemAnotado) -> Unit,
    onCambiarCantidad: (ItemAnotado, Int) -> Unit,
    onCobrar: () -> Unit,
    onAnotar: () -> Unit,
    onClienteSelected: (Cliente) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onLimpiarCliente: () -> Unit,
    onNuevoCliente: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }
    var busquedaProducto by remember { mutableStateOf("") }

    LaunchedEffect(carritoIdx) {
        busquedaProducto = ""
        categoriaSeleccionada = null
    }
    var montoRapido by remember { mutableStateOf("") }
    var mostrarAlertaCliente by remember { mutableStateOf(false) }

    val categorias = remember(productos) {
        productos.map { it.categoria.ifBlank { "Otros" } }.distinct().sorted()
    }

    val productosMostrados = remember(productos, categoriaSeleccionada, busquedaProducto) {
        if (busquedaProducto.isNotBlank()) {
            productos.filter { it.nombre.contains(busquedaProducto, ignoreCase = true) }
                .sortedByDescending { it.vecesUsado }
        } else {
            val cat = categoriaSeleccionada
            if (cat == null) productos.sortedByDescending { it.vecesUsado }
            else productos.filter { (it.categoria.ifBlank { "Otros" }) == cat }
                .sortedByDescending { it.vecesUsado }
        }
    }

    val total = itemsAnotados.sumOf { it.precio * it.cantidad }
    val totalCantidad = itemsAnotados.sumOf { it.cantidad }
    val tieneItems = itemsAnotados.isNotEmpty()
    val tieneCliente = clienteSeleccionado != null
    val buscandoProducto = busquedaProducto.isNotBlank()
    val montoRapidoDouble = montoRapido.toLongOrNull()?.toDouble() ?: 0.0
    val montoRapidoValido = !tieneItems && montoRapidoDouble > 0

    LaunchedEffect(busquedaProducto) {
        if (buscandoProducto) categoriaSeleccionada = null
    }

    // Reset monto al cambiar de modo (p.ej. después de una venta)
    LaunchedEffect(tieneItems) {
        if (!tieneItems) montoRapido = ""
    }

    if (mostrarAlertaCliente && clienteSeleccionado != null) {
        val cliente = clienteSeleccionado
        AlertDialog(
            onDismissRequest = { mostrarAlertaCliente = false },
            title = {
                Text(if (!cliente.habilitado) "Cliente bloqueado" else "Atención: ${cliente.nombre}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (!cliente.habilitado) "${cliente.nombre} está bloqueado por el dueño."
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
                    mostrarAlertaCliente = false
                    if (montoRapidoValido) {
                        onAgregarSugerencia(ItemAnotado("Venta", montoRapidoDouble, null))
                        montoRapido = ""
                    }
                    onAnotar()
                }) {
                    Text("Anotar igual", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAlertaCliente = false }) { Text("Cancelar") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {

        // ── Buscador de producto ──
        OutlinedTextField(
            value = busquedaProducto,
            onValueChange = { busquedaProducto = it },
            placeholder = {
                Text(
                    if (productos.isEmpty()) "Sin productos cargados"
                    else "Buscar producto…",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
            trailingIcon = {
                if (buscandoProducto) {
                    IconButton(onClick = { busquedaProducto = "" }) {
                        Icon(Icons.Default.Clear, null, Modifier.size(14.dp))
                    }
                }
            },
            enabled = productos.isNotEmpty(),
            singleLine = true,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .height(42.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )

        // ── Chips de categoría / fila de productos (solo cuando NO se busca) ──
        if (!buscandoProducto && productos.isNotEmpty()) {
            if (categoriaSeleccionada == null && categorias.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categorias) { cat ->
                        ScannerCategoriaCard(
                            nombre = cat,
                            imagenUrl = primeraImagenCategoria(productos, cat),
                            onClick = { categoriaSeleccionada = cat }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (categorias.size > 1) {
                        IconButton(
                            onClick = { categoriaSeleccionada = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                                Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(end = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(productosMostrados, key = { it.id }) { prod ->
                            val cant = itemsAnotados.firstOrNull { it.productoId == prod.id }?.cantidad ?: 0
                            ScannerProductoCard(
                                prod = prod,
                                cantEnCarrito = cant,
                                onClick = { onAgregarSugerencia(ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id)) }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Área central: búsqueda vertical / cobro rápido / carrito ──
        Box(modifier = Modifier.weight(1f)) {

            when {
                buscandoProducto -> {
                    // ── Resultados de búsqueda vertical ──
                    if (productosMostrados.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.SearchOff, null,
                                    Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Text(
                                    "Sin resultados para \"$busquedaProducto\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(productosMostrados, key = { it.id }) { prod ->
                                val cantEnCarrito = itemsAnotados.firstOrNull { it.productoId == prod.id }?.cantidad ?: 0
                                BuscadorProductoRow(
                                    prod = prod,
                                    cantEnCarrito = cantEnCarrito,
                                    onClick = {
                                        onAgregarSugerencia(ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id))
                                        busquedaProducto = ""
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                !tieneItems -> {
                    // ── Cobro rápido (carrito vacío) ──
                    CobroRapidoSection(
                        monto = montoRapido,
                        onMontoChange = { montoRapido = it },
                        onAgregarAtajo = { delta ->
                            val actual = montoRapido.toLongOrNull() ?: 0L
                            montoRapido = (actual + delta).toString()
                        }
                    )
                }

                else -> {
                    // ── Lista del carrito ──
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(itemsAnotados) { item ->
                            val presentacion = productos.firstOrNull { it.id == item.productoId }?.presentacion ?: ""
                            ItemRemitoRow(item = item, presentacion = presentacion, onCambiarCantidad = onCambiarCantidad)
                            HorizontalDivider()
                        }
                    }

                    // Sugerencias de cliente superpuestas
                    if (!tieneCliente && busquedaCliente.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column {
                                filtradosCliente.take(5).forEach { cliente ->
                                    ListItem(
                                        headlineContent = { Text(cliente.nombre, style = MaterialTheme.typography.bodyMedium) },
                                        supportingContent = if (cliente.telefono.isNotBlank()) {
                                            { Text(cliente.telefono, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        } else null,
                                        modifier = Modifier.clickable { onClienteSelected(cliente) }
                                    )
                                    HorizontalDivider()
                                }
                                val nombreBusqueda = busquedaCliente.trim()
                                val yaExiste = filtradosCliente.any { it.nombre.equals(nombreBusqueda, ignoreCase = true) }
                                if (nombreBusqueda.length >= 2 && !yaExiste && onNuevoCliente != null) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                "Crear \"$nombreBusqueda\"",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        leadingContent = {
                                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        },
                                        modifier = Modifier.clickable { onNuevoCliente(nombreBusqueda) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Selector de cliente (visible cuando hay ítems O hay monto rápido) ──
        if (tieneItems || montoRapidoValido) {
            Column {
                if (!tieneCliente) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Person, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                "Agregá un cliente para anotar en cuenta",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PanelClientePicker(
                        clienteSeleccionado = clienteSeleccionado,
                        busquedaCliente = busquedaCliente,
                        onBusquedaChange = onBusquedaChange,
                        onLimpiarCliente = onLimpiarCliente,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── Botonera ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (tieneItems || montoRapidoValido) {
                Button(
                    onClick = {
                        val clienteAlertado = clienteSeleccionado != null &&
                            (!clienteSeleccionado.habilitado || clienteSeleccionado.alertaEmpleado)
                        if (clienteAlertado) {
                            mostrarAlertaCliente = true
                        } else {
                            if (montoRapidoValido) {
                                onAgregarSugerencia(ItemAnotado("Venta", montoRapidoDouble, null))
                                montoRapido = ""
                            }
                            onAnotar()
                        }
                    },
                    enabled = !cargandoAnotacion && tieneCliente,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (cargandoAnotacion) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (tieneCliente) "Anotar a ${clienteSeleccionado!!.nombre.split(" ").first()}"
                            else "Anotar en cuenta (elegí cliente)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            if (tieneItems || montoRapidoValido) {
                OutlinedButton(
                    onClick = {
                        if (montoRapidoValido) {
                            onAgregarSugerencia(ItemAnotado("Venta", montoRapidoDouble, null))
                            montoRapido = ""
                        }
                        onCobrar()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        when {
                            montoRapidoValido -> "Cobrar \$${montoRapidoDouble.toLong()}"
                            total > 0 && totalCantidad > 1 -> "Cobrar \$${total.toInt()}  ·  $totalCantidad productos"
                            else -> "Cobrar \$${total.toInt()}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** Área de cobro rápido por monto, mostrada cuando el carrito está vacío. */
@Composable
private fun CobroRapidoSection(
    monto: String,
    onMontoChange: (String) -> Unit,
    onAgregarAtajo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = monto,
            onValueChange = { new ->
                onMontoChange(new.filter { it.isDigit() }.take(7))
            },
            placeholder = {
                Text(
                    "0",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            },
            prefix = {
                Text(
                    "$",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Light,
                    color = if (monto.isNotBlank()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            },
            trailingIcon = {
                if (monto.isNotBlank()) {
                    IconButton(onClick = { onMontoChange("") }) {
                        Icon(Icons.Default.Clear, null, Modifier.size(20.dp))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(500L to "+$500", 1000L to "+$1k", 2000L to "+$2k", 5000L to "+$5k").forEach { (valor, label) ->
                SuggestionChip(
                    onClick = { onAgregarAtajo(valor) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
}

/** Fila de resultado en la búsqueda vertical de productos. */
@Composable
private fun BuscadorProductoRow(
    prod: Producto,
    cantEnCarrito: Int,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                prod.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val partes = buildList {
                if (prod.presentacion.isNotBlank()) add(prod.presentacion)
                if (prod.ultimoPrecio > 0) add("\$${prod.ultimoPrecio.toInt()}")
            }
            if (partes.isNotEmpty()) {
                Text(
                    partes.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            if (cantEnCarrito > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "×$cantEnCarrito",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.Add, null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun PanelClientePicker(
    clienteSeleccionado: Cliente?,
    busquedaCliente: String,
    onBusquedaChange: (String) -> Unit,
    onLimpiarCliente: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (clienteSeleccionado != null) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(50))
                .height(42.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Person, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                clienteSeleccionado.nombre,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Close, null,
                Modifier.size(14.dp).clickable { onLimpiarCliente() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        OutlinedTextField(
            value = busquedaCliente,
            onValueChange = onBusquedaChange,
            placeholder = { Text("Cliente…", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(14.dp)) },
            trailingIcon = {
                if (busquedaCliente.isNotBlank()) {
                    IconButton(onClick = { onBusquedaChange("") }) {
                        Icon(Icons.Default.Clear, null, Modifier.size(13.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            modifier = modifier.height(42.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}
