package com.kosking.app.dueno.detalle

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class VisitaExtracto(
    val fecha: Date,
    val tipo: String,
    val conceptos: List<String>,
    val monto: Double,
    val saldoAcumulado: Double,
    val anotadoPor: String = "",
    val movimientoIds: List<String> = emptyList()
)

fun construirExtracto(movimientos: List<Movimiento>): List<VisitaExtracto> {
    val fmtKey = SimpleDateFormat("HH:mm_ddMMyyyy", Locale.getDefault())
    var saldo = 0.0
    return movimientos
        .filter { !it.anulado }
        .sortedBy { it.fecha.toDate() }
        .groupBy { mov -> mov.grupoId.ifBlank { fmtKey.format(mov.fecha.toDate()) } }
        .entries
        .sortedBy { (_, movs) -> movs.first().fecha.toDate() }
        .map { (_, movs) ->
            val mov0 = movs.first()
            val totalCargo = movs.filter { it.tipo == "cargo" }.sumOf { it.monto }
            val totalPago = movs.filter { it.tipo == "pago" }.sumOf { it.monto }
            saldo += totalCargo - totalPago
            val esTodoPago = movs.all { it.tipo == "pago" }
            val conceptos = if (esTodoPago) {
                listOf("Pago")
            } else {
                movs.filter { it.tipo != "pago" }
                    .map { it.producto.ifBlank { "Cargo" } }
            }
            VisitaExtracto(
                fecha = mov0.fecha.toDate(),
                tipo = if (esTodoPago) "pago" else "cargo",
                conceptos = conceptos,
                monto = if (esTodoPago) totalPago else totalCargo,
                saldoAcumulado = saldo,
                anotadoPor = mov0.anotadoPorNombre,
                movimientoIds = movs.map { it.id }
            )
        }
        .reversed()
}

private fun formatearFechaExtracto(fecha: Date): String {
    val hoy = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val ayer = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val anioActual = Calendar.getInstance().get(Calendar.YEAR)
    val anioFecha = Calendar.getInstance().apply { time = fecha }.get(Calendar.YEAR)

    return when {
        fecha.time >= hoy.timeInMillis ->
            "Hoy · ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(fecha)}"
        fecha.time >= ayer.timeInMillis ->
            "Ayer · ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(fecha)}"
        anioFecha < anioActual ->
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(fecha)
        else ->
            SimpleDateFormat("d MMM", Locale.getDefault()).format(fecha)
    }
}

@Composable
fun FilaExtracto(visita: VisitaExtracto, onClick: () -> Unit) {
    val esCargo = visita.tipo != "pago"
    val conceptoStr = visita.conceptos.joinToString(", ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = if (esCargo) MaterialTheme.colorScheme.errorContainer
                            else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (esCargo) Icons.Default.Add else Icons.Default.Remove,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (esCargo) MaterialTheme.colorScheme.error else Color(0xFF388E3C)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                conceptoStr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatearFechaExtracto(visita.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (visita.anotadoPor.isNotBlank()) {
                    Text("·", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        visita.anotadoPor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (esCargo) "+" else "-"}$${"%,.0f".format(visita.monto)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (esCargo) MaterialTheme.colorScheme.error else Color(0xFF388E3C),
                fontSize = 15.sp
            )
            Text(
                "Saldo $${"%,.0f".format(visita.saldoAcumulado)}",
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
