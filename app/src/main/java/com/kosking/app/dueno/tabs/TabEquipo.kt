package com.kosking.app.dueno.tabs

import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.DialogInvitarEmpleado
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Movimiento
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
    val vmCargando by vm.cargando.collectAsState()
    val vmError by vm.error.collectAsState()
    val nombreLocal by vm.nombreLocal.collectAsState()
    var mostrarDialogInvitar by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<String?>(null) }
    var empleadoAEliminar by remember { mutableStateOf<Empleado?>(null) }
    var mostrarResumen by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }

    val empleadosFiltrados = remember(empleados, busqueda) {
        if (busqueda.isBlank()) empleados
        else empleados.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

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
                CardCajaHoy(
                    statsHoy = statsHoy,
                    totalCobradoHoy = totalCobradoHoy,
                    duenoUid = duenoUid,
                    onVerPeriodo = { mostrarResumen = true }
                )
            }
        }

        if (empleados.size > 3) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            Box {
                                if (busqueda.isEmpty()) Text("Buscar empleado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                        }
                    )
                    if (busqueda.isNotEmpty()) {
                        Icon(Icons.Default.Close, "Limpiar", Modifier.size(16.dp).clickable { busqueda = "" },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("EMPLEADOS · ${if (busqueda.isBlank()) empleados.size else "${empleadosFiltrados.size}/${empleados.size}"}",
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
                        "Los empleados pueden anotar y registrar pagos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (empleadosFiltrados.isEmpty() && busqueda.isNotBlank()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SearchOff, null, Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("No encontramos \"$busqueda\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { busqueda = "" }) { Text("Limpiar búsqueda") }
                    }
                }
            }
        } else if (empleados.isEmpty()) {
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
            items(empleadosFiltrados, key = { it.id }) { emp ->
                val ultimoMov = ultimoMovPorUid[emp.uid]
                val reclamos = disputasPorUid[emp.uid] ?: 0
                val esActivoHoy = ultimoMov != null && ultimoMov.fecha.toDate().time >= inicioDia
                CardEmpleado(
                    emp = emp,
                    ultimoMov = ultimoMov,
                    reclamos = reclamos,
                    esActivoHoy = esActivoHoy,
                    inicioDia = inicioDia,
                    fmtFecha = fmtFecha,
                    fmtHora = fmtHora,
                    onEliminar = { empleadoAEliminar = emp }
                )
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
            onDismiss = { mostrarResumen = false }
        )
    }
}
