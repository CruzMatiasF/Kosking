package com.kosking.app.ajustes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.kosking.app.modelo.Cliente
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Alerta(
    val icono: ImageVector,
    val titulo: String,
    val subtitulo: String,
    val colorTipo: AlertaTipo
)

private enum class AlertaTipo { ERROR, WARNING, INFO }

private fun generarAlertas(clientes: List<Cliente>): List<Alerta> {
    val ahora = Date()
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val alertas = mutableListOf<Alerta>()

    // Compromisos de pago vencidos
    clientes.filter { c ->
        c.fechaCompromiso != null && c.fechaCompromiso.toDate().before(ahora) && c.deudaTotal > 0
    }.forEach { c ->
        alertas.add(Alerta(
            icono = Icons.Default.Event,
            titulo = "${c.nombre} — compromiso vencido",
            subtitulo = "Se comprometió a pagar el ${fmt.format(c.fechaCompromiso!!.toDate())} · Debe \$${c.deudaTotal.toInt()}",
            colorTipo = AlertaTipo.ERROR
        ))
    }

    // Clientes con alerta de empleado activa
    clientes.filter { it.alertaEmpleado && it.habilitado }.forEach { c ->
        alertas.add(Alerta(
            icono = Icons.Default.Warning,
            titulo = "${c.nombre} — alerta activa",
            subtitulo = if (c.deudaTotal > 0) "No fiar · Debe \$${c.deudaTotal.toInt()}" else "No fiar",
            colorTipo = AlertaTipo.WARNING
        ))
    }

    // Clientes que alcanzaron o superaron su límite de crédito
    clientes.filter { it.limiteCredito > 0 && it.deudaTotal >= it.limiteCredito }.forEach { c ->
        alertas.add(Alerta(
            icono = Icons.Default.CreditCard,
            titulo = "${c.nombre} — límite alcanzado",
            subtitulo = "Debe \$${c.deudaTotal.toInt()} de \$${c.limiteCredito.toInt()} permitidos",
            colorTipo = AlertaTipo.WARNING
        ))
    }

    // Clientes con deuda sin compromiso ni límite (informativo)
    val yaListados = alertas.map { it.titulo.substringBefore(" —") }.toSet()
    clientes.filter { c ->
        c.deudaTotal > 0 &&
        c.nombre !in yaListados &&
        c.fechaCompromiso == null &&
        (c.limiteCredito == 0.0 || c.deudaTotal < c.limiteCredito) &&
        !c.alertaEmpleado
    }.sortedByDescending { it.deudaTotal }.take(10).forEach { c ->
        alertas.add(Alerta(
            icono = Icons.Default.AttachMoney,
            titulo = c.nombre,
            subtitulo = "Debe \$${c.deudaTotal.toInt()}",
            colorTipo = AlertaTipo.INFO
        ))
    }

    return alertas
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAlertas(
    clientes: List<Cliente>,
    onVolver: () -> Unit
) {
    val alertas = remember(clientes) { generarAlertas(clientes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Notificaciones")
                        if (alertas.isNotEmpty()) {
                            Badge { Text("${alertas.size}") }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (alertas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Todo en orden",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No hay alertas pendientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(alertas) { alerta ->
                    val (iconoTint, contenedorColor) = when (alerta.colorTipo) {
                        AlertaTipo.ERROR -> Pair(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                        AlertaTipo.WARNING -> Pair(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                        AlertaTipo.INFO -> Pair(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    ListItem(
                        headlineContent = {
                            Text(alerta.titulo, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        },
                        supportingContent = {
                            Text(alerta.subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingContent = {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = contenedorColor
                            ) {
                                Icon(
                                    alerta.icono, null,
                                    modifier = Modifier.padding(6.dp).size(20.dp),
                                    tint = iconoTint
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = contenedorColor)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
