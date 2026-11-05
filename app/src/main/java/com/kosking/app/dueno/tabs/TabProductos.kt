package com.kosking.app.dueno.tabs

import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.local.DialogProducto

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kosking.app.modelo.Producto

@Composable
internal fun TabProductos(vm: DuenoViewModel) {
    val productos by vm.productos.collectAsState()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    var productoParaFoto by remember { mutableStateOf<Producto?>(null) }

    val fotoProductoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val prod = productoParaFoto
        if (uri != null && prod != null) {
            vm.subirFotoProducto(prod, uri) {}
        }
        productoParaFoto = null
    }
    var busqueda by remember { mutableStateOf("") }

    val secciones = remember(productos) {
        val cats = productos.map { it.categoria.trim() }
            .filter { it.isNotBlank() }.distinct().sorted()
        val sinCat = productos.any { it.categoria.isBlank() }
        if (sinCat) cats + listOf("") else cats
    }

    val productosBusqueda = remember(productos, busqueda) {
        if (busqueda.isBlank()) emptyList()
        else productos.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box {
                            if (busqueda.isEmpty()) Text("Buscar...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    }
                )
                if (busqueda.isNotEmpty()) {
                    Icon(Icons.Default.Close, "Limpiar",
                        Modifier.size(14.dp).clickable { busqueda = "" },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(
                onClick = { productoEditar = null; mostrarDialogo = true },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(Icons.Default.Add, "Nuevo producto", Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        when {
            productos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No hay productos cargados",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("TocÃ¡ + para agregar el primero",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            busqueda.isNotBlank() -> {
                if (productosBusqueda.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin resultados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(productosBusqueda, key = { it.id }) { prod ->
                            CardProducto(
                                prod = prod,
                                onEditar = { productoEditar = prod; mostrarDialogo = true },
                                onFoto = { productoParaFoto = prod; fotoProductoLauncher.launch("image/*") },
                                onEliminar = { productoAEliminar = prod }
                            )
                        }
                        item(span = { GridItemSpan(3) }) { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
            secciones.isEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(productos, key = { it.id }) { prod ->
                        CardProducto(
                            prod = prod,
                            onEditar = { productoEditar = prod; mostrarDialogo = true },
                            onFoto = { productoParaFoto = prod; fotoProductoLauncher.launch("image/*") },
                            onEliminar = { productoAEliminar = prod }
                        )
                    }
                    item(span = { GridItemSpan(3) }) { Spacer(Modifier.height(80.dp)) }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    secciones.forEach { cat ->
                        val prods = productos.filter {
                            if (cat.isBlank()) it.categoria.isBlank()
                            else it.categoria.trim() == cat
                        }
                        if (prods.isEmpty()) return@forEach
                        item(key = "header_$cat") {
                            Row(
                                Modifier.fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (cat.isBlank()) "Sin categorÃ­a" else cat,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${prods.size} productos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        item(key = "row_$cat") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(prods, key = { it.id }) { prod ->
                                    Box(Modifier.width(110.dp)) {
                                        CardProducto(
                                            prod = prod,
                                            onEditar = { productoEditar = prod; mostrarDialogo = true },
                                            onFoto = { productoParaFoto = prod; fotoProductoLauncher.launch("image/*") },
                                            onEliminar = { productoAEliminar = prod }
                                        )
                                    }
                                }
                            }
                        }
                    }
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

    productoAEliminar?.let { prod ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("Â¿Eliminar ${prod.nombre} de la lista?") },
            confirmButton = {
                TextButton(onClick = { vm.eliminarProducto(prod.id); productoAEliminar = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { productoAEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardProducto(
    prod: Producto,
    onEditar: () -> Unit,
    onFoto: () -> Unit,
    onEliminar: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onEditar, onLongClick = onEliminar),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(if (prod.imagenUrl.isBlank()) Modifier.clickable(onClick = onFoto) else Modifier)
            ) {
                if (prod.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = prod.imagenUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        prod.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                if (prod.imagenUrl.isBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onFoto),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (prod.esOferta) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("OFERTA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    if (!prod.habilitado) {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("OCULTO", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    if (prod.estadoStock == "agotado" || (prod.estadoStock.isBlank() && prod.agotado)) {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer) {
                            Text("AGOTADO", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    } else if (prod.estadoStock == "pocos") {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("POCOS", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    prod.nombre,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$${"%.0f".format(prod.precio)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (prod.esOferta && prod.precioAnterior > 0) {
                        Text(
                            "$${"%.0f".format(prod.precioAnterior)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}
