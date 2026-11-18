package com.kosking.app.dueno.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Local
import com.kosking.app.ui.theme.Verde

@Composable
internal fun CardsResumenDueno(
    localData: Local?,
    totalDeuda: Double,
    clientesConDeuda: List<Cliente>,
    fiadoSemana: Double,
    cobradoSemana: Double,
    fiadoSemanaAnterior: Double,
    cobradoSemanaAnterior: Double,
    fiadoMes: Double,
    cobradoMes: Double,
    fiadoMesAnterior: Double,
    cobradoMesAnterior: Double,
    onAnotar: () -> Unit,
    onVerClientes: () -> Unit,
    onAbrirProductos: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (localData != null && localData.nombre.isNotBlank()) {
            Text(
                localData.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 2.dp)
            )
            if (localData.direccion.isNotBlank()) {
                Text(
                    localData.direccion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                )
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // start=16 + end=32 = 48dp → cardWidth = maxWidth - 48dp
            val hPad = 16.dp
            val peekEnd = 32.dp
            val cardWidth = maxWidth - hPad - peekEnd
            val pagerState = rememberPagerState(pageCount = { 2 })
            Column {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(start = hPad, end = peekEnd),
                    pageSpacing = 10.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
                ) { page ->
                    when (page) {
                        0 -> CardDeudaTotal(
                            cardWidth = cardWidth,
                            totalDeuda = totalDeuda,
                            clientesConDeuda = clientesConDeuda,
                            onAnotar = onAnotar,
                            onVerClientes = onVerClientes,
                            onAbrirProductos = onAbrirProductos
                        )
                        else -> CardResumenPeriodo(
                            cardWidth = cardWidth,
                            fiadoSemana = fiadoSemana,
                            cobradoSemana = cobradoSemana,
                            fiadoSemanaAnterior = fiadoSemanaAnterior,
                            cobradoSemanaAnterior = cobradoSemanaAnterior,
                            fiadoMes = fiadoMes,
                            cobradoMes = cobradoMes,
                            fiadoMesAnterior = fiadoMesAnterior,
                            cobradoMesAnterior = cobradoMesAnterior
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { i ->
                        val selected = pagerState.currentPage == i
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (selected) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }
        }

        HorizontalDivider()
    }
}

@Composable
private fun CardDeudaTotal(
    cardWidth: Dp,
    totalDeuda: Double,
    clientesConDeuda: List<Cliente>,
    onAnotar: () -> Unit,
    onVerClientes: () -> Unit,
    onAbrirProductos: () -> Unit
) {
    val totalClientes = clientesConDeuda.size
    val alDia = remember(totalClientes) { clientesConDeuda.count { it.deudaTotal <= 0 } }

    Card(
        modifier = Modifier.width(cardWidth).height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Te deben en total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$${"%.0f".format(totalDeuda)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (totalDeuda > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                buildString {
                    if (clientesConDeuda.isNotEmpty()) append("${clientesConDeuda.size} deben")
                    if (alDia > 0) append(" · $alDia al día")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AtajoBtn(Icons.Default.Edit, "Anotar", onAnotar, primary = true)
                AtajoBtn(Icons.Default.People, "Clientes", onVerClientes)
                AtajoBtn(Icons.Default.Storefront, "Productos", onAbrirProductos)
            }
        }
    }
}

@Composable
private fun CardResumenPeriodo(
    cardWidth: Dp,
    fiadoSemana: Double,
    cobradoSemana: Double,
    fiadoSemanaAnterior: Double,
    cobradoSemanaAnterior: Double,
    fiadoMes: Double,
    cobradoMes: Double,
    fiadoMesAnterior: Double,
    cobradoMesAnterior: Double
) {
    var esSemana by remember { mutableStateOf(true) }

    val fiado = if (esSemana) fiadoSemana else fiadoMes
    val cobrado = if (esSemana) cobradoSemana else cobradoMes
    val fiadoAnterior = if (esSemana) fiadoSemanaAnterior else fiadoMesAnterior
    val cobradoAnterior = if (esSemana) cobradoSemanaAnterior else cobradoMesAnterior

    val recuperoPct = if (fiado > 0) ((cobrado / fiado) * 100).toInt().coerceAtMost(100) else null

    Card(
        modifier = Modifier.width(cardWidth).height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatSemana("Anotado", fiado, MaterialTheme.colorScheme.error)
                StatSemana("Cobrado", cobrado, Verde)
            }

            HorizontalDivider()

            if (recuperoPct != null) {
                val color = when {
                    recuperoPct >= 80 -> Verde
                    recuperoPct >= 50 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Recuperaste el $recuperoPct% de lo que anotaste",
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                    LinearProgressIndicator(
                        progress = { recuperoPct / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    val periodoLabel = if (esSemana) "semana anterior" else "mes anterior"
                    Text(
                        if (cobradoAnterior > 0)
                            "Período anterior: $${"%.0f".format(cobradoAnterior)} cobrado"
                        else
                            "Sin datos del $periodoLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "Sin movimientos en este período",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AtajoBtn(Icons.Default.DateRange, "Semana", { esSemana = true }, primary = esSemana)
                AtajoBtn(Icons.Default.CalendarMonth, "Mes", { esSemana = false }, primary = !esSemana)
            }
        }
    }
}

@Composable
private fun StatSemana(
    label: String,
    valor: Double,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "$${"%.0f".format(valor)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}


@Composable
private fun AtajoBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    val bgColor = if (primary) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (primary) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = iconColor
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

