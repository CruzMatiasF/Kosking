package com.kosking.app.dueno.local

import com.kosking.app.dueno.DuenoViewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kosking.app.modelo.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaProductos(
    onVolver: () -> Unit,
    esDueno: Boolean = true,
    vm: DuenoViewModel = viewModel()
) {
    val productos by vm.productos.collectAsState()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    var productoParaFoto by remember { mutableStateOf<Producto?>(null) }

    val fotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        val prod = productoParaFoto
        if (uri != null && prod != null) {
            vm.subirFotoProducto(prod, uri) {}
        }
        productoParaFoto = null
    }

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
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (prod.imagenUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = prod.imagenUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (esDueno) {
                                    IconButton(onClick = {
                                        productoParaFoto = prod
                                        fotoLauncher.launch("image/*")
                                    }) {
                                        Icon(
                                            Icons.Default.AddAPhoto,
                                            contentDescription = "Agregar foto",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(prod.nombre, fontWeight = FontWeight.Medium)
                                if (prod.esOferta) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("Oferta", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                if (prod.agotado) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                        Text("Agotado", color = MaterialTheme.colorScheme.onErrorContainer)
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
                                if (esDueno) {
                                    IconButton(onClick = { productoAEliminar = prod }) {
                                        Icon(Icons.Default.Delete, "Eliminar",
                                            tint = MaterialTheme.colorScheme.error)
                                    }
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
            productosExistentes = if (productoEditar == null) productos else emptyList(),
            onGuardar = { nombre, precio, descripcion, habilitado, esOferta, precioAnterior, agotado, estadoStock, categoria ->
                if (productoEditar != null) {
                    vm.actualizarProducto(productoEditar!!.copy(
                        nombre = nombre, precio = precio, descripcion = descripcion,
                        habilitado = habilitado, esOferta = esOferta, precioAnterior = precioAnterior,
                        agotado = agotado, estadoStock = estadoStock, categoria = categoria
                    ))
                } else {
                    vm.agregarProducto(nombre, precio, descripcion, habilitado, esOferta, precioAnterior, agotado, estadoStock, categoria)
                }
                mostrarDialogo = false
            },
            onDismiss = { mostrarDialogo = false }
        )
    }

    if (esDueno) {
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
}

@Composable
internal fun DialogProducto(
    productoInicial: Producto?,
    productosExistentes: List<Producto> = emptyList(),
    onGuardar: (nombre: String, precio: Double, descripcion: String, habilitado: Boolean, esOferta: Boolean, precioAnterior: Double, agotado: Boolean, estadoStock: String, categoria: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(productoInicial?.nombre ?: "") }
    var precio by remember { mutableStateOf(productoInicial?.precio?.let { "%.2f".format(it) } ?: "") }
    var descripcion by remember { mutableStateOf(productoInicial?.descripcion ?: "") }
    var habilitado by remember { mutableStateOf(productoInicial?.habilitado ?: true) }
    var esOferta by remember { mutableStateOf(productoInicial?.esOferta ?: false) }
    var precioAnterior by remember { mutableStateOf(productoInicial?.precioAnterior?.let { if (it > 0) "%.2f".format(it) else "" } ?: "") }
    var estadoStock by remember { mutableStateOf(productoInicial?.estadoStock ?: "") }
    var categoria by remember { mutableStateOf(productoInicial?.categoria ?: "") }
    var mostrarMenuCategoria by remember { mutableStateOf(false) }

    val similarExistente = remember(nombre, productosExistentes) {
        val trimmed = nombre.trim()
        if (trimmed.length >= 3)
            productosExistentes.firstOrNull { it.nombre.contains(trimmed, ignoreCase = true) || trimmed.contains(it.nombre, ignoreCase = true) }
        else null
    }

    val categoriasPredef = listOf("Gaseosas", "Aguas y jugos", "Cervezas", "Galletitas", "Golosinas", "Chocolates", "Lácteos", "Cigarrillos", "Almacén", "Snacks", "Higiene", "Otros")
    val puedeGuardar = nombre.isNotBlank() && (precio.toDoubleOrNull() ?: 0.0) > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (productoInicial != null) "Editar producto" else "Nuevo producto",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                HorizontalDivider()

                // Contenido scrollable
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Nombre
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ej: Coca 500ml, Pan, Cigarrillos") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (similarExistente != null) {
                        Text(
                            "Ya existe \"${similarExistente.nombre}\" — ¿es el mismo?",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Precio + Categoría en dos columnas
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = precio,
                            onValueChange = { precio = it },
                            label = { Text("Precio $") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = categoria,
                                onValueChange = { categoria = it },
                                label = { Text("Categoría") },
                                placeholder = { Text("Gaseosas...") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { mostrarMenuCategoria = true }) {
                                        Icon(Icons.Default.ExpandMore, null, Modifier.size(18.dp))
                                    }
                                },
                                singleLine = true
                            )
                            DropdownMenu(
                                expanded = mostrarMenuCategoria,
                                onDismissRequest = { mostrarMenuCategoria = false }
                            ) {
                                categoriasPredef.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = { categoria = cat; mostrarMenuCategoria = false }
                                    )
                                }
                            }
                        }
                    }

                    // Descripción
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    HorizontalDivider()

                    // Switches compactos
                    SwitchRowItem("Visible en vidriera", "Lo ven clientes y empleados", habilitado) { habilitado = it }

                    // Estado de stock
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Estado del stock", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("" to "Disponible", "pocos" to "Quedan pocos", "agotado" to "Agotado").forEach { (valor, label) ->
                                val seleccionado = estadoStock == valor
                                FilterChip(
                                    selected = seleccionado,
                                    onClick = { estadoStock = valor },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (valor) {
                                            "pocos" -> MaterialTheme.colorScheme.tertiaryContainer
                                            "agotado" -> MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        },
                                        selectedLabelColor = when (valor) {
                                            "pocos" -> MaterialTheme.colorScheme.onTertiaryContainer
                                            "agotado" -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                )
                            }
                        }
                    }

                    SwitchRowItem("Es oferta", "Muestra badge OFERTA en la card", esOferta) { esOferta = it; if (!it) precioAnterior = "" }

                    // Bloque oferta
                    if (esOferta) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = precioAnterior,
                                    onValueChange = { precioAnterior = it },
                                    label = { Text("Precio original $") },
                                    placeholder = { Text("ej: 1500") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                if (precioAnterior.isNotBlank()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("antes", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "$$precioAnterior",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    }
                                } else {
                                    Text(
                                        "se muestra\ntachado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Footer botones
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = {
                            onGuardar(
                                nombre.trim(),
                                precio.toDoubleOrNull() ?: 0.0,
                                descripcion.trim(),
                                habilitado,
                                esOferta,
                                precioAnterior.toDoubleOrNull() ?: 0.0,
                                estadoStock == "agotado",
                                estadoStock,
                                categoria.trim()
                            )
                        },
                        enabled = puedeGuardar,
                        modifier = Modifier.weight(2f)
                    ) { Text(if (productoInicial != null) "Guardar cambios" else "Agregar producto") }
                }
            }
        }
    }
}

@Composable
private fun SwitchRowItem(
    titulo: String,
    subtitulo: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium)
            Text(subtitulo, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f)
        )
    }
}
