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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RegistroClienteSheet(
    nombreInicial: String,
    telefonoInicial: String,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    cargando: Boolean,
    onCrear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nuevo cliente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = nombreInicial,
                onValueChange = onNombreChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            OutlinedTextField(
                value = telefonoInicial,
                onValueChange = onTelefonoChange,
                label = { Text("Teléfono (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Button(
                onClick = onCrear,
                enabled = !cargando && nombreInicial.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (cargando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Crear y seleccionar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
