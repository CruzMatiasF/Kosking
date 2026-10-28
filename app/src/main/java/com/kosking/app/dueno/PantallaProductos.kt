package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.modelo.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaProductos(
    onVolver: () -> Unit,
    vm: DuenoViewModel = viewModel()
) {
    val productos by vm.productos.collectAsState()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de precios") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { productoEditar = null; mostrarDialogo = true }) {
                Icon(Icons.Default.Add, "Agregar producto")
            }
        }
    ) { padding ->
        if (productos.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No hay productos cargados",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Tocá + para agregar precios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    Text(
                        "Los empleados y clientes ven estos precios en tiempo real",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(productos) { prod ->
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(prod.nombre, fontWeight = FontWeight.Medium)
                                if (prod.esOferta) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("Oferta", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                if (!prod.habilitado) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text("Oculto", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        },
                        supportingContent = {
                            if (prod.descripcion.isNotBlank()) Text(prod.descripcion)
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "$${"%.2f".format(prod.precio)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { productoEditar = prod; mostrarDialogo = true }) {
                                    Icon(Icons.Default.Edit, "Editar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { productoAEliminar = prod }) {
                                    Icon(Icons.Default.Delete, "Eliminar",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogProducto(
            productoInicial = productoEditar,
            onGuardar = { nombre, precio, descripcion, habilitado, esOferta, precioAnterior ->
                if (productoEditar != null) {
                    vm.actualizarProducto(productoEditar!!.copy(
                        nombre = nombre, precio = precio, descripcion = descripcion,
                        habilitado = habilitado, esOferta = esOferta, precioAnterior = precioAnterior
                    ))
                } else {
                    vm.agregarProducto(nombre, precio, descripcion, habilitado, esOferta, precioAnterior)
                }
                mostrarDialogo = false
            },
            onDismiss = { mostrarDialogo = false }
        )
    }

    productoAEliminar?.let { prod ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar ${prod.nombre} de la lista de precios?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminarProducto(prod.id)
                    productoAEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
internal fun DialogProducto(
    productoInicial: Producto?,
    onGuardar: (nombre: String, precio: Double, descripcion: String, habilitado: Boolean, esOferta: Boolean, precioAnterior: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(productoInicial?.nombre ?: "") }
    var precio by remember { mutableStateOf(productoInicial?.precio?.let { "%.2f".format(it) } ?: "") }
    var descripcion by remember { mutableStateOf(productoInicial?.descripcion ?: "") }
    var habilitado by remember { mutableStateOf(productoInicial?.habilitado ?: true) }
    var esOferta by remember { mutableStateOf(productoInicial?.esOferta ?: false) }
    var precioAnterior by remember { mutableStateOf(productoInicial?.precioAnterior?.let { if (it > 0) "%.2f".format(it) else "" } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (productoInicial != null) "Editar producto" else "Nuevo producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej: Coca 500ml, Pan, Cigarrillos") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio $") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                ListItem(
                    headlineContent = { Text("Visible en Vidriera") },
                    supportingContent = { Text("Los clientes y empleados pueden verlo") },
                    trailingContent = { Switch(checked = habilitado, onCheckedChange = { habilitado = it }) }
                )
                ListItem(
                    headlineContent = { Text("Es oferta") },
                    supportingContent = { Text("Muestra precio tachado y badge de descuento") },
                    trailingContent = { Switch(checked = esOferta, onCheckedChange = { esOferta = it; if (!it) precioAnterior = "" }) }
                )
                if (esOferta) {
                    OutlinedTextField(
                        value = precioAnterior,
                        onValueChange = { precioAnterior = it },
                        label = { Text("Precio anterior $") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGuardar(
                        nombre.trim(),
                        precio.toDoubleOrNull() ?: 0.0,
                        descripcion.trim(),
                        habilitado,
                        esOferta,
                        precioAnterior.toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = nombre.isNotBlank() && (precio.toDoubleOrNull() ?: 0.0) > 0
            ) { Text(if (productoInicial != null) "Guardar" else "Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
