package com.kosking.app.dueno.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TabClientes(
    clientes: List<Cliente>,
    movimientos: List<Movimiento> = emptyList(),
    onVerCliente: (String) -> Unit,
    onAgregarCliente: () -> Unit,
    onAnotarRapido: ((Cliente) -> Unit)? = null
) {
    var filtro by rememberSaveable { mutableStateOf("deuda") }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var orden by rememberSaveable { mutableStateOf("deuda") }
    var mostrarOrden by remember { mutableStateOf(false) }

    val conDeuda = remember(clientes) { clientes.count { it.deudaTotal > 0 && it.habilitado } }
    val alDia = remember(clientes) { clientes.count { it.deudaTotal <= 0 && it.habilitado } }
    val deudaTotal = remember(clientes) { clientes.sumOf { it.deudaTotal } }

    val ultimaVisita = remember(movimientos) {
        movimientos.filter { !it.anulado }
            .groupBy { it.clienteId }
            .mapValues { (_, movs) -> movs.maxByOrNull { it.fecha.toDate() }?.fecha?.toDate() }
    }

    // Clientes que pagaron este mes calendario
    val pagaronEsteMes = remember(movimientos) {
        val cal = java.util.Calendar.getInstance()
        val mesActual = cal.get(java.util.Calendar.MONTH)
        val anioActual = cal.get(java.util.Calendar.YEAR)
        movimientos
            .filter { !it.anulado && it.tipo == "pago" }
            .filter {
                val c = java.util.Calendar.getInstance().apply { time = it.fecha.toDate() }
                c.get(java.util.Calendar.MONTH) == mesActual && c.get(java.util.Calendar.YEAR) == anioActual
            }
            .map { it.clienteId }
            .toSet()
    }

    val ahora = System.currentTimeMillis()

    val clientesFiltrados = remember(clientes, filtro, busqueda, orden, ultimaVisita, pagaronEsteMes) {
        clientes
            .filter { c ->
                when (filtro) {
                    "deuda" -> c.deudaTotal > 0 && c.habilitado
                    "aldia" -> c.deudaTotal <= 0 && c.habilitado
                    "bloqueado" -> !c.habilitado
                    "pagaron" -> c.id in pagaronEsteMes
                    else -> true
                }
            }
            .filter { c -> busqueda.isBlank() || c.nombre.contains(busqueda, ignoreCase = true) }
            .sortedWith(when (orden) {
                "nombre" -> compareBy { it.nombre }
                "visita" -> compareByDescending { ultimaVisita[it.id]?.time ?: 0L }
                else -> compareByDescending { it.deudaTotal }
            })
    }

    Column(Modifier.fillMaxSize()) {
        // Stats
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatMiniCard("Deuda total", "$${"%.0f".format(deudaTotal)}", isError = deudaTotal > 0, modifier = Modifier.weight(1f))
            StatMiniCard("Deben", "$conDeuda", isError = conDeuda > 0, modifier = Modifier.weight(1f))
            StatMiniCard("Al día", "$alDia", isSuccess = true, modifier = Modifier.weight(1f))
            StatMiniCard(
                "Clientes",
                "${clientes.size}/${DuenoViewModel.LIMITE_CLIENTES_GRATIS}",
                isError = clientes.size >= DuenoViewModel.LIMITE_CLIENTES_GRATIS,
                modifier = Modifier.weight(1f)
            )
        }

        // Búsqueda + orden + agregar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box {
                            if (busqueda.isEmpty()) {
                                Text("Buscar cliente", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    }
                )
                if (busqueda.isNotEmpty()) {
                    Icon(Icons.Default.Close, "Limpiar", Modifier.size(16.dp).clickable { busqueda = "" },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box {
                IconButton(
                    onClick = { mostrarOrden = true },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) { Icon(Icons.Default.SwapVert, "Ordenar", Modifier.size(20.dp)) }
                DropdownMenu(expanded = mostrarOrden, onDismissRequest = { mostrarOrden = false }) {
                    DropdownMenuItem(text = { Text("Por deuda") }, onClick = { orden = "deuda"; mostrarOrden = false },
                        leadingIcon = { if (orden == "deuda") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) })
                    DropdownMenuItem(text = { Text("Por nombre") }, onClick = { orden = "nombre"; mostrarOrden = false },
                        leadingIcon = { if (orden == "nombre") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) })
                    DropdownMenuItem(text = { Text("Última visita") }, onClick = { orden = "visita"; mostrarOrden = false },
                        leadingIcon = { if (orden == "visita") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) })
                }
            }
            IconButton(
                onClick = onAgregarCliente,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) { Icon(Icons.Default.PersonAdd, "Agregar cliente", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        }

        // Segmented control
        val bloqueados = clientes.count { !it.habilitado }
        val mesNombre = remember {
            java.text.SimpleDateFormat("MMM", java.util.Locale.forLanguageTag("es-AR"))
                .format(java.util.Date()).replaceFirstChar { it.uppercase() }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            data class Seg(val key: String, val num: Int, val label: String)
            val segs = listOf(
                Seg("deuda", conDeuda, "Deben"),
                Seg("aldia", alDia, "Al día"),
                Seg("pagaron", pagaronEsteMes.size, mesNombre),
                Seg("todos", clientes.size, "Todos"),
                Seg("bloqueado", bloqueados, "Bloq.")
            )
            segs.forEach { seg ->
                val selected = filtro == seg.key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (selected) Modifier.background(MaterialTheme.colorScheme.surface)
                                .then(Modifier.padding(1.dp))
                            else Modifier
                        )
                        .clickable { filtro = seg.key; busqueda = "" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            seg.num.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            seg.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (clientesFiltrados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (clientes.isEmpty()) "Todavía no hay clientes\nTocá + para agregar el primero"
                    else "Sin resultados para este filtro",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clientesFiltrados, key = { it.id }) { cliente ->
                    CardCliente(
                        cliente = cliente,
                        ultimaVisitaDate = ultimaVisita[cliente.id],
                        ahoraMs = ahora,
                        onVerCliente = onVerCliente,
                        onAnotarRapido = if (onAnotarRapido != null) ({ onAnotarRapido(cliente) }) else null
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun StatMiniCard(
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
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardCliente(
    cliente: Cliente,
    ultimaVisitaDate: java.util.Date?,
    ahoraMs: Long,
    onVerCliente: (String) -> Unit,
    onAnotarRapido: (() -> Unit)? = null
) {
    val diasSinVenir = remember(ultimaVisitaDate, ahoraMs) {
        if (ultimaVisitaDate == null) null
        else ((ahoraMs - ultimaVisitaDate.time) / (1000 * 60 * 60 * 24)).toInt()
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
            .combinedClickable(
                onClick = { onVerCliente(cliente.id) },
                onLongClick = onAnotarRapido
            ),
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
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(onClick = onAnotarRapido),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Anotar",
                                modifier = Modifier.size(14.dp),
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
