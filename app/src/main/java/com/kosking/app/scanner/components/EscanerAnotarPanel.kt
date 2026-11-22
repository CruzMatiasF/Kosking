package com.kosking.app.scanner.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.ItemAnotado
import com.kosking.app.modelo.Producto

@Composable
internal fun EscanerAnotarPanel(
    itemsAnotados: List<ItemAnotado>,
    queryBusqueda: String,
    productos: List<Producto>,
    clienteSeleccionado: Cliente?,
    cargandoAnotacion: Boolean,
    onQueryChange: (String) -> Unit,
    onAgregarSugerencia: (ItemAnotado) -> Unit,
    onAgregarManual: (String) -> Unit,
    onCambiarCantidad: (ItemAnotado, Int) -> Unit,
    onConfirmar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    val categorias = remember(productos) {
        productos.map { it.categoria.ifBlank { "Otros" } }.distinct().sorted()
    }
    val productosFiltrados = remember(productos, categoriaSeleccionada) {
        if (categoriaSeleccionada == null) productos
        else productos.filter { (it.categoria.ifBlank { "Otros" }) == categoriaSeleccionada }
    }

    Column(modifier = modifier.fillMaxSize()) {

        if (productos.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Sin productos cargados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            if (categoriaSeleccionada == null && categorias.size > 1) {
                // Solo categorías
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categorias.forEach { cat ->
                        ElevatedCard(onClick = { categoriaSeleccionada = cat }) {
                            Text(
                                cat,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                // Flecha fija + productos scrolleables
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (categorias.size > 1) {
                        IconButton(
                            onClick = { categoriaSeleccionada = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, "Volver a categorías", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .padding(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        productosFiltrados.forEach { prod ->
                            ElevatedCard(
                                onClick = { onAgregarSugerencia(ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id)) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(prod.nombre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1)
                                    if (prod.ultimoPrecio > 0) Text(
                                        "$${prod.ultimoPrecio.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (itemsAnotados.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Escaneá un producto para empezar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                IconButton(
                                    onClick = { onCambiarCantidad(item, item.cantidad - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
                                Text("${item.cantidad}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                IconButton(
                                    onClick = { onCambiarCantidad(item, item.cantidad + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }

        if (itemsAnotados.isNotEmpty()) {
            val total = itemsAnotados.sumOf { it.precio * it.cantidad }
            Button(
                onClick = onConfirmar,
                enabled = !cargandoAnotacion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .navigationBarsPadding()
                    .height(48.dp)
            ) {
                if (cargandoAnotacion) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        if (clienteSeleccionado != null) "Confirmar para ${clienteSeleccionado.nombre}  ·  $${total.toInt()}"
                        else "Elegir cliente  ·  $${total.toInt()}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
