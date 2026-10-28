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
    val movimientos by vm.movimientosLocal.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val mov = movimientos.find { it.id == movimientoId }
    val cliente = clientes.find { it.id == mov?.clienteId }

    var mostrarDialogAnular by remember { mutableStateOf(false) }
    var motivoAnulacion by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del movimiento") },
            )
        }
    ) { padding ->
        if (mov == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val esCargo = mov.tipo == "cargo"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Banner disputado
            if (mov.disputado && !mov.anulado) {
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
                            Text(
                                "Movimiento no reconocido",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "El cliente indicó que no reconoce este movimiento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Banner anulado
            if (mov.anulado) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Movimiento anulado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (mov.motivoAnulacion.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Motivo: ${mov.motivoAnulacion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Card principal
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (esCargo) "Fiado" else "Pago",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (mov.anulado) {
                            Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                Text("Anulado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (mov.disputado) {
                            Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                Text("En disputa", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Text(
                        "$${String.format("%.2f", mov.monto)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (mov.anulado) MaterialTheme.colorScheme.onSurfaceVariant
                                else if (esCargo) MaterialTheme.colorScheme.error
                                else Color(0xFF2E7D32)
                    )

                    HorizontalDivider()

                    FilaDetalle("Cliente", cliente?.nombre ?: "—")
                    if (mov.producto.isNotBlank()) FilaDetalle("Producto", mov.producto)
                    if (mov.cantidad > 1) FilaDetalle("Cantidad", mov.cantidad.toString())
                    FilaDetalle("Fecha", fmt.format(mov.fecha.toDate()))
                    FilaDetalle("Registrado por", mov.anotadoPorNombre.ifBlank { "—" })
                    if (mov.anulado && mov.motivoAnulacion.isNotBlank()) {
                        FilaDetalle("Motivo anulación", mov.motivoAnulacion)
                    }
                }
            }

            // Botón anular (solo si no está anulado)
            if (!mov.anulado) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { mostrarDialogAnular = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Anular movimiento")
                }
            }
        }
    }

    if (mostrarDialogAnular && mov != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogAnular = false },
            title = { Text("Anular movimiento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Esta acción revierte el efecto en la deuda del cliente y no se puede deshacer.")
                    OutlinedTextField(
                        value = motivoAnulacion,
                        onValueChange = { motivoAnulacion = it },
                        label = { Text("Motivo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.anularMovimiento(
                            movimientoId = mov.id,
                            clienteId = mov.clienteId,
                            monto = mov.monto,
                            tipo = mov.tipo,
                            motivo = motivoAnulacion
                        )
                        mostrarDialogAnular = false
                        onVolver()
                    }
                ) { Text("Anular", color = MaterialTheme.colorScheme.error) }
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
