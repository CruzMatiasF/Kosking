package com.kosking.app.scanner.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.ItemAnotado
import com.kosking.app.modelo.Producto

@Composable
internal fun EscanerVenderPanel(
    itemsVenta: List<ItemAnotado>,
    productos: List<Producto>,
    cargando: Boolean,
    onAgregarProducto: (ItemAnotado) -> Unit,
    onCambiarCantidad: (ItemAnotado, Int) -> Unit,
    onCobrar: () -> Unit,
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

    Column(modifier = modifier.fillMaxSize()) {

        if (productos.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Sin productos cargados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (categoriaSeleccionada == null && categorias.size > 1) {
            // Fila de categorías con imagen
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
            // Fila de productos con imagen
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
                        val cant = itemsVenta.firstOrNull { it.productoId == prod.id }?.cantidad ?: 0
                        ScannerProductoCard(
                            prod = prod,
                            cantEnCarrito = cant,
                            onClick = { onAgregarProducto(ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id)) }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (itemsVenta.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Escaneá o elegí un producto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(itemsVenta) { item ->
                    val presentacion = productos.firstOrNull { it.id == item.productoId }?.presentacion ?: ""
                    ItemRemitoRow(item = item, presentacion = presentacion, onCambiarCantidad = onCambiarCantidad)
                    HorizontalDivider()
                }
            }
        }

        if (itemsVenta.isNotEmpty()) {
            val total = itemsVenta.sumOf { it.precio * it.cantidad }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCobrar,
                    enabled = !cargando,
                    modifier = Modifier.height(46.dp)
                ) {
                    if (cargando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Cobrar", fontWeight = FontWeight.Bold)
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
