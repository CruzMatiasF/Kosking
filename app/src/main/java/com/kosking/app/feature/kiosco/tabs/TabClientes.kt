package com.kosking.app.feature.kiosco.tabs

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import java.util.Calendar

@Composable
internal fun TabClientes(
    clientes: List<Cliente>,
    movimientos: List<Movimiento> = emptyList(),
    modifier: Modifier = Modifier,
    onVerCliente: (String) -> Unit,
    onAgregarCliente: () -> Unit,
    onAnotarRapido: ((Cliente, String) -> Unit)? = null,
    esEmpleado: Boolean = false
) {
    var filtro by rememberSaveable { mutableStateOf("deuda") }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var orden by rememberSaveable { mutableStateOf("deuda") }
    var mostrarOrden by remember { mutableStateOf(false) }
    var vistaLista by rememberSaveable { mutableStateOf(false) }

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
        val cal = Calendar.getInstance()
        val mesActual = cal.get(Calendar.MONTH)
        val anioActual = cal.get(Calendar.YEAR)
        movimientos
            .filter { !it.anulado && it.tipo == "pago" }
            .filter {
                val c = Calendar.getInstance().apply { time = it.fecha.toDate() }
                c.get(Calendar.MONTH) == mesActual && c.get(Calendar.YEAR) == anioActual
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

    Column(modifier.fillMaxSize()) {
        val limite = DuenoViewModel.LIMITE_CLIENTES_GRATIS
        if (clientes.size >= (limite * 0.8).toInt()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.small,
                color = if (clientes.size >= limite) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    if (clientes.size >= limite) "Límite alcanzado: ${clientes.size}/$limite clientes"
                    else "${clientes.size}/$limite clientes — te quedan ${limite - clientes.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (clientes.size >= limite) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

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
            onAgregarCliente = onAgregarCliente,
            esEmpleado = esEmpleado
        )

        if (clientesFiltrados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    if (clientes.isEmpty()) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "Todavía no hay clientes",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!esEmpleado) {
                            Text(
                                "Agregá tu primer cliente para empezar a anotar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = onAgregarCliente) {
                                Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Agregar cliente")
                            }
                        }
                    } else {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            if (busqueda.isNotEmpty()) "No encontramos \"$busqueda\""
                            else "Sin clientes en este filtro",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
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
                                val hoy = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }
                                val dia = Calendar.getInstance().apply {
                                    time = uv
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
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

