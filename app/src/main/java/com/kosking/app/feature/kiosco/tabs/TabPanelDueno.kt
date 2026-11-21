package com.kosking.app.feature.kiosco.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun TabPanelDueno(
    clientes: List<Cliente>,
    movimientosLocal: List<Movimiento>,
    totalDeuda: Double,
    clientesConDeuda: List<Cliente>,
    localData: Local?,
    disputados: List<Movimiento> = emptyList(),
    sobreLimite: List<Cliente> = emptyList(),
    onVerClientes: () -> Unit,
    onAbrirDetalleCliente: (String) -> Unit,
    onAbrirDetalleMovimiento: (String) -> Unit = {},
    onAnotar: () -> Unit,
    onRecordarTodos: () -> Unit,
    onCompartirResumen: () -> Unit = {},
    onWhatsAppCliente: (Cliente) -> Unit,
    onRegistrarPago: (Cliente) -> Unit = {},
    onAbrirProductos: () -> Unit = {},
    esEmpleado: Boolean = false,
    puedeVerDeuda: Boolean = true,
    onAbrirPerfil: () -> Unit = {},
    onAbrirNotificaciones: () -> Unit = {}
) {
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val movimientosHoy = remember(movimientosLocal) {
        val hoy = Calendar.getInstance()
        val hoyYear = hoy.get(Calendar.YEAR)
        val hoyDia  = hoy.get(Calendar.DAY_OF_YEAR)
        val c = Calendar.getInstance()
        movimientosLocal.filter { mov ->
            if (mov.anulado) return@filter false
            c.time = mov.fecha.toDate()
            c.get(Calendar.YEAR) == hoyYear && c.get(Calendar.DAY_OF_YEAR) == hoyDia
        }.sortedByDescending { it.fecha.toDate().time }
    }
    val (fiadoHoy, cobradoHoy) = remember(movimientosHoy) {
        var fiado = 0.0; var cobrado = 0.0
        movimientosHoy.forEach { if (it.tipo == "cargo") fiado += it.monto else cobrado += it.monto }
        fiado to cobrado
    }

    val diasSinPagar = remember(movimientosLocal, clientesConDeuda) {
        val hoyNorm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val ultPago = mutableMapOf<String, Long>()
        val primerCargo = mutableMapOf<String, Long>()
        movimientosLocal.forEach { mov ->
            if (mov.anulado) return@forEach
            val t = mov.fecha.toDate().time
            if (mov.tipo == "pago") {
                ultPago[mov.clienteId] = maxOf(ultPago[mov.clienteId] ?: 0L, t)
            } else {
                primerCargo[mov.clienteId] = minOf(primerCargo[mov.clienteId] ?: Long.MAX_VALUE, t)
            }
        }
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

    val visitasHoy = remember(movimientosHoy, clienteMap) {
        agruparVisitas(movimientosHoy, clienteMap, fmtHora)
    }
    var visitaDetalle by remember { mutableStateOf<VisitaResumen?>(null) }

    val hoy = remember { Date() }
    val fmtCompromiso = remember { SimpleDateFormat("d MMM", Locale.forLanguageTag("es-AR")) }
    val clientesVencidos = remember(clientesConDeuda) {
        clientesConDeuda
            .filter { it.fechaCompromiso != null && it.fechaCompromiso.toDate().before(hoy) }
            .sortedBy { it.fechaCompromiso!!.toDate() }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onAbrirPerfil) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Perfil",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onAbrirNotificaciones) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notificaciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            CardsResumenDueno(
                localData = localData,
                totalDeuda = totalDeuda,
                clientesConDeuda = clientesConDeuda,
                onAnotar = onAnotar,
                onAbrirProductos = onAbrirProductos,
                esEmpleado = esEmpleado,
                puedeVerDeuda = puedeVerDeuda
            )
        }

        if (esEmpleado) {
            item {
                // Accesos rápidos para empleado debajo de la card
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAnotar, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Anotar")
                        }
                        OutlinedButton(onClick = onAbrirProductos) {
                            Icon(Icons.Default.Sell, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Precios")
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        seccionHoy(
            clientes = clientes,
            movimientosHoy = movimientosHoy,
            fiadoHoy = fiadoHoy,
            cobradoHoy = cobradoHoy,
            visitasHoy = visitasHoy,
            onCompartirResumen = if (esEmpleado) ({}) else onCompartirResumen,
            onVerClientes = onVerClientes,
            onAbrirVisita = { visitaDetalle = it }
        )

        if (!esEmpleado) {
            seccionAcuerdosVencidos(
                clientesVencidos = clientesVencidos,
                hoy = hoy,
                fmtCompromiso = fmtCompromiso,
                onRecordarTodos = onRecordarTodos,
                onRegistrarPago = onRegistrarPago,
                onWhatsAppCliente = onWhatsAppCliente,
                onAbrirDetalleCliente = onAbrirDetalleCliente
            )

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
                                Text(
                                    "+ ${sobreLimite.size - 3} más",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    visitaDetalle?.let { visita ->
        DetalleVisitaSheet(visita = visita, onDismiss = { visitaDetalle = null })
    }

}

