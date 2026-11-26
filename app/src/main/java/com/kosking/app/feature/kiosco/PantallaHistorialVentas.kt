package com.kosking.app.feature.kiosco

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Venta
import com.kosking.app.ui.theme.Verde
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorialVentas(
    ventas: List<Venta>,
    onVolver: () -> Unit
) {
    val limite90Dias = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -90) }.timeInMillis
    }

    val fmtHora    = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fmtDiaLong = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR")) }

    val ventasPorDia = remember(ventas) {
        val cal = Calendar.getInstance()
        ventas
            .filter { it.fecha.toDate().time >= limite90Dias }
            .groupBy { venta ->
                cal.time = venta.fecha.toDate()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .entries
            .sortedByDescending { it.key }
            .map { (diaMs, ventasDia) ->
                diaMs to ventasDia.sortedByDescending { it.fecha.seconds }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de ventas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (ventasPorDia.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "Sin ventas en los últimos 90 días",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        val pagerState = rememberPagerState(pageCount = { ventasPorDia.size })
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Navegación de páginas
            val (diaMs, _) = ventasPorDia[pagerState.currentPage]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    enabled = pagerState.currentPage < ventasPorDia.lastIndex
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Día anterior", Modifier.size(20.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        fmtDiaLong.format(Date(diaMs)).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${pagerState.currentPage + 1} / ${ventasPorDia.size}",
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

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                val (_, ventasDia) = ventasPorDia[pageIndex]
                val totalDia = ventasDia.sumOf { it.total }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${ventasDia.size} venta${if (ventasDia.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Total: $${"%.0f".format(totalDia)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Verde
                            )
                        }
                        HorizontalDivider()
                    }

                    items(ventasDia, key = { it.id }) { venta ->
                        val resumenItems = venta.items.take(3).joinToString(", ")
                            .let { if (venta.items.size > 3) "$it…" else it }
                        ListItem(
                            headlineContent = {
                                Text(
                                    "$${"%.0f".format(venta.total)}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Verde
                                )
                            },
                            supportingContent = if (resumenItems.isNotBlank()) {
                                {
                                    Text(
                                        resumenItems,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else null,
                            trailingContent = {
                                Text(
                                    fmtHora.format(venta.fecha.toDate()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
