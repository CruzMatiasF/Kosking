package com.kosking.app.dueno.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import com.kosking.app.util.WhatsAppUtil
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResumenPeriodoSheet(
    movimientos: List<Movimiento>,
    clientes: List<Cliente>,
    duenoUid: String,
    nombreLocal: String,
    onDismiss: () -> Unit
) {
    data class Periodo(val key: String, val label: String, val inicio: Long)

    val context = LocalContext.current
    val ahora = System.currentTimeMillis()
    val periodos = remember {
        val calHoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val calSemana = calHoy.clone() as Calendar
        calSemana.set(Calendar.DAY_OF_WEEK, calSemana.firstDayOfWeek)
        val calMes = calHoy.clone() as Calendar
        calMes.set(Calendar.DAY_OF_MONTH, 1)
        val calMesAnt = calMes.clone() as Calendar
        calMesAnt.add(Calendar.MONTH, -1)
        listOf(
            Periodo("hoy", "Hoy", calHoy.timeInMillis),
            Periodo("semana", "Semana", calSemana.timeInMillis),
            Periodo("mes", "Este mes", calMes.timeInMillis),
            Periodo("mes_ant", "Mes ant.", calMesAnt.timeInMillis)
        )
    }

    var periodoKey by remember { mutableStateOf("mes") }
    val periodo = periodos.first { it.key == periodoKey }

    val finPeriodo = remember(periodoKey) {
        if (periodoKey == "mes_ant") {
            val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            c.timeInMillis - 1
        } else ahora
    }

    val movsFiltrados = remember(movimientos, periodoKey) {
        movimientos.filter {
            !it.anulado &&
            it.fecha.toDate().time >= periodo.inicio &&
            it.fecha.toDate().time <= finPeriodo
        }
    }

    val fiadoTotal = remember(movsFiltrados) { movsFiltrados.filter { it.tipo == "cargo" }.sumOf { it.monto } }
    val cobradoTotal = remember(movsFiltrados) { movsFiltrados.filter { it.tipo == "pago" }.sumOf { it.monto } }
    val cantFiados = remember(movsFiltrados) { movsFiltrados.count { it.tipo == "cargo" } }
    val cantPagos = remember(movsFiltrados) { movsFiltrados.count { it.tipo == "pago" } }
    val saldoNeto = cobradoTotal - fiadoTotal

    val topClientes = remember(movsFiltrados) {
        movsFiltrados
            .filter { it.tipo == "cargo" }
            .groupBy { it.clienteId }
            .mapValues { (_, ms) -> ms.sumOf { it.monto } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    val statsPorPersona = remember(movsFiltrados) {
        data class StatP(val nombre: String, val anoto: Int, val cobro: Double)
        val map = mutableMapOf<String, StatP>()
        movsFiltrados.forEach { mov ->
            val uid = mov.anotadoPorUid.ifBlank { duenoUid }
            val nombre = if (uid == duenoUid) "Vos" else mov.anotadoPorNombre.ifBlank { "Empleado" }
            val a = map[uid] ?: StatP(nombre, 0, 0.0)
            map[uid] = a.copy(anoto = a.anoto + 1, cobro = a.cobro + if (mov.tipo == "pago") mov.monto else 0.0)
        }
        map.values.sortedByDescending { if (it.nombre == "Vos") Int.MAX_VALUE else it.anoto }
    }

    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Resumen del período",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periodos.forEach { p ->
                        FilterChip(
                            selected = periodoKey == p.key,
                            onClick = { periodoKey = p.key },
                            label = { Text(p.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatResumenCard("Anotado", "$${"%.0f".format(fiadoTotal)}", "$cantFiados op.", isError = true, modifier = Modifier.weight(1f))
                    StatResumenCard("Cobrado", "$${"%.0f".format(cobradoTotal)}", "$cantPagos pagos", isSuccess = true, modifier = Modifier.weight(1f))
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (saldoNeto >= 0) MaterialTheme.colorScheme.primaryContainer
                                         else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Saldo neto",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (saldoNeto >= 0) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            "${if (saldoNeto >= 0) "+" else ""}\$${"%.0f".format(saldoNeto)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (saldoNeto >= 0) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            if (saldoNeto >= 0) "Cobraste más de lo que fiaste" else "Fiaste más de lo que cobraste",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (saldoNeto >= 0) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (statsPorPersona.isNotEmpty()) {
                item {
                    Text("Por persona",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                Text("Quién", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text("Anotó", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
                                Text("Cobró", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(76.dp))
                            }
                            HorizontalDivider()
                            statsPorPersona.forEach { stat ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stat.nombre,
                                        fontWeight = if (stat.nombre == "Vos") FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text("${stat.anoto}", modifier = Modifier.width(52.dp),
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "$${"%.0f".format(stat.cobro)}",
                                        modifier = Modifier.width(76.dp),
                                        color = if (stat.cobro > 0) Verde else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (stat.cobro > 0) FontWeight.SemiBold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (topClientes.isNotEmpty()) {
                item {
                    Text("Top clientes (más deben)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            topClientes.forEachIndexed { i, (clienteId, monto) ->
                                val nombreCliente = clienteMap[clienteId]?.nombre ?: "—"
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${i + 1}.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(20.dp))
                                        Text(nombreCliente,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium)
                                    }
                                    Text("$${"%.0f".format(monto)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error)
                                }
                                if (i < topClientes.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }

            if (movsFiltrados.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Sin movimientos en este período",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        WhatsAppUtil.compartir(
                            context,
                            WhatsAppUtil.mensajeResumenPeriodo(
                                nombreLocal = nombreLocal.ifBlank { "Mi local" },
                                labelPeriodo = periodo.label.lowercase(),
                                fiadoTotal = fiadoTotal,
                                cobradoTotal = cobradoTotal,
                                cantFiados = cantFiados,
                                cantPagos = cantPagos,
                                topClientes = topClientes.map { (clienteId, monto) ->
                                    (clienteMap[clienteId]?.nombre ?: "—") to monto
                                }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = movsFiltrados.isNotEmpty()
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir resumen")
                }
            }
        }
    }
}

@Composable
private fun StatResumenCard(
    titulo: String,
    valor: String,
    subtitulo: String,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isSuccess -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = contentColor)
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = contentColor)
            Text(subtitulo, style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
    }
}
