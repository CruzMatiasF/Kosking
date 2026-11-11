package com.kosking.app.dueno.detalle

import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.ui.theme.Verde

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleMovimiento(
    movimientoId: String,
    onVolver: () -> Unit,
    vm: DuenoViewModel
) {
    LaunchedEffect(movimientoId) { vm.seleccionarMovimiento(movimientoId) }
    val mov by vm.movimientoSeleccionado.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val cliente = clientes.find { it.id == mov?.clienteId }

    val cargando by vm.cargando.collectAsState()
    val vmError by vm.error.collectAsState()
    var mostrarDialogAnular by remember { mutableStateOf(false) }
    var motivoAnulacion by remember { mutableStateOf("") }
    var mostrarDialogCorregir by remember { mutableStateOf(false) }
    var montoCorregido by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del movimiento") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        val movActual = mov
        if (movActual == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val esCargo = movActual.tipo == "cargo"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (movActual.disputado && !movActual.anulado) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        Column {
                            Text("Movimiento no reconocido", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("El cliente indicó que no reconoce este movimiento.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            if (movActual.anulado) {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Movimiento anulado", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (movActual.motivoAnulacion.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Motivo: ${movActual.motivoAnulacion}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (esCargo) "Fiado" else "Pago", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (movActual.anulado) {
                            Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                Text("Anulado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (movActual.disputado) {
                            Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                Text("En disputa", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Text("$${"%,.0f".format(movActual.monto)}", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (movActual.anulado) MaterialTheme.colorScheme.onSurfaceVariant
                                else if (esCargo) MaterialTheme.colorScheme.error else Verde)
                    HorizontalDivider()
                    FilaDetalle("Cliente", cliente?.nombre ?: "—")
                    if (movActual.producto.isNotBlank()) FilaDetalle("Descripción", movActual.producto)
                    HorizontalDivider()
                    FilaDetalle("Fecha", fmt.format(movActual.fecha.toDate()))
                    if (movActual.anotadoPorNombre.isNotBlank()) {
                        FilaDetalle("Registrado por", movActual.anotadoPorNombre)
                    }
                    if (movActual.anulado && movActual.motivoAnulacion.isNotBlank()) {
                        FilaDetalle("Motivo anulación", movActual.motivoAnulacion)
                    }
                }
            }

            if (!movActual.anulado) {
                Spacer(Modifier.weight(1f))
                if (movActual.disputado) {
                    OutlinedButton(
                        onClick = { vm.marcarDisputado(movActual.id, false) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Quitar disputa")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = {
                        montoCorregido = "%.0f".format(movActual.monto)
                        mostrarDialogCorregir = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Corregir monto")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { mostrarDialogAnular = true }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Anular movimiento")
                }
            }
        }
    }

    vmError?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.clearError() }) { Text("Cerrar") } }
        )
    }

    val movParaCorregir = mov
    if (mostrarDialogCorregir && movParaCorregir != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogCorregir = false },
            title = { Text("Corregir monto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ingresá el monto correcto. La deuda del cliente se ajustará automáticamente.")
                    OutlinedTextField(
                        value = montoCorregido,
                        onValueChange = { montoCorregido = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto $") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nuevo = montoCorregido.toDoubleOrNull()
                        if (nuevo != null && nuevo > 0) {
                            vm.corregirMontoMovimiento(
                                movimientoId = movParaCorregir.id,
                                clienteId = movParaCorregir.clienteId,
                                montoViejo = movParaCorregir.monto,
                                montoNuevo = nuevo,
                                tipo = movParaCorregir.tipo
                            ) { mostrarDialogCorregir = false }
                        }
                    },
                    enabled = !cargando && montoCorregido.toDoubleOrNull() != null && (montoCorregido.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    if (cargando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogCorregir = false }) { Text("Cancelar") }
            }
        )
    }

    val movParaAnular = mov
    if (mostrarDialogAnular && movParaAnular != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogAnular = false },
            title = { Text("Anular movimiento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Esta acción revierte el efecto en la deuda del cliente y no se puede deshacer.")
                    OutlinedTextField(value = motivoAnulacion, onValueChange = { motivoAnulacion = it },
                        label = { Text("Motivo (opcional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.anularMovimiento(
                            movimientoId = movParaAnular.id, clienteId = movParaAnular.clienteId,
                            monto = movParaAnular.monto, tipo = movParaAnular.tipo, motivo = motivoAnulacion
                        ) {
                            mostrarDialogAnular = false
                            onVolver()
                        }
                        mostrarDialogAnular = false
                    },
                    enabled = !cargando
                ) {
                    if (cargando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Anular", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogAnular = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun FilaDetalle(label: String, valor: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
