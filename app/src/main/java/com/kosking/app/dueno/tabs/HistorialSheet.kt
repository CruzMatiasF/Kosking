package com.kosking.app.dueno.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistorialSheet(
    movimientos: List<Movimiento>,
    clienteMap: Map<String, Cliente>,
    onVerMovimiento: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("EEEE d MMM", Locale.forLanguageTag("es-AR")) }
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var visitaDetalle by remember { mutableStateOf<VisitaResumen?>(null) }

    val agrupado = remember(movimientos) {
        val hoyNorm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        movimientos
            .filter { !it.anulado }
            .sortedByDescending { it.fecha.toDate().time }
            .groupBy {
                val diaCal = Calendar.getInstance().apply {
                    time = it.fecha.toDate()
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                ((hoyNorm.timeInMillis - diaCal.timeInMillis) / 86_400_000L).toInt()
            }
            .entries
            .sortedBy { it.key }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        if (agrupado.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin movimientos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)
            ) {
                item {
                    Text(
                        "Historial",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                agrupado.forEach { (diasAtras, movs) ->
                    val encabezado = when (diasAtras) {
                        0 -> "Hoy"
                        1 -> "Ayer"
                        else -> fmt.format(movs.first().fecha.toDate()).replaceFirstChar { it.uppercase() }
                    }
                    val anotado = movs.filter { it.tipo == "cargo" }.sumOf { it.monto }
                    val cobrado = movs.filter { it.tipo == "pago" }.sumOf { it.monto }

                    item(key = "header_$diasAtras") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                encabezado,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (anotado > 0) Text(
                                    "-$${"%.0f".format(anotado)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                if (cobrado > 0) Text(
                                    "+$${"%.0f".format(cobrado)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Verde
                                )
                            }
                        }
                    }

                    val visitas = agruparVisitas(movs, clienteMap, fmtHora)
                    items(visitas, key = { "v_${diasAtras}_${it.items.firstOrNull()?.id ?: it.hora}" }) { visita ->
                        VisitaCardFull(visita = visita, onClick = { visitaDetalle = visita })
                    }
                    if (diasAtras != agrupado.last().key) {
                        item(key = "sep_$diasAtras") {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }

    visitaDetalle?.let { visita ->
        DetalleVisitaSheet(visita = visita, onDismiss = { visitaDetalle = null })
    }
}

@Composable
private fun VisitaCardFull(visita: VisitaResumen, onClick: () -> Unit) {
    val esPago = visita.totalPago > 0 && visita.totalCargo == 0.0
    val stripeColor = if (esPago) Verde else MaterialTheme.colorScheme.error
    val monto = if (esPago) visita.totalPago else visita.totalCargo
    val montoColor = if (esPago) Verde else MaterialTheme.colorScheme.error
    val montoTexto = "${if (esPago) "+" else "-"}$${"%.0f".format(monto)}"
    val concepto = when {
        esPago -> "Pago"
        visita.items.size == 1 -> visita.items.first().producto.ifBlank { "Cargo" }
        else -> "${visita.items.size} productos"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(stripeColor, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    visita.clienteNombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    concepto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    montoTexto,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = montoColor
                )
                Text(
                    visita.hora,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 32.dp))
}
