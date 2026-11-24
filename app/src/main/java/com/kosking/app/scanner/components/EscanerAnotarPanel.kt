package com.kosking.app.scanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.ItemAnotado
import com.kosking.app.modelo.Producto

@Composable
internal fun EscanerAnotarPanel(
    itemsAnotados: List<ItemAnotado>,
    productos: List<Producto>,
    clientes: List<Cliente>,
    clienteSeleccionado: Cliente?,
    busquedaCliente: String,
    filtradosCliente: List<Cliente>,
    cargandoAnotacion: Boolean,
    onAgregarSugerencia: (ItemAnotado) -> Unit,
    onCambiarCantidad: (ItemAnotado, Int) -> Unit,
    onConfirmar: () -> Unit,
    onClienteSelected: (Cliente) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onLimpiarCliente: () -> Unit,
    onNuevoCliente: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    val categorias = remember(productos) {
        productos.map { it.categoria.ifBlank { "Otros" } }.distinct().sorted()
    }
    val productosFiltrados = remember(productos, categoriaSeleccionada) {
        val cat = categoriaSeleccionada
        if (cat == null) productos.sortedByDescending { it.vecesUsado }
        else productos.filter { (it.categoria.ifBlank { "Otros" }) == cat }.sortedByDescending { it.vecesUsado }
    }
    val sugerenciasCliente = remember(busquedaCliente, clientes, filtradosCliente) {
        if (busquedaCliente.isBlank()) clientes.take(4) else filtradosCliente.take(4)
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ── Selector de cliente ──
        ClientePicker(
            clienteSeleccionado = clienteSeleccionado,
            busquedaCliente = busquedaCliente,
            sugerencias = sugerenciasCliente,
            onBusquedaChange = onBusquedaChange,
            onClienteSelected = onClienteSelected,
            onLimpiarCliente = onLimpiarCliente,
            onNuevoCliente = onNuevoCliente
        )

        HorizontalDivider()

        // ── Catálogo ──
        if (productos.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Sin productos cargados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (categoriaSeleccionada == null && categorias.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (categorias.size > 1) {
                    IconButton(
                        onClick = { categoriaSeleccionada = null },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(end = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(productosFiltrados, key = { it.id }) { prod ->
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

        HorizontalDivider()

        // ── Lista de ítems ──
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (itemsAnotados.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Escaneá un producto para empezar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(itemsAnotados) { item ->
                    ListItem(
                        headlineContent = { Text(item.nombre, style = MaterialTheme.typography.bodyMedium) },
                        supportingContent = {
                            if (item.precio > 0) {
                                Text(
                                    "$${(item.precio * item.cantidad).toInt()}" +
                                        if (item.cantidad > 1) "  ·  x${item.cantidad}" else "",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    "sin precio" + if (item.cantidad > 1) "  ·  x${item.cantidad}" else "",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onCambiarCantidad(item, item.cantidad - 1) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                                }
                                Text("${item.cantidad}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                IconButton(onClick = { onCambiarCantidad(item, item.cantidad + 1) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }

        if (itemsAnotados.isNotEmpty()) {
            val total = itemsAnotados.sumOf { it.precio * it.cantidad }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onConfirmar,
                    enabled = !cargandoAnotacion && clienteSeleccionado != null,
                    modifier = Modifier.height(46.dp)
                ) {
                    if (cargandoAnotacion) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Continuar", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                if (total > 0) {
                    Text(
                        "$${"%.0f".format(total)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ClientePicker(
    clienteSeleccionado: Cliente?,
    busquedaCliente: String,
    sugerencias: List<Cliente>,
    onBusquedaChange: (String) -> Unit,
    onClienteSelected: (Cliente) -> Unit,
    onLimpiarCliente: () -> Unit,
    onNuevoCliente: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (clienteSeleccionado != null) {
            // Cliente seleccionado — chip compacto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Person, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    clienteSeleccionado.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Close, null,
                    Modifier.size(16.dp).clickable { onLimpiarCliente() },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Campo de búsqueda
            OutlinedTextField(
                value = busquedaCliente,
                onValueChange = onBusquedaChange,
                placeholder = { Text("Buscar cliente…", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) },
                trailingIcon = {
                    if (busquedaCliente.isNotBlank()) {
                        IconButton(onClick = { onBusquedaChange("") }) {
                            Icon(Icons.Default.Clear, null, Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .height(44.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )

            // Sugerencias inline
            if (sugerencias.isNotEmpty() || (busquedaCliente.isNotBlank())) {
                val mostrarCrear = busquedaCliente.isNotBlank() && sugerencias.isEmpty()
                LazyRow(
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (mostrarCrear) {
                        item {
                            SuggestionChip(
                                onClick = { onNuevoCliente(busquedaCliente.trim()) },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, null, Modifier.size(12.dp))
                                        Text("\"${busquedaCliente.trim()}\"", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            )
                        }
                    } else {
                        items(sugerencias) { c ->
                            SuggestionChip(
                                onClick = { onClienteSelected(c) },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(c.nombre.take(1).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)), color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                        Text(c.nombre, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
