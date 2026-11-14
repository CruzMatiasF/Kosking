package com.kosking.app.dueno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Producto

internal data class ItemCarrito(val producto: Producto, val cantidad: Int)

@Composable
internal fun FilaProductoAnotar(
    prod: Producto,
    cantidad: Int,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAgregar() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                prod.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (cantidad > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (prod.ultimoPrecio > 0) {
                Text(
                    "$${"%.0f".format(prod.ultimoPrecio)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (cantidad > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (cantidad > 0) {
            IconButton(onClick = onQuitar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
            }
            Text(
                cantidad.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Center
            )
        }
        IconButton(
            onClick = onAgregar,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, "Agregar", Modifier.size(18.dp))
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
