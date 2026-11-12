package com.kosking.app.dueno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Producto

private data class ItemCarrito(val producto: Producto, val cantidad: Int)

@Composable
private fun FilaProductoAnotar(
    prod: Producto,
    cantidad: Int,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                prod.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (cantidad > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (prod.ultimoPrecio > 0) {
                Text(
                    "$${"%.0f".format(prod.ultimoPrecio)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (cantidad > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (cantidad > 0) {
            IconButton(onClick = onQuitar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
            }
            Text(
                cantidad.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Center
            )
        }
        IconButton(
            onClick = onAgregar,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, "Agregar", Modifier.size(18.dp))
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnotarRapidoSheet(
    clientes: List<Cliente>,
    cargando: Boolean,
    clienteInicial: Cliente? = null,
    productos: List<Producto> = emptyList(),
    tipoInicial: String = "cargo",
    onGuardar: (clienteId: String, items: List<Pair<String, Double>>, tipo: String) -> Unit,
    onDismiss: () -> Unit
) {
    val clientesOrdenados = remember(clientes) {
        clientes.sortedByDescending { it.deudaTotal }
    }

    var clienteSeleccionado by remember(clienteInicial) {
        mutableStateOf(clienteInicial ?: clientesOrdenados.firstOrNull())
    }
    var esCargo by remember { mutableStateOf(tipoInicial == "cargo") }
    var pedirConfirmCambio by remember { mutableStateOf(false) }

    val carrito = remember { mutableStateListOf<ItemCarrito>() }

    // Búsqueda de cliente
    var busquedaCliente by remember { mutableStateOf("") }
    var mostrarListaClientes by remember { mutableStateOf(false) }
    val clientesFiltrados = remember(clientesOrdenados, busquedaCliente) {
        if (busquedaCliente.isBlank()) clientesOrdenados
        else clientesOrdenados.filter { it.nombre.contains(busquedaCliente, ignoreCase = true) }
    }

    // Búsqueda de productos
    var busquedaProducto by remember { mutableStateOf("") }
    val productosFiltrados = remember(productos, busquedaProducto) {
        val base = productos.sortedByDescending { it.vecesUsado }
        if (busquedaProducto.isBlank()) base
        else base.filter { it.nombre.contains(busquedaProducto, ignoreCase = true) }
    }

    // Entrada manual "Otro"
    var descripcionOtro by remember { mutableStateOf("") }
    var montoOtroTexto by remember { mutableStateOf("") }
    var mostrarOtro by remember { mutableStateOf(productos.isEmpty()) }

    // Monto único para pago
    var montoTexto by remember { mutableStateOf("") }

    fun limpiarCarrito() {
        carrito.clear()
        mostrarOtro = false
        descripcionOtro = ""
        montoOtroTexto = ""
    }

    fun toggleCarrito(prod: Producto) {
        val idx = carrito.indexOfFirst { it.producto.id == prod.id }
        if (idx >= 0) {
            // tap en seleccionado: incrementar si ya hay, o quitar si es 1
            // usamos long-press para quitar — pero como no hay long-press en chip, tap agrega siempre
            // y el ícono × en el resumen quita
            carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad + 1)
        } else {
            carrito.add(ItemCarrito(prod, 1))
        }
    }

    fun quitarUno(prod: Producto) {
        val idx = carrito.indexOfFirst { it.producto.id == prod.id }
        if (idx < 0) return
        if (carrito[idx].cantidad > 1) {
            carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad - 1)
        } else {
            carrito.removeAt(idx)
        }
    }

    val montoOtroDouble = montoOtroTexto.toDoubleOrNull() ?: 0.0
    val montoUnicoDouble = montoTexto.toDoubleOrNull() ?: 0.0
    val totalCarrito = carrito.sumOf { it.producto.ultimoPrecio * it.cantidad }
    val totalConOtro = totalCarrito + (if (mostrarOtro && montoOtroDouble > 0) montoOtroDouble else 0.0)

    val deudaActual = clienteSeleccionado?.deudaTotal ?: 0.0
    val limiteCredito = clienteSeleccionado?.limiteCredito ?: 0.0
    val deudaFinal = when {
        esCargo -> deudaActual + totalConOtro
        else -> (deudaActual - montoUnicoDouble).coerceAtLeast(0.0)
    }
    val superaLimite = esCargo && limiteCredito > 0 && deudaFinal > limiteCredito

    val puedeGuardar = clienteSeleccionado != null && !cargando && !superaLimite && when {
        esCargo -> totalConOtro > 0
        else -> montoUnicoDouble > 0
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    if (mostrarListaClientes) { mostrarListaClientes = false; busquedaCliente = "" }
                },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            if (clienteInicial != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            clienteInicial.nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (deudaActual > 0) "Debe $${"%.0f".format(deudaActual)}" else "Sin deuda",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (deudaActual > 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Deuda proyectada al lado del nombre
                    if (esCargo && totalConOtro > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "→ $${"%.0f".format(deudaFinal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "quedaría debiendo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text("Anotar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            // ── Nota interna ─────────────────────────────────────────────────
            val nota = clienteSeleccionado?.notaInterna.orEmpty()
            if (nota.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Text(nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }

            // ── Selector de cliente con búsqueda ─────────────────────────────
            if (clienteInicial == null) {
                Column {
                    OutlinedTextField(
                        value = if (mostrarListaClientes) busquedaCliente else clienteSeleccionado?.nombre ?: "",
                        onValueChange = { busquedaCliente = it; mostrarListaClientes = true },
                        label = { Text("Cliente") },
                        placeholder = { Text("Buscar cliente...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (mostrarListaClientes && busquedaCliente.isNotEmpty()) {
                                IconButton(onClick = { busquedaCliente = ""; mostrarListaClientes = false }) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarListaClientes = true }
                    )
                    if (mostrarListaClientes && clientesFiltrados.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                items(clientesFiltrados, key = { it.id }) { cliente ->
                                    ListItem(
                                        headlineContent = { Text(cliente.nombre, style = MaterialTheme.typography.bodyMedium) },
                                        trailingContent = if (cliente.deudaTotal > 0) ({
                                            Text(
                                                "$${"%.0f".format(cliente.deudaTotal)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }) else null,
                                        modifier = Modifier.clickable {
                                            clienteSeleccionado = cliente
                                            busquedaCliente = ""
                                            mostrarListaClientes = false
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }

            // ── Toggle Fiado / Pago ──────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = esCargo,
                    onClick = {
                        mostrarListaClientes = false
                        if (!esCargo) {
                            // volver a Fiado no limpia nada
                            esCargo = true
                        }
                    },
                    label = { Text("Fiado") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !esCargo,
                    onClick = {
                        mostrarListaClientes = false
                        if (esCargo) {
                            val hayItems = carrito.isNotEmpty() || (mostrarOtro && montoOtroTexto.isNotBlank())
                            if (hayItems) pedirConfirmCambio = true
                            else { limpiarCarrito(); esCargo = false }
                        }
                    },
                    label = { Text("Pago") },
                    leadingIcon = { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (pedirConfirmCambio) {
                AlertDialog(
                    onDismissRequest = { pedirConfirmCambio = false },
                    title = { Text("¿Cambiar a Pago?") },
                    text = { Text("Vas a perder los items que ya cargaste. ¿Continuás?") },
                    confirmButton = {
                        TextButton(onClick = { limpiarCarrito(); esCargo = false; pedirConfirmCambio = false }) {
                            Text("Cambiar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pedirConfirmCambio = false }) { Text("Cancelar") }
                    }
                )
            }

            if (esCargo) {
                // ── Buscador de productos ────────────────────────────────────
                OutlinedTextField(
                    value = busquedaProducto,
                    onValueChange = { busquedaProducto = it },
                    label = { Text("Buscar producto") },
                    placeholder = { Text("Ej: coca, pan, arroz...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (busquedaProducto.isNotEmpty()) {
                            IconButton(onClick = { busquedaProducto = "" }) {
                                Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Lista de productos ───────────────────────────────────────
                if (productos.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                            if (productosFiltrados.isEmpty()) {
                                item {
                                    Text(
                                        "Sin resultados — usá \"Otro\" para agregar manual",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(productosFiltrados, key = { it.id }) { prod ->
                                    val cantidad = carrito.find { it.producto.id == prod.id }?.cantidad ?: 0
                                    FilaProductoAnotar(
                                        prod = prod,
                                        cantidad = cantidad,
                                        onAgregar = { toggleCarrito(prod) },
                                        onQuitar = { quitarUno(prod) }
                                    )
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { mostrarOtro = !mostrarOtro }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        "No está en la lista — agregar manual",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Entrada manual "Otro" ────────────────────────────────────
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
                        if (productos.isNotEmpty()) {
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

                // ── Resumen del total ────────────────────────────────────────
                if (totalConOtro > 0) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lista compacta de lo que se lleva
                        Column(modifier = Modifier.weight(1f)) {
                            carrito.forEach { item ->
                                Text(
                                    "${item.producto.nombre}${if (item.cantidad > 1) " ×${item.cantidad}" else ""}  $${"%.0f".format(item.producto.ultimoPrecio * item.cantidad)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (mostrarOtro && montoOtroDouble > 0) {
                                val desc = descripcionOtro.ifBlank { "Otro" }
                                Text(
                                    "$desc  $${"%.0f".format(montoOtroDouble)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "$${"%.0f".format(totalConOtro)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

            } else {
                // ── Modo pago ────────────────────────────────────────────────
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { montoTexto = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto $") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // ── Aviso límite de crédito ──────────────────────────────────────
            if (superaLimite) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "⚠ Supera el límite de crédito (${"$"}${"%.0f".format(limiteCredito)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // ── Botón guardar ────────────────────────────────────────────────
            Button(
                onClick = {
                    val cliente = clienteSeleccionado ?: return@Button
                    val items = buildList {
                        carrito.forEach { item ->
                            add(Pair(item.producto.nombre, item.producto.ultimoPrecio * item.cantidad))
                        }
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
