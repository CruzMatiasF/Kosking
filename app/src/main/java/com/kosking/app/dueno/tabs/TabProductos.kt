package com.kosking.app.dueno.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.datos.CatalogoRepository
import com.kosking.app.modelo.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TabProductos(
    productos: List<Producto>,
    cargando: Boolean,
    onGuardar: (nombre: String, precio: Double, productoId: String?, onExito: () -> Unit) -> Unit,
    onEliminar: (Producto) -> Unit
) {
    var mostrarDialog by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Producto?>(null) }
    var mostrarConfirmarEliminar by remember { mutableStateOf<Producto?>(null) }
    var busqueda by remember { mutableStateOf("") }
    val productosFiltrados = remember(productos, busqueda) {
        if (busqueda.isBlank()) productos
        else productos.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    // Diálogo agregar/editar
    if (mostrarDialog) {
        DialogProducto(
            producto = editando,
            cargando = cargando,
            onGuardar = { nombre, precio ->
                onGuardar(nombre, precio, editando?.id) {
                    mostrarDialog = false
                    editando = null
                }
            },
            onDismiss = { mostrarDialog = false; editando = null }
        )
    }

    // Diálogo confirmar eliminar
    mostrarConfirmarEliminar?.let { prod ->
        AlertDialog(
            onDismissRequest = { mostrarConfirmarEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar \"${prod.nombre}\"? No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { onEliminar(prod); mostrarConfirmarEliminar = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        // Header con botón agregar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Productos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (productos.isEmpty()) "Todavía no hay productos"
                    else "${productos.size} producto${if (productos.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { editando = null; mostrarDialog = true },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Agregar")
            }
        }

        if (productos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box {
                            if (busqueda.isEmpty()) Text("Buscar producto", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    }
                )
                if (busqueda.isNotEmpty()) {
                    Icon(Icons.Default.Close, "Limpiar", Modifier.size(16.dp).clickable { busqueda = "" },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalDivider()

        if (productos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        null,
                        Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Cargá los productos de tu local",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Al anotar un fiado vas a ver los productos acá y solo tenés que tocarlos. No hace falta escribir ni el precio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(onClick = { editando = null; mostrarDialog = true }) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Agregar primer producto")
                    }
                }
            }
        } else if (productosFiltrados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.SearchOff, null, Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text("No encontramos \"$busqueda\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    TextButton(onClick = { busqueda = "" }) { Text("Limpiar búsqueda") }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(productosFiltrados, key = { it.id }) { prod ->
                    ListItem(
                        headlineContent = {
                            Text(prod.nombre, fontWeight = FontWeight.Medium)
                        },
                        supportingContent = {
                            Text(
                                "Usado ${prod.vecesUsado} ${if (prod.vecesUsado == 1) "vez" else "veces"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    if (prod.ultimoPrecio > 0) "$${"%.0f".format(prod.ultimoPrecio)}" else "Sin precio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (prod.ultimoPrecio > 0) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { editando = prod; mostrarDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Editar", Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { mostrarConfirmarEliminar = prod },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Eliminar", Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                }
                            }
                        },
                        modifier = Modifier.clickable { editando = prod; mostrarDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogProducto(
    producto: Producto?,
    cargando: Boolean,
    onGuardar: (nombre: String, precio: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val esEdicion = producto != null

    var nombre by remember(producto) { mutableStateOf(producto?.nombre ?: "") }
    var precioTexto by remember(producto) { mutableStateOf(
        if (producto != null && producto.ultimoPrecio > 0) "%.0f".format(producto.ultimoPrecio) else ""
    ) }
    var busqueda by remember { mutableStateOf(producto?.nombre ?: "") }
    var mostrarSugerencias by remember { mutableStateOf(false) }

    val sugerencias = remember(busqueda) {
        if (busqueda.length >= 2) CatalogoRepository.buscar(context, busqueda).take(6)
        else emptyList()
    }

    val precioDouble = precioTexto.toDoubleOrNull() ?: 0.0
    val puedeGuardar = nombre.isNotBlank() && !cargando

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (esEdicion) "Editar producto" else "Agregar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Campo nombre con sugerencias del catálogo
                Column {
                    OutlinedTextField(
                        value = if (mostrarSugerencias) busqueda else nombre,
                        onValueChange = { v ->
                            busqueda = v
                            nombre = v
                            mostrarSugerencias = v.isNotBlank()
                        },
                        label = { Text("Nombre del producto") },
                        placeholder = { Text("Buscá o escribí el nombre") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (nombre.isNotBlank()) {
                                IconButton(onClick = { nombre = ""; busqueda = ""; mostrarSugerencias = false }) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sugerencias del catálogo
                    if (mostrarSugerencias && sugerencias.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        ) {
                            sugerencias.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            nombre = item.nombre
                                            busqueda = item.nombre
                                            mostrarSugerencias = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    if (item.categoria.isNotBlank()) {
                                        Text(
                                            item.categoria,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = precioTexto,
                    onValueChange = { precioTexto = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Precio $") },
                    placeholder = { Text("Ej: 500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!esEdicion) {
                    Text(
                        "El precio se puede dejar en blanco y completarlo después.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(nombre.trim(), precioDouble) },
                enabled = puedeGuardar
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                else Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
