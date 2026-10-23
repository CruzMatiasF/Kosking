﻿﻿﻿package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.modelo.Oferta
import com.kosking.app.modelo.StockItem
import com.kosking.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisOfertas(navController: NavController, vm: MisOfertasViewModel = viewModel()) {
    val ofertas by vm.ofertas.collectAsStateWithLifecycle()
    val stock by vm.stock.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var ofertaEliminando by remember { mutableStateOf<Oferta?>(null) }

    val activas = ofertas.count { it.activa }

    // Reset Ã©xito â†’ cerrar diÃ¡logo
    LaunchedEffect(estado) {
        if (estado is EstadoOferta.Exito) {
            kotlinx.coroutines.delay(1000)
            mostrarDialogo = false
            vm.estado.value = EstadoOferta.Idle
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
                            Text("Mis Ofertas", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Blanco)
                            Text("$activas activas · ${ofertas.size} en total", fontSize = 12.sp, color = Blanco.copy(alpha = 0.65f))
                        }
                        IconButton(onClick = { mostrarDialogo = true }) {
                            Icon(Icons.Default.Add, null, tint = Blanco)
                        }
                    }
                    // Resumen chips
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ResumenChipOferta("$activas", "Activas", Verde)
                        ResumenChipOferta("${ofertas.size - activas}", "Inactivas", Gris500)
                        val ahorroMax = ofertas.filter { it.activa }
                            .maxOfOrNull { it.precioNormal - it.precioOferta }
                        if (ahorroMax != null && ahorroMax > 0) {
                            ResumenChipOferta("$%.0f".format(ahorroMax), "Mayor ahorro", Rojo)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogo = true },
                containerColor = Rojo,
                contentColor = Blanco,
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Nueva oferta", fontWeight = FontWeight.Bold)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (ofertas.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ðŸ·ï¸", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No hay ofertas todav\u00EDa", fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Toc\u00E1 + para crear tu primera oferta", color = Gris500, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(ofertas, key = { it.id }) { oferta ->
                    OfertaCard(
                        oferta = oferta,
                        onToggle = { vm.toggleActiva(oferta) },
                        onEliminar = { ofertaEliminando = oferta },
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) } // espacio para FAB
        }
    }

    // DiÃ¡logo para crear oferta
    if (mostrarDialogo) {
        CrearOfertaDialog(
            stock = stock,
            estado = estado,
            onCreate = { nombre, normal, oferta, desc -> vm.crearOferta(nombre, normal, oferta, desc) },
            onDismiss = { mostrarDialogo = false; vm.estado.value = EstadoOferta.Idle },
        )
    }

    // ConfirmaciÃ³n eliminaciÃ³n
    ofertaEliminando?.let { oferta ->
        AlertDialog(
            onDismissRequest = { ofertaEliminando = null },
            title = { Text("Eliminar oferta") },
            text = { Text("\u00BFElimin\u00E1s la oferta de \"${oferta.nombreProducto}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { vm.eliminarOferta(oferta.id); ofertaEliminando = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Rojo),
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { ofertaEliminando = null }) { Text("Cancelar") } },
        )
    }

    // Error
    if (estado is EstadoOferta.Error) {
        AlertDialog(
            onDismissRequest = { vm.estado.value = EstadoOferta.Idle },
            title = { Text("Error") },
            text = { Text((estado as EstadoOferta.Error).mensaje) },
            confirmButton = { TextButton(onClick = { vm.estado.value = EstadoOferta.Idle }) { Text("OK") } },
        )
    }
}

