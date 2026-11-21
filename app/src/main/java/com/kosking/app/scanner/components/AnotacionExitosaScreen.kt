package com.kosking.app.scanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.ItemAnotado

@Composable
internal fun AnotacionExitosaScreen(
    cliente: Cliente,
    items: List<ItemAnotado>,
    onEnviarWhatsApp: () -> Unit,
    onCerrar: () -> Unit
) {
    val totalCantidad = items.sumOf { it.cantidad }
    val totalPrecio = items.sumOf { it.precio * it.cantidad }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Anotado para ${cliente.nombre}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "$totalCantidad producto${if (totalCantidad != 1) "s" else ""}  ·  $${totalPrecio.toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onEnviarWhatsApp,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Enviar ticket por WhatsApp", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onCerrar, modifier = Modifier.fillMaxWidth()) {
            Text("Listo, sin enviar")
        }
    }
}
