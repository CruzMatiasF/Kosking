package com.kosking.app.dueno.detalle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VistaCliente(
    nombreCliente: String,
    nombreLocal: String,
    deudaTotal: Double,
    movimientos: List<Movimiento>,
    onCerrar: () -> Unit
) {
    val fmtFecha = remember { SimpleDateFormat("d 'de' MMM", Locale.forLanguageTag("es-AR")) }
    val fmtKey = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()) }

    val visitas = remember(movimientos) {
        movimientos
            .filter { !it.anulado && it.tipo == "cargo" }
            .groupBy { it.grupoId.ifBlank { fmtKey.format(it.fecha.toDate()) } }
            .entries
            .sortedByDescending { (_, movs) -> movs.first().fecha.toDate().time }
    }

    Dialog(
        onDismissRequest = { /* no cerrar con toque accidental */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

                // Barra superior con botón cerrar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cerrar", style = MaterialTheme.typography.labelMedium)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Encabezado
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                nombreLocal,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Hola, $nombreCliente",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Deuda total — prominente
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Tu deuda total",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "$${"%.0f".format(deudaTotal)}",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Historial de visitas
                    if (visitas.isNotEmpty()) {
                        item {
                            Text(
                                "Lo que te llevaste",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        items(visitas) { (_, movs) ->
                            val fecha = fmtFecha.format(movs.first().fecha.toDate())
                            val productos = movs
                                .filter { it.producto.isNotBlank() }
                                .groupBy { it.producto }
                                .map { (prod, ms) ->
                                    val cant = ms.sumOf { it.cantidad }
                                    if (cant > 1) "${cant}× $prod" else prod
                                }

                            if (productos.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        fecha,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    productos.forEach { item ->
                                        Text(
                                            "· $item",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    } else {
                        item {
                            Text(
                                "Sin compras registradas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }

                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "📱 Kosking",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
