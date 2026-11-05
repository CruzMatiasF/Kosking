package com.kosking.app.explorar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kosking.app.cliente.ClienteViewModel
import com.kosking.app.modelo.Producto

@Composable
internal fun DialogConectarComoCliente(
    clienteVm: ClienteViewModel,
    onExito: () -> Unit,
    onDismiss: () -> Unit
) {
    val cargando by clienteVm.cargando.collectAsState()
    val error by clienteVm.error.collectAsState()
    var codigo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { clienteVm.error.value = null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conectar con un local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "El dueño te tiene que dar tu código personal. Lo encontrás en tu perfil dentro del local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.uppercase().take(8) },
                    label = { Text("Código de acceso") },
                    placeholder = { Text("KSK-XXXX") },
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
                onClick = { clienteVm.verificarPorCodigoAcceso(codigo.trim()) { onExito() } },
                enabled = codigo.length >= 6 && !cargando
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Conectar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
internal fun SeccionLabel(texto: String, conteo: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            texto.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (conteo.isNotBlank()) {
            Text(conteo, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun TarjetaProducto(
    producto: Producto,
    nombreLocal: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                if (producto.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = producto.imagenUrl,
                        contentDescription = producto.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                } else {
                    Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when {
                    producto.estadoStock == "agotado" || producto.agotado -> {
                        Box(
                            modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("Agotado", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                    producto.estadoStock == "pocos" -> {
                        Box(
                            modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("Quedan pocos", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                    producto.esOferta && producto.precioAnterior > 0 -> {
                        val descuento = (((producto.precioAnterior - producto.precio) / producto.precioAnterior) * 100).toInt()
                        Box(
                            modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.onSurface)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("−$descuento%", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                if (nombreLocal.isNotBlank()) {
                    Text(nombreLocal, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(producto.nombre, style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$${"%.0f".format(producto.precio)}",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
                    if (producto.esOferta && producto.precioAnterior > 0) {
                        Text("$${"%.0f".format(producto.precioAnterior)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough)
                    }
                }
            }
        }
    }
}

fun iconParaTipo(tipo: String): ImageVector = when (tipo) {
    "Almacén" -> Icons.Default.ShoppingBasket
    "Maxikiosco" -> Icons.Default.ShoppingCart
    "Ropa" -> Icons.Default.Checkroom
    "Corralón" -> Icons.Default.Foundation
    "Farmacia" -> Icons.Default.LocalPharmacy
    "Verdulería" -> Icons.Default.Eco
    else -> Icons.Default.Store
}
