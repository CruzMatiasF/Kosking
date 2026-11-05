package com.kosking.app.dueno.detalle

import com.kosking.app.modelo.Cliente
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date

@Composable
internal fun DialogEditarNota(
    notaActual: String,
    onGuardar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nota by remember { mutableStateOf(notaActual) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nota interna") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Solo visible para vos y tus empleados al anotar. Útil para indicaciones especiales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota") },
                    placeholder = { Text("Ej: No anotarle a los hijos") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(nota.trim()) }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (notaActual.isNotBlank()) {
                    TextButton(onClick = { onGuardar("") }) {
                        Text("Borrar nota", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DialogEditarLimite(
    limiteActual: Double,
    onGuardar: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var valor by remember { mutableStateOf(if (limiteActual > 0) "%.0f".format(limiteActual) else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Límite de crédito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "El cliente no podrá superar este monto de deuda. Dejá vacío para no tener límite.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto máximo $") },
                    placeholder = { Text("Sin límite") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(valor.toDoubleOrNull() ?: 0.0) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DialogEditarFechaPago(
    fechaActual: Date?,
    onGuardar: (Date?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = (fechaActual ?: run {
            java.util.Calendar.getInstance().apply { add(java.util.Calendar.WEEK_OF_YEAR, 1) }.time
        }).time
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedMs = datePickerState.selectedDateMillis
                onGuardar(if (selectedMs != null) Date(selectedMs) else null)
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (fechaActual != null) {
                    TextButton(onClick = { onGuardar(null) }) { Text("Quitar fecha", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DialogEditarCliente(
    cliente: Cliente,
    onGuardar: (Cliente) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(cliente.nombre) }
    var telefono by remember { mutableStateOf(cliente.telefono) }
    var domicilio by remember { mutableStateOf(cliente.domicilio) }
    var habilitado by remember { mutableStateOf(cliente.habilitado) }
    var campoActivo by remember { mutableStateOf<String?>(null) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    campoActivo?.let { campo ->
        val valorActual = when (campo) {
            "nombre" -> nombre
            "telefono" -> telefono
            else -> domicilio
        }
        var draft by remember(campo) { mutableStateOf(valorActual) }
        AlertDialog(
            onDismissRequest = { campoActivo = null },
            title = { Text(when (campo) { "nombre" -> "Nombre"; "telefono" -> "Teléfono"; else -> "Domicilio" }) },
            text = {
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it },
                    singleLine = campo != "domicilio",
                    keyboardOptions = if (campo == "telefono") KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (campo) { "nombre" -> nombre = draft.trim(); "telefono" -> telefono = draft.trim(); else -> domicilio = draft.trim() }
                        campoActivo = null
                    },
                    enabled = campo != "nombre" || draft.isNotBlank()
                ) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { campoActivo = null }) { Text("Cancelar") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Editar cliente")
                Text(
                    cliente.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "DATOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )

                CampoEditable(
                    icon = Icons.Default.Person,
                    label = "Nombre",
                    value = nombre.ifBlank { "—" },
                    onClick = { campoActivo = "nombre" }
                )
                CampoEditable(
                    icon = Icons.Default.Phone,
                    label = "Teléfono",
                    value = telefono.ifBlank { "Sin teléfono" },
                    vacio = telefono.isBlank(),
                    onClick = { campoActivo = "telefono" }
                )
                CampoEditable(
                    icon = Icons.Default.Home,
                    label = "Domicilio",
                    value = domicilio.ifBlank { "Sin domicilio" },
                    vacio = domicilio.isBlank(),
                    onClick = { campoActivo = "domicilio" }
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "ACCESO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Group, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column {
                                Text("Habilitado para anotar", style = MaterialTheme.typography.bodyMedium)
                                if (habilitado) {
                                    Text(
                                        "Empleados pueden anotar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Switch(checked = habilitado, onCheckedChange = { habilitado = it })
                    }
                }

                if (cliente.codigoAcceso.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Key, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Código de acceso · solo lectura",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    cliente.codigoAcceso,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(cliente.codigoAcceso)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, "Copiar", Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGuardar(cliente.copy(
                        nombre = nombre.trim(),
                        telefono = telefono.trim(),
                        domicilio = domicilio.trim(),
                        habilitado = habilitado
                    ))
                },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
internal fun CampoEditable(
    icon: ImageVector,
    label: String,
    value: String,
    vacio: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (vacio) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}
