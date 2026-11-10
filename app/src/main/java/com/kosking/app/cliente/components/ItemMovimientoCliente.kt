package com.kosking.app.cliente.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun ItemMovimientoCliente(mov: Movimiento, onNoReconozco: () -> Unit) {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val esCargo = mov.tipo == "cargo"

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (esCargo) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (esCargo) "↑" else "↓",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (esCargo) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    mov.producto.ifBlank { if (esCargo) "Fiado" else "Pago" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (mov.anulado) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
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
                "por ${mov.anotadoPorNombre} · ${fmt.format(mov.fecha.toDate())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!mov.anulado && !mov.disputado && esCargo) {
                TextButton(
                    onClick = onNoReconozco,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        "No lo reconozco",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Text(
            "${if (esCargo) "+" else "−"}$${"%.0f".format(mov.monto)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = when {
                mov.anulado -> MaterialTheme.colorScheme.onSurfaceVariant
                esCargo -> MaterialTheme.colorScheme.error
                else -> Color(0xFF2E7D32)
            }
        )
    }
}
