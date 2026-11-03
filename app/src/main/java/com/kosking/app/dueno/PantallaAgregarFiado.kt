package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosking.app.modelo.Producto

private data class ItemCarrito(
    val productoId: String,
    val nombre: String,
    val precioUnitario: Double,
    var cantidad: Int = 1
) {
    val total: Double get() = precioUnitario * cantidad
}

/** Wrapper fino para el flujo de dueño — lee del ViewModel y delega al composable unificado. */
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

    PantallaAgregarFiadoUI(
        tipoInicial = tipoInicial,
        onVolver = onVolver,
        productos = productos,
        cargando = cargando,
        error = error,
        deudaActual = cliente?.deudaTotal ?: 0.0,
        limiteCredito = cliente?.limiteCredito ?: 0.0,
        nombreCliente = cliente?.nombre ?: "",
        notaInterna = cliente?.notaInterna ?: "",
        visitantesConocidos = cliente?.visitantesConocidos ?: emptyList(),
        mostrarVisitante = true,
        onConfirmar = { items, tipo, visitante ->
            vm.agregarMovimientos(clienteId, items, tipo, visitante, onGuardado)
        }
    )
}

/** Versión para el flujo de empleado — recibe datos y callback desde Navegacion. */
@Composable
fun PantallaAgregarFiadoConLambda(
    onVolver: () -> Unit,
    onConfirmar: (items: List<Triple<String, Int, Double>>, tipo: String, visitante: String) -> Unit,
    productos: List<Producto> = emptyList(),
    cargando: Boolean = false,
    error: String? = null,
    deudaActual: Double = 0.0,
    limiteCredito: Double = 0.0,
    nombreCliente: String = "",
    nombreEmpleado: String = "",
    visitantesConocidos: List<String> = emptyList()
) {
    PantallaAgregarFiadoUI(
        onVolver = onVolver,
        productos = productos,
        cargando = cargando,
        error = error,
        deudaActual = deudaActual,
        limiteCredito = limiteCredito,
        nombreCliente = nombreCliente,
        nombreEmpleado = nombreEmpleado,
        visitantesConocidos = visitantesConocidos,
        mostrarVisitante = visitantesConocidos.isNotEmpty(),
        onConfirmar = { items, tipo, visitante -> onConfirmar(items, tipo, visitante) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantallaAgregarFiadoUI(
    tipoInicial: String = "cargo",
    onVolver: (() -> Unit)? = null,
    productos: List<Producto> = emptyList(),
    cargando: Boolean = false,
    error: String? = null,
    deudaActual: Double = 0.0,
    limiteCredito: Double = 0.0,
    nombreCliente: String = "",
    nombreEmpleado: String = "",
    notaInterna: String = "",
    visitantesConocidos: List<String> = emptyList(),
    mostrarVisitante: Boolean = false,
    onConfirmar: (items: List<Triple<String, Int, Double>>, tipo: String, visitante: String) -> Unit
) {
    val tipo = tipoInicial
    val carrito = remember { mutableStateListOf<ItemCarrito>() }
    var montoPago by remember { mutableStateOf("") }
    var visitante by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }

    val productosHabilitados = remember(productos) { productos.filter { it.habilitado } }
    val productosFiltrados = remember(productosHabilitados, busqueda) {
        if (busqueda.isBlank()) productosHabilitados
        else productosHabilitados.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }
    val totalCarrito = carrito.sumOf { it.total }
    val montoFinal = if (tipo == "cargo") totalCarrito else montoPago.toDoubleOrNull() ?: 0.0
    val deudaFinal = if (tipo == "cargo") deudaActual + totalCarrito
                     else deudaActual - (montoPago.toDoubleOrNull() ?: 0.0)
    val superaLimite = limiteCredito > 0 && tipo == "cargo" && deudaFinal > limiteCredito

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
                },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Barra inferior con resumen + botón confirmar
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (montoFinal > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Va a quedar debiendo", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$${"%.0f".format(deudaFinal.coerceAtLeast(0.0))}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (deudaFinal > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                        }
                        if (superaLimite) {
                            Text("⚠ Va a superar el límite de crédito",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    val habilitado = !cargando && !superaLimite && montoFinal > 0
                    Button(
                        onClick = {
                            val items = if (tipo == "cargo")
                                carrito.map { Triple(it.nombre, it.cantidad, it.total) }
                            else
                                listOf(Triple("", 1, montoPago.toDoubleOrNull() ?: 0.0))
                            onConfirmar(items, tipo, visitante.trim())
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
                }
            }
        }
    ) { padding ->
        if (tipo == "pago") {
            // ── Pantalla de pago ──────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (notaInterna.isNotBlank()) {
                    NotaInternaCard(notaInterna)
                }
                if (deudaActual > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Deuda actual", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${"%.0f".format(deudaActual)}", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    }
                    HorizontalDivider()
                }
                Text("Monto pagado", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = montoPago,
                    onValueChange = { montoPago = it },
                    placeholder = { Text("0", fontSize = 28.sp) },
                    prefix = { Text("$", fontSize = 28.sp, fontWeight = FontWeight.SemiBold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (nombreEmpleado.isNotBlank()) {
                    Text("Recibido por: $nombreEmpleado", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // ── Pantalla de fiado ─────────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (notaInterna.isNotBlank()) {
                    NotaInternaCard(notaInterna, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                // Buscador
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Search, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            Box {
                                if (busqueda.isEmpty()) Text("Buscar producto...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                        }
                    )
                    if (busqueda.isNotEmpty()) {
                        Icon(Icons.Default.Close, "Limpiar",
                            Modifier.size(16.dp).clickable { busqueda = "" },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Carrito (si hay items)
                if (carrito.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(carrito, key = { it.productoId }) { item ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val idx = carrito.indexOfFirst { it.productoId == item.productoId }
                                            if (item.cantidad > 1) carrito[idx] = item.copy(cantidad = item.cantidad - 1)
                                            else carrito.removeAt(idx)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) { Icon(Icons.Default.Remove, null, Modifier.size(12.dp)) }
                                    Text("${item.nombre} ×${item.cantidad}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("$${"%.0f".format(item.total)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    IconButton(
                                        onClick = {
                                            val idx = carrito.indexOfFirst { it.productoId == item.productoId }
                                            carrito[idx] = item.copy(cantidad = item.cantidad + 1)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) { Icon(Icons.Default.Add, null, Modifier.size(12.dp)) }
                                }
                            }
                        }
                    }
                }

                // Grilla de productos
                if (productosFiltrados.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (productosHabilitados.isEmpty()) "No hay productos cargados"
                            else "Sin resultados para \"$busqueda\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(productosFiltrados, key = { it.id }) { prod ->
                            val itemEnCarrito = carrito.find { it.productoId == prod.id }
                            val enCarrito = itemEnCarrito != null
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val idx = carrito.indexOfFirst { it.productoId == prod.id }
                                        if (idx >= 0) carrito[idx] = carrito[idx].copy(cantidad = carrito[idx].cantidad + 1)
                                        else carrito.add(ItemCarrito(prod.id, prod.nombre, prod.precio))
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (enCarrito) MaterialTheme.colorScheme.primaryContainer
                                                     else MaterialTheme.colorScheme.surface
                                ),
                                border = if (enCarrito)
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else
                                    androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(prod.nombre, style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            color = if (enCarrito) MaterialTheme.colorScheme.onPrimaryContainer
                                                    else MaterialTheme.colorScheme.onSurface)
                                        Text("$${"%.0f".format(prod.precio)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (enCarrito) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (enCarrito) {
                                        Badge(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) { Text("×${itemEnCarrito!!.cantidad}") }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }

                // Quién vino
                if (mostrarVisitante) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("¿Quién vino? (opcional)", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (visitantesConocidos.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(visitantesConocidos) { conocido ->
                                    FilterChip(
                                        selected = visitante == conocido,
                                        onClick = { visitante = if (visitante == conocido) "" else conocido },
                                        label = { Text(conocido) },
                                        leadingIcon = if (visitante == conocido) {
                                            { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = visitante,
                            onValueChange = { visitante = it },
                            label = { Text("Nombre de quien vino") },
                            placeholder = { Text("Ej: María, la hija") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = if (visitante.isNotBlank()) {
                                { IconButton(onClick = { visitante = "" }) { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)) } }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotaInternaCard(nota: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = Color(0xFFF57F17))
            Column {
                Text("Nota del cliente", style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF57F17), fontWeight = FontWeight.SemiBold)
                Text(nota, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037))
            }
        }
    }
}
