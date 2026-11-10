package com.kosking.app.dueno.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Publicacion
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun TabPublicaciones(
    publicaciones: List<Publicacion>,
    cargando: Boolean = false,
    onAgregar: (texto: String, etiqueta: String) -> Unit,
    onEliminar: (String) -> Unit
) {
    var mostrarDialog by remember { mutableStateOf(false) }

    if (mostrarDialog) {
        DialogNuevaPublicacion(
            onConfirmar = { texto, etiqueta ->
                onAgregar(texto, etiqueta)
                mostrarDialog = false
            },
            onDismiss = { mostrarDialog = false }
        )
    }

    Box(Modifier.fillMaxSize()) {
        if (publicaciones.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Campaign, null, Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Sin publicaciones todavía",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Publicá avisos, promos y novedades que aparecen en tu vidriera pública.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            val fmt = remember { SimpleDateFormat("d MMM", Locale.forLanguageTag("es-AR")) }
            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)) {
                items(publicaciones, key = { it.id }) { pub ->
                    PublicacionItem(pub, fmt, onEliminar)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = { mostrarDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Nueva publicación")
        }
    }
}

@Composable
private fun PublicacionItem(
    pub: Publicacion,
    fmt: SimpleDateFormat,
    onEliminar: (String) -> Unit
) {
    var confirmarEliminar by remember { mutableStateOf(false) }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("Eliminar publicación") },
            text = { Text("¿Seguro que querés eliminar esta publicación?") },
            confirmButton = {
                TextButton(onClick = { onEliminar(pub.id); confirmarEliminar = false }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") }
            }
        )
    }

    ListItem(
        headlineContent = {
            Text(pub.texto, style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Text(
                fmt.format(pub.fecha.toDate()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            val (label, color) = when (pub.etiqueta) {
                "promo" -> "PROMO" to MaterialTheme.colorScheme.tertiary
                "aviso" -> "AVISO" to MaterialTheme.colorScheme.error
                else    -> "NUEVO" to MaterialTheme.colorScheme.primary
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        trailingContent = {
            IconButton(onClick = { confirmarEliminar = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Eliminar",
                    Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun DialogNuevaPublicacion(
    onConfirmar: (texto: String, etiqueta: String) -> Unit,
    onDismiss: () -> Unit
) {
    var texto by remember { mutableStateOf("") }
    var etiqueta by remember { mutableStateOf("novedad") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva publicación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Mensaje") },
                    placeholder = { Text("Ej: 20% off en golosinas este finde") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tipo", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("novedad" to "Novedad", "promo" to "Promo", "aviso" to "Aviso").forEach { (key, lbl) ->
                        FilterChip(
                            selected = etiqueta == key,
                            onClick = { etiqueta = key },
                            label = { Text(lbl) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(texto.trim(), etiqueta) },
                enabled = texto.isNotBlank()
            ) { Text("Publicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
