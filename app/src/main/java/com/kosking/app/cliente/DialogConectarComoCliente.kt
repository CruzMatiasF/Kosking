package com.kosking.app.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DialogConectarComoCliente(
    cargando: Boolean,
    error: String?,
    onConectar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var codigo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conectarme como cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ingresá el código que te dio el local",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.uppercase() },
                    placeholder = { Text("Ej: KSK-ABCD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConectar(codigo.trim()) },
                enabled = !cargando && codigo.isNotBlank()
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Conectar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
