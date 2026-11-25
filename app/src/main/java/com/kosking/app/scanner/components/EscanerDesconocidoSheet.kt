package com.kosking.app.scanner.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.scanner.ProductoDetectado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EscanerDesconocidoSheet(
    producto: ProductoDetectado,
    esEmpleado: Boolean = false,
    onAgregar: (nombre: String, precio: Double, guardarEnCatalogo: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember(producto) { mutableStateOf(producto.nombre) }
    var precioTexto by remember(producto) { mutableStateOf(if (producto.precio > 0) producto.precio.toInt().toString() else "") }
    var guardarEnCatalogo by remember { mutableStateOf(!esEmpleado) }

    val precioValido = precioTexto.toDoubleOrNull()?.let { it > 0 } == true

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Producto no encontrado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (producto.codigoBarras.isNotBlank()) {
                Text(
                    "Código: ${producto.codigoBarras}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del producto") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = precioTexto,
                onValueChange = { precioTexto = it },
                label = { Text("Precio") },
                singleLine = true,
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (!esEmpleado) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Guardar en catálogo",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = guardarEnCatalogo, onCheckedChange = { guardarEnCatalogo = it })
                }
            }
            Button(
                onClick = {
                    val precio = precioTexto.toDoubleOrNull() ?: return@Button
                    onAgregar(nombre, precio, guardarEnCatalogo)
                },
                enabled = precioValido,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar al carrito", fontWeight = FontWeight.Bold)
            }
        }
    }
}
