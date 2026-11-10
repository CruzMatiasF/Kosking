package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import com.kosking.app.ui.theme.VerdeClaro
import java.text.SimpleDateFormat

@Composable
fun SeccionCard(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary)
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

data class ItemVisita(
    val id: String,
    val producto: String,
    val monto: Double,
    val tipo: String,
    val grupoId: String = ""
)

data class VisitaResumen(
    val clienteId: String,
    val clienteNombre: String,
    val hora: String,
    val items: List<ItemVisita>,
    val totalCargo: Double,
    val totalPago: Double
)

fun agruparVisitas(
    movimientos: List<Movimiento>,
    clienteMap: Map<String, Cliente>,
    fmt: SimpleDateFormat
): List<VisitaResumen> {
    return movimientos
        .groupBy { mov -> "${mov.clienteId}|${mov.grupoId.ifBlank { fmt.format(mov.fecha.toDate()) }}" }
        .map { (_, movs) ->
            val mov0 = movs.first()
            val cliente = clienteMap[mov0.clienteId]
            val items = movs.map { ItemVisita(it.id, it.producto, it.monto, it.tipo, it.grupoId) }
            VisitaResumen(
                clienteId = mov0.clienteId,
                clienteNombre = cliente?.nombre ?: "Cliente",
                hora = fmt.format(mov0.fecha.toDate()),
                items = items,
                totalCargo = items.filter { it.tipo != "pago" }.sumOf { it.monto },
                totalPago = items.filter { it.tipo == "pago" }.sumOf { it.monto }
            )
        }
        .sortedByDescending { visita ->
            movimientos.firstOrNull { it.clienteId == visita.clienteId }?.fecha?.toDate()?.time ?: 0L
        }
}

@Composable
fun FilaVisitaDashboard(visita: VisitaResumen) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val esCargo = visita.totalCargo > 0 && visita.totalPago == 0.0
        val esPago = visita.totalPago > 0 && visita.totalCargo == 0.0

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = when {
                        esPago -> VerdeClaro
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (esPago) Icons.Default.Remove else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (esPago) Verde else MaterialTheme.colorScheme.error
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                visita.clienteNombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            if (visita.items.size == 1) {
                Text(
                    visita.items.first().producto.ifBlank { visita.items.first().tipo },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            } else {
                Text(
                    "${visita.items.size} ítems",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (visita.totalCargo > 0) {
                Text(
                    "+${"%.0f".format(visita.totalCargo)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (visita.totalPago > 0) {
                Text(
                    "-${"%.0f".format(visita.totalPago)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Verde
                )
            }
            Text(
                visita.hora,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
