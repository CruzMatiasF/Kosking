package com.kosking.app.cliente.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Producto

@Composable
internal fun TabProductosCliente(productos: List<Producto>) {
    LazyColumn {
        item {
            Text(
                "Precios actualizados por el negocio",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        items(productos, key = { it.id }) { prod ->
            ListItem(
                headlineContent = { Text(prod.nombre, fontWeight = FontWeight.Medium) },
                supportingContent = {
                    if (prod.descripcion.isNotBlank()) Text(prod.descripcion)
                },
                trailingContent = {
                    Column(horizontalAlignment = Alignment.End) {
                        if (prod.esOferta && prod.precioAnterior > 0) {
                            Text(
                                "$${"%.0f".format(prod.precioAnterior)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        }
                        Text(
                            "$${"%.0f".format(prod.precio)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (prod.esOferta) {
                            Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text("Oferta", color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
            )
            HorizontalDivider()
        }
    }
}
