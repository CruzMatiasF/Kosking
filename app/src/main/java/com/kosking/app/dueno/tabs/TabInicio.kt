package com.kosking.app.dueno.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
    onVerMovimiento: (String) -> Unit = {},
    onAbrirProductos: () -> Unit = {}
) {
    var mostrarHistorial by remember { mutableStateOf(false) }
    val duenoUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
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
    val clientesUrgentes = clientesVencidos

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

        // Cabecera: deuda total + botón anotar
        item {
            if (localData != null && localData.nombre.isNotBlank()) {
                val diaKey = remember {
                    val dias = listOf("domingo","lunes","martes","miercoles","jueves","viernes","sabado")
                    dias[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
                }
                val horarioHoy = localData.horarios[diaKey]

                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            localData.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (horarioHoy != null) {
                            val (badgeBg, badgeText, badgeLabel) = if (horarioHoy.abierto)
                                Triple(Verde.copy(alpha = 0.12f), Verde, "Abierto")
                            else
                                Triple(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    "Cerrado"
                                )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (localData.direccion.isNotBlank()) {
                        Text(
                            localData.direccion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    if (horarioHoy != null && horarioHoy.abierto) {
                        Text(
                            "${horarioHoy.horaApertura} – ${horarioHoy.horaCierre}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

        // Resumen semanal compacto
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
                    Text(
                        "Esta semana",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Anotado", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${"%.0f".format(semanaActual.fiado)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cobrado", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${"%.0f".format(semanaActual.cobrado)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Verde)
                        }
                    }
                }
            }
        }

        // Lista compacta de últimos movimientos (Hoy)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hoy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fiadoHoy > 0) {
                        Text("-$${"%.0f".format(fiadoHoy)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    if (cobradoHoy > 0) {
                        Text("+$${"%.0f".format(cobradoHoy)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Verde)
                    }
                    if (fiadoHoy > 0 || cobradoHoy > 0) {
                        IconButton(onClick = onCompartirResumen, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Compartir",
                                Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(
                        "Sumá tu primer cliente y empezá a anotar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(onClick = { onTabChange(1) }) {
                        Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar cliente")
                    }
                }
            } else if (movimientosHoy.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text("Todavía nada hoy", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val visitasHoyPanel = agruparVisitas(movimientosHoy, clienteMap, fmtHora).take(5)
                visitasHoyPanel.forEach { visita ->
                    FilaVisitaMP(visita, onClick = { visitaDetalle = visita })
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { mostrarHistorial = true },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Ver historial completo", style = MaterialTheme.typography.labelSmall)
                }
            }
            HorizontalDivider()
        }

        // Lista de clientes con deuda (Acuerdos vencidos)
        seccionAcuerdosVencidos(
            clientesUrgentes = clientesUrgentes,
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

        // Sección compacta de equipo
        if (empleados.isNotEmpty()) {
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
            onVerMovimiento = onVerMovimiento,
            onDismiss = { mostrarHistorial = false }
        )
    }
}

