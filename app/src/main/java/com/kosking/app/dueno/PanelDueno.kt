package com.kosking.app.dueno

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.*

private sealed class SubPantalla {
    object AgregarCliente : SubPantalla()
    data class DetalleCliente(val id: String) : SubPantalla()
    data class AgregarFiado(val clienteId: String) : SubPantalla()
    data class DetalleMovimiento(val movimientoId: String) : SubPantalla()
}

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Clientes", Icons.Default.Person),
    TabItem("Actividad", Icons.Default.List),
    TabItem("Productos", Icons.Default.ShoppingCart),
    TabItem("Empleados", Icons.Default.People)
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
    val clientes by vm.clientes.collectAsState()
    val movimientosLocal by vm.movimientosLocal.collectAsState()
    val empleados by vm.empleados.collectAsState()

    var tabSeleccionado by remember { mutableStateOf(0) }
    var subPantalla by remember { mutableStateOf<SubPantalla?>(null) }
    var mostrarMenuSesion by remember { mutableStateOf(false) }

    // Atrás en sub-pantalla → volver al panel; en panel principal → volver a Explorar
    BackHandler(enabled = subPantalla != null) { subPantalla = null }

    // Sub-pantallas full-screen
    when (val sp = subPantalla) {
        is SubPantalla.AgregarCliente -> {
            PantallaAgregarCliente(onVolver = { subPantalla = null }, vm = vm)
            return
        }
        is SubPantalla.DetalleCliente -> {
            PantallaDetalleCliente(
                clienteId = sp.id,
                onAgregarFiado = { subPantalla = SubPantalla.AgregarFiado(sp.id) },
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
                vm = vm
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
                    Box {
                        IconButton(onClick = { mostrarMenuSesion = true }) {
                            Icon(Icons.Default.MoreVert, "Más opciones")
                        }
                        DropdownMenu(
                            expanded = mostrarMenuSesion,
                            onDismissRequest = { mostrarMenuSesion = false }
                        ) {
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
        },
        floatingActionButton = {
            if (tabSeleccionado == 0) {
                FloatingActionButton(onClick = { subPantalla = SubPantalla.AgregarCliente }) {
                    Icon(Icons.Default.Add, "Agregar cliente")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tabSeleccionado) {
                0 -> TabClientes(
                    clientes = clientes,
                    onVerCliente = { subPantalla = SubPantalla.DetalleCliente(it) }
                )
                1 -> TabActividad(
                    movimientos = movimientosLocal,
                    clientes = clientes,
                    empleados = empleados,
                    onVerMovimiento = { subPantalla = SubPantalla.DetalleMovimiento(it) }
                )
                2 -> TabProductos(vm = vm)
                3 -> TabAjustes(
                    empleados = empleados,
                    onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                    vm = vm
                )
            }
        }
    }
}

// ── Tab Clientes ──────────────────────────────────────────────────────────────

@Composable
private fun TabClientes(
    clientes: List<Cliente>,
    onVerCliente: (String) -> Unit
) {
    var filtro by remember { mutableStateOf("todos") }
    var busqueda by remember { mutableStateOf("") }

    val totalDeuda = remember(clientes) { clientes.sumOf { it.deudaTotal } }
    val conDeuda = remember(clientes) { clientes.count { it.deudaTotal > 0 && it.habilitado } }

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
    }

    Column(Modifier.fillMaxSize()) {
        // Header card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total adeudado", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$${"%.2f".format(totalDeuda)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totalDeuda > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Con deuda", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$conDeuda cliente${if (conDeuda != 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Búsqueda
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar cliente...") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )

        // Filtros
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filtros = listOf("todos" to "Todos", "deuda" to "Con deuda", "aldia" to "Al día", "bloqueado" to "Bloqueados")
            items(filtros) { (key, label) ->
                FilterChip(selected = filtro == key, onClick = { filtro = key }, label = { Text(label) })
            }
        }

        if (clientesFiltrados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (clientes.isEmpty()) "Todavía no hay clientes\nTocá + para agregar el primero"
                    else "Sin resultados",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(clientesFiltrados, key = { it.id }) { cliente ->
                    ListItem(
                        headlineContent = { Text(cliente.nombre, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(
                                if (cliente.deudaTotal > 0) "Debe $${"%.2f".format(cliente.deudaTotal)}"
                                else "Sin deuda"
                            )
                        },
                        trailingContent = {
                            if (!cliente.habilitado)
                                Text("Bloqueado", color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.clickable { onVerCliente(cliente.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

// ── Tab Actividad ─────────────────────────────────────────────────────────────

private enum class PeriodoActividad { HOY, AYER, SEMANA }

@Composable
private fun TabActividad(
    movimientos: List<Movimiento>,
    clientes: List<Cliente>,
    empleados: List<Empleado>,
    onVerMovimiento: (String) -> Unit
) {
    val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }

    var periodo by remember { mutableStateOf(PeriodoActividad.HOY) }
    var empleadoFiltro by remember { mutableStateOf<String?>(null) } // uid o null = todos

    val ahora = System.currentTimeMillis()
    val movsFiltradosPeriodo = remember(movimientos, periodo) {
        val cal = Calendar.getInstance()
        when (periodo) {
            PeriodoActividad.HOY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                movimientos.filter { it.fecha.toDate().time >= cal.timeInMillis }
            }
            PeriodoActividad.AYER -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val finAyer = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -1)
                movimientos.filter { it.fecha.toDate().time in cal.timeInMillis until finAyer }
            }
            PeriodoActividad.SEMANA -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                movimientos.filter { it.fecha.toDate().time >= cal.timeInMillis }
            }
        }
    }

    val movsFiltrados = remember(movsFiltradosPeriodo, empleadoFiltro) {
        if (empleadoFiltro == null) movsFiltradosPeriodo
        else movsFiltradosPeriodo.filter { it.anotadoPorUid == empleadoFiltro }
    }

    val disputados = remember(movsFiltrados) { movsFiltrados.filter { it.disputado && !it.anulado } }

    val nombresEmpleados = remember(movsFiltradosPeriodo) {
        movsFiltradosPeriodo.map { it.anotadoPorNombre to it.anotadoPorUid }
            .distinctBy { it.second }
            .filter { it.first.isNotBlank() }
    }

    Column(Modifier.fillMaxSize()) {
        // Filtros de período
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(selected = periodo == PeriodoActividad.HOY,
                    onClick = { periodo = PeriodoActividad.HOY }, label = { Text("Hoy") })
            }
            item {
                FilterChip(selected = periodo == PeriodoActividad.AYER,
                    onClick = { periodo = PeriodoActividad.AYER }, label = { Text("Ayer") })
            }
            item {
                FilterChip(selected = periodo == PeriodoActividad.SEMANA,
                    onClick = { periodo = PeriodoActividad.SEMANA }, label = { Text("Esta semana") })
            }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

            // Alertas disputados
            if (disputados.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "⚠ ${disputados.size} movimiento${if (disputados.size > 1) "s" else ""} no reconocido${if (disputados.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            disputados.forEach { mov ->
                                val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: "Cliente"
                                TextButton(
                                    onClick = { onVerMovimiento(mov.id) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Revisar — $nombreCliente $${"%.0f".format(mov.monto)}",
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Filtro por empleado (si hay más de uno)
            if (nombresEmpleados.size > 1) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(selected = empleadoFiltro == null,
                                onClick = { empleadoFiltro = null }, label = { Text("Todos") })
                        }
                        items(nombresEmpleados) { (nombre, uid) ->
                            FilterChip(selected = empleadoFiltro == uid,
                                onClick = { empleadoFiltro = uid }, label = { Text(nombre) })
                        }
                    }
                }
            }

            if (movsFiltrados.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("Sin movimientos en este período",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(movsFiltrados.take(100), key = { it.id }) { mov ->
                    val esCargo = mov.tipo == "cargo"
                    val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: "Cliente"
                    ListItem(
                        modifier = Modifier.clickable { onVerMovimiento(mov.id) },
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(nombreCliente, fontWeight = FontWeight.Medium)
                                if (mov.anulado) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text("Anulado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else if (mov.disputado) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                        Text("⚠ No reconocido", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        },
                        supportingContent = {
                            Text("${mov.producto.ifBlank { if (esCargo) "Fiado" else "Pago" }} · ${mov.anotadoPorNombre.ifBlank { "Dueño" }} · ${fmt.format(mov.fecha.toDate())}")
                        },
                        trailingContent = {
                            Text(
                                "${if (esCargo) "+" else "-"}${"%.2f".format(mov.monto)}",
                                color = if (mov.anulado) MaterialTheme.colorScheme.onSurfaceVariant
                                        else if (esCargo) MaterialTheme.colorScheme.error
                                        else Color(0xFF2E7D32),
                                fontWeight = if (mov.anulado) FontWeight.Normal else FontWeight.Medium
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

// ── Tab Productos ─────────────────────────────────────────────────────────────

@Composable
private fun TabProductos(vm: DuenoViewModel) {
    val productos by vm.productos.collectAsState()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<com.kosking.app.modelo.Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<com.kosking.app.modelo.Producto?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (productos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No hay productos cargados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Tocá + para agregar precios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text("Los empleados y clientes ven estos precios en tiempo real",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(productos, key = { it.id }) { prod ->
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
                                Text("$${"%.2f".format(prod.precio)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
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
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { productoEditar = null; mostrarDialogo = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Agregar producto")
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

// ── Tab Ajustes ───────────────────────────────────────────────────────────────

@Composable
private fun TabAjustes(
    empleados: List<Empleado>,
    onEliminarEmpleado: (String) -> Unit,
    vm: DuenoViewModel
) {
    val context = LocalContext.current
    val vmCargando by vm.cargando.collectAsState()
    val vmError by vm.error.collectAsState()
    var mostrarDialogNombre by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<Pair<String, String>?>(null) }
    var empleadoAEliminar by remember { mutableStateOf<Empleado?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("¿Cómo funcionan los permisos?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Los empleados pueden anotar fiados y registrar pagos. El control es por cliente: desde el detalle de cada cliente podés activar o desactivar si los empleados pueden anotar para esa persona.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Empleados", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { mostrarDialogNombre = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar")
                }
            }
        }

        if (empleados.isEmpty()) {
            item {
                Text("Sin empleados. Tocá Agregar para generar un código.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(empleados, key = { it.id }) { emp ->
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(emp.nombre) },
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

    // Dialogs
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
                    Text(codigo, style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

@Composable
private fun PermisoSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) }
    )
}

// ── Panel embebido para usar dentro de PantallaExplorar (TabMás) ──────────────

/**
 * Versión del panel de dueño sin Scaffold propio.
 * Se embebe dentro del TabMás de PantallaExplorar para mantener la barra
 * de navegación principal siempre visible.
 */
@Composable
fun PanelDuenoEmbedido(
    vm: DuenoViewModel,
    onAbrirAgregarCliente: () -> Unit,
    onAbrirDetalleCliente: (String) -> Unit,
    onAbrirAgregarFiado: (String) -> Unit,
    onAbrirDetalleMovimiento: (String) -> Unit
) {
    val clientes by vm.clientes.collectAsState()
    val movimientosLocal by vm.movimientosLocal.collectAsState()
    val empleados by vm.empleados.collectAsState()

    var tabSeleccionado by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabSeleccionado) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = tabSeleccionado == index,
                    onClick = { tabSeleccionado = index },
                    text = { Text(tab.label) }
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (tabSeleccionado) {
                0 -> TabClientes(clientes, onVerCliente = onAbrirDetalleCliente)
                1 -> TabActividad(movimientosLocal, clientes, empleados,
                    onVerMovimiento = onAbrirDetalleMovimiento)
                2 -> TabProductos(vm = vm)
                3 -> TabAjustes(
                    empleados = empleados,
                    onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                    vm = vm
                )
            }
            // FAB para agregar cliente (solo en tab Clientes)
            if (tabSeleccionado == 0) {
                FloatingActionButton(
                    onClick = onAbrirAgregarCliente,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) { Icon(Icons.Default.Add, "Agregar cliente") }
            }
        }
    }
}
