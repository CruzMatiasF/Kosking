package com.kosking.app.dueno

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kosking.app.modelo.Producto

internal data class ItemCarrito(val producto: Producto, val cantidad: Int)

@Composable
internal fun FilaProductoAnotar(
    prod: Producto,
    cantidad: Int,
    esGlobal: Boolean = false,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit,
    onSetCantidad: ((Int) -> Unit)? = null
) {
    var editandoCantidad by remember { mutableStateOf(false) }
    var cantidadTexto by remember(cantidad) { mutableStateOf(cantidad.toString()) }

    var highlighted by remember { mutableStateOf(false) }
    val prevCantidad = remember { mutableIntStateOf(cantidad) }
    LaunchedEffect(cantidad) {
        if (cantidad > prevCantidad.intValue) {
            highlighted = true
            kotlinx.coroutines.delay(350)
            highlighted = false
        }
        prevCantidad.intValue = cantidad
    }
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val bgColor by animateColorAsState(
        targetValue = if (highlighted) highlightColor else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "highlight"
    )

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAgregar()
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (prod.imagenUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(prod.imagenUrl).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                prod.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (cantidad > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (esGlobal && cantidad == 0) {
                Text(
                    "de otros locales · sin precio",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            } else if (prod.ultimoPrecio > 0) {
                Text(
                    "$${"%.0f".format(prod.ultimoPrecio)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (cantidad > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (cantidad > 0) {
                Text(
                    "sin precio — no suma al total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (cantidad > 0) {
            IconButton(onClick = onQuitar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
            }
            if (editandoCantidad && onSetCantidad != null) {
                fun confirmar() {
                    val n = cantidadTexto.toIntOrNull() ?: 0
                    onSetCantidad(if (n > 0) n else cantidad)
                    editandoCantidad = false
                }
                BasicTextField(
                    value = cantidadTexto,
                    onValueChange = { cantidadTexto = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirmar() }),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.width(36.dp),
                    decorationBox = { inner ->
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { inner() }
                    }
                )
            } else {
                Text(
                    cantidad.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(20.dp).clickable(enabled = onSetCantidad != null) {
                        cantidadTexto = cantidad.toString()
                        editandoCantidad = true
                    },
                    textAlign = TextAlign.Center
                )
            }
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

