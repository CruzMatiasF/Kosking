package com.kosking.app.cliente

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVerFiado(
    onVolver: () -> Unit,
    vm: ClienteViewModel = viewModel()
) {
    val movimientos by vm.movimientos.collectAsState()
    val cliente by vm.cliente.collectAsState()
    val productos by vm.productos.collectAsState()
    val localesGuardados by vm.localesGuardados.collectAsState()
    val localSeleccionado by vm.localSeleccionado.collectAsState()
    val localInfo by vm.localInfo.collectAsState()
    val productosHabilitados = productos.filter { it.habilitado }
    val context = LocalContext.current

    var tabSeleccionado by remember { mutableStateOf(0) }
    var movimientoADisputar by remember { mutableStateOf<Movimiento?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localSeleccionado?.nombreLocal ?: "Mi fiado") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.Close, "Salir")
                    }
                },
                actions = {
                    localSeleccionado?.let { local ->
                        TextButton(onClick = { vm.quitarLocal(local.localId) }) {
                            Text("Quitar local", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Selector de locales si hay más de uno
            if (localesGuardados.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(localesGuardados, key = { it.localId }) { lg ->
                        FilterChip(
                            selected = lg.localId == localSeleccionado?.localId,
                            onClick = { vm.seleccionarLocal(lg.localId) },
                            label = { Text(lg.nombreLocal) }
                        )
                    }
                }
            }

            // Banner cuenta pausada
            if (cliente?.habilitado == false) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
                                    val nombre = localSeleccionado?.nombreLocal ?: "el negocio"
                                    val mensaje = "Hola $nombre, me avisaron que mi cuenta en Kosking está pausada."
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://wa.me/$telLocal?text=${Uri.encode(mensaje)}")
                                        }
                                        context.startActivity(intent)
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
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Deuda total", style = MaterialTheme.typography.bodyMedium)
                    val deuda = cliente?.deudaTotal ?: 0.0
                    Text(
                        "$${"%.2f".format(deuda)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (deuda > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (productosHabilitados.isNotEmpty()) {
                TabRow(selectedTabIndex = tabSeleccionado) {
                    Tab(selected = tabSeleccionado == 0, onClick = { tabSeleccionado = 0 }) {
                        Text("Mi fiado", modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Tab(selected = tabSeleccionado == 1, onClick = { tabSeleccionado = 1 }) {
                        Text("Precios del negocio", modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }

            when (tabSeleccionado) {
                0 -> TabFiado(
                    movimientos = movimientos,
                    onNoReconozco = { movimientoADisputar = it }
                )
                1 -> TabProductosCliente(productosHabilitados)
            }
        }
    }

    // Dialog de disputa
    movimientoADisputar?.let { mov ->
        AlertDialog(
            onDismissRequest = { movimientoADisputar = null },
            title = { Text("¿No reconocés este movimiento?") },
            text = {
                val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                Column {
                    Text("${mov.producto.ifBlank { if (mov.tipo == "cargo") "Fiado" else "Pago" }}")
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

@Composable
private fun TabFiado(
    movimientos: List<Movimiento>,
    onNoReconozco: (Movimiento) -> Unit
) {
    LazyColumn {
        if (movimientos.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Sin movimientos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(movimientos, key = { it.id }) { mov ->
                ItemMovimientoCliente(mov = mov, onNoReconozco = { onNoReconozco(mov) })
                HorizontalDivider()
            }
        }
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
                    Text(
                        "$${"%.2f".format(prod.precio)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ItemMovimientoCliente(mov: Movimiento, onNoReconozco: () -> Unit) {
    val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    val esCargo = mov.tipo == "cargo"

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mov.producto.ifBlank { if (esCargo) "Fiado" else "Pago" },
                    modifier = Modifier.weight(1f)
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
        },
        supportingContent = {
            Column {
                Text(fmt.format(mov.fecha.toDate()))
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
        },
        trailingContent = {
            Text(
                "${if (esCargo) "+" else "-"}${"%.2f".format(mov.monto)}",
                color = if (mov.anulado) MaterialTheme.colorScheme.onSurfaceVariant
                        else if (esCargo) MaterialTheme.colorScheme.error
                        else Color(0xFF2E7D32),
                fontWeight = if (mov.anulado) FontWeight.Normal else FontWeight.Medium
            )
        }
    )
}
