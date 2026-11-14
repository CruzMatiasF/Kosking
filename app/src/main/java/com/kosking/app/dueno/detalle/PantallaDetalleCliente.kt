package com.kosking.app.dueno.detalle

import com.kosking.app.dueno.DuenoViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Phone
import com.kosking.app.util.PdfExportUtil
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kosking.app.modelo.Cliente
import com.kosking.app.util.WhatsAppUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleCliente(
    clienteId: String,
    onVolver: () -> Unit,
    onVerMovimiento: (String) -> Unit = {},
    onAnotarFiado: (String) -> Unit = {},
    onCobrar: (String) -> Unit = {},
    vm: DuenoViewModel
) {
    LaunchedEffect(clienteId) { vm.seleccionarCliente(clienteId) }

    val movimientos by vm.movimientosDetalle.collectAsState()
    var movimientosCargados by remember { mutableStateOf(false) }
    LaunchedEffect(movimientos) { movimientosCargados = true }
    val clientes by vm.clientes.collectAsState()
    val cliente = clientes.find { it.id == clienteId }
    val error by vm.error.collectAsState()
    val localData by vm.localData.collectAsState()
    val context = LocalContext.current

    val extracto = remember(movimientos) {
        construirExtracto(movimientos)
    }

    var mostrarConfirmarEliminar by remember { mutableStateOf(false) }
    var mostrarEditar by remember { mutableStateOf(false) }
    var mostrarMenu by remember { mutableStateOf(false) }
    var mostrarConfirmarRecalcular by remember { mutableStateOf(false) }

    if (mostrarConfirmarRecalcular && cliente != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarRecalcular = false },
            title = { Text("Recalcular deuda") },
            text = { Text("Esto suma todos los movimientos reales de ${cliente.nombre} y corrige la deuda si hay diferencias. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmarRecalcular = false
                    vm.recalcularDeuda(clienteId)
                }) { Text("Recalcular") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarRecalcular = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarConfirmarEliminar && cliente != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarEliminar = false },
            title = { Text("Eliminar cliente") },
            text = { Text("¿Seguro que querés eliminar a ${cliente.nombre}? Esta acción no se puede deshacer.") },
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
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
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
                        Box {
                            IconButton(onClick = { mostrarMenu = true }) {
                                Icon(Icons.Default.MoreVert, "Más opciones")
                            }
                            DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
                                if (movimientos.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Exportar PDF") },
                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                        onClick = {
                                            mostrarMenu = false
                                            val intent = PdfExportUtil.generarHistorialCliente(
                                                context = context,
                                                localNombre = localData?.nombre ?: "Mi local",
                                                cliente = cliente,
                                                movimientos = movimientos
                                            )
                                            context.startActivity(android.content.Intent.createChooser(intent, "Compartir PDF"))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Compartir historial") },
                                        onClick = {
                                            mostrarMenu = false
                                            val texto = WhatsAppUtil.historialCliente(
                                                nombreCliente = cliente.nombre,
                                                nombreLocal = localData?.nombre ?: "el local",
                                                deudaTotal = cliente.deudaTotal,
                                                movimientos = movimientos
                                            )
                                            WhatsAppUtil.compartir(context, texto)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Recalcular deuda") },
                                    onClick = { mostrarMenu = false; mostrarConfirmarRecalcular = true }
                                )
                                if (movimientosCargados && movimientos.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Eliminar cliente", color = MaterialTheme.colorScheme.error) },
                                        onClick = { mostrarMenu = false; mostrarConfirmarEliminar = true }
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    text = cliente?.nombre ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                )
            }
        },
    ) { padding ->
        val frecuentes = remember(movimientos) {
            movimientos
                .filter { it.tipo == "cargo" && !it.anulado && it.producto.isNotBlank() }
                .groupBy { it.producto }
                .mapValues { (_, ms) -> ms.size }
                .entries
                .sortedByDescending { it.value }
                .take(5)
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            cliente?.let {
                item { HeroCliente(it) }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (it.telefono.isNotBlank() && it.deudaTotal > 0) {
                            OutlinedButton(
                                onClick = {
                                    val ultimos = movimientos
                                        .filter { m -> !m.anulado }
                                        .sortedByDescending { m -> m.fecha.toDate().time }
                                        .take(5)
                                    WhatsAppUtil.abrirWhatsApp(
                                        context = context,
                                        telefono = it.telefono,
                                        mensaje = WhatsAppUtil.mensajeResumenDetallado(
                                            nombreCliente = it.nombre,
                                            nombreLocal = localData?.nombre ?: "el local",
                                            deudaTotal = it.deudaTotal,
                                            ultimosMovimientos = ultimos
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Phone, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Recordar por WhatsApp — ${"$"}${"%.0f".format(it.deudaTotal)}")
                            }
                        }
                    }
                }
                item { ConfigTiles(it, vm) }
            }

            if (frecuentes.isNotEmpty()) {
                item {
                    Text(
                        "Lo que más pide",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        items(frecuentes) { (nombre, veces) ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 1.dp
                            ) {
                                Text(
                                    "$nombre · $veces×",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (extracto.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Sin movimientos todavía",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Anotá el primer cargo con el botón de arriba",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else {
                item {
                    Text(
                        "Historial",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                items(extracto, key = { "ext_${it.fecha.time}_${it.conceptos.firstOrNull()}" }) { visita ->
                    FilaExtracto(visita, onClick = { visita.movimientoIds.firstOrNull()?.let(onVerMovimiento) })
                }
            }
        }
    }

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

