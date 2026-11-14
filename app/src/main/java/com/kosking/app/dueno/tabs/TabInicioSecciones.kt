package com.kosking.app.dueno.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Date

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
