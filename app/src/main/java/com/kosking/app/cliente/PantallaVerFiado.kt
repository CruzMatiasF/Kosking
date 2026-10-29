package com.kosking.app.cliente

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.explorar.DialogConectarComoCliente
import com.kosking.app.modelo.LocalGuardado
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVerFiado(
    onVolver: () -> Unit,
    vm: ClienteViewModel = viewModel()
) {
    val localesGuardados by vm.localesGuardados.collectAsState()
    val deudasPorLocal by vm.deudasPorLocal.collectAsState()

    var localAbiertoId by remember { mutableStateOf<String?>(null) }
    var mostrarDialogConectar by remember { mutableStateOf(false) }

    val localAbierto = localesGuardados.firstOrNull { it.localId == localAbiertoId }

    BackHandler(enabled = localAbierto != null) {
        localAbiertoId = null
    }

    if (localAbierto != null) {
        LaunchedEffect(localAbierto.localId) { vm.seleccionarLocal(localAbierto.localId) }
        DetalleFiado(
            local = localAbierto,
            onVolver = { localAbiertoId = null },
            vm = vm
        )
    } else {
        ListaLocales(
            locales = localesGuardados,
            deudasPorLocal = deudasPorLocal,
            onSeleccionar = { localAbiertoId = it.localId },
            onQuitarLocal = { vm.quitarLocal(it.localId) },
            onVolver = onVolver,
            onConectarme = { mostrarDialogConectar = true }
        )
    }

    if (mostrarDialogConectar) {
        DialogConectarComoCliente(
            clienteVm = vm,
            onExito = { mostrarDialogConectar = false },
            onDismiss = { mostrarDialogConectar = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListaLocales(
    locales: List<LocalGuardado>,
    deudasPorLocal: Map<String, Double>,
    onSeleccionar: (LocalGuardado) -> Unit,
    onQuitarLocal: (LocalGuardado) -> Unit,
    onVolver: () -> Unit,
    onConectarme: () -> Unit
) {
    var localAConfirmarQuitar by remember { mutableStateOf<LocalGuardado?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ver mi fiado") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (locales.isEmpty()) {
                item {
                    Box(
                        Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                "No estás conectado a ningún local",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Pedile el código al dueño del local para conectarte",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = onConectarme) {
                                Text("Conectarme a un local")
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Tocá un local para ver tus movimientos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(locales, key = { it.localId }) { local ->
                    val deuda = deudasPorLocal[local.localId] ?: 0.0
                    TarjetaLocal(
                        local = local,
                        deuda = deuda,
                        onClick = { onSeleccionar(local) },
                        onDesvincularse = { localAConfirmarQuitar = local }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onConectarme,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Conectarme a otro local")
                    }
                }
            }
        }
    }

    localAConfirmarQuitar?.let { local ->
        AlertDialog(
            onDismissRequest = { localAConfirmarQuitar = null },
            title = { Text("Desvincularse del local") },
            text = { Text("¿Querés desvincularte de ${local.nombreLocal}? Podés volver a conectarte con tu código cuando quieras.") },
            confirmButton = {
                TextButton(onClick = {
                    onQuitarLocal(local)
                    localAConfirmarQuitar = null
                }) { Text("Desvincularse", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { localAConfirmarQuitar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TarjetaLocal(
    local: LocalGuardado,
    deuda: Double,
    onClick: () -> Unit,
    onDesvincularse: () -> Unit
) {
    val tieneDeuda = deuda > 0
    val colorContenedor = if (tieneDeuda)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar con inicial
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = colorContenedor,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            local.nombreLocal.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (tieneDeuda)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        local.nombreLocal,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (tieneDeuda) {
                        Text(
                            "$${"%.0f".format(deuda)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "debés",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Al día",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider()

            TextButton(
                onClick = onDesvincularse,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    "Desvincularse",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleFiado(
    local: LocalGuardado,
    onVolver: () -> Unit,
    vm: ClienteViewModel
) {
    val movimientos by vm.movimientos.collectAsState()
    val cliente by vm.cliente.collectAsState()
    val productos by vm.productos.collectAsState()
    val localInfo by vm.localInfo.collectAsState()
    val productosHabilitados = productos.filter { it.habilitado }
    val context = LocalContext.current
    val deuda = cliente?.deudaTotal ?: 0.0
    val tieneDeuda = deuda > 0

    var tabSeleccionado by remember { mutableStateOf(0) }
    var movimientoADisputar by remember { mutableStateOf<Movimiento?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (tieneDeuda) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    local.nombreLocal.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tieneDeuda) MaterialTheme.colorScheme.onErrorContainer
                                            else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(local.nombreLocal)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Banner cuenta pausada
            if (cliente?.habilitado == false) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Cuenta pausada",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "El negocio pausó tu cuenta temporalmente. Escribile para saber más.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        val telLocal = localInfo?.telefono?.replace(Regex("[^0-9]"), "") ?: ""
                        if (telLocal.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    val mensaje = "Hola ${local.nombreLocal}, me avisaron que mi cuenta en Kosking está pausada."
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://wa.me/$telLocal?text=${Uri.encode(mensaje)}")
                                        })
                                    } catch (_: Exception) {}
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Escribirle al negocio por WhatsApp",
                                    color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Resumen deuda
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "lo que debés",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$${"%.0f".format(deuda)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (tieneDeuda) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }
                if (!tieneDeuda) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "estás al día ✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            if (productosHabilitados.isNotEmpty()) {
                TabRow(selectedTabIndex = tabSeleccionado) {
                    Tab(selected = tabSeleccionado == 0, onClick = { tabSeleccionado = 0 }) {
                        Text("Movimientos", modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Tab(selected = tabSeleccionado == 1, onClick = { tabSeleccionado = 1 }) {
                        Text("Precios", modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }

            when (tabSeleccionado) {
                0 -> TabMovimientos(movimientos = movimientos, onNoReconozco = { movimientoADisputar = it })
                1 -> TabProductosCliente(productosHabilitados)
            }
        }
    }

    movimientoADisputar?.let { mov ->
        AlertDialog(
            onDismissRequest = { movimientoADisputar = null },
            title = { Text("¿No reconocés este movimiento?") },
            text = {
                val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                Column {
                    Text(mov.producto.ifBlank { if (mov.tipo == "cargo") "Fiado" else "Pago" })
                    Text("$${"%.2f".format(mov.monto)} · ${fmt.format(mov.fecha.toDate())}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Se va a marcar como disputa y el dueño del negocio lo va a revisar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.marcarDisputado(mov.id)
                    movimientoADisputar = null
                }) { Text("Reportar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { movimientoADisputar = null }) { Text("Cancelar") }
            }
        )
    }
}

private fun agruparPorDia(movimientos: List<Movimiento>): List<Pair<String, List<Movimiento>>> {
    val hoy = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val ayer = hoy - 86_400_000L
    val fmt = SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es"))

    return movimientos
        .groupBy { mov ->
            val t = mov.fecha.toDate().time
            when {
                t >= hoy -> "Hoy"
                t >= ayer -> "Ayer"
                else -> fmt.format(Date(t)).replaceFirstChar { it.uppercase() }
            }
        }
        .entries
        .map { it.key to it.value }
}

@Composable
private fun TabMovimientos(
    movimientos: List<Movimiento>,
    onNoReconozco: (Movimiento) -> Unit
) {
    if (movimientos.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Sin movimientos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Cuando el negocio anote algo vas a verlo acá",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val grupos = remember(movimientos) { agruparPorDia(movimientos) }

    LazyColumn {
        grupos.forEach { (label, movs) ->
            item(key = "header_$label") {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(movs, key = { it.id }) { mov ->
                ItemMovimientoCliente(mov = mov, onNoReconozco = { onNoReconozco(mov) })
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TabProductosCliente(productos: List<Producto>) {
    LazyColumn {
        item {
            Text(
                "Precios actualizados por el negocio",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        items(productos, key = { it.id }) { prod ->
            ListItem(
                headlineContent = { Text(prod.nombre, fontWeight = FontWeight.Medium) },
                supportingContent = {
                    if (prod.descripcion.isNotBlank()) Text(prod.descripcion)
                },
                trailingContent = {
                    Column(horizontalAlignment = Alignment.End) {
                        if (prod.esOferta && prod.precioAnterior > 0) {
                            Text(
                                "$${"%.0f".format(prod.precioAnterior)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        }
                        Text(
                            "$${"%.0f".format(prod.precio)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (prod.esOferta) {
                            Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text("Oferta", color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ItemMovimientoCliente(mov: Movimiento, onNoReconozco: () -> Unit) {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val esCargo = mov.tipo == "cargo"

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ícono
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (esCargo) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (esCargo) "↑" else "↓",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (esCargo) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    mov.producto.ifBlank { if (esCargo) "Fiado" else "Pago" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (mov.anulado) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )
                if (mov.anulado) {
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("Anulado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (mov.disputado) {
                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text("En disputa", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(
                "por ${mov.anotadoPorNombre} · ${fmt.format(mov.fecha.toDate())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!mov.anulado && !mov.disputado && esCargo) {
                TextButton(
                    onClick = onNoReconozco,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        "No lo reconozco",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Monto
        Text(
            "${if (esCargo) "+" else "−"}$${"%.0f".format(mov.monto)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = when {
                mov.anulado -> MaterialTheme.colorScheme.onSurfaceVariant
                esCargo -> MaterialTheme.colorScheme.error
                else -> Color(0xFF2E7D32)
            }
        )
    }
}
