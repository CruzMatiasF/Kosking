package com.kosking.app.dueno.tabs

import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.DialogInvitarEmpleado

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
                            Text(
                                "Cobrado $${"%.0f".format(totalCobradoHoy)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
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
                                    color = if (stat.cobro > 0) Color(0xFF2E7D32)
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
                        "Los empleados pueden anotar fiados y registrar pagos. Controlá los permisos por cliente desde el detalle de cada uno.",
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
                    val mensaje = "ðŸ‘‹ Te invitan a unirte como empleado a *$local* en Kosking.\n\nSeguí estos pasos:\n1ï¸âƒ£ Descargá la app Kosking desde Play Store\n2ï¸âƒ£ Registrate o iniciá sesión\n3ï¸âƒ£ Tocá \"Soy empleado\" e ingresá el código:\n\n*$codigo*\n\nEl código es de un solo uso."
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
}
