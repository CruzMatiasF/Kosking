package com.kosking.app.home

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import com.kosking.app.util.WhatsAppUtil
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun TabCuaderno(
    vm: DuenoViewModel,
    clientes: List<Cliente>,
    nombreLocal: String = "",
    onVerCliente: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val movimientosLocal by vm.movimientosLocal.collectAsState()

    val inicioMes = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val clientesMap = remember(clientes) { clientes.associateBy { it.id } }
    val fmtHora    = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fmtDiaLong = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR")) }

    // Solo días con movimientos de fiado, más reciente primero
    val paginas = remember(movimientosLocal) {
        val cal = Calendar.getInstance()
        movimientosLocal
            .filter { !it.anulado && it.tipo == "cargo" && it.fecha.toDate().time >= inicioMes }
            .groupBy { mov ->
                cal.time = mov.fecha.toDate()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .entries
            .sortedByDescending { it.key }
            .map { (diaMs, movs) ->
                // Agrupar por grupoId: movimientos sin grupoId son grupos individuales
                val visitas = movs
                    .groupBy { mov -> mov.grupoId.ifBlank { mov.id } }
                    .values
                    .map { grupo ->
                        // Hora = el movimiento más reciente del grupo
                        val horaMs = grupo.maxOf { it.fecha.seconds }
                        VisitaAgrupada(
                            clienteId = grupo.first().clienteId,
                            movimientos = grupo.sortedBy { it.producto },
                            horaMs = horaMs,
                            total = grupo.sumOf { it.monto },
                            anotadoPor = grupo.first().anotadoPorNombre
                        )
                    }
                    .sortedByDescending { it.horaMs }
                PaginaDia(
                    diaMs = diaMs,
                    titulo = fmtDiaLong.format(Date(diaMs)).replaceFirstChar { it.uppercase() },
                    visitas = visitas
                )
            }
    }

    val totalMesAnotado = remember(movimientosLocal) {
        movimientosLocal.sumOf { mov ->
            if (!mov.anulado && mov.tipo == "cargo" && mov.fecha.toDate().time >= inicioMes) mov.monto else 0.0
        }
    }

    if (paginas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.MenuBook, null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    "Sin movimientos este mes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var empleadoFiltro by remember { mutableStateOf("") }

    val pagerState = rememberPagerState(pageCount = { paginas.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        // Resumen del mes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResumenTile(
                label = "Anotado este mes",
                monto = totalMesAnotado,
                color = MaterialTheme.colorScheme.errorContainer,
                onColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()

        // Navegación de páginas
        val paginaActual = paginas[pagerState.currentPage]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                enabled = pagerState.currentPage < paginas.lastIndex
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Día anterior", Modifier.size(20.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    paginaActual.titulo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${pagerState.currentPage + 1} / ${paginas.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                enabled = pagerState.currentPage > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Día siguiente", Modifier.size(20.dp))
            }
        }

        HorizontalDivider()

        // Chips de filtro por empleado (solo si hay más de uno en la página actual)
        val paginaActualEmpleados = remember(pagerState.currentPage, paginas) {
            paginas.getOrNull(pagerState.currentPage)
                ?.visitas
                ?.map { it.anotadoPor }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                ?: emptyList()
        }
        if (paginaActualEmpleados.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = empleadoFiltro.isEmpty(),
                    onClick = { empleadoFiltro = "" },
                    label = { Text("Todos") }
                )
                paginaActualEmpleados.forEach { emp ->
                    FilterChip(
                        selected = empleadoFiltro == emp,
                        onClick = { empleadoFiltro = if (empleadoFiltro == emp) "" else emp },
                        label = { Text(emp) }
                    )
                }
            }
            HorizontalDivider()
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            val pagina = paginas[pageIndex]
            val visitasFiltradas = if (empleadoFiltro.isEmpty()) pagina.visitas
                                   else pagina.visitas.filter { it.anotadoPor == empleadoFiltro }
            val anotadoDia = visitasFiltradas.sumOf { it.total }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Subtotales del día
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (anotadoDia > 0) ResumenTile(
                            label = "Anotado",
                            monto = anotadoDia,
                            color = MaterialTheme.colorScheme.errorContainer,
                            onColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Visitas agrupadas
                items(visitasFiltradas, key = { it.horaMs.toString() + it.clienteId }) { visita ->
                    val nombreCliente = clientesMap[visita.clienteId]?.nombre ?: "Cliente"
                    val telefonoCliente = clientesMap[visita.clienteId]?.telefono ?: ""

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVerCliente(visita.clienteId) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Encabezado: nombre + hora + total + ticket
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    nombreCliente,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (telefonoCliente.isNotBlank() && visita.movimientos.isNotEmpty()) {
                                    val hora = fmtHora.format(Date(visita.horaMs * 1000))
                                    IconButton(
                                        onClick = {
                                            val mensaje = WhatsAppUtil.ticketVisita(
                                                nombreCliente = nombreCliente,
                                                nombreLocal = nombreLocal,
                                                items = visita.movimientos.map { Triple(it.producto, it.monto, it.cantidad) },
                                                tipo = "cargo",
                                                hora = hora
                                            )
                                            WhatsAppUtil.abrirWhatsApp(context, telefonoCliente, mensaje)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Chat,
                                            contentDescription = "Enviar ticket",
                                            modifier = Modifier.size(16.dp),
                                            tint = com.kosking.app.feature.kiosco.ColorWhatsApp
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "-$${"%.0f".format(visita.total)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        fmtHora.format(Date(visita.horaMs * 1000)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (visita.anotadoPor.isNotBlank()) {
                                        Text(
                                            visita.anotadoPor,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // Productos de la visita
                        if (visita.movimientos.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 22.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                visita.movimientos.forEach { mov ->
                                    val descripcion = buildString {
                                        if (mov.producto.isNotBlank()) append(mov.producto)
                                        else append("Cargo")
                                        if (mov.cantidad > 1) append(" ×${mov.cantidad}")
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            descripcion,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "$${"%.0f".format(mov.monto)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private data class VisitaAgrupada(
    val clienteId: String,
    val movimientos: List<Movimiento>,
    val horaMs: Long,
    val total: Double,
    val anotadoPor: String = ""
)

private data class PaginaDia(
    val diaMs: Long,
    val titulo: String,
    val visitas: List<VisitaAgrupada>
)

@Composable
private fun ResumenTile(
    label: String,
    monto: Double,
    color: androidx.compose.ui.graphics.Color,
    onColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(color = color, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = onColor.copy(alpha = 0.7f))
            Text(
                "$${"%.0f".format(monto)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = onColor
            )
        }
    }
}
