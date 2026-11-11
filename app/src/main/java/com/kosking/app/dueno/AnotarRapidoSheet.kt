package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnotarRapidoSheet(
    clientes: List<Cliente>,
    cargando: Boolean,
    clienteInicial: Cliente? = null,
    onGuardar: (clienteId: String, producto: String, monto: Double, tipo: String) -> Unit,
    onDismiss: () -> Unit
) {
    val clientesOrdenados = remember(clientes) {
        clientes.sortedByDescending { it.deudaTotal }
    }

    var clienteSeleccionado by remember(clienteInicial) {
        mutableStateOf(clienteInicial ?: clientesOrdenados.firstOrNull())
    }
    var expandirClientes by remember { mutableStateOf(false) }
    var montoTexto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var esCargo by remember { mutableStateOf(true) }
    val montoFocusRequester = remember { FocusRequester() }
    val descripcionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        montoFocusRequester.requestFocus()
    }

    val montoDouble = montoTexto.toDoubleOrNull() ?: 0.0
    val deudaActual = clienteSeleccionado?.deudaTotal ?: 0.0
    val deudaFinal = if (esCargo) deudaActual + montoDouble else (deudaActual - montoDouble).coerceAtLeast(0.0)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: nombre cliente si viene prefijado, sino título genérico
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
                Text(
                    "Anotar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Nota interna del cliente seleccionado
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

            // Selector de cliente — solo si no viene prefijado
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
                                onClick = {
                                    clienteSeleccionado = cliente
                                    expandirClientes = false
                                }
                            )
                        }
                    }
                }
            }

            // Monto
            OutlinedTextField(
                value = montoTexto,
                onValueChange = { montoTexto = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Monto $") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { descripcionFocusRequester.requestFocus() }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(montoFocusRequester),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                suffix = if (montoDouble > 0 && deudaActual > 0) ({
                    Text(
                        if (esCargo) "→ $${"%,.0f".format(deudaFinal)}"
                        else "→ $${"%,.0f".format(deudaFinal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (esCargo) MaterialTheme.colorScheme.error
                                else Color(0xFF388E3C)
                    )
                }) else null
            )

            // Descripción (opcional, solo para fiado)
            if (esCargo) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    placeholder = { Text("Ej: pan, leche...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().focusRequester(descripcionFocusRequester)
                )
            }

            // Botón guardar con monto en tiempo real
            Button(
                onClick = {
                    val cliente = clienteSeleccionado ?: return@Button
                    onGuardar(
                        cliente.id,
                        descripcion.trim(),
                        montoDouble,
                        if (esCargo) "cargo" else "pago"
                    )
                },
                enabled = !cargando && montoDouble > 0 && clienteSeleccionado != null,
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
                    val label = if (montoDouble > 0)
                        "${if (esCargo) "Registrar fiado" else "Registrar pago"} $${"%,.0f".format(montoDouble)}"
                    else
                        if (esCargo) "Registrar fiado" else "Registrar pago"
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
