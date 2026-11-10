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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LineaExtracto(
    val fecha: Date,
    val concepto: String,
    val movimientoId: String,
    val monto: Double,
    val tipo: String,
    val saldoAcumulado: Double,
    val anulado: Boolean = false
)

fun construirExtracto(movimientos: List<Movimiento>): List<LineaExtracto> {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    var saldo = 0.0
    return movimientos
        .filter { !it.anulado }
        .sortedBy { it.fecha.toDate() }
        .map { mov ->
            val delta = if (mov.tipo == "pago") -mov.monto else mov.monto
            saldo += delta
            LineaExtracto(
                fecha = mov.fecha.toDate(),
                concepto = mov.producto.ifBlank { mov.tipo },
                movimientoId = mov.id,
                monto = mov.monto,
                tipo = mov.tipo,
                saldoAcumulado = saldo,
                anulado = mov.anulado
            )
        }
        .reversed()
}

@Composable
fun FilaExtracto(linea: LineaExtracto, vm: DuenoViewModel, clienteId: String) {
    val esCargo = linea.tipo != "pago"
    val fmtFecha = SimpleDateFormat("d MMM", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
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
                linea.concepto,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                fmtFecha.format(linea.fecha),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (esCargo) "+" else "-"}${"%.0f".format(linea.monto)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (esCargo) MaterialTheme.colorScheme.error else Color(0xFF388E3C),
                fontSize = 15.sp
            )
            Text(
                "Saldo ${"%.0f".format(linea.saldoAcumulado)}",
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
