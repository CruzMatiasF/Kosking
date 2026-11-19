package com.kosking.app.dueno.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Calendar
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
    puedeVerDeuda: Boolean = true
) {
    var mostrarHistorial by remember { mutableStateOf(false) }
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
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

    val visitasHoy = remember(movimientosHoy, clienteMap) {
        agruparVisitas(movimientosHoy, clienteMap, fmtHora)
    }
    var visitaDetalle by remember { mutableStateOf<VisitaResumen?>(null) }

    data class ResumenSemana(val fiado: Double, val cobrado: Double)
    val (semanaActual, semanaAnterior) = remember(movimientosLocal) {
        val ahora = Calendar.getInstance()
        val inicioSemana = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val inicioSemanaAnterior = (inicioSemana.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, -1) }
        val actual = ResumenSemana(
            fiado = movimientosLocal.filter { !it.anulado && it.tipo == "cargo" && it.fecha.toDate().time >= inicioSemana.timeInMillis }.sumOf { it.monto },
            cobrado = movimientosLocal.filter { !it.anulado && it.tipo == "pago" && it.fecha.toDate().time >= inicioSemana.timeInMillis }.sumOf { it.monto }
        )
        val anterior = ResumenSemana(
            fiado = movimientosLocal.filter { !it.anulado && it.tipo == "cargo" && it.fecha.toDate().time >= inicioSemanaAnterior.timeInMillis && it.fecha.toDate().time < inicioSemana.timeInMillis }.sumOf { it.monto },
            cobrado = movimientosLocal.filter { !it.anulado && it.tipo == "pago" && it.fecha.toDate().time >= inicioSemanaAnterior.timeInMillis && it.fecha.toDate().time < inicioSemana.timeInMillis }.sumOf { it.monto }
        )
        Pair(actual, anterior)
    }

    data class ResumenMes(val fiado: Double, val cobrado: Double)
    fun movimientosDeMes(anio: Int, mes: Int): ResumenMes {
        var fiado = 0.0; var cobrado = 0.0
        movimientosLocal.filter { !it.anulado }.forEach { mov ->
            val cal = Calendar.getInstance().also { c -> c.time = mov.fecha.toDate() }
            if (cal.get(Calendar.YEAR) == anio && cal.get(Calendar.MONTH) == mes) {
                if (mov.tipo == "cargo") fiado += mov.monto else cobrado += mov.monto
            }
        }
        return ResumenMes(fiado, cobrado)
    }
    val resumenMesActual = remember(movimientosLocal) {
        val cal = Calendar.getInstance()
        movimientosDeMes(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }
    val resumenMesAnterior = remember(movimientosLocal) {
        val cal = Calendar.getInstance().also { it.add(Calendar.MONTH, -1) }
        movimientosDeMes(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    val hoy = remember { java.util.Date() }
    val fmtCompromiso = remember { SimpleDateFormat("d MMM", Locale.forLanguageTag("es-AR")) }
    val clientesVencidos = remember(clientesConDeuda) {
        clientesConDeuda
            .filter { it.fechaCompromiso != null && it.fechaCompromiso.toDate().before(hoy) }
            .sortedBy { it.fechaCompromiso!!.toDate() }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            CardsResumenDueno(
                localData = localData,
                totalDeuda = totalDeuda,
                clientesConDeuda = clientesConDeuda,
                fiadoSemana = semanaActual.fiado,
                cobradoSemana = semanaActual.cobrado,
                fiadoSemanaAnterior = semanaAnterior.fiado,
                cobradoSemanaAnterior = semanaAnterior.cobrado,
                fiadoMes = resumenMesActual.fiado,
                cobradoMes = resumenMesActual.cobrado,
                fiadoMesAnterior = resumenMesAnterior.fiado,
                cobradoMesAnterior = resumenMesAnterior.cobrado,
                onAnotar = onAnotar,
                onVerClientes = onVerClientes,
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
            onAbrirVisita = { visitaDetalle = it },
            onVerHistorial = { mostrarHistorial = true }
        )

        if (!esEmpleado) {
            // Lista de clientes con deuda (Acuerdos vencidos)
            seccionAcuerdosVencidos(
                clientesVencidos = clientesVencidos,
                hoy = hoy,
                fmtCompromiso = fmtCompromiso,
                onRecordarTodos = onRecordarTodos,
                onRegistrarPago = onRegistrarPago,
                onWhatsAppCliente = onWhatsAppCliente,
                onAbrirDetalleCliente = onAbrirDetalleCliente
            )

            // Alertas: disputas y clientes sobre límite
            alertasCards(
                disputados = disputados,
                clienteMap = clienteMap,
                sobreLimite = sobreLimite,
                onAbrirDetalleMovimiento = onAbrirDetalleMovimiento,
                onAbrirDetalleCliente = onAbrirDetalleCliente
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    visitaDetalle?.let { visita ->
        DetalleVisitaSheet(visita = visita, onDismiss = { visitaDetalle = null })
    }

    if (mostrarHistorial) {
        HistorialSheet(
            movimientos = movimientosLocal,
            clienteMap = clienteMap,
            onVerMovimiento = onAbrirDetalleMovimiento,
            onDismiss = { mostrarHistorial = false }
        )
    }
}

