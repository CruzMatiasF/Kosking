package com.kosking.app.dueno

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import java.text.SimpleDateFormat
import java.util.*

private sealed class SubPantalla {
    object AgregarCliente : SubPantalla()
    data class DetalleCliente(val id: String) : SubPantalla()
    data class AgregarFiado(val clienteId: String, val tipoInicial: String = "cargo") : SubPantalla()
    data class DetalleMovimiento(val movimientoId: String) : SubPantalla()
}

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Resumen", Icons.Default.Home),
    TabItem("Clientes", Icons.Default.Person),
    TabItem("Productos", Icons.Default.ShoppingCart),
    TabItem("Equipo", Icons.Default.People)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelDueno(
    onCerrarSesion: () -> Unit,
    onVolver: () -> Unit = {},
    vm: DuenoViewModel = viewModel()
) {
    val nombreLocal by vm.nombreLocal.collectAsState()
    val codigoLocal by vm.codigoLocal.collectAsState()
    val nombreUsuario by vm.nombreUsuario.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val movimientosLocal by vm.movimientosLocal.collectAsState()
    val empleados by vm.empleados.collectAsState()
    val totalDeuda by vm.totalDeuda.collectAsState()
    val clientesConDeuda by vm.clientesConDeuda.collectAsState()
    val fiadoHoy by vm.resumenHoy.collectAsState()
    val disputados by vm.disputados.collectAsState()
    val sobreLimite by vm.sobreLimite.collectAsState()

    var tabSeleccionado by remember { mutableStateOf(0) }
    var subPantalla by remember { mutableStateOf<SubPantalla?>(null) }
    var mostrarMenuSesion by remember { mutableStateOf(false) }
    var mostrarEditarLocal by remember { mutableStateOf(false) }

    if (mostrarEditarLocal) {
        PantallaEditarLocal(onVolver = { mostrarEditarLocal = false }, vm = vm)
        return
    }

    val fotoLocalLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) vm.subirFotoLocal(uri) {}
    }

    BackHandler(enabled = subPantalla != null) { subPantalla = null }

    when (val sp = subPantalla) {
        is SubPantalla.AgregarCliente -> {
            PantallaAgregarCliente(onVolver = { subPantalla = null }, vm = vm)
            return
        }
        is SubPantalla.DetalleCliente -> {
            PantallaDetalleCliente(
                clienteId = sp.id,
                onAgregarFiado = { tipo -> subPantalla = SubPantalla.AgregarFiado(sp.id, tipo) },
                onVolver = { subPantalla = null },
                vm = vm
            )
            return
        }
        is SubPantalla.AgregarFiado -> {
            PantallaAgregarFiado(
                clienteId = sp.clienteId,
                onGuardado = { subPantalla = SubPantalla.DetalleCliente(sp.clienteId) },
                onVolver = { subPantalla = SubPantalla.DetalleCliente(sp.clienteId) },
                vm = vm,
                tipoInicial = sp.tipoInicial
            )
            return
        }
        is SubPantalla.DetalleMovimiento -> {
            PantallaDetalleMovimiento(
                movimientoId = sp.movimientoId,
                onVolver = { subPantalla = null },
                vm = vm
            )
            return
        }
        null -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(nombreLocal.ifBlank { "Mi local" })
                        if (codigoLocal.isNotBlank()) {
                            Text("Código: $codigoLocal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { fotoLocalLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddAPhoto, "Foto del local")
                    }
                    Box {
                        IconButton(onClick = { mostrarMenuSesion = true }) {
                            Icon(Icons.Default.MoreVert, "Más opciones")
                        }
                        DropdownMenu(
                            expanded = mostrarMenuSesion,
                            onDismissRequest = { mostrarMenuSesion = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar local") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    mostrarMenuSesion = false
                                    mostrarEditarLocal = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                onClick = {
                                    mostrarMenuSesion = false
                                    vm.cerrarSesion()
                                    onCerrarSesion()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = tabSeleccionado == index,
                        onClick = { tabSeleccionado = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tabSeleccionado) {
                0 -> TabResumen(
                    clientes = clientes,
                    movimientos = movimientosLocal,
                    onIrAClientes = { tabSeleccionado = 1 },
                    onVerMovimiento = { subPantalla = SubPantalla.DetalleMovimiento(it) },
                    onEditarLocal = { mostrarEditarLocal = true },
                    nombreLocal = nombreLocal,
                    nombreUsuario = nombreUsuario,
                    totalDeuda = totalDeuda,
                    clientesConDeuda = clientesConDeuda,
                    fiadoHoy = fiadoHoy.second,
                    cobradoHoy = fiadoHoy.third,
                    disputados = disputados,
                    sobreLimite = sobreLimite
                )
                1 -> TabClientes(
                    clientes = clientes,
                    onVerCliente = { subPantalla = SubPantalla.DetalleCliente(it) },
                    onAgregarCliente = { subPantalla = SubPantalla.AgregarCliente }
                )
                2 -> TabProductos(vm = vm, movimientos = movimientosLocal)
                3 -> TabEquipo(
                    empleados = empleados,
                    movimientos = movimientosLocal,
                    onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                    vm = vm
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun resolverNombreAnotador(anotadoPorNombre: String, nombreLocal: String, nombreUsuario: String): String {
    val nombre = anotadoPorNombre.trim()
    return when {
        nombre.isBlank() -> nombreUsuario.ifBlank { "Vos" }
        nombre.equals(nombreLocal, ignoreCase = true) -> nombreUsuario.ifBlank { "Vos" }
        else -> nombre
    }
}

// ── Tab Resumen ───────────────────────────────────────────────────────────────

@Composable
private fun TabResumen(
    clientes: List<Cliente>,
    movimientos: List<Movimiento>,
    onIrAClientes: () -> Unit,
    onVerMovimiento: (String) -> Unit,
    onEditarLocal: () -> Unit = {},
    nombreLocal: String = "",
    nombreUsuario: String = "",
    totalDeuda: Double = 0.0,
    clientesConDeuda: Int = 0,
    fiadoHoy: Double = 0.0,
    cobradoHoy: Double = 0.0,
    disputados: List<Movimiento> = emptyList(),
    sobreLimite: List<Cliente> = emptyList()
) {
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }
    val alertaTotal = disputados.size + sobreLimite.size

    val ultimoMov = movimientos.firstOrNull()
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tarjeta total
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Te deben en total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$${"%.0f".format(totalDeuda)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (clientesConDeuda > 0) {
                        TextButton(onClick = onIrAClientes, contentPadding = PaddingValues(0.dp)) {
                            Text("$clientesConDeuda cliente${if (clientesConDeuda != 1) "s" else ""} con deuda  →",
                                style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Fiado hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+$${"%.0f".format(fiadoHoy)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cobrado hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-$${"%.0f".format(cobradoHoy)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }

        // Acciones rápidas
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onIrAClientes, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Anotar fiado")
                }
                OutlinedButton(onClick = onIrAClientes, modifier = Modifier.weight(1f)) {
                    Text("Cobrar")
                }
            }
        }

        // Acceso rápido a editar local
        item {
            OutlinedButton(
                onClick = onEditarLocal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Editar mi local (horarios, ubicación, foto...)")
            }
        }

        // Alertas
        if (alertaTotal > 0) {
            item {
                Text("PARA REVISAR · $alertaTotal",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(disputados, key = { "disp_${it.id}" }) { mov ->
                val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: "Cliente"
                Card(modifier = Modifier.fillMaxWidth().clickable { onVerMovimiento(mov.id) }) {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        },
                        headlineContent = {
                            Text("$nombreCliente no reconoce un fiado de $${"%.0f".format(mov.monto)}",
                                fontWeight = FontWeight.Medium)
                        },
                        supportingContent = {
                            Text("anotó ${resolverNombreAnotador(mov.anotadoPorNombre, nombreLocal, nombreUsuario)} · ${fmtHora.format(mov.fecha.toDate())}")
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                    )
                }
            }
            items(sobreLimite, key = { "lim_${it.id}" }) { cliente ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onIrAClientes() }) {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
                        },
                        headlineContent = {
                            Text("${cliente.nombre} superó su límite", fontWeight = FontWeight.Medium)
                        },
                        supportingContent = {
                            Text("Debe $${"%.0f".format(cliente.deudaTotal)} · límite $${"%.0f".format(cliente.limiteCredito)}")
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                    )
                }
            }
        }

        // Último movimiento
        if (ultimoMov != null) {
            item {
                Text("ÚLTIMO MOVIMIENTO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                val esCargo = ultimoMov.tipo == "cargo"
                val nombreCliente = clienteMap[ultimoMov.clienteId]?.nombre ?: ""
                Card(modifier = Modifier.fillMaxWidth().clickable { onVerMovimiento(ultimoMov.id) }) {
                    ListItem(
                        leadingContent = {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (esCargo) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        if (esCargo) "+" else "−",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (esCargo) MaterialTheme.colorScheme.onErrorContainer
                                                else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        },
                        headlineContent = {
                            Text(
                                "${ultimoMov.producto.ifBlank { if (esCargo) "Fiado" else "Pago" }} · $nombreCliente",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            Text("${fmtHora.format(ultimoMov.fecha.toDate())} · ${resolverNombreAnotador(ultimoMov.anotadoPorNombre, nombreLocal, nombreUsuario)}")
                        },
                        trailingContent = {
                            Text(
                                "${if (esCargo) "+" else "−"}$${"%.0f".format(ultimoMov.monto)}",
                                fontWeight = FontWeight.SemiBold,
                                color = if (esCargo) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                        }
                    )
                }
            }
        } else if (movimientos.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Todavía no hay movimientos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Tab Clientes ──────────────────────────────────────────────────────────────

@Composable
private fun TabClientes(
    clientes: List<Cliente>,
    onVerCliente: (String) -> Unit,
    onAgregarCliente: () -> Unit
) {
    var filtro by remember { mutableStateOf("deuda") }
    var busqueda by remember { mutableStateOf("") }

    val conDeuda = remember(clientes) { clientes.count { it.deudaTotal > 0 && it.habilitado } }
    val alDia = remember(clientes) { clientes.count { it.deudaTotal <= 0 && it.habilitado } }

    val clientesFiltrados = remember(clientes, filtro, busqueda) {
        clientes
            .filter { c ->
                when (filtro) {
                    "deuda" -> c.deudaTotal > 0 && c.habilitado
                    "aldia" -> c.deudaTotal <= 0 && c.habilitado
                    "bloqueado" -> !c.habilitado
                    else -> true
                }
            }
            .filter { c -> busqueda.isBlank() || c.nombre.contains(busqueda, ignoreCase = true) }
            .sortedByDescending { it.deudaTotal }
    }

    Column(Modifier.fillMaxSize()) {
        // Búsqueda + agregar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar cliente") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) }
            )
            IconButton(
                onClick = onAgregarCliente,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.PersonAdd, "Agregar cliente")
            }
        }

        // Filtros con conteo
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filtro == "deuda",
                    onClick = { filtro = "deuda" },
                    label = { Text("Deben · $conDeuda") }
                )
            }
            item {
                FilterChip(
                    selected = filtro == "aldia",
                    onClick = { filtro = "aldia" },
                    label = { Text("Al día · $alDia") }
                )
            }
            item {
                FilterChip(
                    selected = filtro == "todos",
                    onClick = { filtro = "todos" },
                    label = { Text("Todos") }
                )
            }
            item {
                FilterChip(
                    selected = filtro == "bloqueado",
                    onClick = { filtro = "bloqueado" },
                    label = { Text("Bloqueados") }
                )
            }
        }

        if (clientesFiltrados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (clientes.isEmpty()) "Todavía no hay clientes\nTocá + para agregar el primero"
                    else "Sin resultados para este filtro",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(clientesFiltrados, key = { it.id }) { cliente ->
                    Column(
                        Modifier
                            .clickable { onVerCliente(cliente.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            // Avatar con inicial
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (cliente.deudaTotal > 0) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        cliente.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cliente.nombre, fontWeight = FontWeight.Medium)
                                if (!cliente.habilitado) {
                                    Text("Bloqueado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text(
                                if (cliente.deudaTotal > 0) "$${"%.0f".format(cliente.deudaTotal)}" else "Al día",
                                fontWeight = FontWeight.Bold,
                                color = if (cliente.deudaTotal > 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                            )
                        }
                        // Barra de progreso si tiene límite configurado
                        if (cliente.limiteCredito > 0) {
                            Spacer(Modifier.height(6.dp))
                            val progreso = (cliente.deudaTotal / cliente.limiteCredito).coerceIn(0.0, 1.0).toFloat()
                            val superado = cliente.deudaTotal > cliente.limiteCredito
                            LinearProgressIndicator(
                                progress = { progreso },
                                modifier = Modifier.fillMaxWidth().padding(start = 52.dp),
                                color = if (superado) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (superado) "Superó el límite de $${"%.0f".format(cliente.limiteCredito)}"
                                else "${"%.0f".format(progreso * 100)}% del límite de $${"%.0f".format(cliente.limiteCredito)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (superado) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 52.dp, top = 2.dp)
                            )
                        }
                    }
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Tab Productos ─────────────────────────────────────────────────────────────

@Composable
private fun TabProductos(vm: DuenoViewModel, movimientos: List<Movimiento> = emptyList()) {
    val productos by vm.productos.collectAsState()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    var productoEditarPrecio by remember { mutableStateOf<Producto?>(null) }
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
    var filtro by remember { mutableStateOf("todos") }

    // Conteo de veces que cada producto fue fiado
    val conteoFiados = remember(movimientos) {
        movimientos
            .filter { it.tipo == "cargo" && !it.anulado && it.producto.isNotBlank() }
            .groupBy { it.producto.trim().lowercase() }
            .mapValues { it.value.size }
    }

    val enOferta = remember(productos) { productos.count { it.esOferta && it.habilitado } }
    val ocultos = remember(productos) { productos.count { !it.habilitado } }

    val productosFiltrados = remember(productos, filtro, busqueda, conteoFiados) {
        productos
            .filter { p ->
                when (filtro) {
                    "oferta" -> p.esOferta && p.habilitado
                    "oculto" -> !p.habilitado
                    else -> true
                }
            }
            .filter { p -> busqueda.isBlank() || p.nombre.contains(busqueda, ignoreCase = true) }
            .sortedByDescending { p -> conteoFiados[p.nombre.trim().lowercase()] ?: 0 }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Búsqueda + nuevo
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar producto") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) }
                )
                Button(onClick = { productoEditar = null; mostrarDialogo = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nuevo")
                }
            }

            // Filtros con conteo
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filtro == "todos",
                        onClick = { filtro = "todos" },
                        label = { Text("Todos · ${productos.size}") }
                    )
                }
                item {
                    FilterChip(
                        selected = filtro == "oferta",
                        onClick = { filtro = "oferta" },
                        label = { Text("En oferta · $enOferta") }
                    )
                }
                item {
                    FilterChip(
                        selected = filtro == "oculto",
                        onClick = { filtro = "oculto" },
                        label = { Text("Ocultos · $ocultos") }
                    )
                }
            }

            if (productosFiltrados.isEmpty() && busqueda.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No hay productos cargados",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Tocá Nuevo para agregar precios",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val mostrarOrdenLabel = conteoFiados.isNotEmpty() && busqueda.isBlank() && filtro == "todos"
                if (mostrarOrdenLabel) {
                    Text(
                        "ORDENADOS POR LOS MÁS FIADOS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(productosFiltrados, key = { it.id }) { prod ->
                        val vecesFiado = conteoFiados[prod.nombre.trim().lowercase()] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { productoEditar = prod; mostrarDialogo = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        productoParaFoto = prod
                                        fotoProductoLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
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
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // overlay cámara
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt, null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(prod.nombre, fontWeight = FontWeight.Medium)
                                    if (prod.esOferta) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                            Text("OFERTA",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    if (!prod.habilitado) {
                                        Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                            Text("OCULTO",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                val subtitulo = buildString {
                                    if (prod.descripcion.isNotBlank()) append(prod.descripcion)
                                    if (vecesFiado > 0) {
                                        if (isNotEmpty()) append(" · ")
                                        append("fiado $vecesFiado ve${if (vecesFiado == 1) "z" else "ces"}")
                                    }
                                }
                                if (subtitulo.isNotBlank()) {
                                    Text(subtitulo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            // Precio como botón tappable para edición rápida
                            OutlinedCard(onClick = { productoEditarPrecio = prod }) {
                                Text(
                                    "$${"%.0f".format(prod.precio)}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                    item {
                        Text(
                            "Tocá el precio para cambiarlo rápido.\nTocá el producto para editar todo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Diálogo de edición rápida de precio
    productoEditarPrecio?.let { prod ->
        var nuevoPrecio by remember(prod.id) { mutableStateOf("%.0f".format(prod.precio)) }
        AlertDialog(
            onDismissRequest = { productoEditarPrecio = null },
            title = { Text(prod.nombre) },
            text = {
                OutlinedTextField(
                    value = nuevoPrecio,
                    onValueChange = { nuevoPrecio = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Precio") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val precio = nuevoPrecio.toDoubleOrNull()
                        if (precio != null && precio >= 0) {
                            vm.actualizarProducto(prod.copy(precio = precio))
                            productoEditarPrecio = null
                        }
                    },
                    enabled = nuevoPrecio.toDoubleOrNull() != null
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { productoEditarPrecio = null }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDialogo) {
        DialogProducto(
            productoInicial = productoEditar,
            productosExistentes = if (productoEditar == null) productos else emptyList(),
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
            text = { Text("¿Eliminar ${prod.nombre} de la lista?") },
            confirmButton = {
                TextButton(onClick = { vm.eliminarProducto(prod.id); productoAEliminar = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { productoAEliminar = null }) { Text("Cancelar") } }
        )
    }
}

// ── Tab Equipo ─────────────────────────────────────────────────────────────────

@Composable
private fun TabEquipo(
    empleados: List<Empleado>,
    movimientos: List<Movimiento>,
    onEliminarEmpleado: (String) -> Unit,
    vm: DuenoViewModel
) {
    val context = LocalContext.current
    val vmCargando by vm.cargando.collectAsState()
    val vmError by vm.error.collectAsState()
    var mostrarDialogNombre by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<Pair<String, String>?>(null) }
    var empleadoAEliminar by remember { mutableStateOf<Empleado?>(null) }

    val duenoUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    val inicioDia = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val movsHoy = remember(movimientos, inicioDia) {
        movimientos.filter { !it.anulado && it.fecha.toDate().time >= inicioDia }
    }

    // Caja de hoy: stats por persona (uid → nombre, anotó N, cobró $X)
    data class StatPersona(val uid: String, val nombre: String, val anoto: Int, val cobro: Double)

    val statsHoy = remember(movsHoy, duenoUid) {
        val map = mutableMapOf<String, StatPersona>()
        movsHoy.forEach { mov ->
            val uid = mov.anotadoPorUid.ifBlank { duenoUid }
            val nombre = if (uid == duenoUid) "Vos"
                         else mov.anotadoPorNombre.ifBlank { "Empleado" }
            val actual = map[uid] ?: StatPersona(uid, nombre, 0, 0.0)
            map[uid] = actual.copy(
                anoto = actual.anoto + 1,
                cobro = actual.cobro + if (mov.tipo == "pago") mov.monto else 0.0
            )
        }
        map.values.sortedByDescending { if (it.uid == duenoUid) Int.MAX_VALUE else it.anoto }
    }

    val totalCobradoHoy = remember(statsHoy) { statsHoy.sumOf { it.cobro } }

    // Último movimiento por uid
    val ultimoMovPorUid = remember(movimientos) {
        movimientos.groupBy { it.anotadoPorUid }.mapValues { it.value.first() }
    }

    // Disputas por uid
    val disputasPorUid = remember(movimientos) {
        movimientos.filter { it.disputado && !it.anulado }
            .groupBy { it.anotadoPorUid }
            .mapValues { it.value.size }
    }

    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Caja de hoy
        if (statsHoy.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Caja de hoy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(
                                "Cobrado $${"%.0f".format(totalCobradoHoy)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        // Encabezado tabla
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quién",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                            Text("Anotó",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp))
                            Text("Cobró",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(72.dp))
                        }
                        HorizontalDivider()
                        statsHoy.forEach { stat ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stat.nombre,
                                    fontWeight = if (stat.uid == duenoUid) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f))
                                Text("${stat.anoto}",
                                    modifier = Modifier.width(48.dp),
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "$${"%.0f".format(stat.cobro)}",
                                    modifier = Modifier.width(72.dp),
                                    color = if (stat.cobro > 0) Color(0xFF2E7D32)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (stat.cobro > 0) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Encabezado empleados
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EMPLEADOS · ${empleados.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { mostrarDialogNombre = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Sumar")
                }
            }
        }

        // Info permisos
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null,
                        Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        "Los empleados pueden anotar fiados y registrar pagos. Controlá los permisos por cliente desde el detalle de cada uno.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (empleados.isEmpty()) {
            item {
                Text("Sin empleados. Tocá + Sumar para generar un código.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(empleados, key = { it.id }) { emp ->
                val ultimoMov = ultimoMovPorUid[emp.uid]
                val reclamos = disputasPorUid[emp.uid] ?: 0
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        leadingContent = {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        emp.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        headlineContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(emp.nombre, fontWeight = FontWeight.Medium)
                                if (reclamos > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                        Text("$reclamos reclamo${if (reclamos != 1) "s" else ""}",
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        },
                        supportingContent = {
                            if (ultimoMov != null) {
                                val accion = if (ultimoMov.tipo == "cargo") "Anotó fiado" else "Cobró"
                                Text("Último: hoy ${fmtHora.format(ultimoMov.fecha.toDate())} · $accion",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { empleadoAEliminar = emp }) {
                                Icon(Icons.Default.Delete, "Eliminar",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }
    }

    if (mostrarDialogNombre) {
        DialogNombreEmpleado(
            cargando = vmCargando,
            error = vmError,
            onGuardar = { nombre, whatsapp ->
                vm.generarCodigoEmpleado(nombre) { codigo ->
                    mostrarDialogNombre = false
                    codigoGenerado = codigo to whatsapp
                }
            },
            onDismiss = { mostrarDialogNombre = false }
        )
    }

    empleadoAEliminar?.let { emp ->
        AlertDialog(
            onDismissRequest = { empleadoAEliminar = null },
            title = { Text("Eliminar empleado") },
            text = { Text("¿Eliminar a ${emp.nombre}? Se va a desconectar del local.") },
            confirmButton = {
                TextButton(onClick = { onEliminarEmpleado(emp.id); empleadoAEliminar = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { empleadoAEliminar = null }) { Text("Cancelar") } }
        )
    }

    codigoGenerado?.let { (codigo, whatsapp) ->
        AlertDialog(
            onDismissRequest = { codigoGenerado = null },
            title = { Text("Código generado") },
            text = {
                Column {
                    Text("Compartí este código con el empleado:")
                    Spacer(Modifier.height(12.dp))
                    Text(codigo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("El empleado lo ingresa en la app para acceder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                if (whatsapp.isNotBlank()) {
                    TextButton(onClick = {
                        val numero = whatsapp.replace(Regex("[^0-9]"), "")
                        val mensaje = "Tu código de empleado para Kosking es: $codigo — Descargá la app y usá este código para conectarte al local."
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        codigoGenerado = null
                    }) { Text("Enviar por WhatsApp") }
                } else {
                    TextButton(onClick = { codigoGenerado = null }) { Text("Listo") }
                }
            },
            dismissButton = if (whatsapp.isNotBlank()) {
                { TextButton(onClick = { codigoGenerado = null }) { Text("Cerrar") } }
            } else null
        )
    }
}

// ── Panel embebido para usar dentro de PantallaExplorar (TabMás) ──────────────

@Composable
fun PanelDuenoEmbedido(
    vm: DuenoViewModel,
    tabSeleccionado: Int = 0,
    onTabChange: (Int) -> Unit = {},
    onAbrirAgregarCliente: () -> Unit,
    onAbrirDetalleCliente: (String) -> Unit,
    onAbrirAgregarFiado: (clienteId: String, tipo: String) -> Unit,
    onAbrirDetalleMovimiento: (String) -> Unit
) {
    val clientes by vm.clientes.collectAsState()
    val movimientosLocal by vm.movimientosLocal.collectAsState()
    val empleados by vm.empleados.collectAsState()
    val nombreLocal by vm.nombreLocal.collectAsState()
    val nombreUsuario by vm.nombreUsuario.collectAsState()
    val totalDeuda by vm.totalDeuda.collectAsState()
    val clientesConDeuda by vm.clientesConDeuda.collectAsState()
    val fiadoHoy by vm.resumenHoy.collectAsState()
    val disputados by vm.disputados.collectAsState()
    val sobreLimite by vm.sobreLimite.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabSeleccionado) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = tabSeleccionado == index,
                    onClick = { onTabChange(index) },
                    icon = { Icon(tab.icon, null, Modifier.size(18.dp)) },
                    text = { Text(tab.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (tabSeleccionado) {
                0 -> TabResumen(
                    clientes = clientes,
                    movimientos = movimientosLocal,
                    onIrAClientes = { onTabChange(1) },
                    onVerMovimiento = onAbrirDetalleMovimiento,
                    nombreLocal = nombreLocal,
                    nombreUsuario = nombreUsuario,
                    totalDeuda = totalDeuda,
                    clientesConDeuda = clientesConDeuda,
                    fiadoHoy = fiadoHoy.second,
                    cobradoHoy = fiadoHoy.third,
                    disputados = disputados,
                    sobreLimite = sobreLimite
                )
                1 -> TabClientes(
                    clientes = clientes,
                    onVerCliente = onAbrirDetalleCliente,
                    onAgregarCliente = onAbrirAgregarCliente
                )
                2 -> TabProductos(vm = vm, movimientos = movimientosLocal)
                3 -> TabEquipo(
                    empleados = empleados,
                    movimientos = movimientosLocal,
                    onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                    vm = vm
                )
            }
        }
    }
}
