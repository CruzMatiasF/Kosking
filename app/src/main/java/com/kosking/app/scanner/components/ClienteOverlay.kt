package com.kosking.app.scanner.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente

@Composable
internal fun ClienteOverlay(
    clienteSeleccionado: Cliente?,
    busquedaCliente: String,
    mostrarBuscarCliente: Boolean,
    clientesOrdenados: List<Cliente>,
    filtradosCliente: List<Cliente>,
    focusCliente: FocusRequester,
    onClienteSelected: (Cliente) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onMostrarBuscar: () -> Unit,
    onLimpiarCliente: () -> Unit,
    onNuevoCliente: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sugerencias encima del campo
        if (mostrarBuscarCliente) {
            val sugs = if (busquedaCliente.isBlank()) clientesOrdenados.take(4) else filtradosCliente.take(4)
            val mostrarCrear = busquedaCliente.isNotBlank() && filtradosCliente.isEmpty()
            if (sugs.isNotEmpty() || mostrarCrear) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Column {
                        if (mostrarCrear) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        "Nuevo cliente: \"${busquedaCliente.trim()}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingContent = { Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.clickable { onNuevoCliente(busquedaCliente.trim()) }
                            )
                        } else {
                            sugs.forEach { c ->
                                ListItem(
                                    headlineContent = {
                                        Text(c.nombre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    },
                                    supportingContent = if (c.deudaTotal > 0) ({
                                        Text(
                                            "Debe $${c.deudaTotal.toInt()}",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }) else null,
                                    modifier = Modifier.clickable { onClienteSelected(c) }
                                )
                                if (c != sugs.last()) HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        // Chip o campo inline
        if (clienteSeleccionado != null && !mostrarBuscarCliente) {
            Surface(
                onClick = onMostrarBuscar,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Text(
                        clienteSeleccionado.nombre,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Icon(
                        Icons.Default.Close,
                        null,
                        modifier = Modifier.size(13.dp).clickable { onLimpiarCliente() },
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            BasicTextField(
                value = busquedaCliente,
                onValueChange = { onBusquedaChange(it) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusCliente),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                decorationBox = { inner ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.55f),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(15.dp), tint = Color.White)
                            Box(modifier = Modifier.weight(1f)) {
                                if (busquedaCliente.isEmpty()) {
                                    Text(
                                        "Cliente (opcional)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                inner()
                            }
                            if (busquedaCliente.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Close, null,
                                    modifier = Modifier.size(14.dp).clickable {
                                        onBusquedaChange("")
                                        onLimpiarCliente()
                                    },
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}
