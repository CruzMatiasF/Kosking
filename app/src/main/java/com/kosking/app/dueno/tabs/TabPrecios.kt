package com.kosking.app.dueno.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TabPrecios(
    productos: List<Producto>,
    onAgregar: (nombre: String, precio: Double, categoria: String, descripcion: String, precioPublico: Boolean) -> Unit,
    onActualizar: (Producto) -> Unit,
    onEliminar: (String) -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    var mostrarDialog by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }

    val productosFiltrados = remember(productos, busqueda) {
        if (busqueda.isBlank()) productos
        else productos.filter {
            it.nombre.contains(busqueda, ignoreCase = true) ||
            it.categoria.contains(busqueda, ignoreCase = true)
        }
    }

    val porCategoria = remember(productosFiltrados) {
        productosFiltrados
            .sortedBy { it.nombre }
            .groupBy { it.categoria.ifBlank { "Sin categoría" } }
            .toSortedMap()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Buscador
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box {
                            if (busqueda.isEmpty()) Text("Buscar producto...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    }
                )
                if (busqueda.isNotEmpty()) {
                    Icon(Icons.Default.Close, "Limpiar",
                        Modifier.size(18.dp).clickable { busqueda = "" },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (productosFiltrados.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Storefront, null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (productos.isEmpty()) "Todavía no cargaste productos\nTocá + para agregar el primero"
                            else "Sin resultados",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    porCategoria.forEach { (categoria, items) ->
                        item(key = "cat_$categoria") {
                            Text(
                                categoria,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                        items(items, key = { it.id }) { producto ->
                            ProductoItemDueno(
                                producto = producto,
                                onEditar = { productoEditar = producto; mostrarDialog = true },
                                onEliminar = { onEliminar(producto.id) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { productoEditar = null; mostrarDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Agregar producto")
        }
    }

    if (mostrarDialog) {
        DialogProducto(
            producto = productoEditar,
            onConfirmar = { nombre, precio, categoria, descripcion, esOferta, precioAnterior, precioPublico, agotado ->
                val p = productoEditar
                if (p == null) {
                    onAgregar(nombre, precio, categoria, descripcion, precioPublico)
                } else {
                    onActualizar(p.copy(
                        nombre = nombre,
                        precio = precio,
                        categoria = categoria,
                        descripcion = descripcion,
                        esOferta = esOferta,
                        precioAnterior = precioAnterior,
                        precioPublico = precioPublico,
                        agotado = agotado
                    ))
                }
                mostrarDialog = false
            },
            onDismiss = { mostrarDialog = false }
        )
    }
}

@Composable
private fun ProductoItemDueno(
    producto: Producto,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    var mostrarConfirmarEliminar by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(producto.nombre, fontWeight = FontWeight.Medium)
                if (!producto.habilitado) {
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("Oculto", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (producto.agotado) {
                    Surface(shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("AGOTADO", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
                if (producto.esOferta) {
                    Surface(shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer) {
                        Text("OFERTA", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
        },
        supportingContent = if (producto.descripcion.isNotBlank()) ({
            Text(producto.descripcion, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }) else null,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$${"%.0f".format(producto.precio)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (producto.esOferta) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (producto.esOferta && producto.precioAnterior > 0) {
                        Text("$${"%.0f".format(producto.precioAnterior)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough)
                    }
                }
                IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Editar", Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { mostrarConfirmarEliminar = true },
                    modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Eliminar", Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )

    if (mostrarConfirmarEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarEliminar = false },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminás \"${producto.nombre}\"? No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { mostrarConfirmarEliminar = false; onEliminar() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogProducto(
    producto: Producto?,
    onConfirmar: (nombre: String, precio: Double, categoria: String, descripcion: String, esOferta: Boolean, precioAnterior: Double, precioPublico: Boolean, agotado: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember(producto) { mutableStateOf(producto?.nombre ?: "") }
    var precioTexto by remember(producto) { mutableStateOf(if (producto != null) "%.0f".format(producto.precio) else "") }
    var categoria by remember(producto) { mutableStateOf(producto?.categoria ?: "") }
    var descripcion by remember(producto) { mutableStateOf(producto?.descripcion ?: "") }
    var esOferta by remember(producto) { mutableStateOf(producto?.esOferta ?: false) }
    var precioPublico by remember(producto) { mutableStateOf(producto?.precioPublico ?: false) }
    var agotado by remember(producto) { mutableStateOf(producto?.agotado ?: false) }
    var precioAnteriorTexto by remember(producto) { mutableStateOf(if (producto?.precioAnterior != null && producto.precioAnterior > 0) "%.0f".format(producto.precioAnterior) else "") }

    val precio = precioTexto.toDoubleOrNull() ?: -1.0
    val precioValido = precio > 0
    val nombreValido = nombre.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (producto == null) "Nuevo producto" else "Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = nombre.isNotBlank() && nombre.length < 2
                )
                OutlinedTextField(
                    value = precioTexto,
                    onValueChange = { precioTexto = it },
                    label = { Text("Precio *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Text("$", style = MaterialTheme.typography.bodyLarge) },
                    isError = precioTexto.isNotBlank() && !precioValido
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Es oferta", modifier = Modifier.weight(1f))
                    Switch(checked = esOferta, onCheckedChange = { esOferta = it })
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sin stock (agotado)", modifier = Modifier.weight(1f))
                    Switch(checked = agotado, onCheckedChange = { agotado = it })
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Precio público")
                        Text("Visible en la vidriera del local",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = precioPublico, onCheckedChange = { precioPublico = it })
                }
                if (esOferta) {
                    OutlinedTextField(
                        value = precioAnteriorTexto,
                        onValueChange = { precioAnteriorTexto = it },
                        label = { Text("Precio anterior (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Text("$", style = MaterialTheme.typography.bodyLarge) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmar(
                        nombre.trim(),
                        precio,
                        categoria.trim(),
                        descripcion.trim(),
                        esOferta,
                        precioAnteriorTexto.toDoubleOrNull() ?: 0.0,
                        precioPublico,
                        agotado
                    )
                },
                enabled = nombreValido && precioValido
            ) { Text(if (producto == null) "Agregar" else "Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
