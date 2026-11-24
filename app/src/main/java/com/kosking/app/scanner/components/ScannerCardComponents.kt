package com.kosking.app.scanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kosking.app.modelo.Producto

@Composable
internal fun ScannerCategoriaCard(
    nombre: String,
    imagenUrl: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.width(70.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(imagenUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        nombre.take(2).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                nombre,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
internal fun ScannerProductoCard(
    prod: Producto,
    cantEnCarrito: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ElevatedCardDefaults.elevatedCardColors(
            containerColor = if (cantEnCarrito > 0)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.width(70.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (prod.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(prod.imagenUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.ShoppingCart, null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                if (cantEnCarrito > 0) {
                    Badge(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp)) {
                        Text("$cantEnCarrito")
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)) {
                Text(
                    prod.nombre,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (cantEnCarrito > 0)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (prod.presentacion.isNotBlank()) {
                    Text(
                        prod.presentacion,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (cantEnCarrito > 0)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (prod.ultimoPrecio > 0) {
                    Text(
                        "$${prod.ultimoPrecio.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (cantEnCarrito > 0)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** Devuelve la URL de imagen del primer producto de la categoría que tenga una. */
internal fun primeraImagenCategoria(productos: List<Producto>, categoria: String): String =
    productos.firstOrNull {
        (it.categoria.ifBlank { "Otros" }) == categoria && it.imagenUrl.isNotBlank()
    }?.imagenUrl ?: ""
