package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Producto

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
    val deudaActual = clientes.find { it.id == clienteId }?.deudaTotal ?: 0.0

    var productoSeleccionado by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(tipoInicial) }

    val montoDouble = monto.toDoubleOrNull() ?: 0.0
    val deudaFinal = if (tipo == "cargo") deudaActual + montoDouble else deudaActual - montoDouble

    val productosHabilitados = productos.filter { it.habilitado }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tipo == "cargo") "Agregar fiado" else "Registrar pago") },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 16.dp)) {
            // Tabs Fiado / Pago
            Row {
                FilterChip(selected = tipo == "cargo", onClick = { tipo = "cargo" },
                    label = { Text("Fiado") }, modifier = Modifier.padding(end = 8.dp))
                FilterChip(selected = tipo == "pago", onClick = { tipo = "pago" },
                    label = { Text("Pago") })
            }
            Spacer(Modifier.height(16.dp))

            // Chips de productos (solo en fiado)
            if (tipo == "cargo" && productosHabilitados.isNotEmpty()) {
                Text("Producto", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(productosHabilitados, key = { it.id }) { prod ->
                        FilterChip(
                            selected = productoSeleccionado == prod.id,
                            onClick = {
                                if (productoSeleccionado == prod.id) {
                                    productoSeleccionado = ""
                                    monto = ""
                                } else {
                                    productoSeleccionado = prod.id
                                    monto = "%.2f".format(prod.precio)
                                }
                            },
                            label = { Text("${prod.nombre} $${"%.0f".format(prod.precio)}") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Monto
            OutlinedTextField(
                value = monto,
                onValueChange = {
                    monto = it
                    productoSeleccionado = ""
                },
                label = { Text("Monto $") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            // Preview deuda
            if (montoDouble > 0) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Va a quedar debiendo", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$${"%.2f".format(deudaFinal)}",
                            fontWeight = FontWeight.Bold,
                            color = if (deudaFinal > 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val nombreProd = productosHabilitados.find { it.id == productoSeleccionado }?.nombre ?: ""
                    vm.agregarMovimiento(clienteId, nombreProd, 1, montoDouble, tipo, onGuardado)
                },
                enabled = !cargando && montoDouble > 0,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (tipo == "cargo") "Agregar fiado" else "Registrar pago")
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
    var productoSeleccionado by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("cargo") }

    val montoDouble = monto.toDoubleOrNull() ?: 0.0
    val deudaFinal = if (tipo == "cargo") deudaActual + montoDouble else deudaActual - montoDouble
    val productosHabilitados = productos.filter { it.habilitado }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tipo == "cargo") "Agregar fiado" else "Registrar pago") },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 16.dp)) {
            // Tabs: siempre disponibles Fiado y Pago
            Row {
                FilterChip(selected = tipo == "cargo", onClick = { tipo = "cargo" },
                    label = { Text("Fiado") }, modifier = Modifier.padding(end = 8.dp))
                FilterChip(selected = tipo == "pago", onClick = { tipo = "pago" },
                    label = { Text("Pago") })
            }
            Spacer(Modifier.height(16.dp))

            // Chips de productos
            if (tipo == "cargo" && productosHabilitados.isNotEmpty()) {
                Text("Producto", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(productosHabilitados, key = { it.id }) { prod ->
                        FilterChip(
                            selected = productoSeleccionado == prod.id,
                            onClick = {
                                if (productoSeleccionado == prod.id) {
                                    productoSeleccionado = ""
                                    monto = ""
                                } else {
                                    productoSeleccionado = prod.id
                                    monto = "%.2f".format(prod.precio)
                                }
                            },
                            label = { Text("${prod.nombre} $${"%.0f".format(prod.precio)}") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it; productoSeleccionado = "" },
                label = { Text("Monto $") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            // Header con info del cliente
            if (nombreCliente.isNotBlank() || deudaActual > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (nombreCliente.isNotBlank()) {
                            Text(nombreCliente, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Deuda actual: $${"%.2f".format(deudaActual)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (limiteCredito > 0) {
                            val porcentaje = (deudaActual / limiteCredito).toFloat().coerceIn(0f, 1f)
                            val colorBarra = when {
                                deudaActual > limiteCredito -> MaterialTheme.colorScheme.error
                                porcentaje >= 0.8f -> androidx.compose.ui.graphics.Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            LinearProgressIndicator(
                                progress = { porcentaje },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = colorBarra,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                            Text(
                                "Límite: $${"%.0f".format(limiteCredito)}" +
                                    if (deudaActual > limiteCredito) " · EXCEDIDO" else " · ${"%.0f".format(porcentaje * 100)}% usado",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (deudaActual > limiteCredito) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (montoDouble > 0) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Va a quedar debiendo", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "$${"%.2f".format(deudaFinal)}",
                                fontWeight = FontWeight.Bold,
                                color = if (deudaFinal > 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                            )
                        }
                        if (limiteCredito > 0 && tipo == "cargo" && deudaFinal > limiteCredito) {
                            Spacer(Modifier.height(4.dp))
                            Text("⚠ Va a superar el límite de crédito",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        if (nombreEmpleado.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (tipo == "cargo") "Anotado por: $nombreEmpleado"
                                else "Pago recibido por: $nombreEmpleado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val nombreProd = productosHabilitados.find { it.id == productoSeleccionado }?.nombre ?: ""
                    onAgregar(nombreProd, 1, montoDouble, tipo)
                },
                enabled = !cargando && montoDouble > 0,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (tipo == "cargo") "Agregar fiado" else "Registrar pago")
            }
        }
    }
}
