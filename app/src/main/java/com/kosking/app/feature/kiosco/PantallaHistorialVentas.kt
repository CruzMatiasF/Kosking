package com.kosking.app.feature.kiosco

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Venta
import com.kosking.app.ui.theme.Verde
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

private sealed class EntradaActividad {
    abstract val fechaMs: Long

    data class EntradaVenta(val venta: Venta) : EntradaActividad() {
        override val fechaMs get() = venta.fecha.toDate().time
    }

    data class EntradaMovs(
        val clienteNombre: String,
        val descripcion: String,
        val total: Double,
        val tipo: String,
        override val fechaMs: Long
    ) : EntradaActividad()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorialVentas(
    ventas: List<Venta>,
    movimientos: List<Movimiento> = emptyList(),
    clienteMap: Map<String, Cliente> = emptyMap(),
    onVolver: () -> Unit
) {
    val limite90Dias = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -90) }.timeInMillis
    }

    val fmtHora    = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fmtDiaLong = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR")) }

    val actividadPorDia = remember(ventas, movimientos, clienteMap) {
        val cal = Calendar.getInstance()

        fun dayKey(timeMs: Long): Long {
            cal.timeInMillis = timeMs
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val entradas = mutableListOf<EntradaActividad>()

        ventas.filter { it.fecha.toDate().time >= limite90Dias }
              .forEach { entradas.add(EntradaActividad.EntradaVenta(it)) }

        val movsFiltrados = movimientos.filter {
            !it.anulado && it.fecha.toDate().time >= limite90Dias
        }
        // Agrupar por grupoId; si está vacío cada movimiento es su propia entrada
        val grupos = movsFiltrados.groupBy { if (it.grupoId.isBlank()) it.id else it.grupoId }
        grupos.values.forEach { grupo ->
            val clienteNombre = clienteMap[grupo.first().clienteId]?.nombre ?: "Cliente"
            val items = grupo.map { mov ->
                when {
                    mov.producto.isBlank() -> ""
                    mov.cantidad > 1       -> "${mov.producto} x${mov.cantidad}"
                    else                   -> mov.producto
                }
            }.filter { it.isNotBlank() }
            val descripcion = items.joinToString(", ").ifBlank {
                if (grupo.first().tipo == "pago") "Cobro" else "Movimiento"
            }
            val total   = grupo.sumOf { it.monto }
            val tipo    = grupo.first().tipo
            val fechaMs = grupo.minOf { it.fecha.toDate().time }
            entradas.add(EntradaActividad.EntradaMovs(clienteNombre, descripcion, total, tipo, fechaMs))
        }

        entradas
            .groupBy { dayKey(it.fechaMs) }
            .entries
            .sortedByDescending { it.key }
            .map { (diaMs, lista) -> diaMs to lista.sortedByDescending { it.fechaMs } }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Historial de actividad", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (actividadPorDia.isEmpty()) {
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
                        "Sin actividad en los últimos 90 días",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        val pagerState = rememberPagerState(pageCount = { actividadPorDia.size })
        val scope = rememberCoroutineScope()
        val (diaMs, _) = actividadPorDia[pagerState.currentPage]

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Fecha del día seleccionado ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    fmtDiaLong.format(Date(diaMs)).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${pagerState.currentPage + 1} / ${actividadPorDia.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                val (_, entradasDia) = actividadPorDia[pageIndex]
                val totalVentasDia = entradasDia.filterIsInstance<EntradaActividad.EntradaVenta>().sumOf { it.venta.total }
                val cantVentas = entradasDia.count { it is EntradaActividad.EntradaVenta }
                val cantMovs   = entradasDia.count { it is EntradaActividad.EntradaMovs }

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
                            val resumen = buildString {
                                if (cantVentas > 0) append("$cantVentas venta${if (cantVentas != 1) "s" else ""}")
                                if (cantVentas > 0 && cantMovs > 0) append(" · ")
                                if (cantMovs > 0) append("$cantMovs anotac${if (cantMovs != 1) "iones" else "ión"}")
                            }
                            Text(
                                resumen,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (totalVentasDia > 0) Text(
                                "$${"%.0f".format(totalVentasDia)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Verde
                            )
                        }
                        HorizontalDivider()
                    }

                    items(entradasDia, key = {
                        when (it) {
                            is EntradaActividad.EntradaVenta -> "v_${it.venta.id}"
                            is EntradaActividad.EntradaMovs  -> "m_${it.fechaMs}_${it.clienteNombre}"
                        }
                    }) { entrada ->
                        when (entrada) {
                            is EntradaActividad.EntradaVenta -> {
                                val venta = entrada.venta
                                val resumenItems = venta.items.take(3).joinToString(", ")
                                    .let { if (venta.items.size > 3) "$it…" else it }
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.ShoppingCart, null,
                                            Modifier.size(18.dp), tint = Verde
                                        )
                                    },
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
                            }
                            is EntradaActividad.EntradaMovs -> {
                                val esPago = entrada.tipo == "pago"
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            if (esPago) Icons.Default.CheckCircle else Icons.Default.Edit,
                                            null,
                                            Modifier.size(18.dp),
                                            tint = if (esPago) Verde else MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    headlineContent = {
                                        Text(entrada.clienteNombre, fontWeight = FontWeight.SemiBold)
                                    },
                                    supportingContent = {
                                        Text(
                                            if (esPago) "Cobro · $${"%.0f".format(entrada.total)}"
                                            else "${entrada.descripcion} · $${"%.0f".format(entrada.total)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    trailingContent = {
                                        Text(
                                            fmtHora.format(Date(entrada.fechaMs)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }

            // ── Navegación de días (abajo) ──
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    enabled = pagerState.currentPage < actividadPorDia.lastIndex
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Día anterior")
                }
                Text(
                    "Cambiar día",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Día siguiente")
                }
            }
        }
    }
}
