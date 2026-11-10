package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PantallaTodasVisitas(
    movimientosLocal: List<Movimiento>,
    clientes: List<Cliente>,
    onVolver: () -> Unit
) {
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fmtDia = remember { SimpleDateFormat("dd 'de' MMMM", Locale.forLanguageTag("es-AR")) }
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }
    val todasVisitas = remember(movimientosLocal, clienteMap) {
        agruparVisitas(movimientosLocal.filter { !it.anulado }, clienteMap, fmtHora)
    }
    val visitasPorFecha = remember(todasVisitas) {
        todasVisitas.groupBy { v ->
            val mov = movimientosLocal.firstOrNull { m ->
                m.clienteId == v.clienteId && fmtHora.format(m.fecha.toDate()) == v.hora
            }
            mov?.fecha?.toDate()?.let {
                val cal = Calendar.getInstance().apply { time = it }
                Triple(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR), fmtDia.format(it))
            } ?: Triple(0, 0, "")
        }.entries.sortedByDescending { it.key.first * 1000 + it.key.second }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Actividad") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            visitasPorFecha.forEach { (fechaKey, visitasDelDia) ->
                item(key = "fecha_${fechaKey.first}_${fechaKey.second}") {
                    Text(
                        fechaKey.third,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(visitasDelDia, key = { "all_${it.items.firstOrNull()?.grupoId?.ifBlank { it.items.firstOrNull()?.id } ?: it.hora}" }) { visita ->
                    FilaVisitaDashboard(visita)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