@Composable
private fun ResumenChipOferta(valor: String, etiqueta: String, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = Blanco.copy(alpha = 0.15f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(valor, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 13.sp)
            Text(etiqueta, color = Blanco.copy(alpha = 0.75f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun OfertaCard(oferta: Oferta, onToggle: () -> Unit, onEliminar: () -> Unit) {
    val descuento = if (oferta.precioNormal > 0)
        ((1 - oferta.precioOferta / oferta.precioNormal) * 100).toInt()
    else 0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = if (oferta.activa) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral: roja si activa, gris si no
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(if (oferta.activa) Rojo else Gris200),
            )
            Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        oferta.nombreProducto,
                        fontWeight = FontWeight.SemiBold,
                        color = if (oferta.activa) AzulNavy else Gris500,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (descuento > 0) {
                        Surface(shape = RoundedCornerShape(6.dp), color = RojoClaro) {
                            Text(
                                "-$descuento%",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Rojo,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
                if (!oferta.descripcion.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(oferta.descripcion, fontSize = 11.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "$%.0f".format(oferta.precioNormal),
                        fontSize = 12.sp,
                        color = Gris500,
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                    )
                    Text(
                        "$%.0f".format(oferta.precioOferta),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (oferta.activa) Rojo else Gris500,
                        fontSize = 16.sp,
                    )
                }
            }
            // Acciones
            Column(
                modifier = Modifier.padding(end = 4.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Switch(
                    checked = oferta.activa,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Blanco, checkedTrackColor = Verde),
                    modifier = Modifier.size(40.dp),
                )
                IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Gris500, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearOfertaDialog(
    stock: List<StockItem>,
    estado: EstadoOferta,
    onCreate: (String, Double, Double, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nombreProducto by remember { mutableStateOf("") }
    var precioNormalTexto by remember { mutableStateOf("") }
    var precioOfertaTexto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var expandido by remember { mutableStateOf(false) }

    val guardando = estado is EstadoOferta.Guardando
    val exito = estado is EstadoOferta.Exito

    val descuento = run {
        val n = precioNormalTexto.toDoubleOrNull() ?: 0.0
        val o = precioOfertaTexto.toDoubleOrNull() ?: 0.0
        if (n > 0 && o > 0 && o < n) ((1 - o / n) * 100).toInt() else 0
    }

    AlertDialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        containerColor = Blanco,
        title = { Text("Nueva oferta", fontWeight = FontWeight.Bold, color = AzulNavy) },
        text = {
            if (exito) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("\u00A1Oferta creada!", fontWeight = FontWeight.Bold, color = Verde, fontSize = 15.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Seleccionar desde stock o escribir nombre libre
                    if (stock.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandido,
                            onExpandedChange = { expandido = it },
                        ) {
                            OutlinedTextField(
                                value = nombreProducto,
                                onValueChange = { nombreProducto = it },
                                label = { Text("Producto") },
                                placeholder = { Text("Escrib\u00ED o eleg\u00ED del stock") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                                singleLine = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Rojo, unfocusedBorderColor = Gris500.copy(alpha = 0.4f), focusedLabelColor = Rojo, unfocusedLabelColor = Gris500, focusedTextColor = AzulNavy, unfocusedTextColor = AzulNavy, cursorColor = Rojo),
                            )
                            ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                                stock.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(item.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Text("Precio actual: $%.0f".format(item.precio), fontSize = 11.sp, color = Gris500)
                                            }
                                        },
                                        onClick = {
                                            nombreProducto = item.nombre
                                            precioNormalTexto = "%.0f".format(item.precio)
                                            expandido = false
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = nombreProducto,
                            onValueChange = { nombreProducto = it },
                            label = { Text("Nombre del producto") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Rojo, unfocusedBorderColor = Gris500.copy(alpha = 0.4f), focusedLabelColor = Rojo, unfocusedLabelColor = Gris500, focusedTextColor = AzulNavy, unfocusedTextColor = AzulNavy, cursorColor = Rojo),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = precioNormalTexto,
                            onValueChange = { if (it.all { c -> c.isDigit() }) precioNormalTexto = it },
                            label = { Text("Precio normal") },
                            prefix = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Rojo, unfocusedBorderColor = Gris500.copy(alpha = 0.4f), focusedLabelColor = Rojo, unfocusedLabelColor = Gris500, focusedTextColor = AzulNavy, unfocusedTextColor = AzulNavy, cursorColor = Rojo),
                        )
                        OutlinedTextField(
                            value = precioOfertaTexto,
                            onValueChange = { if (it.all { c -> c.isDigit() }) precioOfertaTexto = it },
                            label = { Text("Precio oferta") },
                            prefix = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Rojo, unfocusedBorderColor = Gris500.copy(alpha = 0.4f), focusedLabelColor = Rojo, unfocusedLabelColor = Gris500, focusedTextColor = AzulNavy, unfocusedTextColor = AzulNavy, cursorColor = Rojo),
                        )
                    }
                    if (descuento > 0) {
                        Surface(shape = RoundedCornerShape(8.dp), color = RojoClaro) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Default.LocalOffer, null, tint = Rojo, modifier = Modifier.size(16.dp))
                                Text("Descuento del $descuento%", color = Rojo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripci\u00F3n (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Rojo, unfocusedBorderColor = Gris500.copy(alpha = 0.4f), focusedLabelColor = Rojo, unfocusedLabelColor = Gris500, focusedTextColor = AzulNavy, unfocusedTextColor = AzulNavy, cursorColor = Rojo),
                    )
                }
            }
        },
        confirmButton = {
            if (!exito) {
                val valido = nombreProducto.isNotBlank() &&
                    precioNormalTexto.isNotBlank() &&
                    precioOfertaTexto.isNotBlank() &&
                    (precioOfertaTexto.toDoubleOrNull() ?: 0.0) < (precioNormalTexto.toDoubleOrNull() ?: 0.0)
                Button(
                    onClick = {
                        onCreate(
                            nombreProducto,
                            precioNormalTexto.toDouble(),
                            precioOfertaTexto.toDouble(),
                            descripcion,
                        )
                    },
                    enabled = valido && !guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = Rojo),
                ) {
                    if (guardando) CircularProgressIndicator(color = Blanco, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Crear oferta", fontWeight = FontWeight.Bold)
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

