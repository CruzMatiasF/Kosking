package com.kosking.app.dueno

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Producto

internal data class ItemCarrito(val producto: Producto, val cantidad: Int)

@Composable
internal fun FilaProductoAnotar(
    prod: Producto,
    cantidad: Int,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit,
    onSetCantidad: ((Int) -> Unit)? = null
) {
    var editandoCantidad by remember { mutableStateOf(false) }
    var cantidadTexto by remember(cantidad) { mutableStateOf(cantidad.toString()) }

    var highlighted by remember { mutableStateOf(false) }
    val prevCantidad = remember { mutableIntStateOf(cantidad) }
    LaunchedEffect(cantidad) {
        if (cantidad > prevCantidad.intValue) {
            highlighted = true
            kotlinx.coroutines.delay(350)
            highlighted = false
        }
        prevCantidad.intValue = cantidad
    }
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val bgColor by animateColorAsState(
        targetValue = if (highlighted) highlightColor else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "highlight"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onAgregar() }
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
            } else if (cantidad > 0) {
                Text(
                    "sin precio — no suma al total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (cantidad > 0) {
            IconButton(onClick = onQuitar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
            }
            if (editandoCantidad && onSetCantidad != null) {
                fun confirmar() {
                    val n = cantidadTexto.toIntOrNull() ?: 0
                    onSetCantidad(if (n > 0) n else cantidad)
                    editandoCantidad = false
                }
                BasicTextField(
                    value = cantidadTexto,
                    onValueChange = { cantidadTexto = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirmar() }),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.width(36.dp),
                    decorationBox = { inner ->
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { inner() }
                    }
                )
            } else {
                Text(
                    cantidad.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(20.dp).clickable(enabled = onSetCantidad != null) {
                        cantidadTexto = cantidad.toString()
                        editandoCantidad = true
                    },
                    textAlign = TextAlign.Center
                )
            }
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

@Composable
internal fun ZonaCliente(
    clienteInicial: Cliente?,
    clienteSeleccionado: Cliente?,
    mostrarListaClientes: Boolean,
    busquedaCliente: String,
    clientesFiltrados: List<Cliente>,
    clienteFocusRequester: FocusRequester,
    esCargo: Boolean,
    deudaActual: Double,
    deudaFinal: Double,
    totalConOtro: Double,
    hayItemsEnCarrito: Boolean,
    montoOtroTexto: String,
    onClienteSeleccionado: (Cliente) -> Unit,
    onMostrarListaClientes: (Boolean) -> Unit,
    onBusquedaClienteChange: (String) -> Unit,
    onEsCargoChange: (Boolean) -> Unit,
    onPedirConfirmCambio: () -> Unit,
    limpiarCarrito: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Handle bar
        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(width = 32.dp, height = 4.dp)) {}
        }

        // Selector de cliente
        if (clienteInicial != null && clienteSeleccionado != null) {
            // Cliente fijo — mostrar como chip con deuda proyectada
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            clienteSeleccionado.nombre.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(clienteSeleccionado.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (esCargo && totalConOtro > 0) {
                        Text(
                            "Deuda: $${"%.0f".format(deudaActual)} → $${"%.0f".format(deudaFinal)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Deuda actual: $${"%.0f".format(deudaActual)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Selector con búsqueda
            OutlinedTextField(
                value = busquedaCliente,
                onValueChange = {
                    onBusquedaClienteChange(it)
                    onMostrarListaClientes(true)
                },
                placeholder = { Text("Buscar cliente…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (clienteSeleccionado != null) {
                        IconButton(onClick = { onMostrarListaClientes(!mostrarListaClientes) }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).focusRequester(clienteFocusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            if (mostrarListaClientes && clientesFiltrados.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                    items(clientesFiltrados) { cliente ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onClienteSeleccionado(cliente)
                                onMostrarListaClientes(false)
                                onBusquedaClienteChange("")
                            }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(cliente.nombre.take(1).uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cliente.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (cliente.deudaTotal > 0) {
                                    Text("Debe $${"%.0f".format(cliente.deudaTotal)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            // Chip del cliente seleccionado
            if (clienteSeleccionado != null && !mostrarListaClientes) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(clienteSeleccionado.nombre.take(1).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Text(clienteSeleccionado.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (esCargo && totalConOtro > 0) {
                        Text("→ $${"%.0f".format(deudaFinal)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Nota interna
        if (clienteSeleccionado?.notaInterna?.isNotBlank() == true) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(clienteSeleccionado.notaInterna, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        // Toggle Cargo / Pago
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = esCargo,
                onClick = {
                    if (!esCargo) {
                        if (hayItemsEnCarrito) onPedirConfirmCambio()
                        else { limpiarCarrito(); onEsCargoChange(true) }
                    }
                },
                label = { Text("Cargo") }
            )
            FilterChip(
                selected = !esCargo,
                onClick = {
                    if (esCargo) {
                        if (hayItemsEnCarrito) onPedirConfirmCambio()
                        else { limpiarCarrito(); onEsCargoChange(false) }
                    }
                },
                label = { Text("Pago") }
            )
        }
    }
}
