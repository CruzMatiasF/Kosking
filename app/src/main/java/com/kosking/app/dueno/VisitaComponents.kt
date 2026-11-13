package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import com.kosking.app.ui.theme.VerdeClaro
import java.text.SimpleDateFormat
import kotlin.math.abs

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
    val fechaMs: Long,
    val items: List<ItemVisita>,
    val totalCargo: Double,
    val totalPago: Double,
    val anotadoPor: String = ""
)

data class ClienteVisitasHoy(
    val clienteId: String,
    val clienteNombre: String,
    val visitas: List<VisitaResumen>
)

fun agruparPorCliente(visitas: List<VisitaResumen>): List<ClienteVisitasHoy> {
    return visitas
        .groupBy { it.clienteId }
        .map { (clienteId, grupoVisitas) ->
            ClienteVisitasHoy(
                clienteId = clienteId,
                clienteNombre = grupoVisitas.first().clienteNombre,
                visitas = grupoVisitas.sortedBy { it.fechaMs }
            )
        }
        .sortedByDescending { it.visitas.maxOf { v -> v.fechaMs } }
}

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

@Composable
fun GrupoClienteDashboard(grupo: ClienteVisitasHoy) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            grupo.clienteNombre,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        grupo.visitas.forEach { visita ->
            SubFilaVisita(visita)
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 6.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun SubFilaVisita(visita: VisitaResumen) {
    val esTodoPago = visita.items.all { it.tipo == "pago" }
    val productosStr = if (esTodoPago) {
        "Pago"
    } else {
        visita.items
            .filter { it.tipo != "pago" }
            .map { it.producto.ifBlank { "Cargo" } }
            .joinToString(", ")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            visita.hora,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.width(38.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                productosStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (visita.anotadoPor.isNotBlank()) {
                Text(
                    visita.anotadoPor,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (visita.totalCargo > 0) {
            Text(
                "+${"%.0f".format(visita.totalCargo)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
        if (visita.totalPago > 0) {
            Text(
                "-${"%.0f".format(visita.totalPago)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Verde,
                fontSize = 12.sp
            )
        }
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
                val item0 = visita.items.first()
                Text(
                    item0.producto.ifBlank { if (item0.tipo == "cargo") "Cargo" else "Pago" },
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
