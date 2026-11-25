package com.kosking.app.feature.kiosco

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualizarPreciosSheet(
    productos: List<Producto>,
    vm: DuenoViewModel,
    onDismiss: () -> Unit
) {
    // Mapa mutable: id -> texto del precio que el usuario edita
    val precios = remember(productos) {
        mutableStateMapOf<String, String>().also { map ->
            productos.forEach { p ->
                map[p.id] = if (p.ultimoPrecio > 0) "%.0f".format(p.ultimoPrecio) else ""
            }
        }
    }

    val cambios: Map<String, Double> = remember(precios.toMap()) {
        productos.mapNotNull { p ->
            val nuevo = precios[p.id]?.toDoubleOrNull() ?: return@mapNotNull null
            if (nuevo != p.ultimoPrecio) p.id to nuevo else null
        }.toMap()
    }

    var guardando by remember { mutableStateOf(false) }
    var guardadoOk by remember { mutableStateOf(false) }

    // Agrupar por categoría
    val secciones: List<Pair<String, List<Producto>>> = remember(productos) {
        val conCat = productos
            .filter { it.categoria.isNotBlank() }
            .groupBy { it.categoria }
            .entries.sortedBy { it.key }
            .map { it.key to it.value }
        val sinCat = productos.filter { it.categoria.isBlank() }
        if (sinCat.isEmpty()) conCat else conCat + ("Otros" to sinCat)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Actualizar precios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${productos.size} producto${if (productos.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cerrar")
                }
            }

            HorizontalDivider()

            if (guardadoOk) {
                Box(
                    Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Precios actualizados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(onClick = onDismiss) { Text("Listo") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    secciones.forEach { (categoria, prods) ->
                        item(key = "cat_$categoria") {
                            Text(
                                categoria.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(prods, key = { it.id }) { prod ->
                            val textoActual = precios[prod.id] ?: ""
                            val cambio = textoActual.toDoubleOrNull()
                            val cambioDetectado = cambio != null && cambio != prod.ultimoPrecio

                            ListItem(
                                headlineContent = {
                                    Text(prod.nombre, fontWeight = FontWeight.Medium)
                                },
                                supportingContent = if (cambioDetectado && prod.ultimoPrecio > 0) ({
                                    Text(
                                        "Antes: ${"$"}${"%.0f".format(prod.ultimoPrecio)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }) else null,
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "$",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedTextField(
                                            value = textoActual,
                                            onValueChange = { nuevo ->
                                                // Solo dígitos y hasta un punto/coma
                                                val limpio = nuevo.filter { it.isDigit() || it == '.' }.take(8)
                                                precios[prod.id] = limpio
                                            },
                                            modifier = Modifier.width(90.dp),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Next
                                            ),
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (cambioDetectado) FontWeight.Bold else FontWeight.Normal,
                                                color = if (cambioDetectado) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = if (cambioDetectado) MaterialTheme.colorScheme.primary
                                                                     else MaterialTheme.colorScheme.outline,
                                                unfocusedBorderColor = if (cambioDetectado) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                            ),
                                        )
                                        if (cambioDetectado) {
                                            Icon(
                                                Icons.Default.Edit,
                                                null,
                                                Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }

                // Botón guardar
                HorizontalDivider()
                val cantCambios = cambios.size
                Button(
                    onClick = {
                        if (cantCambios > 0) {
                            guardando = true
                            vm.actualizarPrecios(cambios) {
                                guardando = false
                                guardadoOk = true
                            }
                        }
                    },
                    enabled = cantCambios > 0 && !guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (cantCambios == 0) "Sin cambios"
                        else "Guardar $cantCambios cambio${if (cantCambios != 1) "s" else ""}"
                    )
                }
            }
        }
    }
}
