package com.kosking.app.dueno.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import com.kosking.app.ui.theme.VerdeClaro
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun TabReportes(
    movimientos: List<Movimiento>,
    clientes: List<Cliente>
) {
    val ahora = Calendar.getInstance()

    val inicioMesActual = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val inicioMesAnterior = (inicioMesActual.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    val inicioHace6Meses = (inicioMesActual.clone() as Calendar).apply { add(Calendar.MONTH, -5) }

    val fmtMes = remember { SimpleDateFormat("MMM", Locale.forLanguageTag("es-AR")) }
    val fmtMesCompleto = remember { SimpleDateFormat("MMMM", Locale.forLanguageTag("es-AR")) }

    val movsFiltrados = remember(movimientos) { movimientos.filter { !it.anulado } }

    // Mes actual vs anterior
    val fiadoMesActual = remember(movsFiltrados) {
        movsFiltrados.filter { it.tipo == "cargo" && it.fecha.toDate().time >= inicioMesActual.timeInMillis }.sumOf { it.monto }
    }
    val cobradoMesActual = remember(movsFiltrados) {
        movsFiltrados.filter { it.tipo == "pago" && it.fecha.toDate().time >= inicioMesActual.timeInMillis }.sumOf { it.monto }
    }
    val fiadoMesAnterior = remember(movsFiltrados) {
        movsFiltrados.filter { it.tipo == "cargo" && it.fecha.toDate().time >= inicioMesAnterior.timeInMillis && it.fecha.toDate().time < inicioMesActual.timeInMillis }.sumOf { it.monto }
    }
    val cobradoMesAnterior = remember(movsFiltrados) {
        movsFiltrados.filter { it.tipo == "pago" && it.fecha.toDate().time >= inicioMesAnterior.timeInMillis && it.fecha.toDate().time < inicioMesActual.timeInMillis }.sumOf { it.monto }
    }

    // Top productos
    val topProductos = remember(movsFiltrados) {
        movsFiltrados
            .filter { it.tipo == "cargo" && it.producto.isNotBlank() }
            .groupBy { it.producto }
            .map { (nombre, movs) -> nombre to movs.size }
            .sortedByDescending { it.second }
            .take(5)
    }

    // Top deudores
    val topDeudores = remember(clientes) {
        clientes.filter { it.deudaTotal > 0 }.sortedByDescending { it.deudaTotal }.take(5)
    }

    // Últimos 6 meses
    val ultimos6Meses = remember(movsFiltrados) {
        (0..5).map { mesesAtras ->
            val inicio = (inicioMesActual.clone() as Calendar).apply { add(Calendar.MONTH, -mesesAtras) }
            val fin = (inicio.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val fiado = movsFiltrados.filter { it.tipo == "cargo" && it.fecha.toDate().time >= inicio.timeInMillis && it.fecha.toDate().time < fin.timeInMillis }.sumOf { it.monto }
            val cobrado = movsFiltrados.filter { it.tipo == "pago" && it.fecha.toDate().time >= inicio.timeInMillis && it.fecha.toDate().time < fin.timeInMillis }.sumOf { it.monto }
            Triple(fmtMes.format(inicio.time).replaceFirstChar { it.uppercase() }, fiado, cobrado)
        }.reversed()
    }
    val maxMonto = remember(ultimos6Meses) { ultimos6Meses.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Reportes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Este mes vs anterior
        item {
            Text(
                "ESTE MES VS MES ANTERIOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.8f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MesCard(
                    modifier = Modifier.weight(1f),
                    label = "Anotado",
                    mesActual = fiadoMesActual,
                    mesAnterior = fiadoMesAnterior,
                    nombreMesActual = fmtMesCompleto.format(inicioMesActual.time).replaceFirstChar { it.uppercase() },
                    nombreMesAnterior = fmtMesCompleto.format(inicioMesAnterior.time).replaceFirstChar { it.uppercase() },
                    colorActual = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                )
                MesCard(
                    modifier = Modifier.weight(1f),
                    label = "Cobrado",
                    mesActual = cobradoMesActual,
                    mesAnterior = cobradoMesAnterior,
                    nombreMesActual = fmtMesCompleto.format(inicioMesActual.time).replaceFirstChar { it.uppercase() },
                    nombreMesAnterior = fmtMesCompleto.format(inicioMesAnterior.time).replaceFirstChar { it.uppercase() },
                    colorActual = Verde,
                    containerColor = VerdeClaro.copy(alpha = 0.5f)
                )
            }
        }

        // Últimos 6 meses (barras simples)
        item {
            Text(
                "ÚLTIMOS 6 MESES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.8f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            Spacer(Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ultimos6Meses.forEach { (mes, fiado, cobrado) ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(mes, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (fiado > 0) Text("A $${"%.0f".format(fiado)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    if (cobrado > 0) Text("C $${"%.0f".format(cobrado)}", style = MaterialTheme.typography.labelSmall, color = Verde)
                                }
                            }
                            // Barra fiado
                            if (fiado > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (fiado / maxMonto).toFloat().coerceIn(0.02f, 1f))
                                        .height(5.dp)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                )
                            }
                            // Barra cobrado
                            if (cobrado > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (cobrado / maxMonto).toFloat().coerceIn(0.02f, 1f))
                                        .height(5.dp)
                                        .background(Verde.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top productos
        if (topProductos.isNotEmpty()) {
            item {
                Text(
                    "PRODUCTOS MÁS ANOTADOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(0.8f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
                Spacer(Modifier.height(8.dp))
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        val maxVeces = topProductos.firstOrNull()?.second ?: 1
                        topProductos.forEachIndexed { i, (nombre, veces) ->
                            ListItem(
                                leadingContent = {
                                    Text(
                                        "${i + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(20.dp)
                                    )
                                },
                                headlineContent = { Text(nombre, style = MaterialTheme.typography.bodyMedium) },
                                trailingContent = {
                                    Text(
                                        "$veces ${if (veces == 1) "vez" else "veces"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            if (i < topProductos.size - 1) HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                        }
                    }
                }
            }
        }

        // Top deudores
        if (topDeudores.isNotEmpty()) {
            item {
                Text(
                    "MAYORES DEUDAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(0.8f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
                Spacer(Modifier.height(8.dp))
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        topDeudores.forEachIndexed { i, cliente ->
                            ListItem(
                                leadingContent = {
                                    Text(
                                        "${i + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(20.dp)
                                    )
                                },
                                headlineContent = { Text(cliente.nombre, style = MaterialTheme.typography.bodyMedium) },
                                trailingContent = {
                                    Text(
                                        "$${"%.0f".format(cliente.deudaTotal)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                            if (i < topDeudores.size - 1) HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MesCard(
    modifier: Modifier = Modifier,
    label: String,
    mesActual: Double,
    mesAnterior: Double,
    nombreMesActual: String,
    nombreMesAnterior: String,
    colorActual: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(nombreMesActual, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${"%.0f".format(mesActual)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorActual)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(nombreMesAnterior, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${"%.0f".format(mesAnterior)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (mesAnterior > 0) {
                val pct = ((mesActual - mesAnterior) / mesAnterior * 100).toInt()
                Text(
                    "${if (pct >= 0) "+" else ""}$pct% vs $nombreMesAnterior",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (label == "Anotado") {
                        if (pct > 0) MaterialTheme.colorScheme.error else Verde
                    } else {
                        if (pct >= 0) Verde else MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}
