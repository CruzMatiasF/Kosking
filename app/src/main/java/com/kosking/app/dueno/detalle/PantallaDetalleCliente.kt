package com.kosking.app.dueno.detalle

import com.kosking.app.dueno.DuenoViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleCliente(
    clienteId: String,
    onVolver: () -> Unit,
    onVerMovimiento: (String) -> Unit = {},
    onAnotarFiado: (String) -> Unit = {},
    onCobrar: (String) -> Unit = {},
    vm: DuenoViewModel = viewModel()
) {
    LaunchedEffect(clienteId) { vm.seleccionarCliente(clienteId) }

    val movimientos by vm.movimientosDetalle.collectAsState()
    var movimientosCargados by remember { mutableStateOf(false) }
    LaunchedEffect(movimientos) { movimientosCargados = true }
    val clientes by vm.clientes.collectAsState()
    val cliente = clientes.find { it.id == clienteId }
    val error by vm.error.collectAsState()

    val extracto = remember(movimientos) {
        construirExtracto(movimientos)
    }

    var mostrarConfirmarEliminar by remember { mutableStateOf(false) }
    var mostrarEditar by remember { mutableStateOf(false) }

    if (mostrarConfirmarEliminar && cliente != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarEliminar = false },
            title = { Text("Eliminar cliente") },
            text = { Text("Â¿Seguro que querÃ©s eliminar a ${cliente.nombre}? Esta acciÃ³n no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmarEliminar = false
                        vm.eliminarCliente(cliente) { onVolver() }
                    }
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarEliminar = false }) { Text("Cancelar") }
            }
        )
    }

    error?.let {
        if (it.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { vm.clearError() },
                title = { Text("No se puede eliminar") },
                text = { Text(it) },
                confirmButton = {
                    TextButton(onClick = { vm.clearError() }) { Text("Entendido") }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                        Spacer(Modifier.weight(1f))
                        if (cliente != null) {
                            IconButton(onClick = { mostrarEditar = true }) {
                                Icon(Icons.Default.Edit, "Editar cliente")
                            }
                        }
                        if (movimientosCargados && movimientos.isEmpty() && cliente != null) {
                            IconButton(onClick = { mostrarConfirmarEliminar = true }) {
                                Icon(Icons.Default.Delete, "Eliminar cliente",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Text(
                        text = cliente?.nombre ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                    )
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            cliente?.let {
                // Hero: deuda + info del cliente
                item { HeroCliente(it) }
                // Botones anotar / cobrar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAnotarFiado(clienteId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Anotar fiado")
                        }
                        OutlinedButton(
                            onClick = { onCobrar(clienteId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cobrar")
                        }
                    }
                }
                // Tiles de configuraciÃ³n
                item { ConfigTiles(it, vm) }
            }

            if (extracto.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Sin movimientos todavÃ­a",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item {
                    Text(
                        "HISTORIAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                items(extracto, key = { "ext_${it.fecha.time}_${it.concepto}_${it.movimientoId}" }) { linea ->
                    FilaExtracto(linea, vm, clienteId)
                }
            }
        }
    }

    // DiÃ¡logo editar cliente
    if (mostrarEditar && cliente != null) {
        DialogEditarCliente(
            cliente = cliente,
            onGuardar = { actualizado ->
                vm.actualizarCliente(actualizado)
                mostrarEditar = false
            },
            onDismiss = { mostrarEditar = false }
        )
    }
}

@Composable
private fun HeroCliente(cliente: Cliente) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Text(
            "Deuda total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
            letterSpacing = 0.8.sp
        )
        Text(
            "$${"%.0f".format(cliente.deudaTotal)}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = if (cliente.deudaTotal > 0) Color(0xFFFF5252) else Color(0xFF69F0AE)
        )

        if (cliente.limiteCredito > 0) {
            Spacer(Modifier.height(8.dp))
            val progreso = (cliente.deudaTotal / cliente.limiteCredito).coerceIn(0.0, 1.0).toFloat()
            val superado = cliente.deudaTotal > cliente.limiteCredito
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = if (superado) Color(0xFFFF5252) else Color(0xFFFFD740),
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (superado) "SuperÃ³ el lÃ­mite" else "${"%.0f".format(progreso * 100)}% usado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
                Text(
                    "LÃ­mite $${"%.0f".format(cliente.limiteCredito)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
            }
        }

        if (cliente.domicilio.isNotBlank() || cliente.telefono.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cliente.domicilio.isNotBlank()) InfoChip(Icons.Default.Home, cliente.domicilio)
                if (cliente.telefono.isNotBlank()) InfoChip(Icons.Default.Phone, cliente.telefono)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, texto: String) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f))
        Text(texto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
    }
}

