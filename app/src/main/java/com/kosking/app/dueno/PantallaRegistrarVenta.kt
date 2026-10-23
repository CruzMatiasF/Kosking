package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
fun PantallaRegistrarVenta(navController: NavController, vm: RegistrarVentaViewModel = viewModel()) {
    val stock by vm.stockFiltrado.collectAsStateWithLifecycle()
    val carrito by vm.carrito.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    // Al registrar con éxito, volver atrás
    LaunchedEffect(estado) {
        if (estado is EstadoRegistro.Exito) {
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient)
                    .statusBarsPadding(),
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Registrar venta",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Blanco,
                            )
                            Text(
                                "${carrito.size} productos en el carrito",
                                fontSize = 12.sp,
                                color = Blanco.copy(alpha = 0.65f),
                            )
                        }
                    }
                    // Búsqueda en stock
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Blanco.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Search, null, tint = Blanco.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it; vm.query.value = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Blanco, fontSize = 14.sp),
                                decorationBox = { inner ->
                                    if (query.isEmpty()) Text("Buscar en stock...", color = Blanco.copy(alpha = 0.45f), fontSize = 14.sp)
                                    inner()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (carrito.isNotEmpty()) {
                Surface(color = Blanco, shadowElevation = 12.dp) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        // Resumen del carrito
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            carrito.values.forEach { ic ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "${ic.stockItem.nombre} x${ic.cantidad}",
                                        fontSize = 12.sp,
                                        color = Gris700,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "$%.0f".format(ic.stockItem.precio * ic.cantidad),
                                        fontSize = 12.sp,
                                        color = AzulNavy,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Gris200)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Total", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AzulNavy)
                                Text(
                                    "$%.0f".format(total),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Verde,
                                )
                            }
                        }
                        Button(
                            onClick = { vm.registrarVenta() },
                            enabled = estado !is EstadoRegistro.Cargando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Blanco),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        ) {
                            if (estado is EstadoRegistro.Cargando) {
                                CircularProgressIndicator(color = Blanco, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Confirmar venta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                            Text("📦", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Sin productos en stock", color = Gris500, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(stock, key = { it.productoId }) { item ->
                    val enCarrito = carrito[item.productoId]?.cantidad ?: 0
                    StockItemRow(
                        item = item,
                        enCarrito = enCarrito,
                        onAgregar = { vm.agregarAlCarrito(item) },
                        onQuitar = { vm.quitarDelCarrito(item.productoId) },
                    )
                }
            }
        }
    }

    // Error snackbar
    if (estado is EstadoRegistro.Error) {
        AlertDialog(
            onDismissRequest = { vm.estado.value = EstadoRegistro.Idle },
            title = { Text("Error") },
            text = { Text((estado as EstadoRegistro.Error).mensaje) },
            confirmButton = {
                TextButton(onClick = { vm.estado.value = EstadoRegistro.Idle }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun StockItemRow(
    item: StockItem,
    enCarrito: Int,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AzulFondo),
                contentAlignment = Alignment.Center,
            ) {
                Text("🛒", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.nombre,
                    fontWeight = FontWeight.SemiBold,
                    color = AzulNavy,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$%.0f".format(item.precio), fontWeight = FontWeight.Bold, color = Rojo, fontSize = 14.sp)
                    val stockColor = if (item.cantidad > 3) Verde else Naranja
                    Text("Stock: ${item.cantidad}", fontSize = 11.sp, color = stockColor)
                }
            }
            Spacer(Modifier.width(8.dp))
            // Controles de cantidad
            if (enCarrito == 0) {
                IconButton(
                    onClick = onAgregar,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Rojo),
                ) {
                    Icon(Icons.Default.Add, null, tint = Blanco, modifier = Modifier.size(18.dp))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onQuitar,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Gris200),
                    ) {
                        Icon(Icons.Default.Remove, null, tint = AzulNavy, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        "$enCarrito",
                        fontWeight = FontWeight.ExtraBold,
                        color = AzulNavy,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    IconButton(
                        onClick = onAgregar,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Rojo),
                    ) {
                        Icon(Icons.Default.Add, null, tint = Blanco, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// Necesitamos el import de BasicTextField
@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    textStyle: androidx.compose.ui.text.TextStyle,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = textStyle,
        decorationBox = decorationBox,
        modifier = modifier,
    )
}
