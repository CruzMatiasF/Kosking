package com.kosking.app.cliente.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal fun agruparPorDia(movimientos: List<Movimiento>): List<Pair<String, List<Movimiento>>> {
    val hoy = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val ayer = hoy - 86_400_000L
    val fmt = SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es"))

    return movimientos
        .groupBy { mov ->
            val t = mov.fecha.toDate().time
            when {
                t >= hoy -> "Hoy"
                t >= ayer -> "Ayer"
                else -> fmt.format(Date(t)).replaceFirstChar { it.uppercase() }
            }
        }
        .entries
        .map { it.key to it.value }
}

@Composable
internal fun TabMovimientos(
    movimientos: List<Movimiento>,
    onNoReconozco: (Movimiento) -> Unit
) {
    val activos = remember(movimientos) { movimientos.filter { !it.anulado } }
    val anulados = remember(movimientos) { movimientos.filter { it.anulado } }
    var mostrarAnulados by remember { mutableStateOf(false) }

    if (activos.isEmpty() && anulados.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Sin movimientos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Cuando el negocio anote algo vas a verlo acá",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val grupos = remember(activos) { agruparPorDia(activos) }

    LazyColumn {
        if (activos.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No hay movimientos activos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            grupos.forEach { (label, movs) ->
                item(key = "header_$label") {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                items(movs, key = { it.id }) { mov ->
                    ItemMovimientoCliente(mov = mov, onNoReconozco = { onNoReconozco(mov) })
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
                }
            }
        }

        if (anulados.isNotEmpty()) {
            item(key = "anulados_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarAnulados = !mostrarAnulados }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Movimientos anulados · ${anulados.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        if (mostrarAnulados) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (mostrarAnulados) {
                items(anulados, key = { "anulado_${it.id}" }) { mov ->
                    ItemMovimientoCliente(mov = mov, onNoReconozco = {})
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
