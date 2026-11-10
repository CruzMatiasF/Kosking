package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnotarRapidoSheet(
    clientes: List<Cliente>,
    productos: List<Producto>,
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Anotar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

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

            // Selector de cliente
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

            // Monto — teclado numérico, auto-focused
            OutlinedTextField(
                value = montoTexto,
                onValueChange = { montoTexto = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Monto $") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            // Descripción (opcional)
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Botón guardar
            Button(
                onClick = {
                    val monto = montoTexto.toDoubleOrNull() ?: return@Button
                    val cliente = clienteSeleccionado ?: return@Button
                    onGuardar(
                        cliente.id,
                        descripcion.trim(),
                        monto,
                        if (esCargo) "cargo" else "pago"
                    )
                },
                enabled = !cargando && montoTexto.toDoubleOrNull() != null
                        && montoTexto.toDoubleOrNull()!! > 0
                        && clienteSeleccionado != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (cargando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        if (esCargo) "Registrar fiado" else "Registrar pago",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
