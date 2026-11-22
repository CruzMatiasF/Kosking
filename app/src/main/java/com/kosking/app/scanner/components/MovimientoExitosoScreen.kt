package com.kosking.app.scanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.ItemAnotado

@Composable
fun MovimientoExitosoScreen(
    titulo: String,
    items: List<ItemAnotado>,
    onInicio: () -> Unit,
    onCerrar: () -> Unit,
    onWhatsApp: (() -> Unit)? = null
) {
    val total = items.sumOf { it.precio * it.cantidad }
    val totalCantidad = items.sumOf { it.cantidad }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IconButton(
            onClick = onCerrar,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .statusBarsPadding()
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$totalCantidad producto${if (totalCantidad != 1) "s" else ""}  ·  $${"%.0f".format(total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(40.dp))
            if (onWhatsApp != null) {
                Button(
                    onClick = onWhatsApp,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar ticket por WhatsApp", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onInicio,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Inicio", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onInicio,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Inicio", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
