package com.kosking.app.dueno

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun DialogInvitarEmpleado(
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
