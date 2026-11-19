package com.kosking.app.dueno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Producto

@Composable
internal fun ZonaProductos(
    productos: List<Producto>,
    busquedaProducto: String,
    productosFiltrados: List<Producto>,
    carrito: List<ItemCarrito>,
    mostrarOtro: Boolean,
    productosGlobales: List<String> = emptyList(),
    onBusquedaChange: (String) -> Unit,
    onToggleMostrarOtro: () -> Unit,
    onToggleCarrito: (Producto) -> Unit,
    onQuitarUno: (Producto) -> Unit,
    onSetCantidad: (Producto, Int) -> Unit,
    onUsarProductoGlobal: (Producto) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = busquedaProducto,
            onValueChange = onBusquedaChange,
            placeholder = { Text("Buscar producto…") },
            leadingIcon = { Icon(Icons.Default.Add, null) },
            trailingIcon = {
                if (busquedaProducto.isNotEmpty()) {
                    IconButton(onClick = { onBusquedaChange("") }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Search
            )
        )

        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
            items(productosFiltrados) { prod ->
                val cantidad = carrito.find { it.producto.id == prod.id }?.cantidad ?: 0
                FilaProductoAnotar(
                    prod = prod,
                    cantidad = cantidad,
                    onAgregar = { onToggleCarrito(prod) },
                    onQuitar = { onQuitarUno(prod) },
                    onSetCantidad = { n -> onSetCantidad(prod, n) }
                )
            }

            if (productosGlobales.isNotEmpty()) {
                item {
                    Text(
                        "TAMBIÉN EN OTROS LOCALES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                items(productosGlobales) { nombre ->
                    val prodTemp = Producto(id = "global_$nombre", nombre = nombre)
                    val cantidad = carrito.find { it.producto.id == prodTemp.id }?.cantidad ?: 0
                    FilaProductoAnotar(
                        prod = prodTemp,
                        cantidad = cantidad,
                        esGlobal = true,
                        onAgregar = { onUsarProductoGlobal(prodTemp) },
                        onQuitar = { onQuitarUno(prodTemp) },
                        onSetCantidad = { n -> onSetCantidad(prodTemp, n) }
                    )
                }
            }

            item {
                TextButton(
                    onClick = onToggleMostrarOtro,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (mostrarOtro) "Ocultar campo manual" else "No está en la lista — cargar manualmente")
                }
            }
        }
    }
}

@Composable
internal fun ZonaResumen(
    mostrarOtro: Boolean,
    descripcionOtro: String,
    montoOtroTexto: String,
    sugerenciasOtro: List<String>,
    superaLimite: Boolean,
    limiteCredito: Double,
    carrito: List<ItemCarrito>,
    montoOtroDouble: Double,
    totalConOtro: Double,
    pedirConfirmSinPrecio: Boolean,
    cargando: Boolean,
    puedeGuardar: Boolean,
    clienteSeleccionado: Cliente?,
    onDescripcionOtroChange: (String) -> Unit,
    onMontoOtroTextoChange: (String) -> Unit,
    onSugerenciaSelected: (String) -> Unit,
    onOcultarOtro: () -> Unit,
    onPedirConfirmSinPrecioChange: (Boolean) -> Unit,
    buildItems: () -> List<Triple<String, Double, Int>>,
    onGuardar: (clienteId: String, items: List<Triple<String, Double, Int>>, tipo: String) -> Unit
) {
    val hayProductosSinPrecio = carrito.any { it.producto.ultimoPrecio <= 0 }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {

        // Campo manual "Otro"
        if (mostrarOtro) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = descripcionOtro,
                        onValueChange = onDescripcionOtroChange,
                        placeholder = { Text("Descripción") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = montoOtroTexto,
                        onValueChange = onMontoOtroTextoChange,
                        placeholder = { Text("Monto") },
                        prefix = { Text("$") },
                        singleLine = true,
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                    )
                    IconButton(onClick = onOcultarOtro) {
                        Icon(Icons.Default.Close, "Quitar")
                    }
                }
                if (sugerenciasOtro.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            sugerenciasOtro.forEach { sug ->
                                Text(
                                    sug,
                                    modifier = Modifier.fillMaxWidth().clickable { onSugerenciaSelected(sug) }.padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        // Warning: supera límite de crédito
        if (superaLimite) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        "Supera el límite de crédito ($${"%.0f".format(limiteCredito)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Warning: productos sin precio
        if (hayProductosSinPrecio) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        "Algunos productos no tienen precio y no suman al total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Resumen del carrito
        if (carrito.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                carrito.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("${item.cantidad}× ${item.producto.nombre}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        if (item.producto.ultimoPrecio > 0) {
                            Text("$${"%.0f".format(item.producto.ultimoPrecio * item.cantidad)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        } else {
                            Text("sin precio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (mostrarOtro && montoOtroDouble > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(descripcionOtro.ifBlank { "Otro" }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("$${"%.0f".format(montoOtroDouble)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Total", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("$${"%.0f".format(totalConOtro)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Diálogo confirmación sin precio
        if (pedirConfirmSinPrecio) {
            AlertDialog(
                onDismissRequest = { onPedirConfirmSinPrecioChange(false) },
                title = { Text("Productos sin precio") },
                text = { Text("Hay productos sin precio asignado. Se guardarán pero no sumarán al total de la deuda. ¿Continuar igual?") },
                confirmButton = {
                    TextButton(onClick = {
                        onPedirConfirmSinPrecioChange(false)
                        clienteSeleccionado?.let { onGuardar(it.id, buildItems(), "cargo") }
                    }) { Text("Guardar igual") }
                },
                dismissButton = {
                    TextButton(onClick = { onPedirConfirmSinPrecioChange(false) }) { Text("Cancelar") }
                }
            )
        }

        // Botón guardar
        val haptic = LocalHapticFeedback.current
        var exito by remember { mutableStateOf(false) }
        val prevCargando = remember { mutableStateOf(false) }
        LaunchedEffect(cargando) {
            if (prevCargando.value && !cargando) {
                exito = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                kotlinx.coroutines.delay(600)
                exito = false
            }
            prevCargando.value = cargando
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (hayProductosSinPrecio) {
                    onPedirConfirmSinPrecioChange(true)
                } else {
                    clienteSeleccionado?.let { onGuardar(it.id, buildItems(), "cargo") }
                }
            },
            enabled = puedeGuardar,
            colors = if (exito) ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2E7D32))
                     else ButtonDefaults.buttonColors(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp)
        ) {
            when {
                exito -> Icon(Icons.Default.Check, null, Modifier.size(22.dp))
                cargando -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else -> Text(
                    if (totalConOtro > 0) "Guardar cargo $${"%.0f".format(totalConOtro)}" else "Guardar",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
