package com.kosking.app.dueno.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import java.util.Calendar

@Composable
internal fun TabClientes(
    clientes: List<Cliente>,
    movimientos: List<Movimiento> = emptyList(),
    onVerCliente: (String) -> Unit,
    onAgregarCliente: () -> Unit,
    onAnotarRapido: ((Cliente, String) -> Unit)? = null
) {
    var filtro by rememberSaveable { mutableStateOf("deuda") }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var orden by rememberSaveable { mutableStateOf("deuda") }
    var mostrarOrden by remember { mutableStateOf(false) }
    var vistaLista by rememberSaveable { mutableStateOf(false) }

    val conDeuda = remember(clientes) { clientes.count { it.deudaTotal > 0 && it.habilitado } }
    val alDia = remember(clientes) { clientes.count { it.deudaTotal <= 0 && it.habilitado } }
    val deudaTotal = remember(clientes) { clientes.sumOf { it.deudaTotal } }

    data class ResumenMes(val fiado: Double, val cobrado: Double)
    fun movimientosDeMes(anio: Int, mes: Int): ResumenMes {
        var fiado = 0.0; var cobrado = 0.0
        movimientos.filter { !it.anulado }.forEach { mov ->
            val cal = Calendar.getInstance().also { c -> c.time = mov.fecha.toDate() }
            if (cal.get(Calendar.YEAR) == anio && cal.get(Calendar.MONTH) == mes) {
                if (mov.tipo == "cargo") fiado += mov.monto else cobrado += mov.monto
            }
        }
        return ResumenMes(fiado, cobrado)
    }
    val resumenMes = remember(movimientos) {
        val cal = Calendar.getInstance()
        movimientosDeMes(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }
    val resumenMesAnterior = remember(movimientos) {
        val cal = Calendar.getInstance().also { it.add(Calendar.MONTH, -1) }
        movimientosDeMes(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

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

        // Resumen del mes
        if (resumenMes.fiado > 0 || resumenMes.cobrado > 0) {
            val saldoMes = resumenMes.cobrado - resumenMes.fiado
            val pctFiado = if (resumenMesAnterior.fiado > 0)
                ((resumenMes.fiado - resumenMesAnterior.fiado) / resumenMesAnterior.fiado * 100).toInt()
            else null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                    if (resumenMes.fiado > 0) {
                        Column {
                            Text("Anotado este mes", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${"%.0f".format(resumenMes.fiado)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (resumenMes.cobrado > 0) {
                        Column {
                            Text("Cobrado", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${"%.0f".format(resumenMes.cobrado)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Verde)
                        }
                    }
                    if (resumenMes.fiado > 0 && resumenMes.cobrado > 0) {
                        Column {
                            Text("Saldo", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${if (saldoMes >= 0) "+" else ""}$${"%.0f".format(saldoMes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (saldoMes >= 0) Verde else MaterialTheme.colorScheme.error
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
            IconButton(
                onClick = { vistaLista = !vistaLista },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) { Icon(if (vistaLista) Icons.Default.GridView else Icons.Default.ViewList, "Cambiar vista", Modifier.size(20.dp)) }
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
                Seg("pagaron", pagaronEsteMes.size, "Pagaron"),
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = androidx.compose.ui.Modifier.padding(32.dp)
                ) {
                    if (clientes.isEmpty()) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = androidx.compose.ui.Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "Todavía no tenés clientes",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Agregá tu primer cliente para empezar a anotar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(onClick = onAgregarCliente) {
                            Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Agregar cliente")
                        }
                    } else {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = androidx.compose.ui.Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            if (busqueda.isNotEmpty()) "No encontramos \"$busqueda\""
                            else "Sin clientes en este filtro",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (busqueda.isNotEmpty()) {
                            TextButton(onClick = { busqueda = "" }) { Text("Limpiar búsqueda") }
                        }
                    }
                }
            }
        } else if (vistaLista) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                lazyItems(clientesFiltrados, key = { it.id }) { cliente ->
                    ListItem(
                        headlineContent = { Text(cliente.nombre, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            val dias = remember(ultimaVisita, cliente.id) {
                                val uv = ultimaVisita[cliente.id] ?: return@remember null
                                val hoy = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                                }
                                val dia = java.util.Calendar.getInstance().apply {
                                    time = uv
                                    set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                                }
                                ((hoy.timeInMillis - dia.timeInMillis) / 86_400_000L).toInt()
                            }
                            val visitaTxt = when {
                                dias == null -> "Sin movimientos"
                                dias == 0 -> "Hoy"
                                dias == 1 -> "Ayer"
                                else -> "Hace $dias días"
                            }
                            Text(
                                if (cliente.deudaTotal > 0) "$${"%.0f".format(cliente.deudaTotal)} · $visitaTxt"
                                else visitaTxt,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (cliente.deudaTotal > 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = if (onAnotarRapido != null && cliente.habilitado) ({
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = { onAnotarRapido(cliente, "cargo") },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Debe", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { onAnotarRapido(cliente, "pago") },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Cobrar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }) else null,
                        modifier = Modifier.clickable { onVerCliente(cliente.id) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
                lazyItems(listOf(Unit)) { Spacer(Modifier.height(80.dp)) }
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
                        onAnotarRapido = if (onAnotarRapido != null) ({ onAnotarRapido(cliente, "cargo") }) else null
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

