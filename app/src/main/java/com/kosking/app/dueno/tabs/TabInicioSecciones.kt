package com.kosking.app.dueno.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

internal fun LazyListScope.seccionCabecera(
    localData: Local?,
    totalDeuda: Double,
    clientesConDeuda: List<Cliente>,
    onAnotar: () -> Unit
) {
    item {
        if (localData != null && localData.nombre.isNotBlank()) {
            val diaKey = remember {
                val dias = listOf("domingo","lunes","martes","miercoles","jueves","viernes","sabado")
                dias[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
            }
            val horarioHoy = localData.horarios[diaKey]
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        localData.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (horarioHoy != null) {
                        val (badgeBg, badgeText, badgeLabel) = if (horarioHoy.abierto)
                            Triple(Verde.copy(alpha = 0.12f), Verde, "Abierto")
                        else Triple(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.onSurfaceVariant, "Cerrado"
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = badgeBg) {
                            Text(badgeLabel, style = MaterialTheme.typography.labelSmall, color = badgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                if (localData.direccion.isNotBlank()) {
                    Text(localData.direccion, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (localData.horarios[diaKey]?.abierto == true) {
                    val h = localData.horarios[diaKey]!!
                    Text("${h.horaApertura} – ${h.horaCierre}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Deuda total", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "$${"%.0f".format(totalDeuda)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (totalDeuda > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (clientesConDeuda.isNotEmpty()) {
                    Text(
                        "${clientesConDeuda.size} cliente${if (clientesConDeuda.size != 1) "s" else ""} deben",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(onClick = onAnotar, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Anotar", style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider()
    }
}

internal fun LazyListScope.seccionResumenSemanal(
    fiadoSemana: Double,
    cobradoSemana: Double
) {
    item {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Esta semana", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Anotado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${"%.0f".format(fiadoSemana)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Cobrado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${"%.0f".format(cobradoSemana)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Verde)
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.seccionHoy(
    clientes: List<Cliente>,
    movimientosHoy: List<Movimiento>,
    fiadoHoy: Double,
    cobradoHoy: Double,
    visitasHoy: List<VisitaResumen>,
    onCompartirResumen: () -> Unit,
    onTabChange: (Int) -> Unit,
    onAbrirVisita: (VisitaResumen) -> Unit,
    onVerHistorial: () -> Unit
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hoy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (fiadoHoy > 0) Text("-$${"%.0f".format(fiadoHoy)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                if (cobradoHoy > 0) Text("+$${"%.0f".format(cobradoHoy)}", style = MaterialTheme.typography.labelSmall, color = Verde)
                if (fiadoHoy > 0 || cobradoHoy > 0) {
                    IconButton(onClick = onCompartirResumen, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Compartir", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (clientes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.People, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Todavía no hay clientes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Sumá tu primer cliente y empezá a anotar.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = { onTabChange(1) }) {
                    Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar cliente")
                }
            }
        } else if (movimientosHoy.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text("Todavía nada hoy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            visitasHoy.take(5).forEach { visita ->
                FilaVisitaMP(visita, onClick = { onAbrirVisita(visita) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onVerHistorial, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Ver historial completo", style = MaterialTheme.typography.labelSmall)
            }
        }
        HorizontalDivider()
    }
}

internal fun LazyListScope.seccionAcuerdosVencidos(
    clientesUrgentes: List<Cliente>,
    clientesVencidos: List<Cliente>,
    hoy: Date,
    fmtCompromiso: SimpleDateFormat,
    onRecordarTodos: () -> Unit,
    onRegistrarPago: (Cliente) -> Unit,
    onWhatsAppCliente: (Cliente) -> Unit,
    onAbrirDetalleCliente: (String) -> Unit
) {
    if (clientesUrgentes.isEmpty()) return

    item {
        val clientesConFono = remember(clientesVencidos) {
            clientesVencidos.filter { it.telefono.isNotBlank() }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Acuerdos vencidos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (clientesConFono.isNotEmpty()) {
                TextButton(
                    onClick = onRecordarTodos,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Recordar a todos", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    items(clientesUrgentes, key = { "deuda_${it.id}" }) { cliente ->
        val esVencido = cliente.fechaCompromiso != null && cliente.fechaCompromiso.toDate().before(hoy)
        val diasVencido = if (esVencido) {
            val diff = hoy.time - cliente.fechaCompromiso!!.toDate().time
            (diff / 86_400_000L).toInt().coerceAtLeast(1)
        } else 0
        ListItem(
            headlineContent = { Text(cliente.nombre, fontWeight = FontWeight.Medium) },
            supportingContent = {
                if (esVencido) {
                    Text(
                        "Prometió el ${fmtCompromiso.format(cliente.fechaCompromiso!!.toDate())} · vencido hace $diasVencido ${if (diasVencido == 1) "día" else "días"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        "Deuda: $${"%.0f".format(cliente.deudaTotal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (esVencido) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$${"%.0f".format(cliente.deudaTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { onRegistrarPago(cliente) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Cobrar", style = MaterialTheme.typography.labelSmall)
                    }
                    if (cliente.telefono.isNotBlank()) {
                        IconButton(
                            onClick = { onWhatsAppCliente(cliente) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Avisar por WhatsApp",
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            modifier = Modifier.clickable { onAbrirDetalleCliente(cliente.id) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
    }
}

internal fun LazyListScope.alertasCards(
    disputados: List<Movimiento>,
    clienteMap: Map<String, Cliente>,
    sobreLimite: List<Cliente>,
    onAbrirDetalleMovimiento: (String) -> Unit,
    onAbrirDetalleCliente: (String) -> Unit
) {
    if (disputados.isNotEmpty()) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(
                            "${disputados.size} movimiento${if (disputados.size != 1) "s" else ""} en disputa",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    disputados.take(3).forEach { mov ->
                        val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: "Cliente"
                        val concepto = mov.producto.ifBlank { if (mov.tipo == "cargo") "Cargo" else "Pago" }
                        Text(
                            "• $nombreCliente — $concepto $${"%.0f".format(mov.monto)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAbrirDetalleMovimiento(mov.id) }
                                .padding(vertical = 2.dp)
                        )
                    }
                    if (disputados.size > 3) {
                        Text("+ ${disputados.size - 3} más",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }

    if (sobreLimite.isNotEmpty()) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CreditCard, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(
                            "${sobreLimite.size} cliente${if (sobreLimite.size != 1) "s" else ""} excedieron su límite",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    sobreLimite.take(3).forEach { c ->
                        Text(
                            "• ${c.nombre} — ${"$"}${"%.0f".format(c.deudaTotal)} / ${"$"}${"%.0f".format(c.limiteCredito)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAbrirDetalleCliente(c.id) }
                                .padding(vertical = 2.dp)
                        )
                    }
                    if (sobreLimite.size > 3) {
                        Text("+ ${sobreLimite.size - 3} más",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}
