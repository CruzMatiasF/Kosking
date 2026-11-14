package com.kosking.app.dueno.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.ui.theme.Verde

@Composable
internal fun ResumenMesClientes(
    fiadoMes: Double,
    cobradoMes: Double,
    fiadoMesAnterior: Double
) {
    if (fiadoMes <= 0 && cobradoMes <= 0) return
    val saldo = cobradoMes - fiadoMes
    val pctFiado = if (fiadoMesAnterior > 0)
        ((fiadoMes - fiadoMesAnterior) / fiadoMesAnterior * 100).toInt()
    else null
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            if (fiadoMes > 0) {
                Column {
                    Text("Anotado este mes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${"%.0f".format(fiadoMes)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
            }
            if (cobradoMes > 0) {
                Column {
                    Text("Cobrado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${"%.0f".format(cobradoMes)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Verde)
                }
            }
            if (fiadoMes > 0 && cobradoMes > 0) {
                Column {
                    Text("Saldo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${if (saldo >= 0) "+" else ""}$${"%.0f".format(saldo)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (saldo >= 0) Verde else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        if (pctFiado != null) {
            Text(
                "${if (pctFiado >= 0) "↑" else "↓"} ${kotlin.math.abs(pctFiado)}% vs mes pasado",
                style = MaterialTheme.typography.labelSmall,
                color = if (pctFiado > 0) MaterialTheme.colorScheme.error else Verde
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
}

@Composable
internal fun BarraFiltrosClientes(
    busqueda: String,
    orden: String,
    vistaLista: Boolean,
    filtro: String,
    conDeuda: Int,
    alDia: Int,
    pagaronEsteMes: Int,
    totalClientes: Int,
    bloqueados: Int,
    onBusquedaChange: (String) -> Unit,
    onOrdenChange: (String) -> Unit,
    onVistaChange: (Boolean) -> Unit,
    onFiltroChange: (String) -> Unit,
    onAgregarCliente: () -> Unit
) {
    var mostrarOrden by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = busqueda,
                onValueChange = onBusquedaChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner ->
                    Box {
                        if (busqueda.isEmpty()) Text("Buscar cliente", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                }
            )
            if (busqueda.isNotEmpty()) {
                Icon(Icons.Default.Close, "Limpiar", Modifier.size(16.dp).clickable { onBusquedaChange("") }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = { onVistaChange(!vistaLista) }, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Icon(if (vistaLista) Icons.Default.GridView else Icons.Default.ViewList, "Vista", Modifier.size(20.dp))
        }
        Box {
            IconButton(onClick = { mostrarOrden = true }, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Icon(Icons.Default.SwapVert, "Ordenar", Modifier.size(20.dp))
            }
            DropdownMenu(expanded = mostrarOrden, onDismissRequest = { mostrarOrden = false }) {
                DropdownMenuItem(text = { Text("Por deuda") }, onClick = { onOrdenChange("deuda"); mostrarOrden = false }, leadingIcon = { if (orden == "deuda") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) })
                DropdownMenuItem(text = { Text("Por nombre") }, onClick = { onOrdenChange("nombre"); mostrarOrden = false }, leadingIcon = { if (orden == "nombre") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) })
                DropdownMenuItem(text = { Text("Última visita") }, onClick = { onOrdenChange("visita"); mostrarOrden = false }, leadingIcon = { if (orden == "visita") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) })
            }
        }
        IconButton(onClick = onAgregarCliente, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Icon(Icons.Default.PersonAdd, "Agregar", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }

    // Segmented control
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        data class Seg(val key: String, val num: Int, val label: String)
        val segs = listOf(Seg("deuda", conDeuda, "Deben"), Seg("aldia", alDia, "Al día"), Seg("pagaron", pagaronEsteMes, "Pagaron"), Seg("todos", totalClientes, "Todos"), Seg("bloqueado", bloqueados, "Bloq."))
        segs.forEach { seg ->
            val selected = filtro == seg.key
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .then(if (selected) Modifier.background(MaterialTheme.colorScheme.surface).then(Modifier.padding(1.dp)) else Modifier)
                    .clickable { onFiltroChange(seg.key); onBusquedaChange("") }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(seg.num.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(seg.label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun StatMiniCard(
    label: String,
    valor: String,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    isError -> MaterialTheme.colorScheme.error
                    isSuccess -> Verde
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
internal fun CardCliente(
    cliente: Cliente,
    ultimaVisitaDate: java.util.Date?,
    ahoraMs: Long,
    onVerCliente: (String) -> Unit,
    onAnotarRapido: (() -> Unit)? = null
) {
    val diasSinVenir = remember(ultimaVisitaDate) {
        if (ultimaVisitaDate == null) null
        else {
            val hoy = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }
            val dia = java.util.Calendar.getInstance().apply {
                time = ultimaVisitaDate
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }
            ((hoy.timeInMillis - dia.timeInMillis) / 86_400_000L).toInt()
        }
    }

    val accentColor = when {
        !cliente.habilitado -> MaterialTheme.colorScheme.outlineVariant
        cliente.limiteCredito > 0 && cliente.deudaTotal > cliente.limiteCredito -> MaterialTheme.colorScheme.error
        cliente.deudaTotal > 300 -> MaterialTheme.colorScheme.error
        cliente.deudaTotal > 50 -> MaterialTheme.colorScheme.secondary
        cliente.deudaTotal > 0 -> Verde
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val visitaTexto = when {
        diasSinVenir == null && cliente.deudaTotal > 0 -> "+90 días"
        diasSinVenir == null -> "Nuevo"
        diasSinVenir == 0 -> "Hoy"
        diasSinVenir == 1 -> "Ayer"
        diasSinVenir < 7 -> "Hace $diasSinVenir días"
        else -> "Hace ${diasSinVenir / 7} sem."
    }
    val visitaEsAlerta = (diasSinVenir == null && cliente.deudaTotal > 0) ||
        (diasSinVenir != null && diasSinVenir >= 7 && cliente.deudaTotal > 0)
    val visitaEsWarning = diasSinVenir != null && diasSinVenir in 3..6 && cliente.deudaTotal > 0

    val dotColor = when {
        visitaEsAlerta -> MaterialTheme.colorScheme.error
        visitaEsWarning -> MaterialTheme.colorScheme.secondary
        else -> Verde
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVerCliente(cliente.id) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor)
            )
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val avatarBg = when {
                    !cliente.habilitado -> MaterialTheme.colorScheme.surfaceVariant
                    cliente.deudaTotal > 0 -> accentColor.copy(alpha = 0.18f)
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        cliente.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (cliente.deudaTotal > 0 && cliente.habilitado) accentColor
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    cliente.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (cliente.deudaTotal > 0 && cliente.habilitado) {
                    Text(
                        "$${"%.0f".format(cliente.deudaTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            visitaTexto,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (onAnotarRapido != null && cliente.habilitado) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(onClick = onAnotarRapido),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Anotar",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                if (cliente.limiteCredito > 0) {
                    val progreso = (cliente.deudaTotal / cliente.limiteCredito).coerceIn(0.0, 1.0).toFloat()
                    val superado = cliente.deudaTotal > cliente.limiteCredito
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = if (superado) MaterialTheme.colorScheme.error else accentColor,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}
