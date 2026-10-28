package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleCliente(
    clienteId: String,
    onAgregarFiado: () -> Unit,
    onVolver: () -> Unit,
    vm: DuenoViewModel = viewModel()
) {
    LaunchedEffect(clienteId) { vm.seleccionarCliente(clienteId) }

    val movimientos by vm.movimientosDetalle.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val cliente = clientes.find { it.id == clienteId }
    val error by vm.error.collectAsState()
    val cargando by vm.cargando.collectAsState()

    var mostrarConfirmarEliminar by remember { mutableStateOf(false) }

    if (mostrarConfirmarEliminar && cliente != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarEliminar = false },
            title = { Text("Eliminar cliente") },
            text = { Text("¿Seguro que querés eliminar a ${cliente.nombre}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmarEliminar = false
                        vm.eliminarCliente(cliente) { onVolver() }
                    }
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarEliminar = false }) { Text("Cancelar") }
            }
        )
    }

    error?.let {
        if (it.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { vm.error.value = null },
                title = { Text("No se puede eliminar") },
                text = { Text(it) },
                confirmButton = {
                    TextButton(onClick = { vm.error.value = null }) { Text("Entendido") }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cliente?.nombre ?: "") },
                actions = {
                    if (movimientos.isEmpty() && cliente != null) {
                        IconButton(onClick = { mostrarConfirmarEliminar = true }) {
                            Icon(Icons.Default.Delete, "Eliminar cliente",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAgregarFiado) {
                Icon(Icons.Default.Add, "Agregar movimiento")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            cliente?.let {
                item { ResumenCliente(it, vm) }
            }
            if (movimientos.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Sin movimientos todavía",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(movimientos) { mov ->
                    ItemMovimiento(mov)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ResumenCliente(cliente: Cliente, vm: DuenoViewModel) {


    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Deuda total", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$${"%.2f".format(cliente.deudaTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (cliente.deudaTotal > 0)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            if (cliente.domicilio.isNotBlank())
                Text("📍 ${cliente.domicilio}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (cliente.telefono.isNotBlank())
                Text("📱 ${cliente.telefono}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (cliente.codigoAcceso.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "🔑 Código: ${cliente.codigoAcceso}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Permiso para empleados
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.toggleHabilitado(cliente) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Empleados pueden anotar",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (cliente.habilitado)
                            "Los empleados pueden registrar fiados y pagos"
                        else
                            "Solo vos podés anotar para este cliente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = cliente.habilitado,
                    onCheckedChange = { vm.toggleHabilitado(cliente) }
                )
            }
        }
    }
}

@Composable
private fun ItemMovimiento(mov: Movimiento) {
    val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    val esCargo = mov.tipo == "cargo"

    ListItem(
        headlineContent = { Text(mov.producto.ifBlank { if (esCargo) "Fiado" else "Pago" }) },
        supportingContent = {
            Text("${mov.anotadoPorNombre} · ${fmt.format(mov.fecha.toDate())}")
        },
        trailingContent = {
            Text(
                "${if (esCargo) "+" else "-"}${"%.2f".format(mov.monto)}",
                color = if (esCargo) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium
            )
        }
    )
}
