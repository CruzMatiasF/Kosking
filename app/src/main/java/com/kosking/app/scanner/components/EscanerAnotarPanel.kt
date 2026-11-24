package com.kosking.app.scanner.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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

    Column(modifier = modifier.fillMaxSize().imePadding()) {

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

        // ── Lista de ítems + sugerencias de cliente ──
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (itemsAnotados.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Escaneá un producto para empezar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(itemsAnotados) { item ->
                        val presentacion = productos.firstOrNull { it.id == item.productoId }?.presentacion ?: ""
                        ItemRemitoRow(item = item, presentacion = presentacion, onCambiarCantidad = onCambiarCantidad)
                        HorizontalDivider()
                    }
                }
            }
            // Sugerencias de cliente superpuestas al final de la lista
            if (clienteSeleccionado == null && busquedaCliente.isNotBlank() && filtradosCliente.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column {
                        filtradosCliente.take(6).forEach { cliente ->
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
        }

        HorizontalDivider()

        // ── Barra inferior: cliente + continuar + total ──
        val total = itemsAnotados.sumOf { it.precio * it.cantidad }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClientePicker(
                clienteSeleccionado = clienteSeleccionado,
                busquedaCliente = busquedaCliente,
                onBusquedaChange = onBusquedaChange,
                onLimpiarCliente = onLimpiarCliente,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onConfirmar,
                enabled = !cargandoAnotacion && clienteSeleccionado != null && itemsAnotados.isNotEmpty(),
                modifier = Modifier.height(42.dp)
            ) {
                if (cargandoAnotacion) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Continuar", fontWeight = FontWeight.Bold)
            }
            Text(
                if (total > 0) "$${"%.0f".format(total)}" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.widthIn(min = 48.dp)
            )
        }
    }
}

@Composable
private fun ClientePicker(
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
