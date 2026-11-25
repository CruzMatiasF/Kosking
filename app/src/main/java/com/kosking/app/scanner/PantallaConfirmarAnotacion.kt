package com.kosking.app.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.ItemAnotado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmarSheet(
    cliente: Cliente?,
    items: List<ItemAnotado>,
    tipo: String,
    cargando: Boolean,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    val total = items.sumOf { it.precio * it.cantidad }
    val esCargo = tipo == "cargo"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Encabezado: tipo + cliente
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    if (esCargo) Icons.Default.Person else Icons.Default.ShoppingCart,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (esCargo) "Anotar en cuenta" else "Registrar venta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (cliente != null) {
                        Text(
                            cliente.nombre,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (cliente.deudaTotal > 0) {
                            Text(
                                "Debe \$${cliente.deudaTotal.toInt()} · quedará \$${(cliente.deudaTotal + total).toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    "\$${total.toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            // Lista de productos (sin scroll propio — el sheet scrollea)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            if (item.cantidad > 1) {
                                Text(
                                    "x${item.cantidad}  ·  \$${item.precio.toInt()} c/u",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "\$${(item.precio * item.cantidad).toInt()}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (item != items.last()) HorizontalDivider(thickness = 0.5.dp)
                }
            }

            HorizontalDivider()

            // Botón confirmar
            Button(
                onClick = onConfirmar,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (esCargo) "Anotar \$${total.toInt()} a ${cliente?.nombre ?: ""}"
                    else "Confirmar venta \$${total.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}
