package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.KeyboardType
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
    onVolver: () -> Unit,
    onVerMovimiento: (String) -> Unit = {},
    vm: DuenoViewModel = viewModel()
) {
    LaunchedEffect(clienteId) { vm.seleccionarCliente(clienteId) }

    val movimientos by vm.movimientosDetalle.collectAsState()
    // Esperar al menos un tick de carga antes de mostrar el botón eliminar
    var movimientosCargados by remember { mutableStateOf(false) }
    LaunchedEffect(movimientos) { movimientosCargados = true }
    val clientes by vm.clientes.collectAsState()
    val cliente = clientes.find { it.id == clienteId }
    val error by vm.error.collectAsState()
    val cargando by vm.cargando.collectAsState()
    val nombreLocal by vm.nombreLocal.collectAsState()
    val nombreUsuario by vm.nombreUsuario.collectAsState()

    var mostrarConfirmarEliminar by remember { mutableStateOf(false) }
    var mostrarEditar by remember { mutableStateOf(false) }

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
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (cliente != null) {
                        IconButton(onClick = { mostrarEditar = true }) {
                            Icon(Icons.Default.Edit, "Editar cliente")
                        }
                    }
                    if (movimientosCargados && movimientos.isEmpty() && cliente != null) {
                        IconButton(onClick = { mostrarConfirmarEliminar = true }) {
                            Icon(Icons.Default.Delete, "Eliminar cliente",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
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
                    ItemMovimiento(
                        mov = mov,
                        nombreLocal = nombreLocal,
                        nombreUsuario = nombreUsuario,
                        onClick = { onVerMovimiento(mov.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // Diálogo editar cliente
    if (mostrarEditar && cliente != null) {
        DialogEditarCliente(
            cliente = cliente,
            onGuardar = { actualizado ->
                vm.actualizarCliente(actualizado)
                mostrarEditar = false
            },
            onDismiss = { mostrarEditar = false }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogEditarCliente(
    cliente: Cliente,
    onGuardar: (Cliente) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(cliente.nombre) }
    var telefono by remember { mutableStateOf(cliente.telefono) }
    var domicilio by remember { mutableStateOf(cliente.domicilio) }
    var limiteCredito by remember { mutableStateOf(if (cliente.limiteCredito > 0) "%.0f".format(cliente.limiteCredito) else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefono, onValueChange = { telefono = it },
                    label = { Text("Teléfono") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = domicilio, onValueChange = { domicilio = it },
                    label = { Text("Domicilio") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limiteCredito,
                    onValueChange = { limiteCredito = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Límite de crédito $") },
                    placeholder = { Text("Sin límite") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGuardar(cliente.copy(
                        nombre = nombre.trim(),
                        telefono = telefono.trim(),
                        domicilio = domicilio.trim(),
                        limiteCredito = limiteCredito.toDoubleOrNull() ?: 0.0
                    ))
                },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ItemMovimiento(mov: Movimiento, nombreLocal: String = "", nombreUsuario: String = "", onClick: () -> Unit = {}) {
    val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    val esCargo = mov.tipo == "cargo"
    val quienAnoto = run {
        val n = mov.anotadoPorNombre.trim()
        when {
            n.isBlank() -> nombreUsuario.ifBlank { "Vos" }
            n.equals(nombreLocal, ignoreCase = true) -> nombreUsuario.ifBlank { "Vos" }
            else -> n
        }
    }

    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    ListItem(
        headlineContent = {
            Text(
                mov.producto.ifBlank { if (esCargo) "Fiado" else "Pago" },
                color = if (mov.anulado) dimColor else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (mov.anulado) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
        },
        supportingContent = {
            if (mov.anulado) {
                Text("Anulado · ${fmt.format(mov.fecha.toDate())}", color = dimColor)
            } else {
                Text("$quienAnoto · ${fmt.format(mov.fecha.toDate())}")
            }
        },
        trailingContent = {
            Text(
                "${if (esCargo) "+" else "-"}${"%.2f".format(mov.monto)}",
                color = if (mov.anulado) dimColor
                        else if (esCargo) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium,
                textDecoration = if (mov.anulado) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