@Composable
private fun ConfigTiles(cliente: Cliente, vm: DuenoViewModel) {
    var mostrarDialogoLimite by remember { mutableStateOf(false) }
    var mostrarDialogoFechaPago by remember { mutableStateOf(false) }
    var mostrarDialogoNota by remember { mutableStateOf(false) }

    if (mostrarDialogoNota) {
        DialogEditarNota(
            notaActual = cliente.notaInterna,
            onGuardar = { nuevaNota ->
                vm.actualizarCliente(cliente.copy(notaInterna = nuevaNota))
                mostrarDialogoNota = false
            },
            onDismiss = { mostrarDialogoNota = false }
        )
    }

    if (mostrarDialogoFechaPago) {
        DialogEditarFechaPago(
            fechaActual = cliente.fechaPago?.toDate(),
            onGuardar = { nuevaFecha ->
                val ts = if (nuevaFecha != null) com.google.firebase.Timestamp(nuevaFecha) else null
                vm.actualizarCliente(cliente.copy(fechaPago = ts))
                mostrarDialogoFechaPago = false
            },
            onDismiss = { mostrarDialogoFechaPago = false }
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

    val hoy = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.time
    }
    val fechaPagoDate = cliente.fechaPago?.toDate()
    val vencida = fechaPagoDate != null && fechaPagoDate.before(hoy) && cliente.deudaTotal > 0
    val fmtFecha = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        // Tiles en grilla: fecha de pago + lÃ­mite de crÃ©dito
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tile fecha de pago
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { mostrarDialogoFechaPago = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = if (vencida) MaterialTheme.colorScheme.errorContainer
                                            else MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth, null, Modifier.size(18.dp),
                                tint = if (vencida) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Icon(
                            Icons.Default.Edit, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Fecha de pago",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    if (fechaPagoDate != null) {
                        Text(
                            fmtFecha.format(fechaPagoDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (vencida) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        if (vencida) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Vencida",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            "Sin acordar",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tile lÃ­mite de crÃ©dito
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { mostrarDialogoLimite = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CreditCard, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Icon(
                            Icons.Default.Edit, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "LÃ­mite de crÃ©dito",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (cliente.limiteCredito > 0) "$${"%.0f".format(cliente.limiteCredito)}" else "Sin lÃ­mite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (cliente.limiteCredito > 0) {
                        val disponible = (cliente.limiteCredito - cliente.deudaTotal).coerceAtLeast(0.0)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$${"%.0f".format(disponible)} disp.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Nota interna
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
                            color = if (cliente.notaInterna.isNotBlank()) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit, null, Modifier.size(18.dp),
                        tint = if (cliente.notaInterna.isNotBlank()) Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nota interna", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun DialogEditarNota(
    notaActual: String,
    onGuardar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nota by remember { mutableStateOf(notaActual) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nota interna") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Solo visible para vos y tus empleados al anotar. Ãštil para indicaciones especiales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota") },
                    placeholder = { Text("Ej: No anotarle a los hijos") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(nota.trim()) }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (notaActual.isNotBlank()) {
                    TextButton(onClick = { onGuardar("") }) {
                        Text("Borrar nota", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogEditarLimite(
    limiteActual: Double,
    onGuardar: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var valor by remember { mutableStateOf(if (limiteActual > 0) "%.0f".format(limiteActual) else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LÃ­mite de crÃ©dito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "El cliente no podrÃ¡ superar este monto de deuda. DejÃ¡ vacÃ­o para no tener lÃ­mite.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto mÃ¡ximo $") },
                    placeholder = { Text("Sin lÃ­mite") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(valor.toDoubleOrNull() ?: 0.0) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private data class LineaExtracto(
    val fecha: Date,
    val concepto: String,
    val monto: Double,
    val saldo: Double,
    val esPago: Boolean,
    val pagado: Boolean = false,
    val movimientoId: String? = null,
    val items: List<Movimiento> = emptyList(),
    val anotadoPor: String = "",
    val tieneAnulados: Boolean = false,
    val visitante: String = "",
    val saldoParcial: Double? = null  // falta este monto para cubrir este cargo
)

private fun construirExtracto(movimientos: List<Movimiento>): List<LineaExtracto> {
    val ordenados = movimientos.sortedBy { it.fecha.toDate() }
    val cargos = ordenados.filter { it.tipo == "cargo" && !it.anulado }
    val pagos = ordenados.filter { it.tipo == "pago" && !it.anulado }
    val anulados = ordenados.filter { it.anulado }

    data class LineaRaw(val fecha: Date, val concepto: String, val monto: Double, val movimientoId: String?, val esPago: Boolean, val items: List<Movimiento> = emptyList(), val anotadoPor: String = "", val tieneAnulados: Boolean = false, val visitante: String = "")

    val lineasRaw = mutableListOf<LineaRaw>()

    // Agrupar cargos: por grupoId si existe, sino ventana 30 min (compatibilidad con datos viejos)
    fun flushGrupo(grupo: List<Movimiento>) {
        if (grupo.isEmpty()) return
        val prods = grupo.map { it.producto.ifBlank { "Fiado" } }
        val concepto = if (prods.size == 1) prods[0] else "${prods.size} productos"
        val visitante = grupo.firstOrNull { it.visitante.isNotBlank() }?.visitante ?: ""
        lineasRaw.add(LineaRaw(grupo.first().fecha.toDate(), concepto, grupo.sumOf { it.monto }, grupo.first().id, false, grupo, grupo.firstOrNull { it.anotadoPorNombre.isNotBlank() }?.anotadoPorNombre ?: "", grupo.any { it.anulado }, visitante))
    }
    cargos.groupBy { it.grupoId }.values.forEach { flushGrupo(it.sortedBy { m -> m.fecha.toDate() }) }

    for (pago in pagos) {
        lineasRaw.add(LineaRaw(pago.fecha.toDate(), "Pago recibido", pago.monto, pago.id, true, listOf(pago)))
    }

    // Anulados: agrupar por grupoId igual que los cargos activos
    val anuladosConGrupo = anulados.filter { it.grupoId.isNotBlank() }
    val anuladosSinGrupo = anulados.filter { it.grupoId.isBlank() }
    anuladosConGrupo.groupBy { it.grupoId }.values.forEach { grupo ->
        val sorted = grupo.sortedBy { it.fecha.toDate() }
        val prods = sorted.map { it.producto.ifBlank { if (it.tipo == "pago") "Pago" else "Fiado" } }
        val concepto = when {
            prods.size == 1 -> prods[0]
            prods.size == 2 -> "${prods[0]}, ${prods[1]}"
            else -> "${prods[0]} Â· ${prods.size} Ã­tems"
        }
        lineasRaw.add(LineaRaw(sorted.first().fecha.toDate(), concepto, sorted.sumOf { it.monto }, sorted.first().id, sorted.first().tipo == "pago", sorted, sorted.firstOrNull { it.anotadoPorNombre.isNotBlank() }?.anotadoPorNombre ?: "", tieneAnulados = true))
    }
    for (mov in anuladosSinGrupo) {
        lineasRaw.add(LineaRaw(mov.fecha.toDate(), mov.producto.ifBlank { if (mov.tipo == "pago") "Pago" else "Fiado" }, mov.monto, mov.id, mov.tipo == "pago", listOf(mov), mov.anotadoPorNombre, tieneAnulados = true))
    }

    lineasRaw.sortBy { it.fecha }

    var saldo = 0.0
    val conSaldo = lineasRaw.map { raw ->
        val delta = if (raw.esPago) -raw.monto else raw.monto
        if (!raw.tieneAnulados) saldo += delta
        LineaExtracto(raw.fecha, raw.concepto, delta, saldo, raw.esPago, movimientoId = raw.movimientoId, items = raw.items, anotadoPor = raw.anotadoPor, tieneAnulados = raw.tieneAnulados, visitante = raw.visitante)
    }

    // Tachado FIFO: los pagos mÃ¡s viejos saldan los cargos mÃ¡s viejos
    var pagoRestante = conSaldo.filter { it.esPago && !it.tieneAnulados }.sumOf { -it.monto }
    val resultado = conSaldo.map { linea ->
        if (!linea.esPago && !linea.tieneAnulados && pagoRestante > 0) {
            val cubierto = pagoRestante.coerceAtMost(linea.monto)
            val pagado = pagoRestante >= linea.monto
            pagoRestante = (pagoRestante - linea.monto).coerceAtLeast(0.0)
            val saldoParcial = if (!pagado && cubierto > 0) linea.monto - cubierto else null
            linea.copy(pagado = pagado, saldoParcial = saldoParcial)
        } else linea
    }
    return resultado.reversed()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaExtracto(linea: LineaExtracto, vm: DuenoViewModel, clienteId: String) {
    val fmt = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }
    var mostrarDetalle by remember { mutableStateOf(false) }
    val alphaMuted = if (linea.pagado) 0.45f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable { mostrarDetalle = true },
        shape = RoundedCornerShape(10.dp)
    ) {
        val tachado = linea.tieneAnulados || linea.pagado
        val (tipoIcon, tipoColor) = when {
            linea.tieneAnulados -> Icons.Default.RemoveCircleOutline to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            linea.esPago        -> Icons.Default.ArrowDownward to Color(0xFF2E7D32)
            else                -> Icons.Default.ArrowUpward to MaterialTheme.colorScheme.error
        }
        val montoColor = when {
            tachado      -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            linea.esPago -> Color(0xFF2E7D32)
            else         -> MaterialTheme.colorScheme.error
        }

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ãcono circular de tipo
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tipoColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(tipoIcon, null, Modifier.size(18.dp), tint = tipoColor)
            }

            // Centro: tipo Â· concepto + hora Â· quien anotÃ³ Â· visitante
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val etiquetaTipo = when {
                    linea.tieneAnulados -> "Anulado"
                    linea.esPago        -> "Pago"
                    else                -> "Fiado"
                }
                Text(
                    etiquetaTipo,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tipoColor.copy(alpha = if (tachado) 0.4f else 1f),
                )
                Text(
                    linea.concepto,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alphaMuted),
                    textDecoration = if (tachado) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        fmt.format(linea.fecha),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaMuted)
                    )
                    if (linea.visitante.isNotBlank()) {
                        Text("Â·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Icon(Icons.Default.Person, null, Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaMuted))
                        Text(
                            linea.visitante,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaMuted)
                        )
                    }
                }
            }

            // Monto + saldo parcial a la derecha
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (linea.esPago) "âˆ’$${"%.0f".format(-linea.monto)}"
                    else "+$${"%.0f".format(linea.monto)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = montoColor,
                    textDecoration = if (tachado) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                if (linea.saldoParcial != null) {
                    Text(
                        "Saldo $${"%.0f".format(linea.saldoParcial)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    var confirmarAnularTodo by remember { mutableStateOf(false) }
    var itemAAnular by remember { mutableStateOf<Movimiento?>(null) }

    if (mostrarDetalle) {
        ModalBottomSheet(onDismissRequest = { mostrarDetalle = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Cabecera: fecha a la izquierda Â· nombre empleado a la derecha Â· badges
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(fmt.format(linea.fecha), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (linea.pagado) {
                            Text("Pagado", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))
                        }
                        if (linea.tieneAnulados) {
                            Text(
                                "Anulado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        if (linea.anotadoPor.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Person, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(linea.anotadoPor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Ãtems individuales
                linea.items.forEach { mov ->
                    val esAnulado = mov.anulado
                    val nombreProd = mov.producto.ifBlank { if (mov.tipo == "cargo") "Fiado" else "Pago" }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            nombreProd,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = if (esAnulado) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (esAnulado) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                        if (mov.cantidad > 1) {
                            Text(
                                "x${mov.cantidad}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        Text(
                            "${if (mov.tipo == "cargo") "+" else "âˆ’"}$${"%.0f".format(mov.monto)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                esAnulado -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                mov.tipo == "cargo" -> MaterialTheme.colorScheme.error
                                else -> Color(0xFF2E7D32)
                            },
                            textDecoration = if (esAnulado) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                        if (!esAnulado && !linea.tieneAnulados && !linea.pagado) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = { itemAAnular = mov },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Anular", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // Total (solo para cargos no anulados)
                if (!linea.esPago && !linea.tieneAnulados) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total visita", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "+$${"%.0f".format(linea.monto)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (linea.pagado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                }

                // BotÃ³n anular todo el grupo (solo si no estÃ¡ ya anulado ni pagado)
                if (!linea.tieneAnulados && !linea.pagado) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { confirmarAnularTodo = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (linea.items.size > 1) "Anular visita completa" else "Anular")
                    }
                }
            }
        }
    }

    // Confirmar anular TODO el grupo
    if (confirmarAnularTodo) {
        AlertDialog(
            onDismissRequest = { confirmarAnularTodo = false },
            title = { Text("Anular visita") },
            text = { Text("Se anularÃ¡ toda esta visita y se descontarÃ¡ del saldo del cliente.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarAnularTodo = false
                    mostrarDetalle = false
                    linea.items.forEach { mov ->
                        if (!mov.anulado) {
                            vm.anularMovimiento(mov.id, clienteId, mov.monto, mov.tipo, "Anulado por el dueÃ±o")
                        }
                    }
                }) { Text("Anular", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarAnularTodo = false }) { Text("Cancelar") }
            }
        )
    }

    // Confirmar anular Ã­tem individual
    itemAAnular?.let { mov ->
        AlertDialog(
            onDismissRequest = { itemAAnular = null },
            title = { Text("Anular Ã­tem") },
            text = { Text("Se anularÃ¡ \"${mov.producto.ifBlank { "Fiado" }}\" ($${"%.0f".format(mov.monto)}) y se descontarÃ¡ del saldo.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.anularMovimiento(mov.id, clienteId, mov.monto, mov.tipo, "Anulado por el dueÃ±o")
                    itemAAnular = null
                }) { Text("Anular", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { itemAAnular = null }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogEditarFechaPago(
    fechaActual: Date?,
    onGuardar: (Date?) -> Unit,
    onDismiss: () -> Unit
) {
    val cal = remember {
        java.util.Calendar.getInstance().apply {
            if (fechaActual != null) time = fechaActual
            else add(java.util.Calendar.WEEK_OF_YEAR, 1)
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = (fechaActual ?: run {
            java.util.Calendar.getInstance().apply { add(java.util.Calendar.WEEK_OF_YEAR, 1) }.time
        }).time
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedMs = datePickerState.selectedDateMillis
                onGuardar(if (selectedMs != null) Date(selectedMs) else null)
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (fechaActual != null) {
                    TextButton(onClick = { onGuardar(null) }) { Text("Quitar fecha", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogEditarCliente(
    cliente: Cliente,
    onGuardar: (Cliente) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(cliente.nombre) }
    var telefono by remember { mutableStateOf(cliente.telefono) }
    var domicilio by remember { mutableStateOf(cliente.domicilio) }
    var habilitado by remember { mutableStateOf(cliente.habilitado) }
    var campoActivo by remember { mutableStateOf<String?>(null) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Sub-dialogo para editar un campo de texto
    campoActivo?.let { campo ->
        val valorActual = when (campo) {
            "nombre" -> nombre
            "telefono" -> telefono
            else -> domicilio
        }
        var draft by remember(campo) { mutableStateOf(valorActual) }
        AlertDialog(
            onDismissRequest = { campoActivo = null },
            title = { Text(when (campo) { "nombre" -> "Nombre"; "telefono" -> "TelÃ©fono"; else -> "Domicilio" }) },
            text = {
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it },
                    singleLine = campo != "domicilio",
                    keyboardOptions = if (campo == "telefono") KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (campo) { "nombre" -> nombre = draft.trim(); "telefono" -> telefono = draft.trim(); else -> domicilio = draft.trim() }
                        campoActivo = null
                    },
                    enabled = campo != "nombre" || draft.isNotBlank()
                ) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { campoActivo = null }) { Text("Cancelar") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Editar cliente")
                Text(
                    cliente.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "DATOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )

                // Campo nombre
                CampoEditable(
                    icon = Icons.Default.Person,
                    label = "Nombre",
                    value = nombre.ifBlank { "â€”" },
                    onClick = { campoActivo = "nombre" }
                )
                // Campo telÃ©fono
                CampoEditable(
                    icon = Icons.Default.Phone,
                    label = "TelÃ©fono",
                    value = telefono.ifBlank { "Sin telÃ©fono" },
                    vacio = telefono.isBlank(),
                    onClick = { campoActivo = "telefono" }
                )
                // Campo domicilio
                CampoEditable(
                    icon = Icons.Default.Home,
                    label = "Domicilio",
                    value = domicilio.ifBlank { "Sin domicilio" },
                    vacio = domicilio.isBlank(),
                    onClick = { campoActivo = "domicilio" }
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "ACCESO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )

                // Toggle habilitado
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Group, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column {
                                Text("Habilitado para anotar", style = MaterialTheme.typography.bodyMedium)
                                if (habilitado) {
                                    Text(
                                        "Empleados pueden anotar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Switch(checked = habilitado, onCheckedChange = { habilitado = it })
                    }
                }

                // CÃ³digo de acceso (solo lectura)
                if (cliente.codigoAcceso.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Key, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "CÃ³digo de acceso Â· solo lectura",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    cliente.codigoAcceso,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(cliente.codigoAcceso)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, "Copiar", Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGuardar(cliente.copy(
                        nombre = nombre.trim(),
                        telefono = telefono.trim(),
                        domicilio = domicilio.trim(),
                        habilitado = habilitado
                    ))
                },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CampoEditable(
    icon: ImageVector,
    label: String,
    value: String,
    vacio: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (vacio) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

