package com.kosking.app.dueno.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
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
    empleados: List<Empleado> = emptyList(),
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
    onAbrirProductos: () -> Unit = {}
) {
    var mostrarHistorial by remember { mutableStateOf(false) }
    val duenoUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
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

    val hoy = remember { java.util.Date() }
    val fmtCompromiso = remember { SimpleDateFormat("d MMM", Locale.forLanguageTag("es-AR")) }
    val clientesVencidos = remember(clientesConDeuda) {
        clientesConDeuda
            .filter { it.fechaCompromiso != null && it.fechaCompromiso.toDate().before(hoy) }
            .sortedBy { it.fechaCompromiso!!.toDate() }
    }

    // Caja del día por persona (para sección equipo)
    data class StatPersona(val uid: String, val nombre: String, val anoto: Int, val cobro: Double)
    val inicioDiaEquipo = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val statsHoyEquipo = remember(movimientosLocal, duenoUid, inicioDiaEquipo) {
        if (empleados.isEmpty()) return@remember emptyList()
        val movsHoyEq = movimientosLocal.filter { !it.anulado && it.fecha.toDate().time >= inicioDiaEquipo }
        val map = mutableMapOf<String, StatPersona>()
        movsHoyEq.forEach { mov ->
            val uid = mov.anotadoPorUid.ifBlank { duenoUid }
            val nombre = if (uid == duenoUid) "Vos"
                         else mov.anotadoPorNombre.ifBlank { "Empleado" }
            val actual = map[uid] ?: StatPersona(uid, nombre, 0, 0.0)
            map[uid] = actual.copy(
                anoto = actual.anoto + 1,
                cobro = actual.cobro + if (mov.tipo == "pago") mov.monto else 0.0
            )
        }
        map.values.sortedByDescending { if (it.uid == duenoUid) Int.MAX_VALUE else it.anoto }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        seccionCabecera(localData = localData, totalDeuda = totalDeuda, clientesConDeuda = clientesConDeuda, onAnotar = onAnotar)

        seccionResumenSemanal(fiadoSemana = semanaActual.fiado, cobradoSemana = semanaActual.cobrado)

        seccionHoy(
            clientes = clientes,
            movimientosHoy = movimientosHoy,
            fiadoHoy = fiadoHoy,
            cobradoHoy = cobradoHoy,
            visitasHoy = visitasHoy,
            onCompartirResumen = onCompartirResumen,
            onTabChange = onTabChange,
            onAbrirVisita = { visitaDetalle = it },
            onVerHistorial = { mostrarHistorial = true }
        )

        // Lista de clientes con deuda (Acuerdos vencidos)
        seccionAcuerdosVencidos(
            clientesUrgentes = clientesVencidos,
            clientesVencidos = clientesVencidos,
            hoy = hoy,
            fmtCompromiso = fmtCompromiso,
            onRecordarTodos = onRecordarTodos,
            onRegistrarPago = onRegistrarPago,
            onWhatsAppCliente = onWhatsAppCliente,
            onAbrirDetalleCliente = onAbrirDetalleCliente
        )

        // Accesos rápidos: Clientes y Productos
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onTabChange(1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.People, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clientes", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onAbrirProductos,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Storefront, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Productos", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Alertas: disputas y clientes sobre límite
        alertasCards(
            disputados = disputados,
            clienteMap = clienteMap,
            sobreLimite = sobreLimite,
            onAbrirDetalleMovimiento = onAbrirDetalleMovimiento,
            onAbrirDetalleCliente = onAbrirDetalleCliente
        )

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

