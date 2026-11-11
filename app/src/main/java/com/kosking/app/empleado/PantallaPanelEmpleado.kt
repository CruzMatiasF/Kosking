package com.kosking.app.empleado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import coil.compose.AsyncImage
import com.kosking.app.dueno.AnotarRapidoSheet
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Permisos
import com.kosking.app.modelo.Producto
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPanelEmpleado(
    onVerCliente: (String) -> Unit,
    onCerrarSesion: () -> Unit,
    onVolver: () -> Unit = {},
    tabInicial: Int = 0,
    vm: EmpleadoViewModel
) {
    val nombreLocal by vm.nombreLocal.collectAsState()
    val nombreEmpleado by vm.nombreEmpleado.collectAsState()
    val clientes by vm.clientesHabilitados.collectAsState()
    val movimientos by vm.movimientosLocal.collectAsState()
    val permisos by vm.permisos.collectAsState()
    val productos by vm.productos.collectAsState()
    var mostrarMenu by remember { mutableStateOf(false) }
    var mostrarConfirmDesvincularse by remember { mutableStateOf(false) }
    var mostrarEditarNombre by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var tabActual by remember { mutableStateOf(tabInicial) }
    var clienteConAlerta by remember { mutableStateOf<com.kosking.app.modelo.Cliente?>(null) }
    var clienteAnotar by remember { mutableStateOf<com.kosking.app.modelo.Cliente?>(null) }

    // El back natural va a EXPLORAR (que está en el back stack)

    // Mini stats: movimientos anotados HOY por este empleado
    val ahora = System.currentTimeMillis()
    val calHoy = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val movsHoy = remember(movimientos) {
        movimientos.filter { it.fecha.toDate().time >= calHoy.timeInMillis && !it.anulado }
    }
    val fiadosHoy = remember(movsHoy) { movsHoy.count { it.tipo == "cargo" } }
    val cobradoHoy = remember(movsHoy) { movsHoy.filter { it.tipo == "pago" }.sumOf { it.monto } }

    val clientesFiltrados = remember(clientes, busqueda) {
        if (busqueda.isBlank()) clientes
        else clientes.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombreLocal.ifBlank { "Kosking" }) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { mostrarMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Opciones")
                        }
                        DropdownMenu(
                            expanded = mostrarMenu,
                            onDismissRequest = { mostrarMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar mi nombre") },
                                onClick = { mostrarMenu = false; mostrarEditarNombre = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Desvincularme del local") },
                                onClick = { mostrarMenu = false; mostrarConfirmDesvincularse = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                onClick = { mostrarMenu = false; vm.cerrarSesion(); onCerrarSesion() }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.tertiary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationBar {
                NavigationBarItem(
                    selected = tabActual == 1,
                    onClick = { tabActual = 1; busqueda = "" },
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Clientes") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tabActual == 0,
                    onClick = { tabActual = 0; busqueda = "" },
                    icon = { Icon(Icons.Default.Store, null) },
                    label = { Text("Precios") },
                    colors = navColors
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Barra de búsqueda unificada — siempre visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            if (busqueda.isEmpty()) Text(
                                if (tabActual == 0) "Buscar producto o precio..."
                                else "Buscar cliente...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    }
                )
                if (busqueda.isNotEmpty()) {
                    Icon(Icons.Default.Close, "Limpiar", Modifier.size(18.dp).clickable { busqueda = "" },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (tabActual == 0) {
                TabPreciosSinBusqueda(
                    productos = productos.filter { it.habilitado },
                    busqueda = busqueda,
                    puedeEditar = permisos.editarPrecios,
                    onActualizarPrecio = { prod, precio -> vm.actualizarPrecio(prod, precio) }
                )
            } else {
                // Mini stats
                if (!permisos.anotarFiados || !permisos.registrarPagos) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, null,
                                tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                            Text(
                                buildString {
                                    if (!permisos.anotarFiados) append("No podés anotar fiaditos. ")
                                    if (!permisos.registrarPagos) append("No podés registrar pagos.")
                                }.trim(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$fiadosHoy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("fiado${if (fiadosHoy != 1) "s" else ""} hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$${"%.0f".format(cobradoHoy)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("cobrado hoy", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (clientesFiltrados.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    if (clientes.isEmpty()) "No hay clientes todavía" else "Sin resultados",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(clientesFiltrados, key = { it.id }) { cliente ->
                            ClienteItemEmpleado(
                                cliente = cliente,
                                mostrarDeuda = permisos.verTotal,
                                onClick = if (cliente.habilitado) ({
                                    if (cliente.alertaEmpleado) clienteConAlerta = cliente
                                    else onVerCliente(cliente.id)
                                }) else null,
                                onAnotarRapido = if (cliente.habilitado && (permisos.anotarFiados || permisos.registrarPagos)) ({
                                    if (cliente.alertaEmpleado) clienteConAlerta = cliente
                                    else clienteAnotar = cliente
                                }) else null
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    clienteConAlerta?.let { c ->
        AlertDialog(
            onDismissRequest = { clienteConAlerta = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFE65100)) },
            title = { Text("Atención") },
            text = { Text("El dueño marcó a ${c.nombre} con una alerta. Revisá antes de anotar.") },
            confirmButton = {
                TextButton(onClick = { clienteConAlerta = null; onVerCliente(c.id) }) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { clienteConAlerta = null }) { Text("Cancelar") }
            }
        )
    }

    clienteAnotar?.let { c ->
        AnotarRapidoSheet(
            clientes = clientes,
            productos = productos,
            cargando = false,
            clienteInicial = c,
            onGuardar = { clienteId, producto, monto, tipo ->
                vm.agregarMovimientos(
                    clienteId = clienteId,
                    items = listOf(Triple(producto, 1, monto)),
                    tipo = tipo,
                    onExito = { clienteAnotar = null }
                )
            },
            onDismiss = { clienteAnotar = null }
        )
    }

    var nombreEditar by remember(nombreEmpleado) { mutableStateOf(nombreEmpleado) }

    if (mostrarEditarNombre) {
        AlertDialog(
            onDismissRequest = { mostrarEditarNombre = false },
            title = { Text("Editar mi nombre") },
            text = {
                OutlinedTextField(
                    value = nombreEditar,
                    onValueChange = { nombreEditar = it },
                    label = { Text("Tu nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nombreEditar.isNotBlank()) {
                            vm.actualizarMiNombre(nombreEditar.trim())
                            mostrarEditarNombre = false
                        }
                    },
                    enabled = nombreEditar.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { mostrarEditarNombre = false }) { Text("Cancelar") } }
        )
    }

    if (mostrarConfirmDesvincularse) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmDesvincularse = false },
            title = { Text("Desvincularme del local") },
            text = { Text("¿Querés dejar de trabajar en ${nombreLocal.ifBlank { "este local" }}? Vas a perder el acceso.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmDesvincularse = false
                    vm.desvincularme { onCerrarSesion() }
                }) { Text("Desvincularme", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmDesvincularse = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TabPreciosSinBusqueda(
    productos: List<Producto>,
    busqueda: String,
    puedeEditar: Boolean = false,
    onActualizarPrecio: (Producto, Double) -> Unit = { _, _ -> }
) {
    val productosFiltrados = remember(productos, busqueda) {
        if (busqueda.isBlank()) productos
        else productos.filter {
            it.nombre.contains(busqueda, ignoreCase = true) ||
            it.categoria.contains(busqueda, ignoreCase = true)
        }
    }
    val porCategoria = remember(productosFiltrados) {
        productosFiltrados.groupBy { it.categoria.ifBlank { "Sin categoría" } }.toSortedMap()
    }

    if (productosFiltrados.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (productos.isEmpty()) "El dueño todavía no cargó precios" else "Sin resultados",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            porCategoria.forEach { (categoria, items) ->
                item(key = "cat_$categoria") {
                    Text(categoria, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                }
                items(items, key = { it.id }) { producto ->
                    ProductoPrecioItem(producto, puedeEditar, onActualizarPrecio)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductoPrecioItem(
    producto: Producto,
    puedeEditar: Boolean = false,
    onActualizarPrecio: (Producto, Double) -> Unit = { _, _ -> }
) {
    var mostrarEditar by remember { mutableStateOf(false) }

    if (mostrarEditar) {
        var precioTexto by remember { mutableStateOf("%.0f".format(producto.precio)) }
        AlertDialog(
            onDismissRequest = { mostrarEditar = false },
            title = { Text("Actualizar precio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(producto.nombre, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = precioTexto,
                        onValueChange = { precioTexto = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Nuevo precio") },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        precioTexto.toDoubleOrNull()?.let { onActualizarPrecio(producto, it) }
                        mostrarEditar = false
                    },
                    enabled = precioTexto.toDoubleOrNull() != null
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarEditar = false }) { Text("Cancelar") }
            }
        )
    }

    ListItem(
        leadingContent = if (producto.imagenUrl.isNotBlank()) ({
            AsyncImage(
                model = producto.imagenUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
            )
        }) else null,
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(producto.nombre, fontWeight = FontWeight.Medium)
                if (producto.esOferta) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text("OFERTA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
        },
        supportingContent = if (producto.descripcion.isNotBlank() || producto.updatedAt > 0) ({
            Column {
                if (producto.descripcion.isNotBlank()) {
                    Text(producto.descripcion, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (producto.updatedAt > 0) {
                    val diff = System.currentTimeMillis() - producto.updatedAt
                    val texto = when {
                        diff < 3_600_000L -> "actualizado hace ${diff / 60_000} min"
                        diff < 86_400_000L -> "actualizado hace ${diff / 3_600_000}h"
                        diff < 7 * 86_400_000L -> "actualizado hace ${diff / 86_400_000} días"
                        else -> null
                    }
                    texto?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
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
                        Text(
                            "$${"%.0f".format(producto.precioAnterior)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                if (puedeEditar) {
                    IconButton(onClick = { mostrarEditar = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Editar precio",
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    )
}

@Composable
private fun ClienteItemEmpleado(
    cliente: Cliente,
    mostrarDeuda: Boolean = true,
    onClick: (() -> Unit)?,
    onAnotarRapido: (() -> Unit)? = null
) {
    val tieneLimite = cliente.limiteCredito > 0
    val sobreLimite = tieneLimite && cliente.deudaTotal > cliente.limiteCredito
    val porcentaje = if (tieneLimite) (cliente.deudaTotal / cliente.limiteCredito).toFloat().coerceIn(0f, 1f) else 0f
    val colorBarra = when {
        sobreLimite -> MaterialTheme.colorScheme.error
        porcentaje >= 0.8f -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.primary
    }

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cliente.nombre, fontWeight = FontWeight.Medium)
                if (!cliente.habilitado) {
                    Spacer(Modifier.width(6.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("Bloqueado", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (cliente.alertaEmpleado) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Warning, "Alerta",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(16.dp))
                }
                if (sobreLimite) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Warning, "Sobre límite",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp))
                }
            }
        },
        supportingContent = {
            if (cliente.habilitado) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        when {
                            !mostrarDeuda && cliente.deudaTotal > 0 -> "Tiene deuda"
                            cliente.deudaTotal > 0 -> "Debe $${"%.2f".format(cliente.deudaTotal)}"
                            else -> "Sin deuda"
                        }
                    )
                    if (tieneLimite) {
                        LinearProgressIndicator(
                            progress = { porcentaje },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = colorBarra,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "Límite: $${"%.0f".format(cliente.limiteCredito)}" +
                                if (sobreLimite) " · EXCEDIDO" else " · ${"%.0f".format(porcentaje * 100)}% usado",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sobreLimite) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text("El dueño no permitió anotar para este cliente",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = if (onAnotarRapido != null) ({
            IconButton(onClick = onAnotarRapido, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, "Anotar rápido",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }) else null,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}
