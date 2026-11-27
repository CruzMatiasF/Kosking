package com.kosking.app.feature.kiosco.tabs

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kosking.app.datos.CatalogoRepository
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabProductos(
    productos: List<Producto>,
    cargando: Boolean,
    modifier: Modifier = Modifier,
    mostrarHeader: Boolean = true,
    mostrarDialogExterno: Boolean = false,
    onDismissDialogExterno: () -> Unit = {},
    onBuscarGlobal: suspend (String) -> List<String> = { emptyList() },
    esEmpleado: Boolean = false,
    vm: DuenoViewModel? = null,
    onAbrirEscanerCargar: (() -> Unit)? = null,
    onGuardar: (nombre: String, precio: Double, categoria: String, productoId: String?, imagenUri: Uri?, presentacion: String, codigoBarras: String, esPorPeso: Boolean, onExito: () -> Unit) -> Unit,
    onEliminar: (Producto) -> Unit,
    onVolver: (() -> Unit)? = null
) {
    var mostrarDialog by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Producto?>(null) }
    LaunchedEffect(mostrarDialogExterno) {
        if (mostrarDialogExterno) { editando = null; mostrarDialog = true }
    }
    LaunchedEffect(mostrarDialog) {
        if (!mostrarDialog && mostrarDialogExterno) onDismissDialogExterno()
    }
    var mostrarConfirmarEliminar by remember { mutableStateOf<Producto?>(null) }
    var busqueda by remember { mutableStateOf("") }
    val productosFiltrados = remember(productos, busqueda) {
        if (busqueda.isBlank()) productos
        else productos.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    // Agrupar por categoría: productos sin categoría van a "Otros" al final
    val secciones: List<Pair<String, List<Producto>>> = remember(productos) {
        val conCategoria = productos
            .filter { it.categoria.isNotBlank() }
            .groupBy { it.categoria }
            .entries
            .sortedBy { it.key }
            .map { it.key to it.value }
        val sinCategoria = productos.filter { it.categoria.isBlank() }
        if (sinCategoria.isEmpty()) conCategoria
        else conCategoria + ("Otros" to sinCategoria)
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try { CatalogoRepository.migrarSiVacio(context) } catch (_: Exception) {}
    }

    if (mostrarDialog) {
        DialogProducto(
            producto = editando,
            categoriasExistentes = remember(productos) {
                productos.map { it.categoria }.filter { it.isNotBlank() }.distinct().sorted()
            },
            cargando = cargando,
            onBuscarGlobal = onBuscarGlobal,
            onGuardar = { nombre, precio, categoria, imagenUri, presentacion, codigoBarras, esPorPeso ->
                onGuardar(nombre, precio, categoria, editando?.id, imagenUri, presentacion, codigoBarras, esPorPeso) {
                    mostrarDialog = false
                    editando = null
                }
            },
            onDismiss = { mostrarDialog = false; editando = null }
        )
    }

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

    if (onVolver != null) BackHandler(onBack = onVolver)

    Column(modifier.fillMaxSize()) {
        // Header con botón agregar
        if (mostrarHeader) Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onVolver != null) {
                    IconButton(onClick = onVolver, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(Modifier.width(4.dp))
                }
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
            } // cierre Row interno
            if (!esEmpleado) {
                var menuNuevo by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { menuNuevo = true },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nuevo producto")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = menuNuevo,
                        onDismissRequest = { menuNuevo = false }
                    ) {
                        if (onAbrirEscanerCargar != null) {
                            DropdownMenuItem(
                                text = { Text("Escanear") },
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, Modifier.size(18.dp)) },
                                onClick = { menuNuevo = false; onAbrirEscanerCargar() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Cargar manual") },
                            leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) },
                            onClick = { menuNuevo = false; editando = null; mostrarDialog = true }
                        )
                    }
                }
            }
        }

        if (productos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
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

        when {
            productos.isEmpty() -> {
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
                            if (esEmpleado) "Sin productos cargados" else "Cargá los productos de tu local",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (esEmpleado) "El dueño todavía no cargó productos al catálogo."
                            else "Al anotar vas a ver los productos acá y solo tenés que tocarlos. No hace falta escribir ni el precio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (!esEmpleado) {
                            Button(onClick = { editando = null; mostrarDialog = true }) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Agregar primer producto")
                            }
                        }
                    }
                }
            }

            busqueda.isNotBlank() && productosFiltrados.isEmpty() -> {
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
                            textAlign = TextAlign.Center)
                        TextButton(onClick = { busqueda = "" }) { Text("Limpiar búsqueda") }
                    }
                }
            }

            // Búsqueda activa: lista plana vertical
            busqueda.isNotBlank() -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(productosFiltrados, key = { it.id }) { prod ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 }
                        ) {
                            ProductoListItem(
                                prod = prod,
                                esEmpleado = esEmpleado,
                                onEditar = { editando = prod; mostrarDialog = true },
                                onEliminar = { mostrarConfirmarEliminar = prod }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }

            // Sin búsqueda: secciones por categoría con scroll horizontal
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    secciones.forEach { (categoria, prods) ->
                        item(key = "header_$categoria") {
                            Text(
                                categoria.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
                            )
                        }
                        item(key = "row_$categoria") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(prods, key = { it.id }) { prod ->
                                    ProductoCard(
                                        prod = prod,
                                        esEmpleado = esEmpleado,
                                        onEditar = { editando = prod; mostrarDialog = true },
                                        onEliminar = { mostrarConfirmarEliminar = prod }
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProductoCard(
    prod: Producto,
    esEmpleado: Boolean = false,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val context = LocalContext.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )
    Card(
        modifier = Modifier.width(140.dp).scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().pointerInput(esEmpleado) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { if (!esEmpleado) onEditar() }
                )
            }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(90.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (prod.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(prod.imagenUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    prod.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (prod.ultimoPrecio > 0) "$${"%.0f".format(prod.ultimoPrecio)}" else "Sin precio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (prod.ultimoPrecio > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!esEmpleado) {
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        IconButton(onClick = onEditar, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, "Editar", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onEliminar, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, "Eliminar", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoListItem(
    prod: Producto,
    esEmpleado: Boolean = false,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(prod.nombre, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                "Usado ${prod.vecesUsado} ${if (prod.vecesUsado == 1) "vez" else "veces"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = if (prod.imagenUrl.isNotBlank()) ({
            AsyncImage(
                model = ImageRequest.Builder(context).data(prod.imagenUrl).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
            )
        }) else ({
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    prod.nombre.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }),
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (prod.ultimoPrecio > 0) "$${"%.0f".format(prod.ultimoPrecio)}" else "Sin precio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (prod.ultimoPrecio > 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!esEmpleado) {
                    IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Editar", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Eliminar", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            }
        },
        modifier = if (esEmpleado) Modifier else Modifier.clickable(onClick = onEditar)
    )
}


