package com.kosking.app.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Venta
import com.kosking.app.ui.theme.Verde
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun TabHistorial(
    vm: DuenoViewModel,
    clientes: List<Cliente>,
    onVerCliente: (String) -> Unit = {}
) {
    val movimientosLocal by vm.movimientosLocal.collectAsState()
    val ventasLocal by vm.ventasLocal.collectAsState()

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

    val ventasPorDia = remember(ventasLocal) {
        val cal = Calendar.getInstance()
        ventasLocal
            .filter { it.fecha.toDate().time >= inicioMes }
            .groupBy { venta ->
                cal.time = venta.fecha.toDate()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
    }

    // Solo días con movimientos, más reciente primero (página 0 = hoy)
    val paginas = remember(movimientosLocal) {
        val cal = Calendar.getInstance()
        movimientosLocal
            .filter { !it.anulado && it.fecha.toDate().time >= inicioMes }
            .groupBy { mov ->
                cal.time = mov.fecha.toDate()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .entries
            .sortedByDescending { it.key }
            .map { (diaMs, movs) ->
                PaginaDia(
                    diaMs = diaMs,
                    titulo = fmtDiaLong.format(Date(diaMs)).replaceFirstChar { it.uppercase() },
                    movimientos = movs.sortedByDescending { it.fecha.seconds }
                )
            }
    }

    val (totalMesAnotado, totalMesCobrado) = remember(movimientosLocal) {
        var anotado = 0.0; var cobrado = 0.0
        movimientosLocal.forEach { mov ->
            if (!mov.anulado && mov.fecha.toDate().time >= inicioMes) {
                if (mov.tipo == "cargo") anotado += mov.monto else cobrado += mov.monto
            }
        }
        anotado to cobrado
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

    val pagerState = rememberPagerState(pageCount = { paginas.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        // Resumen del mes arriba
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResumenTile(
                label = "Anotado mes",
                monto = totalMesAnotado,
                color = MaterialTheme.colorScheme.errorContainer,
                onColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            ResumenTile(
                label = "Cobrado mes",
                monto = totalMesCobrado,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                onColor = MaterialTheme.colorScheme.onTertiaryContainer,
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

        // Pager: cada página es un día
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val pagina = paginas[pageIndex]
            val anotadoDia = pagina.movimientos.filter { it.tipo == "cargo" }.sumOf { it.monto }
            val cobradoDia = pagina.movimientos.filter { it.tipo == "pago" }.sumOf { it.monto }
            val ventasDia = ventasPorDia[pagina.diaMs] ?: emptyList()
            val totalVentasDia = ventasDia.sumOf { it.total }

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
                        if (cobradoDia > 0) ResumenTile(
                            label = "Cobrado",
                            monto = cobradoDia,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        if (totalVentasDia > 0) ResumenTile(
                            label = "Ventas",
                            monto = totalVentasDia,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Movimientos de fiado del día
                items(pagina.movimientos, key = { it.id }) { mov ->
                    val nombreCliente = clientesMap[mov.clienteId]?.nombre ?: "Cliente"
                    val esPago = mov.tipo == "pago"
                    ListItem(
                        headlineContent = { Text(nombreCliente, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(
                                if (esPago) "Pago" else mov.producto.ifBlank { "Cargo" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${if (esPago) "+" else "-"}$${"%.0f".format(mov.monto)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (esPago) Verde else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    fmtHora.format(mov.fecha.toDate()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.clickable { onVerCliente(mov.clienteId) }
                    )
                    HorizontalDivider()
                }

                // Ventas en efectivo del día
                if (ventasDia.isNotEmpty()) {
                    item {
                        Text(
                            "VENTAS EN EFECTIVO · ${ventasDia.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(ventasDia.sortedByDescending { it.fecha.seconds }, key = { it.id }) { venta ->
                        val resumenItems = venta.items.take(3).joinToString(", ")
                            .let { if (venta.items.size > 3) "$it…" else it }
                        ListItem(
                            headlineContent = {
                                Text("$${"%.0f".format(venta.total)}",
                                    fontWeight = FontWeight.Medium,
                                    color = Verde)
                            },
                            supportingContent = if (resumenItems.isNotBlank()) {
                                { Text(resumenItems, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                            } else null,
                            trailingContent = {
                                Text(fmtHora.format(venta.fecha.toDate()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private data class PaginaDia(
    val diaMs: Long,
    val titulo: String,
    val movimientos: List<Movimiento>
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
