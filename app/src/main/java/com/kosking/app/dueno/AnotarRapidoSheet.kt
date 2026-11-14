package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Producto


@OptIn(ExperimentalMaterial3Api::class)
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

    var busquedaCliente by remember { mutableStateOf("") }
    var mostrarListaClientes by remember { mutableStateOf(false) }
    val clientesFiltrados = remember(clientesOrdenados, busquedaCliente) {
        if (busquedaCliente.isBlank()) clientesOrdenados
        else clientesOrdenados.filter { it.nombre.contains(busquedaCliente, ignoreCase = true) }
    }

    var busquedaProducto by remember { mutableStateOf("") }
    val productosFiltrados = remember(productos, busquedaProducto) {
        val base = productos.sortedByDescending { it.vecesUsado }
        if (busquedaProducto.isBlank()) base
        else base.filter { it.nombre.contains(busquedaProducto, ignoreCase = true) }
    }

    var descripcionOtro by remember { mutableStateOf("") }
    var montoOtroTexto by remember { mutableStateOf("") }
    var mostrarOtro by remember { mutableStateOf(productos.isEmpty()) }
    var montoTexto by remember { mutableStateOf("") }

    fun limpiarCarrito() {
        carrito.clear()
        mostrarOtro = false
        descripcionOtro = ""
        montoOtroTexto = ""
    }

    fun toggleCarrito(prod: Producto) {
        val idx = carrito.indexOfFirst { it.producto.id == prod.id }
        if (idx >= 0) carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad + 1)
        else carrito.add(ItemCarrito(prod, 1))
    }

    fun quitarUno(prod: Producto) {
        val idx = carrito.indexOfFirst { it.producto.id == prod.id }
        if (idx < 0) return
        if (carrito[idx].cantidad > 1) carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad - 1)
        else carrito.removeAt(idx)
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    if (mostrarListaClientes) { mostrarListaClientes = false; busquedaCliente = "" }
                }
        ) {
            // ── ZONA 1: Cliente + nota + toggle ─────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Handle manual
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(4.dp))

                // Selector cliente
                if (clienteInicial != null) {
                    // Cliente fijo: mostrar nombre + deuda proyectada
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Avatar
                            val avatarColors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary
                            )
                            val color = avatarColors[kotlin.math.abs(clienteInicial.nombre.hashCode()) % avatarColors.size]
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    clienteInicial.nombre.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column {
                                Text(clienteInicial.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (deudaActual > 0) "Debe $${"%.0f".format(deudaActual)}" else "Sin deuda",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (deudaActual > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (esCargo && totalConOtro > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "→ $${"%.0f".format(deudaFinal)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text("quedaría debiendo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    // Selector con búsqueda
                    Column {
                        OutlinedTextField(
                            value = if (mostrarListaClientes) busquedaCliente else clienteSeleccionado?.nombre ?: "",
                            onValueChange = { busquedaCliente = it; mostrarListaClientes = true },
                            label = { Text("Cliente") },
                            placeholder = { Text("Buscar cliente...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                            trailingIcon = {
                                Icon(
                                    if (mostrarListaClientes) Icons.Default.Close else Icons.Default.KeyboardArrowDown,
                                    null, Modifier.size(18.dp).clickable {
                                        if (mostrarListaClientes) { mostrarListaClientes = false; busquedaCliente = "" }
                                        else mostrarListaClientes = true
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable { mostrarListaClientes = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (mostrarListaClientes && clientesFiltrados.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                    items(clientesFiltrados, key = { it.id }) { cliente ->
                                        ListItem(
                                            headlineContent = { Text(cliente.nombre, style = MaterialTheme.typography.bodyMedium) },
                                            trailingContent = if (cliente.deudaTotal > 0) ({
                                                Text("$${"%.0f".format(cliente.deudaTotal)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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

                // Nota interna
                val nota = clienteSeleccionado?.notaInterna.orEmpty()
                if (nota.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Text(nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                // Toggle Fiado / Pago
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = esCargo,
                        onClick = { mostrarListaClientes = false; if (!esCargo) esCargo = true },
                        label = { Text("Cargo") },
                        leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !esCargo,
                        onClick = {
                            mostrarListaClientes = false
                            if (esCargo) {
                                val hayItems = carrito.isNotEmpty() || (mostrarOtro && montoOtroTexto.isNotBlank())
                                if (hayItems) pedirConfirmCambio = true else { limpiarCarrito(); esCargo = false }
                            }
                        },
                        label = { Text("Pago") },
                        leadingIcon = { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            // ── ZONA 2: Productos (ocupa todo el espacio disponible) ─────────
            if (esCargo) {
                if (productos.isEmpty()) {
                    // Sin productos — aviso centrado
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Todavía no cargaste productos", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            Text(
                                "Andá a la pestaña Productos y cargalos para anotarlos más rápido.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Búsqueda fija
                    OutlinedTextField(
                        value = busquedaProducto,
                        onValueChange = { busquedaProducto = it },
                        placeholder = { Text("Buscar producto...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (busquedaProducto.isNotEmpty()) {
                                IconButton(onClick = { busquedaProducto = "" }) { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )

                    // Lista scrolleable que llena el espacio
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (busquedaProducto.isBlank()) {
                            // Estado inicial — invitar a escribir
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Escribí para buscar un producto",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (productosFiltrados.isEmpty()) {
                            item {
                                Text(
                                    "Sin resultados para \"$busquedaProducto\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(productosFiltrados, key = { it.id }) { prod ->
                                val cantidad = carrito.find { it.producto.id == prod.id }?.cantidad ?: 0
                                FilaProductoAnotar(prod = prod, cantidad = cantidad, onAgregar = { toggleCarrito(prod) }, onQuitar = { quitarUno(prod) })
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { mostrarOtro = !mostrarOtro }.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("No está en la lista — agregar manual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else {
                // Modo pago — campo monto centrado
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    OutlinedTextField(
                        value = montoTexto,
                        onValueChange = { montoTexto = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto $") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            HorizontalDivider()

            // ── ZONA 3: Entrada manual + resumen + botón (siempre visible) ───
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Entrada manual "Otro"
                if (esCargo && (mostrarOtro || productos.isEmpty())) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = descripcionOtro,
                            onValueChange = { descripcionOtro = it },
                            label = { Text("Descripción") },
                            placeholder = { Text("Ej: pan") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = montoOtroTexto,
                            onValueChange = { montoOtroTexto = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("$") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(90.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (productos.isNotEmpty()) {
                            IconButton(onClick = { mostrarOtro = false; descripcionOtro = ""; montoOtroTexto = "" }) {
                                Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Aviso límite de crédito
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

                // Resumen compacto del carrito
                if (esCargo && totalConOtro > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column(modifier = Modifier.weight(1f)) {
                            carrito.forEach { item ->
                                Text(
                                    "${item.producto.nombre}${if (item.cantidad > 1) " ×${item.cantidad}" else ""}  $${"%.0f".format(item.producto.ultimoPrecio * item.cantidad)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (mostrarOtro && montoOtroDouble > 0) {
                                Text(
                                    "${descripcionOtro.ifBlank { "Otro" }}  $${"%.0f".format(montoOtroDouble)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "$${"%.0f".format(totalConOtro)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
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
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (cargando) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        val monto = if (esCargo) totalConOtro else montoUnicoDouble
                        val label = if (monto > 0)
                            "${if (esCargo) "Registrar" else "Registrar pago"} ${"$"}${"%.0f".format(monto)}"
                        else if (esCargo) "Registrar" else "Registrar pago"
                        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
