package com.kosking.app.empleado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DialogProducto
import com.kosking.app.modelo.Cliente
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
    val clientes by vm.clientesHabilitados.collectAsState()
    val movimientos by vm.movimientosLocal.collectAsState()
    val productos by vm.productos.collectAsState()
    var mostrarMenu by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var tabActual by remember { mutableStateOf(tabInicial) }

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
                                text = { Text("Cerrar sesión") },
                                onClick = {
                                    mostrarMenu = false
                                    vm.cerrarSesion()
                                    onCerrarSesion()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabActual) {
                Tab(selected = tabActual == 0, onClick = { tabActual = 0 },
                    text = { Text("Clientes") })
                Tab(selected = tabActual == 1, onClick = { tabActual = 1 },
                    text = { Text("Precios") })
            }

            when (tabActual) {
                0 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$fiadosHoy", style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold)
                                        Text("fiado${if (fiadosHoy != 1) "s" else ""} hoy",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    VerticalDivider(modifier = Modifier.height(40.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$${"%.0f".format(cobradoHoy)}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold)
                                        Text("cobrado hoy",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = busqueda,
                                onValueChange = { busqueda = it },
                                placeholder = { Text("Buscar cliente...") },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                singleLine = true
                            )
                        }
                        item {
                            Text("Clientes", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                        if (clientesFiltrados.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        if (clientes.isEmpty()) "No hay clientes todavía"
                                        else "Sin resultados",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(clientesFiltrados, key = { it.id }) { cliente ->
                                ClienteItemEmpleado(
                                    cliente = cliente,
                                    onClick = if (cliente.habilitado) ({ onVerCliente(cliente.id) }) else null
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
                1 -> TabPreciosEmpleado(vm = vm, productos = productos)
            }
        }
    }
}

@Composable
private fun TabPreciosEmpleado(vm: EmpleadoViewModel, productos: List<Producto>) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (productos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No hay productos cargados",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Tocá + para agregar precios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text("Podés agregar y editar precios. Solo el dueño puede eliminar.",
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
}

@Composable
private fun ClienteItemEmpleado(cliente: Cliente, onClick: (() -> Unit)?) {
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
                        if (cliente.deudaTotal > 0) "Debe $${"%.2f".format(cliente.deudaTotal)}"
                        else "Sin deuda"
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
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}
