package com.kosking.app.dueno.tabs

import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.DialogInvitarEmpleado
import com.kosking.app.ui.theme.Verde

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Movimiento
import com.kosking.app.util.WhatsAppUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun TabEquipo(
    empleados: List<Empleado>,
    movimientos: List<Movimiento>,
    clientes: List<com.kosking.app.modelo.Cliente> = emptyList(),
    onEliminarEmpleado: (String) -> Unit,
    vm: DuenoViewModel
) {
    val context = LocalContext.current
    val vmCargando by vm.cargando.collectAsState()
    val vmError by vm.error.collectAsState()
    val nombreLocal by vm.nombreLocal.collectAsState()
    var mostrarDialogInvitar by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<String?>(null) }
    var empleadoAEliminar by remember { mutableStateOf<Empleado?>(null) }
    var mostrarResumen by remember { mutableStateOf(false) }

    val duenoUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    val inicioDia = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val movsHoy = remember(movimientos, inicioDia) {
        movimientos.filter { !it.anulado && it.fecha.toDate().time >= inicioDia }
    }

    data class StatPersona(val uid: String, val nombre: String, val anoto: Int, val cobro: Double)

    val statsHoy = remember(movsHoy, duenoUid) {
        val map = mutableMapOf<String, StatPersona>()
        movsHoy.forEach { mov ->
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

    val totalCobradoHoy = remember(statsHoy) { statsHoy.sumOf { it.cobro } }

    val fmtFecha = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val ultimoMovPorUid = remember(movimientos) {
        movimientos
            .filter { !it.anulado }
            .groupBy { it.anotadoPorUid }
            .mapValues { it.value.first() }
    }

    val disputasPorUid = remember(movimientos) {
        movimientos.filter { it.disputado && !it.anulado }
            .groupBy { it.anotadoPorUid }
            .mapValues { it.value.size }
    }

    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (statsHoy.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Caja de hoy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Cobrado $${"%.0f".format(totalCobradoHoy)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Verde,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = { mostrarResumen = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Ver período", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quién",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                            Text("Anotó",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp))
                            Text("Cobró",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(72.dp))
                        }
                        HorizontalDivider()
                        statsHoy.forEach { stat ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stat.nombre,
                                    fontWeight = if (stat.uid == duenoUid) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f))
                                Text("${stat.anoto}",
                                    modifier = Modifier.width(48.dp),
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "$${"%.0f".format(stat.cobro)}",
                                    modifier = Modifier.width(72.dp),
                                    color = if (stat.cobro > 0) Verde
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (stat.cobro > 0) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EMPLEADOS · ${empleados.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(
                    onClick = { mostrarDialogInvitar = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sumar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null,
                        Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        "Los empleados pueden anotar fiados y registrar pagos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (empleados.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Groups, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("Sin empleados todavía",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tocá + Sumar para invitar a alguien",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        } else {
            items(empleados, key = { it.id }) { emp ->
                val ultimoMov = ultimoMovPorUid[emp.uid]
                val reclamos = disputasPorUid[emp.uid] ?: 0
                val esActivoHoy = ultimoMov != null && ultimoMov.fecha.toDate().time >= inicioDia

                val avatarColors = listOf(
                    MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
                )
                val (avatarBg, avatarFg) = avatarColors[Math.floorMod(emp.nombre.hashCode(), 3)]

                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(
                            start = 12.dp, end = 4.dp, top = 10.dp,
                            bottom = if (esActivoHoy || reclamos > 0) 8.dp else 10.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(avatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    emp.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = avatarFg
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(emp.nombre,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyLarge)
                                if (ultimoMov != null) {
                                    val accion = if (ultimoMov.tipo == "cargo") "Anotó fiado" else "Cobró"
                                    val esHoy = ultimoMov.fecha.toDate().time >= inicioDia
                                    val fechaStr = if (esHoy) "hoy ${fmtHora.format(ultimoMov.fecha.toDate())}"
                                                  else fmtFecha.format(ultimoMov.fecha.toDate())
                                    Text("Último: $fechaStr · $accion",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { empleadoAEliminar = emp }) {
                                Icon(Icons.Default.Delete, "Desvincular",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (esActivoHoy || reclamos > 0) {
                            Row(
                                Modifier.padding(start = 56.dp, top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (esActivoHoy) {
                                    Surface(shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("Activo hoy",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                if (reclamos > 0) {
                                    Surface(shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.errorContainer) {
                                        Text("$reclamos reclamo${if (reclamos != 1) "s" else ""}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogInvitar) {
        val localContext = LocalContext.current
        DialogInvitarEmpleado(
            cargando = vmCargando,
            error = vmError,
            onGenerar = { whatsapp, email ->
                vm.generarCodigoEmpleado { codigo ->
                    mostrarDialogInvitar = false
                    val local = nombreLocal.ifBlank { "Kosking" }
                    val mensaje = " Te invitan a unirte como empleado a *$local* en Kosking.\n\nSeguí estos pasos:\n1. Descargá la app Kosking desde Play Store\n2. Registrate o iniciá sesión\n3. Tocá \"Soy empleado\" e ingresá el código:\n\n*$codigo*\n\nEl código es de un solo uso."
                    if (whatsapp.isNotBlank()) {
                        val numero = whatsapp.replace(Regex("[^0-9]"), "")
                        try {
                            localContext.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
                            })
                        } catch (_: Exception) {}
                    } else if (email.isNotBlank()) {
                        try {
                            localContext.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                                putExtra(Intent.EXTRA_SUBJECT, "Tu código de empleado - Kosking")
                                putExtra(Intent.EXTRA_TEXT, mensaje)
                            })
                        } catch (_: Exception) {}
                    } else {
                        codigoGenerado = codigo
                    }
                }
            },
            onDismiss = { mostrarDialogInvitar = false }
        )
    }

    empleadoAEliminar?.let { emp ->
        AlertDialog(
            onDismissRequest = { empleadoAEliminar = null },
            title = { Text("Eliminar empleado") },
            text = { Text("¿Eliminar a ${emp.nombre}? Se va a desconectar del local.") },
            confirmButton = {
                TextButton(onClick = { onEliminarEmpleado(emp.id); empleadoAEliminar = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { empleadoAEliminar = null }) { Text("Cancelar") } }
        )
    }

    codigoGenerado?.let { codigo ->
        AlertDialog(
            onDismissRequest = { codigoGenerado = null },
            title = { Text("Código de invitación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Compartí este código con el empleado:")
                    Text(codigo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("El empleado lo ingresa en la app para conectarse al local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { codigoGenerado = null }) { Text("Listo") }
            }
        )
    }

    if (mostrarResumen) {
        ResumenPeriodoSheet(
            movimientos = movimientos,
            clientes = clientes,
            duenoUid = duenoUid,
            nombreLocal = nombreLocal,
            context = context,
            onDismiss = { mostrarResumen = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumenPeriodoSheet(
    movimientos: List<Movimiento>,
    clientes: List<com.kosking.app.modelo.Cliente>,
    duenoUid: String,
    nombreLocal: String,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    data class Periodo(val key: String, val label: String, val inicio: Long)

    val ahora = System.currentTimeMillis()
    val periodos = remember {
        val calHoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val calSemana = calHoy.clone() as Calendar
        calSemana.set(Calendar.DAY_OF_WEEK, calSemana.firstDayOfWeek)
        val calMes = calHoy.clone() as Calendar
        calMes.set(Calendar.DAY_OF_MONTH, 1)
        val calMesAnt = calMes.clone() as Calendar
        calMesAnt.add(Calendar.MONTH, -1)
        listOf(
            Periodo("hoy", "Hoy", calHoy.timeInMillis),
            Periodo("semana", "Semana", calSemana.timeInMillis),
            Periodo("mes", "Este mes", calMes.timeInMillis),
            Periodo("mes_ant", "Mes ant.", calMesAnt.timeInMillis)
        )
    }

    var periodoKey by remember { mutableStateOf("mes") }
    val periodo = periodos.first { it.key == periodoKey }

    val finPeriodo = remember(periodoKey) {
        if (periodoKey == "mes_ant") {
            val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            c.timeInMillis - 1
        } else ahora
    }

    val movsFiltrados = remember(movimientos, periodoKey) {
        movimientos.filter {
            !it.anulado &&
            it.fecha.toDate().time >= periodo.inicio &&
            it.fecha.toDate().time <= finPeriodo
        }
    }

    val fiadoTotal = remember(movsFiltrados) { movsFiltrados.filter { it.tipo == "cargo" }.sumOf { it.monto } }
    val cobradoTotal = remember(movsFiltrados) { movsFiltrados.filter { it.tipo == "pago" }.sumOf { it.monto } }
    val cantFiados = remember(movsFiltrados) { movsFiltrados.count { it.tipo == "cargo" } }
    val cantPagos = remember(movsFiltrados) { movsFiltrados.count { it.tipo == "pago" } }
    val saldoNeto = cobradoTotal - fiadoTotal

    val topClientes = remember(movsFiltrados) {
        movsFiltrados
            .filter { it.tipo == "cargo" }
            .groupBy { it.clienteId }
            .mapValues { (_, ms) -> ms.sumOf { it.monto } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    val statsPorPersona = remember(movsFiltrados) {
        data class StatP(val nombre: String, val anoto: Int, val cobro: Double)
        val map = mutableMapOf<String, StatP>()
        movsFiltrados.forEach { mov ->
            val uid = mov.anotadoPorUid.ifBlank { duenoUid }
            val nombre = if (uid == duenoUid) "Vos" else mov.anotadoPorNombre.ifBlank { "Empleado" }
            val a = map[uid] ?: StatP(nombre, 0, 0.0)
            map[uid] = a.copy(anoto = a.anoto + 1, cobro = a.cobro + if (mov.tipo == "pago") mov.monto else 0.0)
        }
        map.values.sortedByDescending { if (it.nombre == "Vos") Int.MAX_VALUE else it.anoto }
    }

    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título + selector de período
            item {
                Text("Resumen del período",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periodos.forEach { p ->
                        FilterChip(
                            selected = periodoKey == p.key,
                            onClick = { periodoKey = p.key },
                            label = { Text(p.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Cards de stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatResumenCard("Fiado", "$${"%.0f".format(fiadoTotal)}", "$cantFiados op.", isError = true, modifier = Modifier.weight(1f))
                    StatResumenCard("Cobrado", "$${"%.0f".format(cobradoTotal)}", "$cantPagos pagos", isSuccess = true, modifier = Modifier.weight(1f))
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (saldoNeto >= 0) MaterialTheme.colorScheme.primaryContainer
                                         else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Saldo neto",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (saldoNeto >= 0) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            "${if (saldoNeto >= 0) "+" else ""}\$${"%.0f".format(saldoNeto)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (saldoNeto >= 0) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            if (saldoNeto >= 0) "Cobraste más de lo que fiaste" else "Fiaste más de lo que cobraste",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (saldoNeto >= 0) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Por persona
            if (statsPorPersona.isNotEmpty()) {
                item {
                    Text("Por persona",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                Text("Quién", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text("Anotó", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
                                Text("Cobró", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(76.dp))
                            }
                            HorizontalDivider()
                            statsPorPersona.forEach { stat ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stat.nombre,
                                        fontWeight = if (stat.nombre == "Vos") FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text("${stat.anoto}", modifier = Modifier.width(52.dp),
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "$${"%.0f".format(stat.cobro)}",
                                        modifier = Modifier.width(76.dp),
                                        color = if (stat.cobro > 0) Verde else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (stat.cobro > 0) FontWeight.SemiBold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top clientes
            if (topClientes.isNotEmpty()) {
                item {
                    Text("Top clientes (fiado generado)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            topClientes.forEachIndexed { i, (clienteId, monto) ->
                                val nombreCliente = clienteMap[clienteId]?.nombre ?: "—"
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${i + 1}.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(20.dp))
                                        Text(nombreCliente,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium)
                                    }
                                    Text("$${"%.0f".format(monto)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error)
                                }
                                if (i < topClientes.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Sin movimientos
            if (movsFiltrados.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Sin movimientos en este período",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }

            // Botón compartir
            item {
                Button(
                    onClick = {
                        WhatsAppUtil.compartir(
                            context,
                            WhatsAppUtil.mensajeResumenPeriodo(
                                nombreLocal = nombreLocal.ifBlank { "Mi local" },
                                labelPeriodo = periodo.label.lowercase(),
                                fiadoTotal = fiadoTotal,
                                cobradoTotal = cobradoTotal,
                                cantFiados = cantFiados,
                                cantPagos = cantPagos,
                                topClientes = topClientes.map { (clienteId, monto) ->
                                    (clienteMap[clienteId]?.nombre ?: "—") to monto
                                }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = movsFiltrados.isNotEmpty()
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir resumen")
                }
            }
        }
    }
}

@Composable
private fun StatResumenCard(
    titulo: String,
    valor: String,
    subtitulo: String,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isSuccess -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = contentColor)
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = contentColor)
            Text(subtitulo, style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
    }
}
