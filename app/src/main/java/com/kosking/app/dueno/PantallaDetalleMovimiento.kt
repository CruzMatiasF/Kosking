package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleMovimiento(
    movimientoId: String,
    onVolver: () -> Unit,
    vm: DuenoViewModel = viewModel()
) {
    LaunchedEffect(movimientoId) { vm.seleccionarMovimiento(movimientoId) }
    val mov by vm.movimientoSeleccionado.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val cliente = clientes.find { it.id == mov?.clienteId }

    val cargando by vm.cargando.collectAsState()
    val vmError by vm.error.collectAsState()
    var mostrarDialogAnular by remember { mutableStateOf(false) }
    var motivoAnulacion by remember { mutableStateOf("") }

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
                        Text("⚠", style = MaterialTheme.typography.titleMedium)
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
                    Text("$${String.format("%.2f", movActual.monto)}", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (movActual.anulado) MaterialTheme.colorScheme.onSurfaceVariant
                                else if (esCargo) MaterialTheme.colorScheme.error else Color(0xFF2E7D32))
                    HorizontalDivider()
                    FilaDetalle("Cliente", cliente?.nombre ?: "—")
                    if (movActual.producto.isNotBlank()) FilaDetalle("Producto", movActual.producto)
                    if (movActual.cantidad > 1) FilaDetalle("Cantidad", movActual.cantidad.toString())
                    FilaDetalle("Fecha", fmt.format(movActual.fecha.toDate()))
                    FilaDetalle("Registrado por", movActual.anotadoPorNombre.ifBlank { "—" })
                    if (movActual.anulado && movActual.motivoAnulacion.isNotBlank()) {
                        FilaDetalle("Motivo anulación", movActual.motivoAnulacion)
                    }
                }
            }

            if (!movActual.anulado) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { vm.marcarDisputado(movActual.id, !movActual.disputado) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (movActual.disputado)
                        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    else
                        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (movActual.disputado) "Quitar disputa" else "Marcar como disputado")
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
            onDismissRequest = { vm.error.value = null },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("Cerrar") } }
        )
    }

    if (mostrarDialogAnular) {
        val m = mov ?: return
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
                            movimientoId = m.id, clienteId = m.clienteId,
                            monto = m.monto, tipo = m.tipo, motivo = motivoAnulacion
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
