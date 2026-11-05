package com.kosking.app.dueno.detalle

import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Movimiento
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class LineaExtracto(
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
    val saldoParcial: Double? = null
)

internal fun construirExtracto(movimientos: List<Movimiento>): List<LineaExtracto> {
    val ordenados = movimientos.sortedBy { it.fecha.toDate() }
    val cargos = ordenados.filter { it.tipo == "cargo" && !it.anulado }
    val pagos = ordenados.filter { it.tipo == "pago" && !it.anulado }
    val anulados = ordenados.filter { it.anulado }

    data class LineaRaw(val fecha: Date, val concepto: String, val monto: Double, val movimientoId: String?, val esPago: Boolean, val items: List<Movimiento> = emptyList(), val anotadoPor: String = "", val tieneAnulados: Boolean = false, val visitante: String = "")

    val lineasRaw = mutableListOf<LineaRaw>()

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

    val anuladosConGrupo = anulados.filter { it.grupoId.isNotBlank() }
    val anuladosSinGrupo = anulados.filter { it.grupoId.isBlank() }
    anuladosConGrupo.groupBy { it.grupoId }.values.forEach { grupo ->
        val sorted = grupo.sortedBy { it.fecha.toDate() }
        val prods = sorted.map { it.producto.ifBlank { if (it.tipo == "pago") "Pago" else "Fiado" } }
        val concepto = when {
            prods.size == 1 -> prods[0]
            prods.size == 2 -> "${prods[0]}, ${prods[1]}"
            else -> "${prods[0]} · ${prods.size} ítems"
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
internal fun FilaExtracto(linea: LineaExtracto, vm: DuenoViewModel, clienteId: String) {
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tipoColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(tipoIcon, null, Modifier.size(18.dp), tint = tipoColor)
            }

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
                        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Icon(Icons.Default.Person, null, Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaMuted))
                        Text(
                            linea.visitante,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaMuted)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (linea.esPago) "−${"%.0f".format(-linea.monto)}"
                    else "+${"%.0f".format(linea.monto)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = montoColor,
                    textDecoration = if (tachado) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                if (linea.saldoParcial != null) {
                    Text(
                        "Saldo ${"%.0f".format(linea.saldoParcial)}",
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
                            "${if (mov.tipo == "cargo") "+" else "−"}${"%.0f".format(mov.monto)}",
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

                if (!linea.esPago && !linea.tieneAnulados) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total visita", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "+${"%.0f".format(linea.monto)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (linea.pagado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                }

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

    if (confirmarAnularTodo) {
        AlertDialog(
            onDismissRequest = { confirmarAnularTodo = false },
            title = { Text("Anular visita") },
            text = { Text("Se anulará toda esta visita y se descontará del saldo del cliente.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarAnularTodo = false
                    mostrarDetalle = false
                    linea.items.forEach { mov ->
                        if (!mov.anulado) {
                            vm.anularMovimiento(mov.id, clienteId, mov.monto, mov.tipo, "Anulado por el dueño")
                        }
                    }
                }) { Text("Anular", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarAnularTodo = false }) { Text("Cancelar") }
            }
        )
    }

    itemAAnular?.let { mov ->
        AlertDialog(
            onDismissRequest = { itemAAnular = null },
            title = { Text("Anular ítem") },
            text = { Text("Se anulará \"${mov.producto.ifBlank { "Fiado" }}\" (${"%.0f".format(mov.monto)}) y se descontará del saldo.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.anularMovimiento(mov.id, clienteId, mov.monto, mov.tipo, "Anulado por el dueño")
                    itemAAnular = null
                }) { Text("Anular", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { itemAAnular = null }) { Text("Cancelar") }
            }
        )
    }
}
