package com.kosking.app.scanner.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.scanner.ProductoDetectado

@Composable
internal fun EscanerCargarPanel(
    productoParaCargar: ProductoDetectado?,
    nombreCarga: String,
    precioCarga: String,
    cargandoVm: Boolean,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onEscanearOtro: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (productoParaCargar == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Escaneá el código del producto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            if (productoParaCargar.nombre.isBlank()) {
                OutlinedTextField(
                    value = nombreCarga,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre del producto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            } else {
                Text(productoParaCargar.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = precioCarga,
                onValueChange = onPrecioChange,
                label = { Text("Precio en tu local") },
                prefix = { Text("$") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Button(
                onClick = onGuardar,
                enabled = !cargandoVm && (productoParaCargar.nombre.isNotBlank() || nombreCarga.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (cargandoVm) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Guardar al local", fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onEscanearOtro, modifier = Modifier.fillMaxWidth()) {
                Text("Escanear otro")
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.navigationBarsPadding())
    }
}
