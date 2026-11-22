package com.kosking.app.scanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (mostrarCrear) {
                            Surface(
                                onClick = { onNuevoCliente(busquedaCliente.trim()) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        "\"${busquedaCliente.trim()}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            sugs.forEach { c ->
                                Surface(
                                    onClick = { onClienteSelected(c) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                c.nombre.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Text(
                                            c.nombre,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
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
