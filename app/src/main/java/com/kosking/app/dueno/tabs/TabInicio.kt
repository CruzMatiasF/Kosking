package com.kosking.app.dueno.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.GrupoClienteDashboard
import com.kosking.app.dueno.SeccionCard
import com.kosking.app.dueno.agruparPorCliente
import com.kosking.app.dueno.agruparVisitas
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import com.kosking.app.ui.theme.Verde
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun TabInicio(
    clientes: List<Cliente>,
    movimientosLocal: List<Movimiento>,
    totalDeuda: Double,
    clientesConDeuda: List<Cliente>,
    localData: Local?,
    disputados: List<Movimiento> = emptyList(),
    sobreLimite: List<Cliente> = emptyList(),
    onTabChange: (Int) -> Unit,
    onAbrirDetalleCliente: (String) -> Unit,
    onAbrirDetalleMovimiento: (String) -> Unit = {},
    onAnotar: () -> Unit,
    onRecordarTodos: () -> Unit,
    onCompartirResumen: () -> Unit = {},
    onWhatsAppCliente: (Cliente) -> Unit,
    onRegistrarPago: (Cliente) -> Unit = {},
    onVerMovimiento: (String) -> Unit = {}
) {
    var mostrarHistorial by remember { mutableStateOf(false) }
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fechaHoy = remember {
        SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR")).format(java.util.Date()).replaceFirstChar { it.uppercase() }
    }

    val movimientosHoy = remember(movimientosLocal) {
        val hoy = Calendar.getInstance()
        movimientosLocal.filter { mov ->
            if (mov.anulado) return@filter false
            val cal = Calendar.getInstance().apply { time = mov.fecha.toDate() }
            cal.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR)
        }.sortedByDescending { it.fecha.toDate().time }
    }
    val fiadoHoy = remember(movimientosHoy) { movimientosHoy.filter { it.tipo == "cargo" }.sumOf { it.monto } }
    val cobradoHoy = remember(movimientosHoy) { movimientosHoy.filter { it.tipo == "pago" }.sumOf { it.monto } }

    val diasSinPagar = remember(movimientosLocal, clientesConDeuda) {
        val hoyNorm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val ultPago = movimientosLocal
            .filter { it.tipo == "pago" && !it.anulado }
            .groupBy { it.clienteId }
            .mapValues { (_, ms) -> ms.maxOf { it.fecha.toDate().time } }
        val primerCargo = movimientosLocal
            .filter { it.tipo == "cargo" && !it.anulado }
            .groupBy { it.clienteId }
            .mapValues { (_, ms) -> ms.minOf { it.fecha.toDate().time } }
        clientesConDeuda.associate { c ->
            val ref = ultPago[c.id] ?: primerCargo[c.id]
            c.id to if (ref != null) {
                val diaCal = Calendar.getInstance().apply {
                    timeInMillis = ref
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                ((hoyNorm.timeInMillis - diaCal.timeInMillis) / 86_400_000L).toInt()
            } else null
        }
    }

    val gruposHoy = remember(movimientosHoy, clienteMap) {
        agruparPorCliente(agruparVisitas(movimientosHoy, clienteMap, fmtHora))
    }

    val clientesUrgentes = remember(clientesConDeuda, diasSinPagar) {
        clientesConDeuda
            .sortedByDescending { diasSinPagar[it.id] ?: 0 }
            .take(5)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Cabecera: deuda total + botón anotar
        item {
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
                        color = if (totalDeuda > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (clientesConDeuda.isNotEmpty()) {
                        Text(
                            "${clientesConDeuda.size} cliente${if (clientesConDeuda.size != 1) "s" else ""} deben",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (fiadoHoy > 0 || cobradoHoy > 0) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (fiadoHoy > 0) Text(
                                "↑ $${"%.0f".format(fiadoHoy)} fiado hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            if (cobradoHoy > 0) Text(
                                "↓ $${"%.0f".format(cobradoHoy)} cobrado hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = Verde
                            )
                        }
                    }
                }
                Button(
                    onClick = onAnotar,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Anotar", style = MaterialTheme.typography.labelLarge)
                }
            }
            HorizontalDivider()
        }

        // Alertas: disputas pendientes
        if (disputados.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp))
                            Text(
                                "${disputados.size} movimiento${if (disputados.size != 1) "s" else ""} en disputa",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        disputados.take(3).forEach { mov ->
                            val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: "Cliente"
                            val concepto = mov.producto.ifBlank { if (mov.tipo == "cargo") "Fiado" else "Pago" }
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

        // Alertas: clientes sobre límite
        if (sobreLimite.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CreditCard, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp))
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

        // Hoja del día
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hoy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(fechaHoy, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (gruposHoy.isNotEmpty()) {
                    TextButton(onClick = onCompartirResumen,
                        contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Compartir", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Empty state primer uso
        if (clientes.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Storefront, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("¡Bienvenido a Kosking!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Para empezar: agregá tus productos con precio en la pestaña Productos, después sumá tus clientes. Así anotar un fiado es solo tocar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onTabChange(2) }) {
                            Icon(Icons.Default.Storefront, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Productos")
                        }
                        Button(onClick = { onTabChange(1) }) {
                            Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clientes")
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        if (gruposHoy.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text("Todavía nada hoy", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
        } else {
            val gruposMostrados = gruposHoy.take(5)
            val gruposRestantes = gruposHoy.size - gruposMostrados.size
            items(gruposMostrados, key = { "hoy_${it.clienteId}" }) {
                GrupoClienteDashboard(it)
            }
            if (gruposRestantes > 0) {
                item {
                    Text(
                        "y $gruposRestantes cliente${if (gruposRestantes != 1) "s" else ""} más hoy",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            item { HorizontalDivider() }
        }

        // Lista de clientes con deuda
        if (clientesUrgentes.isNotEmpty()) {
            item {
                val clientesConFono = remember(clientesUrgentes) {
                    clientesUrgentes.filter { it.telefono.isNotBlank() }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Deudas urgentes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                val dias = diasSinPagar[cliente.id]
                ListItem(
                    headlineContent = { Text(cliente.nombre, fontWeight = FontWeight.Medium) },
                    supportingContent = if (cliente.notaInterna.isNotBlank() || dias == null || dias >= 7) ({
                        Column {
                            if (cliente.notaInterna.isNotBlank()) {
                                Text(
                                    cliente.notaInterna,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (dias == null) {
                                Text(
                                    "Sin pagar hace más de 90 días",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (dias >= 7) {
                                val colorDias = if (dias >= 30) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                Text(
                                    if (dias >= 30) "Sin pagar hace $dias días" else "Hace $dias días",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorDias
                                )
                            }
                        }
                    }) else null,
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$${"%.0f".format(cliente.deudaTotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
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

        item {
            TextButton(
                onClick = { mostrarHistorial = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.History, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ver historial completo", style = MaterialTheme.typography.labelMedium)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (mostrarHistorial) {
        HistorialSheet(
            movimientos = movimientosLocal,
            clienteMap = clienteMap,
            onVerMovimiento = onVerMovimiento,
            onDismiss = { mostrarHistorial = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorialSheet(
    movimientos: List<Movimiento>,
    clienteMap: Map<String, Cliente>,
    onVerMovimiento: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("EEEE d MMM", Locale.forLanguageTag("es-AR")) }
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val agrupado = remember(movimientos) {
        val hoyNorm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        movimientos
            .filter { !it.anulado }
            .sortedByDescending { it.fecha.toDate().time }
            .groupBy {
                val diaCal = Calendar.getInstance().apply {
                    time = it.fecha.toDate()
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                ((hoyNorm.timeInMillis - diaCal.timeInMillis) / 86_400_000L).toInt()
            }
            .entries
            .sortedBy { it.key }
            .take(14)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (agrupado.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin movimientos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)
            ) {
                item {
                    Text(
                        "Historial",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                agrupado.forEach { (diasAtras, movs) ->
                    val encabezado = when (diasAtras) {
                        0 -> "Hoy"
                        1 -> "Ayer"
                        else -> fmt.format(movs.first().fecha.toDate()).replaceFirstChar { it.uppercase() }
                    }
                    item(key = "header_$diasAtras") {
                        Text(
                            encabezado,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(movs, key = { it.id }) { mov ->
                        val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: "—"
                        val esCargo = mov.tipo == "cargo"
                        ListItem(
                            headlineContent = { Text(nombreCliente, style = MaterialTheme.typography.bodyMedium) },
                            supportingContent = {
                                Text(
                                    mov.producto.ifBlank { if (esCargo) "Fiado" else "Pago" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${if (esCargo) "+" else "-"}${"$"}${"%.0f".format(mov.monto)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (esCargo) MaterialTheme.colorScheme.error else Verde
                                    )
                                    Text(fmtHora.format(mov.fecha.toDate()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.clickable { onVerMovimiento(mov.id) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}
