package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Producto

// Item del carrito: producto (puede ser "") + monto + cantidad
private data class ItemCarrito(
    val productoId: String,
    val nombre: String,
    val precioUnitario: Double,
    var cantidad: Int = 1
) {
    val total: Double get() = precioUnitario * cantidad
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarFiado(
    clienteId: String,
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    vm: DuenoViewModel,
    tipoInicial: String = "cargo"
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    val productos by vm.productos.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val cliente = clientes.find { it.id == clienteId }
    val deudaActual = cliente?.deudaTotal ?: 0.0

    var tipo by remember { mutableStateOf(tipoInicial) }
    val carrito = remember { mutableStateListOf<ItemCarrito>() }

    // Para pagos: monto libre
    var montoPago by remember { mutableStateOf("") }

    val productosHabilitados = remember(productos) { productos.filter { it.habilitado } }
    val totalCarrito = carrito.sumOf { it.total }
    val deudaFinal = if (tipo == "cargo") deudaActual + totalCarrito
                     else deudaActual - (montoPago.toDoubleOrNull() ?: 0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (tipo == "cargo") "Anotar fiado" else "Registrar pago")
                        if (cliente != null)
                            Text(cliente.nombre, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tipo: Fiado / Pago
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipo == "cargo",
                        onClick = { tipo = "cargo"; carrito.clear(); montoPago = "" },
                        label = { Text("Fiado") }
                    )
                    FilterChip(
                        selected = tipo == "pago",
                        onClick = { tipo = "pago"; carrito.clear(); montoPago = "" },
                        label = { Text("Pago") }
                    )
                }
            }

            if (tipo == "cargo") {
                // Chips de productos
                if (productosHabilitados.isNotEmpty()) {
                    item {
                        Text("Productos", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(productosHabilitados, key = { it.id }) { prod ->
                                val enCarrito = carrito.any { it.productoId == prod.id }
                                FilterChip(
                                    selected = enCarrito,
                                    onClick = {
                                        if (enCarrito) {
                                            carrito.removeAll { it.productoId == prod.id }
                                        } else {
                                            carrito.add(ItemCarrito(prod.id, prod.nombre, prod.precio))
                                        }
                                    },
                                    label = { Text("${prod.nombre} $${"%.0f".format(prod.precio)}") },
                                    leadingIcon = if (enCarrito) {
                                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Carrito
                if (carrito.isNotEmpty()) {
                    item {
                        Text("Carrito", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(carrito, key = { it.productoId }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.nombre, fontWeight = FontWeight.Medium)
                                    Text("$${"%.0f".format(item.precioUnitario)} c/u",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                // Controles de cantidad
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val idx = carrito.indexOf(item)
                                            if (item.cantidad > 1) carrito[idx] = item.copy(cantidad = item.cantidad - 1)
                                            else carrito.remove(item)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) }
                                    Text(
                                        "${item.cantidad}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(28.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = {
                                            val idx = carrito.indexOf(item)
                                            carrito[idx] = item.copy(cantidad = item.cantidad + 1)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "$${"%.0f".format(item.total)}",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Pago: monto libre
                item {
                    OutlinedTextField(
                        value = montoPago,
                        onValueChange = { montoPago = it },
                        label = { Text("Monto pagado $") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Resumen de deuda
            val montoTotal = if (tipo == "cargo") totalCarrito else montoPago.toDoubleOrNull() ?: 0.0
            if (montoTotal > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (tipo == "cargo") {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total a anotar", style = MaterialTheme.typography.bodyMedium)
                                    Text("$${"%.2f".format(montoTotal)}", fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Va a quedar debiendo", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "$${"%.2f".format(deudaFinal.coerceAtLeast(0.0))}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (deudaFinal > 0) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            error?.let {
                item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

            // Botón confirmar
            item {
                val habilitado = !cargando && (
                    (tipo == "cargo" && carrito.isNotEmpty()) ||
                    (tipo == "pago" && (montoPago.toDoubleOrNull() ?: 0.0) > 0)
                )
                Button(
                    onClick = {
                        if (tipo == "cargo") {
                            val items = carrito.map { item -> item.nombre to item.total }
                            vm.agregarMovimientos(clienteId, items, "cargo", onGuardado)
                        } else {
                            vm.agregarMovimiento(clienteId, "", 1, montoPago.toDouble(), "pago", onGuardado)
                        }
                    },
                    enabled = habilitado,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(
                        if (tipo == "cargo") "Confirmar fiado $${"%.0f".format(totalCarrito)}"
                        else "Registrar pago $${"%.0f".format(montoPago.toDoubleOrNull() ?: 0.0)}"
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarFiadoConLambda(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    onAgregar: (producto: String, cantidad: Int, monto: Double, tipo: String) -> Unit,
    productos: List<Producto> = emptyList(),
    cargando: Boolean = false,
    error: String? = null,
    deudaActual: Double = 0.0,
    limiteCredito: Double = 0.0,
    nombreCliente: String = "",
    nombreEmpleado: String = ""
) {
    val carrito = remember { mutableStateListOf<ItemCarrito>() }
    var tipo by remember { mutableStateOf("cargo") }
    var montoPago by remember { mutableStateOf("") }

    val productosHabilitados = remember(productos) { productos.filter { it.habilitado } }
    val totalCarrito = carrito.sumOf { it.total }
    val montoTotal = if (tipo == "cargo") totalCarrito else montoPago.toDoubleOrNull() ?: 0.0
    val deudaFinal = if (tipo == "cargo") deudaActual + totalCarrito
                     else deudaActual - (montoPago.toDoubleOrNull() ?: 0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (tipo == "cargo") "Anotar fiado" else "Registrar pago")
                        if (nombreCliente.isNotBlank())
                            Text(nombreCliente, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tipo == "cargo", onClick = { tipo = "cargo"; carrito.clear(); montoPago = "" }, label = { Text("Fiado") })
                    FilterChip(selected = tipo == "pago", onClick = { tipo = "pago"; carrito.clear(); montoPago = "" }, label = { Text("Pago") })
                }
            }

            if (tipo == "cargo" && productosHabilitados.isNotEmpty()) {
                item {
                    Text("Productos", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(productosHabilitados, key = { it.id }) { prod ->
                            val enCarrito = carrito.any { it.productoId == prod.id }
                            FilterChip(
                                selected = enCarrito,
                                onClick = {
                                    if (enCarrito) carrito.removeAll { it.productoId == prod.id }
                                    else carrito.add(ItemCarrito(prod.id, prod.nombre, prod.precio))
                                },
                                label = { Text("${prod.nombre} $${"%.0f".format(prod.precio)}") },
                                leadingIcon = if (enCarrito) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                            )
                        }
                    }
                }
            }

            if (tipo == "cargo" && carrito.isNotEmpty()) {
                items(carrito, key = { it.productoId }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.nombre, fontWeight = FontWeight.Medium)
                                Text("$${"%.0f".format(item.precioUnitario)} c/u",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val idx = carrito.indexOf(item)
                                    if (item.cantidad > 1) carrito[idx] = item.copy(cantidad = item.cantidad - 1)
                                    else carrito.remove(item)
                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) }
                                Text("${item.cantidad}", style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                IconButton(onClick = {
                                    val idx = carrito.indexOf(item)
                                    carrito[idx] = item.copy(cantidad = item.cantidad + 1)
                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
                                Spacer(Modifier.width(8.dp))
                                Text("$${"%.0f".format(item.total)}", fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            if (tipo == "pago") {
                item {
                    OutlinedTextField(
                        value = montoPago, onValueChange = { montoPago = it },
                        label = { Text("Monto pagado $") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (montoTotal > 0) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (tipo == "cargo") {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total a anotar")
                                    Text("$${"%.2f".format(montoTotal)}", fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Va a quedar debiendo")
                                Text("$${"%.2f".format(deudaFinal.coerceAtLeast(0.0))}", fontWeight = FontWeight.Bold,
                                    color = if (deudaFinal > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            }
                            if (limiteCredito > 0 && tipo == "cargo" && deudaFinal > limiteCredito) {
                                Text("⚠ Va a superar el límite de crédito",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            if (nombreEmpleado.isNotBlank()) {
                                Text(if (tipo == "cargo") "Anotado por: $nombreEmpleado" else "Pago recibido por: $nombreEmpleado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }

            item {
                val habilitado = !cargando && (
                    (tipo == "cargo" && carrito.isNotEmpty()) ||
                    (tipo == "pago" && (montoPago.toDoubleOrNull() ?: 0.0) > 0)
                )
                Button(
                    onClick = {
                        if (tipo == "cargo") {
                            carrito.forEach { item ->
                                onAgregar(item.nombre, item.cantidad, item.total, "cargo")
                            }
                        } else {
                            onAgregar("", 1, montoPago.toDouble(), "pago")
                        }
                    },
                    enabled = habilitado,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(if (tipo == "cargo") "Confirmar fiado $${"%.0f".format(totalCarrito)}"
                              else "Registrar pago $${"%.0f".format(montoPago.toDoubleOrNull() ?: 0.0)}")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
