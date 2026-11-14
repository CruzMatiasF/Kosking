package com.kosking.app.dueno.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
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

    val ahora = remember { System.currentTimeMillis() }

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

        ResumenMesClientes(
            fiadoMes = resumenMes.fiado,
            cobradoMes = resumenMes.cobrado,
            fiadoMesAnterior = resumenMesAnterior.fiado
        )

        val bloqueados = clientes.count { !it.habilitado }
        BarraFiltrosClientes(
            busqueda = busqueda,
            orden = orden,
            vistaLista = vistaLista,
            filtro = filtro,
            conDeuda = conDeuda,
            alDia = alDia,
            pagaronEsteMes = pagaronEsteMes.size,
            totalClientes = clientes.size,
            bloqueados = bloqueados,
            onBusquedaChange = { busqueda = it },
            onOrdenChange = { orden = it },
            onVistaChange = { vistaLista = it },
            onFiltroChange = { filtro = it },
            onAgregarCliente = onAgregarCliente
        )

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

