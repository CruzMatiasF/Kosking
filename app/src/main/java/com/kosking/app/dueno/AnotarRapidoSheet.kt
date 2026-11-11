package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Producto

private data class ItemCarrito(val producto: Producto, val cantidad: Int)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnotarRapidoSheet(
    clientes: List<Cliente>,
    cargando: Boolean,
    clienteInicial: Cliente? = null,
    productos: List<Producto> = emptyList(),
    onGuardar: (clienteId: String, items: List<Pair<String, Double>>, tipo: String) -> Unit,
    onDismiss: () -> Unit
) {
    val clientesOrdenados = remember(clientes) {
        clientes.sortedByDescending { it.deudaTotal }
    }

    var clienteSeleccionado by remember(clienteInicial) {
        mutableStateOf(clienteInicial ?: clientesOrdenados.firstOrNull())
    }
    var expandirClientes by remember { mutableStateOf(false) }
    var esCargo by remember { mutableStateOf(true) }

    // Carrito de productos (modo fiado)
    val carrito = remember { mutableStateListOf<ItemCarrito>() }

    // Entrada manual "Otro"
    var descripcionOtro by remember { mutableStateOf("") }
    var montoOtroTexto by remember { mutableStateOf("") }
    var mostrarOtro by remember { mutableStateOf(false) }

    // Monto único para pago
    var montoTexto by remember { mutableStateOf("") }

    // Limpiar carrito al cambiar a pago
    LaunchedEffect(esCargo) {
        if (!esCargo) {
            carrito.clear()
            mostrarOtro = false
            descripcionOtro = ""
            montoOtroTexto = ""
        }
    }

    fun agregarAlCarrito(prod: Producto) {
        val idx = carrito.indexOfFirst { it.producto.id == prod.id }
        if (idx >= 0) {
            carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad + 1)
        } else {
            carrito.add(ItemCarrito(prod, 1))
        }
    }

    fun quitarDelCarrito(prod: Producto) {
        val idx = carrito.indexOfFirst { it.producto.id == prod.id }
        if (idx >= 0) {
            if (carrito[idx].cantidad > 1) {
                carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad - 1)
            } else {
                carrito.removeAt(idx)
            }
        }
    }

    val montoOtroDouble = montoOtroTexto.toDoubleOrNull() ?: 0.0
    val montoUnicoDouble = montoTexto.toDoubleOrNull() ?: 0.0

    val totalCarrito = carrito.sumOf { it.producto.ultimoPrecio * it.cantidad }
    val totalConOtro = totalCarrito + (if (mostrarOtro && montoOtroDouble > 0) montoOtroDouble else 0.0)

    val deudaActual = clienteSeleccionado?.deudaTotal ?: 0.0
    val deudaFinal = when {
        esCargo -> deudaActual + totalConOtro
        else -> (deudaActual - montoUnicoDouble).coerceAtLeast(0.0)
    }

    val puedeGuardar = clienteSeleccionado != null && !cargando && when {
        esCargo -> totalConOtro > 0
        else -> montoUnicoDouble > 0
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            if (clienteInicial != null) {
                Column {
                    Text(
                        clienteInicial.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (deudaActual > 0) {
                        Text(
                            "Debe $${"%.0f".format(deudaActual)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "Sin deuda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text("Anotar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            // Nota interna
            val nota = clienteSeleccionado?.notaInterna.orEmpty()
            if (nota.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = Color(0xFFF57F17))
                        Text(nota, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037))
                    }
                }
            }

            // Toggle Fiado / Pago
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = esCargo,
                    onClick = { esCargo = true },
                    label = { Text("Fiado") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !esCargo,
                    onClick = { esCargo = false },
                    label = { Text("Pago") },
                    leadingIcon = { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (esCargo) {
                // ── Grilla de productos ───────────────────────────────────────
                if (productos.isNotEmpty()) {
                    Text(
                        "¿Qué llevó?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        productos.take(10).forEach { prod ->
                            val enCarrito = carrito.find { it.producto.id == prod.id }
                            val cantidad = enCarrito?.cantidad ?: 0
                            FilterChip(
                                selected = cantidad > 0,
                                onClick = { agregarAlCarrito(prod) },
                                label = {
                                    Text(
                                        if (prod.ultimoPrecio > 0)
                                            "${prod.nombre} · ${"$"}${"%.0f".format(prod.ultimoPrecio)}"
                                        else prod.nombre,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                trailingIcon = if (cantidad > 0) ({
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) { Text("$cantidad", style = MaterialTheme.typography.labelSmall) }
                                }) else null
                            )
                        }
                        FilterChip(
                            selected = mostrarOtro,
                            onClick = { mostrarOtro = true },
                            label = { Text("Otro") },
                            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(14.dp)) }
                        )
                    }
                }

                // ── Carrito ───────────────────────────────────────────────────
                if (carrito.isNotEmpty()) {
                    HorizontalDivider()
                    carrito.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.producto.nombre,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { quitarDelCarrito(item.producto) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, null, Modifier.size(14.dp))
                                }
                                Text(
                                    "${item.cantidad}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.widthIn(min = 20.dp)
                                )
                                IconButton(
                                    onClick = { agregarAlCarrito(item.producto) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                                }
                                Text(
                                    "${"$"}${"%.0f".format(item.producto.ultimoPrecio * item.cantidad)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.widthIn(min = 56.dp)
                                )
                            }
                        }
                    }
                }

                // ── Entrada manual "Otro" ─────────────────────────────────────
                if (mostrarOtro || productos.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = descripcionOtro,
                            onValueChange = { descripcionOtro = it },
                            label = { Text("Descripción") },
                            placeholder = { Text("Ej: pan") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = montoOtroTexto,
                            onValueChange = { montoOtroTexto = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("$") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.width(90.dp)
                        )
                        if (mostrarOtro) {
                            IconButton(onClick = {
                                mostrarOtro = false
                                descripcionOtro = ""
                                montoOtroTexto = ""
                            }) {
                                Icon(Icons.Default.Clear, null, Modifier.size(16.dp))
                            }
                        }
                    }
                }

            } else {
                // ── Modo pago: campo monto simple ─────────────────────────────
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { montoTexto = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto $") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Selector de cliente
            if (clienteInicial == null) {
                ExposedDropdownMenuBox(
                    expanded = expandirClientes,
                    onExpandedChange = { expandirClientes = it }
                ) {
                    OutlinedTextField(
                        value = clienteSeleccionado?.nombre ?: "Seleccioná un cliente",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cliente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirClientes) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandirClientes,
                        onDismissRequest = { expandirClientes = false }
                    ) {
                        clientesOrdenados.forEach { cliente ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cliente.nombre)
                                        if (cliente.deudaTotal > 0) {
                                            Text(
                                                "$${"%.0f".format(cliente.deudaTotal)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                },
                                onClick = { clienteSeleccionado = cliente; expandirClientes = false }
                            )
                        }
                    }
                }
            }

            // Total + deuda proyectada
            if (esCargo && totalConOtro > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${"$"}${"%.0f".format(totalConOtro)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (deudaActual > 0) {
                            Text(
                                "→ ${"$"}${"%.0f".format(deudaFinal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Botón guardar
            Button(
                onClick = {
                    val cliente = clienteSeleccionado ?: return@Button
                    val items = buildList {
                        carrito.forEach { item -> add(Pair(item.producto.nombre, item.producto.ultimoPrecio * item.cantidad)) }
                        if (mostrarOtro && montoOtroDouble > 0) add(Pair(descripcionOtro.trim(), montoOtroDouble))
                        if (!esCargo) add(Pair("", montoUnicoDouble))
                    }
                    onGuardar(cliente.id, items, if (esCargo) "cargo" else "pago")
                },
                enabled = puedeGuardar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    val monto = if (esCargo) totalConOtro else montoUnicoDouble
                    val label = if (monto > 0)
                        "${if (esCargo) "Registrar fiado" else "Registrar pago"} ${"$"}${"%.0f".format(monto)}"
                    else
                        if (esCargo) "Registrar fiado" else "Registrar pago"
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
