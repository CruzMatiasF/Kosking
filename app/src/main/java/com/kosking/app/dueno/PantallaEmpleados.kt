package com.kosking.app.dueno

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
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
    val nombreLocal by vm.nombreLocal.collectAsState()
    var mostrarDialogInvitar by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<String?>(null) }
    var empleadoAEliminar by remember { mutableStateOf<Empleado?>(null) }
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogInvitar = true }) {
                Icon(Icons.Default.Add, "Agregar empleado")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(empleados) { emp ->
                ListItem(
                    headlineContent = { Text(emp.nombre) },
                    trailingContent = {
                        IconButton(onClick = { empleadoAEliminar = emp }) {
                            Icon(Icons.Default.Delete, "Desvincular",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (mostrarDialogInvitar) {
        DialogInvitarEmpleado(
            cargando = cargando,
            error = error,
            onGenerar = { whatsapp, email ->
                vm.generarCodigoEmpleado { codigo ->
                    mostrarDialogInvitar = false
                    val local = nombreLocal.ifBlank { "Kosking" }
                    val mensaje = "👋 Te invitan a unirte como empleado a *$local* en Kosking.\n\nSeguí estos pasos:\n1️⃣ Descargá la app Kosking desde Play Store\n2️⃣ Registrate o iniciá sesión\n3️⃣ Tocá \"Soy empleado\" e ingresá el código:\n\n*$codigo*\n\nEl código es de un solo uso."
                    if (whatsapp.isNotBlank()) {
                        val numero = whatsapp.replace(Regex("[^0-9]"), "")
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
                            })
                        } catch (_: Exception) {}
                    } else if (email.isNotBlank()) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                                putExtra(Intent.EXTRA_SUBJECT, "Tu código de empleado - Kosking")
                                putExtra(Intent.EXTRA_TEXT, mensaje)
                            })
                        } catch (_: Exception) {}
                    } else {
                        codigoGenerado = codigo
                    }
                }
            },
            onDismiss = { mostrarDialogInvitar = false }
        )
    }

    codigoGenerado?.let { codigo ->
        AlertDialog(
            onDismissRequest = { codigoGenerado = null },
            title = { Text("Código de invitación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Compartí este código con el empleado:")
                    Text(
                        codigo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "El empleado lo ingresa en la app para conectarse al local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { codigoGenerado = null }) { Text("Listo") }
            }
        )
    }

    empleadoAEliminar?.let { emp ->
        AlertDialog(
            onDismissRequest = { empleadoAEliminar = null },
            title = { Text("Desvincular empleado") },
            text = { Text("¿Desvincular a ${emp.nombre} del local? Dejará de tener acceso.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminarEmpleado(emp.id)
                    empleadoAEliminar = null
                }) { Text("Desvincular", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { empleadoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
internal fun DialogInvitarEmpleado(
    cargando: Boolean,
    error: String?,
    onGenerar: (whatsapp: String, email: String) -> Unit,
    onDismiss: () -> Unit
) {
    var usaWhatsapp by remember { mutableStateOf(true) }
    var whatsapp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val contacto = if (usaWhatsapp) whatsapp else email

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invitar empleado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Se genera un código para que el empleado se una al local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = usaWhatsapp,
                        onClick = { usaWhatsapp = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("WhatsApp") }
                    )
                    SegmentedButton(
                        selected = !usaWhatsapp,
                        onClick = { usaWhatsapp = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Email") }
                    )
                }
                if (usaWhatsapp) {
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("Número de WhatsApp") },
                        placeholder = { Text("Ej: 5491112345678") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("empleado@email.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGenerar(whatsapp, email) },
                enabled = !cargando
            ) {
                Text(
                    when {
                        contacto.isNotBlank() && usaWhatsapp -> "Enviar por WhatsApp"
                        contacto.isNotBlank() -> "Enviar por email"
                        else -> "Generar código"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
