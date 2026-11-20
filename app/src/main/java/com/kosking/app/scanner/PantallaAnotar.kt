package com.kosking.app.scanner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Producto

data class ItemAnotado(
    val nombre: String,
    val precio: Double,
    val productoId: String? = null,
    val cantidad: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAnotar(
    vm: DuenoViewModel,
    productoInicial: ItemAnotado? = null,
    onCerrar: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var precioManual by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(if (productoInicial != null) listOf(productoInicial) else listOf()) }
    var mostrarEscaner by remember { mutableStateOf(false) }
    var mostrarElegirCliente by remember { mutableStateOf(false) }
    val productos by vm.productos.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val cargando by vm.cargando.collectAsState()
    val focusRequester = remember { FocusRequester() }

    val sugerencias = remember(query, productos) {
        if (query.isBlank()) emptyList()
        else productos.filter { it.nombre.contains(query, ignoreCase = true) }.take(4)
    }

    fun agregarItem(item: ItemAnotado) {
        val idx = items.indexOfFirst { it.productoId != null && it.productoId == item.productoId }
        items = if (idx >= 0) {
            items.mapIndexed { i, it -> if (i == idx) it.copy(cantidad = it.cantidad + 1) else it }
        } else {
            items + item
        }
        query = ""
        precioManual = ""
    }

    BackHandler(enabled = mostrarElegirCliente) { mostrarElegirCliente = false }
    BackHandler(enabled = mostrarEscaner) { mostrarEscaner = false }

    if (mostrarEscaner) {
        ScannerView(
            titulo = "Escanear",
            onCodigoDetectado = { codigo ->
                mostrarEscaner = false
                val prod = productos.firstOrNull { it.codigoBarras == codigo }
                if (prod != null) {
                    agregarItem(ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id))
                } else {
                    query = codigo
                }
            },
            onCerrar = { mostrarEscaner = false }
        )
        return
    }

    if (mostrarElegirCliente) {
        ElegirCliente(
            clientes = clientes,
            cargando = cargando,
            onElegir = { cliente ->
                vm.agregarMovimientos(
                    clienteId = cliente.id,
                    items = items.map { Triple(it.nombre, it.precio, it.cantidad) },
                    tipo = "cargo",
                    onExito = { onCerrar() }
                )
            },
            onCerrar = { mostrarElegirCliente = false }
        )
        return
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anotar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarEscaner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── Lista de items anotados (ocupa el espacio disponible) ──
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                reverseLayout = false
            ) {
                if (items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Escribí un producto abajo para empezar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(items) { item ->
                        ListItem(
                            headlineContent = {
                                Text(item.nombre, fontWeight = FontWeight.Medium)
                            },
                            supportingContent = {
                                Text(
                                    "$${(item.precio * item.cantidad).toInt()}" +
                                        if (item.cantidad > 1) "  ·  x${item.cantidad}" else "",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        items = if (item.cantidad > 1)
                                            items.map { if (it === item) it.copy(cantidad = it.cantidad - 1) else it }
                                        else items - item
                                    }) {
                                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp))
                                    }
                                    Text("${item.cantidad}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        items = items.map { if (it === item) it.copy(cantidad = it.cantidad + 1) else it }
                                    }) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            // ── Sugerencias (aparecen solo cuando hay texto) ──
            if (sugerencias.isNotEmpty()) {
                HorizontalDivider()
                sugerencias.forEach { prod ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { agregarItem(ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id)) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            prod.nombre,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (prod.ultimoPrecio > 0) {
                            Text(
                                "$${prod.ultimoPrecio.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider()
                }
            }

            // Opción manual si escribió texto que no matchea exacto
            if (query.isNotBlank() && sugerencias.none { it.nombre.equals(query.trim(), ignoreCase = true) }) {
                if (sugerencias.isEmpty()) HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "\"${query.trim()}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = precioManual,
                        onValueChange = { precioManual = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder = { Text("Precio") },
                        prefix = { Text("$") },
                        modifier = Modifier.width(110.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { agregarItem(ItemAnotado(query.trim(), precioManual.toDoubleOrNull() ?: 0.0)) },
                        enabled = precioManual.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Check, null,
                            tint = if (precioManual.isNotBlank()) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
            }

            // ── Campo de búsqueda fijo abajo ──
            Surface(shadowElevation = 4.dp) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; precioManual = "" },
                        placeholder = { Text("Escribir producto...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; precioManual = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    if (items.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        val total = items.sumOf { it.precio * it.cantidad }
                        Button(
                            onClick = { mostrarElegirCliente = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(
                                "Elegir cliente  ·  $${total.toInt()}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElegirCliente(
    clientes: List<Cliente>,
    cargando: Boolean,
    onElegir: (Cliente) -> Unit,
    onCerrar: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val filtrados = remember(busqueda, clientes) {
        if (busqueda.isBlank()) clientes
        else clientes.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("¿A quién anotamos?", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar cliente...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(filtrados) { cliente ->
                        ListItem(
                            headlineContent = { Text(cliente.nombre) },
                            supportingContent = if (cliente.deudaTotal > 0) {
                                { Text("Debe $${cliente.deudaTotal.toInt()}", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.clickable { onElegir(cliente) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
