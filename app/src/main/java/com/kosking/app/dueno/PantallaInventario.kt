package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.modelo.StockItem
import com.kosking.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInventario(navController: NavController, vm: InventarioViewModel = viewModel()) {
    val stock by vm.stockFiltrado.collectAsStateWithLifecycle()
    val stockTotal by vm.stock.collectAsStateWithLifecycle()
    val filtroActual by vm.filtro.collectAsStateWithLifecycle()
    val estadoEdicion by vm.estadoEdicion.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var itemEditando by remember { mutableStateOf<StockItem?>(null) }
    var itemEliminando by remember { mutableStateOf<StockItem?>(null) }

    // Reset Ã©xito
    LaunchedEffect(estadoEdicion) {
        if (estadoEdicion is EstadoEdicion.Exito) {
            kotlinx.coroutines.delay(1500)
            vm.estadoEdicion.value = EstadoEdicion.Idle
            itemEditando = null
        }
    }

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(HeaderGradient).statusBarsPadding()) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Inventario", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Blanco)
                            Text("${stockTotal.size} productos en total", fontSize = 12.sp, color = Blanco.copy(alpha = 0.65f))
                        }
                    }
                    // Buscador
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Blanco.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Search, null, tint = Blanco.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = query,
                                onValueChange = { query = it; vm.query.value = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Blanco, fontSize = 14.sp),
                                decorationBox = { inner ->
                                    if (query.isEmpty()) Text("Buscar producto...", color = Blanco.copy(alpha = 0.45f), fontSize = 14.sp)
                                    inner()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    // Filtros
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val filtros = listOf(
                            FiltroStock.TODOS to "Todos",
                            FiltroStock.BAJO to "Stock bajo (â‰¤3)",
                            FiltroStock.AGOTADO to "Agotado",
                        )
                        items(filtros) { (filtro, label) ->
                            val sel = filtroActual == filtro
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (sel) Blanco else Blanco.copy(alpha = 0.15f),
                                onClick = { vm.filtro.value = filtro },
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    fontSize = 12.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sel) AzulNavy else Blanco,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (stock.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ðŸ“¦", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Sin productos", color = Gris500, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(stock, key = { it.productoId }) { item ->
                    StockItemCard(
                        item = item,
                        onEditar = { itemEditando = item },
                        onEliminar = { itemEliminando = item },
                    )
                }
            }
        }
    }

    // DiÃ¡logo de ediciÃ³n
    itemEditando?.let { item ->
        EditarStockDialog(
            item = item,
            guardando = estadoEdicion is EstadoEdicion.Guardando,
            exito = estadoEdicion is EstadoEdicion.Exito,
            onGuardar = { cant, precio -> vm.actualizarStock(item.productoId, cant, precio) },
            onDismiss = { itemEditando = null; vm.estadoEdicion.value = EstadoEdicion.Idle },
        )
    }

    // DiÃ¡logo de confirmaciÃ³n de eliminaciÃ³n
    itemEliminando?.let { item ->
        AlertDialog(
            onDismissRequest = { itemEliminando = null },
            title = { Text("Eliminar producto") },
            text = { Text("Â¿EliminÃ¡s \"${item.nombre}\" del inventario?") },
            confirmButton = {
                TextButton(
                    onClick = { vm.eliminarProducto(item.productoId); itemEliminando = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Rojo),
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { itemEliminando = null }) { Text("Cancelar") }
            },
        )
    }

    // Snackbar de error de ediciÃ³n
    if (estadoEdicion is EstadoEdicion.Error) {
        AlertDialog(
            onDismissRequest = { vm.estadoEdicion.value = EstadoEdicion.Idle },
            title = { Text("Error") },
            text = { Text((estadoEdicion as EstadoEdicion.Error).mensaje) },
            confirmButton = {
                TextButton(onClick = { vm.estadoEdicion.value = EstadoEdicion.Idle }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun StockItemCard(item: StockItem, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val stockColor = when {
        item.cantidad == 0 -> Rojo
        item.cantidad <= 3 -> Naranja
        else -> Verde
    }
    val stockLabel = when {
        item.cantidad == 0 -> "Agotado"
        item.cantidad <= 3 -> "Stock bajo"
        else -> "En stock"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Indicador de stock (barra lateral)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(stockColor),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = stockColor.copy(alpha = 0.1f)) {
                        Text(
                            "${item.cantidad} uds Â· $stockLabel",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            color = stockColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text("$%.0f".format(item.precio), fontWeight = FontWeight.Bold, color = Rojo, fontSize = 14.sp)
                }
            }
            // Acciones
            IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, null, tint = Azul, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, null, tint = Rojo.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EditarStockDialog(
    item: StockItem,
    guardando: Boolean,
    exito: Boolean,
    onGuardar: (Int, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var cantidadTexto by remember(item.productoId) { mutableStateOf(item.cantidad.toString()) }
    var precioTexto by remember(item.productoId) { mutableStateOf("%.0f".format(item.precio)) }

    AlertDialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        containerColor = Blanco,
        title = {
            Text(item.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AzulNavy, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (exito) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Verde, modifier = Modifier.size(20.dp))
                        Text("Guardado correctamente", color = Verde, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedTextField(
                        value = cantidadTexto,
                        onValueChange = { if (it.all { c -> c.isDigit() }) cantidadTexto = it },
                        label = { Text("Cantidad en stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Azul, unfocusedBorderColor = Gris500.copy(alpha = 0.4f), focusedLabelColor = Azul, unfocusedLabelColor = Gris500, focusedTextColor = AzulNavy, unfocusedTextColor = AzulNavy, cursorColor = Azul),
                    )
                    OutlinedTextField(
                        value = precioTexto,
                        onValueChange = { if (it.all { c -> c.isDigit() }) precioTexto = it },
                        label = { Text("Precio ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Azul, unfocusedBorderColor = Gris500.copy(alpha = 0.4f), focusedLabelColor = Azul, unfocusedLabelColor = Gris500, focusedTextColor = AzulNavy, unfocusedTextColor = AzulNavy, cursorColor = Azul),
                        prefix = { Text("$") },
                    )
                }
            }
        },
        confirmButton = {
            if (!exito) {
                Button(
                    onClick = {
                        val cant = cantidadTexto.toIntOrNull() ?: return@Button
                        val precio = precioTexto.toDoubleOrNull() ?: return@Button
                        onGuardar(cant, precio)
                    },
                    enabled = !guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = Azul),
                ) {
                    if (guardando) {
                        CircularProgressIndicator(color = Blanco, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !guardando) {
                Text(if (exito) "Cerrar" else "Cancelar", color = Gris500)
            }
        },
    )
}

