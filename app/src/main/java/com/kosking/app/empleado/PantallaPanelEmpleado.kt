package com.kosking.app.empleado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPanelEmpleado(
    onVerCliente: (String) -> Unit,
    onCerrarSesion: () -> Unit,
    onVolver: () -> Unit = {},
    vm: EmpleadoViewModel
) {
    val nombreLocal by vm.nombreLocal.collectAsState()
    val clientes by vm.clientesHabilitados.collectAsState()
    val movimientos by vm.movimientosLocal.collectAsState()
    var mostrarMenu by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }

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
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Mini stats
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

            // Búsqueda
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
}

@Composable
private fun ClienteItemEmpleado(cliente: Cliente, onClick: (() -> Unit)?) {
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
            }
        },
        supportingContent = {
            if (cliente.habilitado) {
                Text(
                    if (cliente.deudaTotal > 0) "Debe $${"%.2f".format(cliente.deudaTotal)}"
                    else "Sin deuda"
                )
            } else {
                Text("El dueño no permitió anotar para este cliente",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}
