package com.kosking.app.dueno

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import com.kosking.app.modelo.Empleado
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEmpleados(
    onVolver: () -> Unit,
    vm: DuenoViewModel = viewModel()
) {
    val empleados by vm.empleados.collectAsState()
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    var mostrarDialogNombre by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<Pair<String, String>?>(null) } // codigo, whatsapp
    var empleadoAEliminar by remember { mutableStateOf<Empleado?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Empleados") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogNombre = true }) {
                Icon(Icons.Default.Add, "Generar código")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(empleados) { emp ->
                ListItem(
                    headlineContent = { Text(emp.nombre) },
                    trailingContent = {
                        IconButton(onClick = { empleadoAEliminar = emp }) {
                            Icon(Icons.Default.Delete, "Eliminar",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (mostrarDialogNombre) {
        DialogNombreEmpleado(
            cargando = cargando,
            error = error,
            onGuardar = { nombre, whatsapp ->
                vm.generarCodigoEmpleado(nombre) { codigo ->
                    mostrarDialogNombre = false
                    codigoGenerado = codigo to whatsapp
                }
            },
            onDismiss = { mostrarDialogNombre = false }
        )
    }

    empleadoAEliminar?.let { emp ->
        AlertDialog(
            onDismissRequest = { empleadoAEliminar = null },
            title = { Text("Eliminar empleado") },
            text = { Text("¿Eliminar a ${emp.nombre}? Se va a desconectar del local.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminarEmpleado(emp.id)
                    empleadoAEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { empleadoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    codigoGenerado?.let { (codigo, whatsapp) ->
        AlertDialog(
            onDismissRequest = { codigoGenerado = null },
            title = { Text("Código generado") },
            text = {
                Column {
                    Text("Compartí este código con el empleado:")
                    Spacer(Modifier.height(12.dp))
                    Text(
                        codigo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "El empleado lo ingresa en la app para acceder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                if (whatsapp.isNotBlank()) {
                    TextButton(onClick = {
                        val numero = whatsapp.replace(Regex("[^0-9]"), "")
                        val mensaje = "Tu código de empleado para Kosking es: $codigo — Descargá la app y usá este código para conectarte al local."
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        codigoGenerado = null
                    }) { Text("Enviar por WhatsApp") }
                } else {
                    TextButton(onClick = { codigoGenerado = null }) { Text("Listo") }
                }
            },
            dismissButton = if (whatsapp.isNotBlank()) {
                { TextButton(onClick = { codigoGenerado = null }) { Text("Cerrar") } }
            } else null
        )
    }
}

@Composable
internal fun DialogNombreEmpleado(
    cargando: Boolean,
    error: String?,
    onGuardar: (nombre: String, whatsapp: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo empleado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del empleado") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp (opcional)") },
                    placeholder = { Text("Ej: 5491112345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(nombre, whatsapp) },
                enabled = !cargando && nombre.isNotBlank()
            ) { Text("Generar código") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
