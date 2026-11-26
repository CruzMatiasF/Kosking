package com.kosking.app.scanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    onAgregarSugerencia: (ItemAnotado) -> Unit,
    onCambiarCantidad: (ItemAnotado, Int) -> Unit,
    tipoMovimiento: com.kosking.app.scanner.TipoMovimiento,
    onCobrar: () -> Unit,
    onAnotar: () -> Unit,
    onCargarNuevo: () -> Unit,
    onClienteSelected: (Cliente) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onLimpiarCliente: () -> Unit,
    modifier: Modifier = Modifier
) {
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }
    var mostrarAlertaCliente by remember { mutableStateOf(false) }

    if (mostrarAlertaCliente && clienteSeleccionado != null) {
        val cliente = clienteSeleccionado
        AlertDialog(
            onDismissRequest = { mostrarAlertaCliente = false },
            title = { Text(if (!cliente.habilitado) "Cliente bloqueado" else "Atención: ${cliente.nombre}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (!cliente.habilitado) "${cliente.nombre} está bloqueado por el dueño."
                        else "El dueño pidió que lo avises antes de anotar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (cliente.notaInterna.isNotBlank()) {
                        Text("\"${cliente.notaInterna}\"", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarAlertaCliente = false; onAnotar() }) {
                    Text("Anotar igual", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAlertaCliente = false }) { Text("Cancelar") }
            }
        )
    }

    val categorias = remember(productos) {
        productos.map { it.categoria.ifBlank { "Otros" } }.distinct().sorted()
    }

    var busquedaCategoria by remember { mutableStateOf("") }
    LaunchedEffect(categoriaSeleccionada) { busquedaCategoria = "" }

    val productosMostrados = remember(productos, categoriaSeleccionada, busquedaCategoria) {
        val cat = categoriaSeleccionada
        val base = if (cat == null) productos.sortedByDescending { it.vecesUsado }
                   else productos.filter { (it.categoria.ifBlank { "Otros" }) == cat }
                       .sortedByDescending { it.vecesUsado }
        if (busquedaCategoria.isBlank()) base
        else base.filter { prod ->
            prod.nombre.split(" ").any { it.startsWith(busquedaCategoria, ignoreCase = true) }
        }
    }

    val total = itemsAnotados.sumOf { it.precio * it.cantidad }
    val totalCantidad = itemsAnotados.sumOf { it.cantidad }
    val tieneItems = itemsAnotados.isNotEmpty()
    val tieneCliente = clienteSeleccionado != null

    Box(modifier = modifier.fillMaxSize()) {

        // ── Contenido principal (categorías + carrito) ──
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Chips de categoría / fila de productos ──
            if (productos.isNotEmpty()) {
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
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            Text(
                                categoriaSeleccionada ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Search, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                BasicTextField(
                                    value = busquedaCategoria,
                                    onValueChange = { busquedaCategoria = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                    decorationBox = { inner ->
                                        Box {
                                            if (busquedaCategoria.isEmpty()) Text("Buscar...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            inner()
                                        }
                                    }
                                )
                                if (busquedaCategoria.isNotEmpty()) {
                                    Icon(Icons.Default.Close, "Limpiar", Modifier.size(14.dp).clickable { busquedaCategoria = "" }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(start = 4.dp, end = 10.dp, bottom = 4.dp),
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

            // ── Área central: carrito / empty state ──
            Box(modifier = Modifier.weight(1f)) {

                when {
                    !tieneItems -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner, null,
                                    Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Text(
                                    "Escaneá un producto o elegilo de arriba",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
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

                    }
                }
            }

            HorizontalDivider()

            // Espacio reservado para la barra inferior fija
            Spacer(modifier = Modifier.height(56.dp))
        }

        // ── Sugerencias de cliente (flotando sobre la barra) ──
        if (!tieneCliente && busquedaCliente.isNotBlank()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 10.dp, end = 10.dp, bottom = 56.dp)
                    .fillMaxWidth(),
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
                }
            }
        }

        // ── Barra inferior fija: [cliente] [Continuar] [total] — tercios iguales ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tercio izquierdo — picker cliente (solo en modo Anotar)
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (tipoMovimiento == com.kosking.app.scanner.TipoMovimiento.ANOTAR) {
                    PanelClientePicker(
                        clienteSeleccionado = clienteSeleccionado,
                        busquedaCliente = busquedaCliente,
                        onBusquedaChange = onBusquedaChange,
                        onLimpiarCliente = onLimpiarCliente,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            // Tercio central — botón Continuar
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        when (tipoMovimiento) {
                            com.kosking.app.scanner.TipoMovimiento.ANOTAR -> {
                                val alerta = clienteSeleccionado != null &&
                                    (!clienteSeleccionado.habilitado || clienteSeleccionado.notaInterna.isNotBlank())
                                if (alerta) mostrarAlertaCliente = true else onAnotar()
                            }
                            com.kosking.app.scanner.TipoMovimiento.CARGAR_NUEVO -> onCargarNuevo()
                        }
                    },
                    enabled = !cargandoAnotacion && when (tipoMovimiento) {
                        com.kosking.app.scanner.TipoMovimiento.ANOTAR -> tieneCliente
                        com.kosking.app.scanner.TipoMovimiento.CARGAR_NUEVO -> true
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    if (cargandoAnotacion) {
                        CircularProgressIndicator(Modifier.size(14.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Continuar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            // Tercio derecho — total
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (total > 0) "\$${total.toInt()}" else "-",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (tieneItems) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
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
                .height(42.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Person, null, Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                clienteSeleccionado.nombre,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Close, null,
                Modifier.size(16.dp).clickable { onLimpiarCliente() },
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    } else {
        Row(
            modifier = modifier
                .height(42.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Person, null, Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = busquedaCliente,
                onValueChange = onBusquedaChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { inner ->
                    Box {
                        if (busquedaCliente.isEmpty()) Text(
                            "Cliente…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        inner()
                    }
                }
            )
            if (busquedaCliente.isNotEmpty()) {
                Icon(
                    Icons.Default.Close, "Limpiar",
                    Modifier.size(14.dp).clickable { onBusquedaChange("") },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
