package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun ZonaPago(
    deudaActual: Double,
    montoUnicoTexto: String,
    montoUnicoDouble: Double,
    deudaFinal: Double,
    cargando: Boolean,
    puedeGuardar: Boolean,
    onMontoChange: (String) -> Unit,
    onGuardar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        if (deudaActual > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Deuda actual", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$${"%.0f".format(deudaActual)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            "¿Cuánto pagó?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = montoUnicoTexto,
            onValueChange = { onMontoChange(it.filter { c -> c.isDigit() || c == '.' }) },
            placeholder = { Text("0") },
            prefix = { Text("$ ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (deudaActual > 0) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = { onMontoChange("%.0f".format(deudaActual)) },
                    label = { Text("Todo ($${"%.0f".format(deudaActual)})") }
                )
            }
        }

        if (montoUnicoDouble > 0 && deudaActual > 0) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Deuda restante", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$${"%.0f".format(deudaFinal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (deudaFinal <= 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onGuardar,
            enabled = puedeGuardar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp)
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    if (montoUnicoDouble > 0) "Registrar pago $${"%.0f".format(montoUnicoDouble)}"
                    else "Registrar pago",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
