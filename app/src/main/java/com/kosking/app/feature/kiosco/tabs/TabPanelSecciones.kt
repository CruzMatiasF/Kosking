package com.kosking.app.feature.kiosco.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Venta
import com.kosking.app.ui.theme.Verde
import java.text.SimpleDateFormat
import java.util.Date

internal fun LazyListScope.seccionHoy(
    ventas: List<Venta> = emptyList(),
    onVerCuaderno: () -> Unit = {},
    onVerTodasVentas: () -> Unit = {},
    esEmpleado: Boolean = false
) {
    item {
        val totalVentas = ventas.sumOf { it.total }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ventas del día", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (totalVentas > 0) Text(
                "🛒$${"%.0f".format(totalVentas)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Verde
            )
        }
        if (ventas.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, null, Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                Text("Sin ventas hoy", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (esEmpleado) "Usá el Escáner para registrar una venta."
                    else "Usá el Escáner o el botón Vender.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val fmtHora = remember { SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
            ventas.take(5).forEach { venta ->
                val resumenItems = venta.items.take(3).joinToString(", ")
                    .let { if (venta.items.size > 3) "$it…" else it }
                ListItem(
                    headlineContent = {
                        Text("$${"%.0f".format(venta.total)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Verde)
                    },
                    supportingContent = if (resumenItems.isNotBlank()) {
                        { Text(resumenItems, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            if (ventas.size > 5) {
                TextButton(
                    onClick = onVerTodasVentas,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver todas (${ventas.size})", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        HorizontalDivider()
    }
}

internal fun LazyListScope.seccionAcuerdosVencidos(
    clientesVencidos: List<Cliente>,
    hoy: Date,
    fmtCompromiso: SimpleDateFormat,
    onRecordarTodos: () -> Unit,
    onRegistrarPago: (Cliente) -> Unit,
    onWhatsAppCliente: (Cliente) -> Unit,
    onAbrirDetalleCliente: (String) -> Unit
) {
    if (clientesVencidos.isEmpty()) return

    item {
        val clientesConFono = remember(clientesVencidos) {
            clientesVencidos.filter { it.telefono.isNotBlank() }
        }
        var expandido by remember { mutableStateOf(false) }
        val MAX = 3
        val visibles = if (expandido) clientesVencidos else clientesVencidos.take(MAX)
        val restantes = clientesVencidos.size - MAX

        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Acuerdos vencidos · ${clientesVencidos.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (clientesConFono.isNotEmpty()) {
                    TextButton(
                        onClick = onRecordarTodos,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Recordar a todos", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibles.forEach { cliente ->
                    val diasVencido = run {
                        val diff = hoy.time - cliente.fechaCompromiso!!.toDate().time
                        (diff / 86_400_000L).toInt().coerceAtLeast(1)
                    }
                    Surface(
                        onClick = { onAbrirDetalleCliente(cliente.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        cliente.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Prometió el ${fmtCompromiso.format(cliente.fechaCompromiso!!.toDate())} · hace $diasVencido ${if (diasVencido == 1) "día" else "días"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    "$${"%.0f".format(cliente.deudaTotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onRegistrarPago(cliente) },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Cobrar", style = MaterialTheme.typography.labelMedium)
                                }
                                if (cliente.telefono.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { onWhatsAppCliente(cliente) },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Avisar", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                if (!expandido && restantes > 0) {
                    TextButton(
                        onClick = { expandido = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver $restantes más", style = MaterialTheme.typography.labelMedium)
                    }
                } else if (expandido && clientesVencidos.size > MAX) {
                    TextButton(
                        onClick = { expandido = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver menos", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
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
                        val concepto = mov.producto.ifBlank { if (mov.tipo == "cargo") "Anotación" else "Cobro" }
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
