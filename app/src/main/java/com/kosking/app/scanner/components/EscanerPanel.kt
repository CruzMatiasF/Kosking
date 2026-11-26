package com.kosking.app.scanner.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    onCobrar: () -> Unit,
    onClienteSelected: (Cliente) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onLimpiarCliente: () -> Unit,
    modifier: Modifier = Modifier
) {
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    val categorias = remember(productos) {
        productos.map { it.categoria.ifBlank { "Otros" } }.distinct().sorted()
    }

    val productosMostrados = remember(productos, categoriaSeleccionada) {
        val cat = categoriaSeleccionada
        if (cat == null) productos.sortedByDescending { it.vecesUsado }
        else productos.filter { (it.categoria.ifBlank { "Otros" }) == cat }
            .sortedByDescending { it.vecesUsado }
    }

    val total = itemsAnotados.sumOf { it.precio * it.cantidad }
    val totalCantidad = itemsAnotados.sumOf { it.cantidad }
    val tieneItems = itemsAnotados.isNotEmpty()
    val tieneCliente = clienteSeleccionado != null

    Column(modifier = modifier.fillMaxSize().imePadding()) {

        // ── Selector de cliente (siempre visible arriba) ──
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

        // Sugerencias de cliente
        if (!tieneCliente && busquedaCliente.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
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

        HorizontalDivider()

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

        // ── Botonera ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (tieneItems) {
                OutlinedButton(
                    onClick = { onCobrar() },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        when {
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
