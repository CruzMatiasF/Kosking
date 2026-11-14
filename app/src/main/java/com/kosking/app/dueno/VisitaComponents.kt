package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import java.text.SimpleDateFormat
import kotlin.math.abs

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
    val fechaMs: Long,
    val items: List<ItemVisita>,
    val totalCargo: Double,
    val totalPago: Double,
    val anotadoPor: String = ""
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
                fechaMs = mov0.fecha.toDate().time,
                items = items,
                totalCargo = items.filter { it.tipo != "pago" }.sumOf { it.monto },
                totalPago = items.filter { it.tipo == "pago" }.sumOf { it.monto },
                anotadoPor = mov0.anotadoPorNombre
            )
        }
        .sortedByDescending { it.fechaMs }
}

private val avatarColors = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350),
    Color(0xFFEC407A), Color(0xFF7E57C2), Color(0xFF42A5F5),
    Color(0xFF66BB6A), Color(0xFFFF7043)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilaVisitaMP(visita: VisitaResumen, onClick: () -> Unit) {
    val esPago = visita.totalPago > 0 && visita.totalCargo == 0.0
    val inicial = visita.clienteNombre.firstOrNull()?.uppercaseChar() ?: '?'
    val avatarColor = avatarColors[abs(visita.clienteNombre.hashCode()) % avatarColors.size]

    val subtitulo = when {
        esPago -> "Pago"
        visita.items.size == 1 -> visita.items.first().producto.ifBlank { "Cargo" }
        else -> "${visita.items.size} productos"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(avatarColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                inicial.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                visita.clienteNombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleVisitaSheet(visita: VisitaResumen, onDismiss: () -> Unit) {
    val esPago = visita.totalPago > 0 && visita.totalCargo == 0.0
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    visita.clienteNombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    visita.hora,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (visita.anotadoPor.isNotBlank()) {
                Text(
                    "Anotado por ${visita.anotadoPor}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            if (esPago) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pago recibido", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${"%.0f".format(visita.totalPago)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Verde
                    )
                }
            } else {
                visita.items.filter { it.tipo != "pago" }.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            item.producto.ifBlank { "Cargo" },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${"%.0f".format(item.monto)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${"%.0f".format(visita.totalCargo)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

