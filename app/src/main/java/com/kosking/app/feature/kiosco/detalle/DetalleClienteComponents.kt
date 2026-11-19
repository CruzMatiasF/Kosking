package com.kosking.app.dueno.detalle

import com.kosking.app.dueno.DuenoViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosking.app.modelo.Cliente
import com.kosking.app.ui.theme.Verde
import com.kosking.app.ui.theme.VerdeClaro
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun HeroCliente(cliente: Cliente) {
    val sinDeuda = cliente.deudaTotal <= 0
    val bgColor = if (sinDeuda) MaterialTheme.colorScheme.tertiaryContainer
                  else MaterialTheme.colorScheme.errorContainer
    val onBgColor = if (sinDeuda) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
    ) {
        Text(
            "Deuda total",
            style = MaterialTheme.typography.labelSmall,
            color = onBgColor.copy(alpha = 0.65f),
            letterSpacing = 0.8.sp
        )
        Text(
            "$${"%.0f".format(cliente.deudaTotal)}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = onBgColor
        )

        if (cliente.limiteCredito > 0) {
            Spacer(Modifier.height(8.dp))
            val progreso = (cliente.deudaTotal / cliente.limiteCredito).coerceIn(0.0, 1.0).toFloat()
            val superado = cliente.deudaTotal > cliente.limiteCredito
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = if (superado) MaterialTheme.colorScheme.error else Verde,
                trackColor = onBgColor.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (superado) "Superó el límite" else "${"%.0f".format(progreso * 100)}% usado",
                    style = MaterialTheme.typography.labelSmall,
                    color = onBgColor.copy(alpha = 0.65f)
                )
                Text(
                    "Límite $${"%.0f".format(cliente.limiteCredito)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onBgColor.copy(alpha = 0.65f)
                )
            }
        }

        if (cliente.domicilio.isNotBlank() || cliente.telefono.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cliente.domicilio.isNotBlank()) InfoChip(Icons.Default.Home, cliente.domicilio, onBgColor)
                if (cliente.telefono.isNotBlank()) InfoChip(Icons.Default.Phone, cliente.telefono, onBgColor)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, texto: String, onBgColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .background(
                color = onBgColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, Modifier.size(13.dp), tint = onBgColor.copy(alpha = 0.7f))
        Text(texto, style = MaterialTheme.typography.labelSmall, color = onBgColor.copy(alpha = 0.9f))
    }
}

@Composable
internal fun ConfigTiles(cliente: Cliente, vm: DuenoViewModel) {
    var mostrarDialogoLimite by remember { mutableStateOf(false) }
    var mostrarDialogoNota by remember { mutableStateOf(false) }
    var mostrarDialogoCompromiso by remember { mutableStateOf(false) }

    if (mostrarDialogoNota) {
        DialogEditarNota(
            notaActual = cliente.notaInterna,
            alertaActual = cliente.alertaEmpleado,
            onGuardar = { nuevaNota, nuevaAlerta ->
                vm.actualizarCliente(cliente.copy(notaInterna = nuevaNota, alertaEmpleado = nuevaAlerta))
                mostrarDialogoNota = false
            },
            onDismiss = { mostrarDialogoNota = false }
        )
    }

    if (mostrarDialogoCompromiso) {
        DialogFechaCompromiso(
            fechaActual = cliente.fechaCompromiso,
            onGuardar = { ts ->
                vm.setFechaCompromiso(cliente.id, ts)
                mostrarDialogoCompromiso = false
            },
            onDismiss = { mostrarDialogoCompromiso = false }
        )
    }

    if (mostrarDialogoLimite) {
        DialogEditarLimite(
            limiteActual = cliente.limiteCredito,
            onGuardar = { nuevoLimite ->
                vm.actualizarCliente(cliente.copy(limiteCredito = nuevoLimite))
                mostrarDialogoLimite = false
            },
            onDismiss = { mostrarDialogoLimite = false }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { mostrarDialogoLimite = true },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CreditCard, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Límite de crédito", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (cliente.limiteCredito > 0) "$${"%.0f".format(cliente.limiteCredito)}" else "Sin límite",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (cliente.limiteCredito > 0) {
                    val disponible = (cliente.limiteCredito - cliente.deudaTotal).coerceAtLeast(0.0)
                    Text(
                        "$${"%.0f".format(disponible)} disp.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Verde,
                        modifier = Modifier.background(VerdeClaro, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Icon(Icons.Default.Edit, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.clickable { mostrarDialogoNota = true },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = if (cliente.notaInterna.isNotBlank()) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit, null, Modifier.size(18.dp),
                        tint = if (cliente.notaInterna.isNotBlank()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Nota interna", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (cliente.alertaEmpleado) {
                            Icon(Icons.Default.Warning, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Text(
                        if (cliente.notaInterna.isNotBlank()) cliente.notaInterna else "Sin nota",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (cliente.notaInterna.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.Edit, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(8.dp))
        val fmtCompromiso = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR")) }
        val compromisoVencido = cliente.fechaCompromiso?.let { it.toDate().before(Date()) } ?: false
        Card(
            modifier = Modifier.clickable { mostrarDialogoCompromiso = true },
            shape = RoundedCornerShape(12.dp),
            colors = if (compromisoVencido) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                     else CardDefaults.cardColors()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(34.dp).background(
                        color = when {
                            compromisoVencido -> MaterialTheme.colorScheme.error
                            cliente.fechaCompromiso != null -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(10.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp),
                        tint = when {
                            compromisoVencido -> MaterialTheme.colorScheme.onError
                            cliente.fechaCompromiso != null -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Acuerdo de pago", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                        color = if (compromisoVencido) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
                    Text(
                        when {
                            compromisoVencido -> "Venció: ${fmtCompromiso.format(cliente.fechaCompromiso!!.toDate()).replaceFirstChar { it.uppercase() }}"
                            cliente.fechaCompromiso != null -> "Paga el ${fmtCompromiso.format(cliente.fechaCompromiso.toDate()).replaceFirstChar { it.uppercase() }}"
                            else -> "Sin fecha acordada"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            compromisoVencido -> MaterialTheme.colorScheme.onErrorContainer
                            cliente.fechaCompromiso != null -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (cliente.fechaCompromiso != null) {
                    IconButton(onClick = { vm.setFechaCompromiso(cliente.id, null) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Quitar fecha", Modifier.size(14.dp),
                            tint = if (compromisoVencido) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = if (!cliente.habilitado) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Block, null, Modifier.size(18.dp),
                        tint = if (!cliente.habilitado) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bloquear cliente", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        if (!cliente.habilitado) "Cliente bloqueado"
                        else "El cliente puede seguir fiando",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!cliente.habilitado) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = !cliente.habilitado,
                    onCheckedChange = { bloquear -> vm.actualizarCliente(cliente.copy(habilitado = !bloquear)) }
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}
