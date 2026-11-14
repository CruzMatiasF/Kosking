package com.kosking.app.ajustes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEstadisticas(
    vm: DuenoViewModel,
    onVolver: () -> Unit
) {
    val clientes by vm.clientes.collectAsState()
    val movimientos by vm.movimientosLocal.collectAsState()
    val totalDeuda by vm.totalDeuda.collectAsState()

    // ── Métricas calculadas ──────────────────────────────────────────────────
    val ars = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    val clientesActivos = clientes.count { it.deudaTotal > 0 }
    val clienteTopDeudor = clientes.filter { it.deudaTotal > 0 }.maxByOrNull { it.deudaTotal }

    val inicioMes = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val hace7 = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val movsMes = remember(movimientos) { movimientos.filter { !it.anulado && it.fecha.toDate().time >= inicioMes } }
    val fiadoMes = remember(movsMes) { movsMes.filter { it.tipo == "cargo" }.sumOf { it.monto } }
    val cobradoMes = remember(movsMes) { movsMes.filter { it.tipo == "pago" }.sumOf { it.monto } }

    val movsPorDia: Map<String, Int> = remember(movimientos) {
        buildMap {
            repeat(7) { i ->
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val label = "${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}"
                put(label, 0)
            }
            movimientos.filter { !it.anulado && it.fecha.toDate().time >= hace7 }.forEach { mov ->
                val c = Calendar.getInstance().apply { time = mov.fecha.toDate() }
                val label = "${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}"
                put(label, (get(label) ?: 0) + 1)
            }
        }
    }
    val maxMovDia = remember(movsPorDia) { movsPorDia.values.maxOrNull()?.coerceAtLeast(1) ?: 1 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Resumen general ──────────────────────────────────────────────
            Text("RESUMEN GENERAL", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    icono = Icons.Default.AccountBalance,
                    titulo = "Deuda total",
                    valor = ars.format(totalDeuda),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icono = Icons.Default.People,
                    titulo = "Con deuda",
                    valor = "$clientesActivos clientes",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Mes actual ──────────────────────────────────────────────────
            Text("ESTE MES", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    icono = Icons.AutoMirrored.Filled.TrendingUp,
                    titulo = "Fiado",
                    valor = ars.format(fiadoMes),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icono = Icons.Default.Payments,
                    titulo = "Cobrado",
                    valor = ars.format(cobradoMes),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Cliente top ──────────────────────────────────────────────────
            if (clienteTopDeudor != null) {
                Text("MAYOR DEUDOR", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp))

                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    ListItem(
                        headlineContent = {
                            Text(clienteTopDeudor.nombre, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        },
                        supportingContent = {
                            Text("Debe ${ars.format(clienteTopDeudor.deudaTotal)}",
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                        },
                        leadingContent = {
                            Icon(Icons.Default.Person, null, Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }

            // ── Actividad últimos 7 días ─────────────────────────────────────
            Text("ACTIVIDAD — ÚLTIMOS 7 DÍAS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    movsPorDia.entries.reversed().forEach { (dia, cant) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(dia, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(40.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(
                                progress = { cant.toFloat() / maxMovDia },
                                modifier = Modifier.weight(1f).height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text("$cant", style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(24.dp),
                                fontWeight = if (cant > 0) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    valor: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icono, null, Modifier.size(22.dp), tint = color)
            Text(titulo, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
        }
    }
}
