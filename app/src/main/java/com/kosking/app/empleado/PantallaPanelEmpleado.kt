package com.kosking.app.empleado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.AnotarRapidoSheet
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
    val nombreEmpleado by vm.nombreEmpleado.collectAsState()
    val clientes by vm.clientesHabilitados.collectAsState()
    val movimientos by vm.movimientosLocal.collectAsState()
    var mostrarMenu by remember { mutableStateOf(false) }
    var mostrarConfirmDesvincularse by remember { mutableStateOf(false) }
    var mostrarEditarNombre by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var clienteConAlerta by remember { mutableStateOf<Cliente?>(null) }
    var clienteAnotar by remember { mutableStateOf<Cliente?>(null) }

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
                        DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
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
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Mini stats del día
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

            // Barra de búsqueda
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
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box {
                            if (busqueda.isEmpty()) Text("Buscar cliente...",
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
                            onClick = if (cliente.habilitado) ({
                                if (cliente.alertaEmpleado) clienteConAlerta = cliente
                                else onVerCliente(cliente.id)
                            }) else null,
                            onAnotarRapido = if (cliente.habilitado) ({
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

    clienteConAlerta?.let { c ->
        AlertDialog(
            onDismissRequest = { clienteConAlerta = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFE65100)) },
            title = { Text("Atención") },
            text = { Text("El dueño marcó a ${c.nombre} con una alerta. Revisá antes de anotar.") },
            confirmButton = {
                TextButton(onClick = { clienteConAlerta = null; onVerCliente(c.id) }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { clienteConAlerta = null }) { Text("Cancelar") }
            }
        )
    }

    clienteAnotar?.let { c ->
        AnotarRapidoSheet(
            clientes = clientes,
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
                    onClick = { if (nombreEditar.isNotBlank()) { vm.actualizarMiNombre(nombreEditar.trim()); mostrarEditarNombre = false } },
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
                TextButton(onClick = { mostrarConfirmDesvincularse = false; vm.desvincularme { onCerrarSesion() } }) {
                    Text("Desvincularme", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmDesvincularse = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ClienteItemEmpleado(
    cliente: Cliente,
    onClick: (() -> Unit)?,
    onAnotarRapido: (() -> Unit)? = null
) {
    val tieneLimite = cliente.limiteCredito > 0
    val sobreLimite = tieneLimite && cliente.deudaTotal > cliente.limiteCredito

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
                    Icon(Icons.Default.Warning, "Alerta", tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                }
            }
        },
        supportingContent = {
            if (cliente.habilitado) {
                Text(when {
                    cliente.deudaTotal > 0 -> "Debe $${"%.0f".format(cliente.deudaTotal)}"
                    else -> "Sin deuda"
                })
            } else {
                Text("El dueño no permitió anotar para este cliente",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = if (onAnotarRapido != null) ({
            IconButton(onClick = onAnotarRapido, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, "Anotar", tint = MaterialTheme.colorScheme.primary)
            }
        }) else null,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}
