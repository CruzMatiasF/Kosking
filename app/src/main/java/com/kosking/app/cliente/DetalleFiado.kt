package com.kosking.app.cliente

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.cliente.components.TabMovimientos
import com.kosking.app.cliente.components.TabProductosCliente
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.LocalGuardado
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetalleFiado(
    local: LocalGuardado,
    movimientos: List<Movimiento>,
    cliente: Cliente?,
    productos: List<Producto>,
    localInfo: Local?,
    onVolver: () -> Unit,
    onMarcarDisputado: (String) -> Unit
) {
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

            if (cliente?.habilitado == false) {
                BannerCuentaPausada(nombreLocal = local.nombreLocal, localInfo = localInfo, context = context)
            }

            CabeceraDeuda(
                deuda = deuda,
                tieneDeuda = tieneDeuda,
                nombreLocal = local.nombreLocal,
                movimientos = movimientos,
                localInfo = localInfo
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
                    onMarcarDisputado(mov.id)
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
private fun BannerCuentaPausada(nombreLocal: String, localInfo: Local?, context: android.content.Context) {
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
                        val mensaje = "Hola $nombreLocal, me avisaron que mi cuenta en Kosking está pausada."
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

@Composable
private fun CabeceraDeuda(
    deuda: Double,
    tieneDeuda: Boolean,
    nombreLocal: String,
    movimientos: List<Movimiento>,
    localInfo: Local?
) {
    val context = LocalContext.current

    val ultimoFiado = remember(movimientos) {
        movimientos.filter { it.tipo == "cargo" && !it.anulado }
            .maxByOrNull { it.fecha.toDate().time }
    }
    val diasDesdeUltimo = remember(ultimoFiado) {
        ultimoFiado?.let {
            val diff = System.currentTimeMillis() - it.fecha.toDate().time
            (diff / 86_400_000L).toInt()
        }
    }

    if (tieneDeuda) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Tu deuda en $nombreLocal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
                Text(
                    "$${"%.0f".format(deuda)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                diasDesdeUltimo?.let { dias ->
                    val cuando = when (dias) {
                        0 -> "anotado hoy"
                        1 -> "último anotado ayer"
                        else -> "último anotado hace $dias días"
                    }
                    Text(
                        cuando,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                    )
                }
                val telLocal = localInfo?.telefono?.replace(Regex("[^0-9]"), "") ?: ""
                if (telLocal.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val msg = "Hola, quiero coordinar el pago de mi deuda de \$${
                                "%.0f".format(deuda)
                            } en $nombreLocal."
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/$telLocal?text=${Uri.encode(msg)}")
                                })
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("Coordinar pago por WhatsApp")
                    }
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("✓", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        "Estás al día",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "No tenés deuda en $nombreLocal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
