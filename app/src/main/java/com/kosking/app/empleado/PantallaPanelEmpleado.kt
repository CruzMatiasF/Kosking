package com.kosking.app.empleado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.modelo.StockItem
import com.kosking.app.modelo.Venta
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A)),
)

private val fmt = NumberFormat.getNumberInstance(Locale("es", "AR"))
private fun formatPrecio(v: Double) = "$${fmt.format(v.toLong())}"
private val fmtHora = SimpleDateFormat("HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPanelEmpleado(navController: NavController, vm: PanelEmpleadoViewModel = viewModel()) {
    val tienda by vm.tienda.collectAsStateWithLifecycle()
    val ventasDelTurno by vm.ventasDelTurno.collectAsStateWithLifecycle()
    val totalTurno by vm.totalTurno.collectAsStateWithLifecycle()
    val estadoVenta by vm.estadoVenta.collectAsStateWithLifecycle()

    var mostrarDialogoVenta by remember { mutableStateOf(false) }

    // Limpiar estado Exito para no mostrar dialogo stale
    LaunchedEffect(estadoVenta) {
        if (estadoVenta is EstadoVentaEmpleado.Exito) {
            kotlinx.coroutines.delay(200)
            vm.estadoVenta.value = EstadoVentaEmpleado.Idle
        }
    }

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BG)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Hola, ${vm.nombreEmpleado} 👋",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Blanco,
                            )
                            Text(
                                tienda?.nombre ?: "Cargando...",
                                fontSize = 13.sp,
                                color = Blanco.copy(alpha = 0.65f),
                            )
                        }
                        // Volver al home
                        IconButton(onClick = {
                            navController.navigate(Rutas.HOME) { popUpTo(0) { inclusive = false } }
                        }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Blanco.copy(alpha = 0.7f))
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = Verde.copy(alpha = 0.2f)) {
                            Text(
                                "Turno activo",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 12.sp,
                                color = Verde,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TurnoCard(
                            titulo = "Total del turno",
                            valor = formatPrecio(totalTurno),
                            icono = Icons.Default.AttachMoney,
                            color = Verde,
                            modifier = Modifier.weight(1f),
                        )
                        TurnoCard(
                            titulo = "Ventas realizadas",
                            valor = ventasDelTurno.size.toString(),
                            icono = Icons.Default.Receipt,
                            color = Azul,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Blanco, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { mostrarDialogoVenta = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Rojo, contentColor = Blanco),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Registrar venta", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))

            // Acciones rápidas
            Text(
                "Acciones rápidas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AzulNavy,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccionEmpleadoCard(
                    icono = Icons.Default.Add,
                    etiqueta = "Registrar\nventa",
                    color = Rojo,
                    modifier = Modifier.weight(1f),
                    onClick = { mostrarDialogoVenta = true },
                )
                AccionEmpleadoCard(
                    icono = Icons.Default.Inventory,
                    etiqueta = "Ver\nstock",
                    color = Verde,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Rutas.INVENTARIO) },
                )
                AccionEmpleadoCard(
                    icono = Icons.Default.LocalOffer,
                    etiqueta = "Ver\nofertas",
                    color = Naranja,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Rutas.MIS_OFERTAS) },
                )
            }

            Spacer(Modifier.height(24.dp))

            // Ventas del turno
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Ventas del turno",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AzulNavy,
                    modifier = Modifier.weight(1f),
                )
                Text("${ventasDelTurno.size} ventas", fontSize = 13.sp, color = Gris500)
            }
            Spacer(Modifier.height(8.dp))

            if (ventasDelTurno.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Sin ventas todavía", color = Gris500, fontWeight = FontWeight.Medium)
                        Text("Registrá la primera venta del turno", fontSize = 13.sp, color = Gris500)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ventasDelTurno.forEach { venta -> VentaItem(venta) }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Total del turno
            if (ventasDelTurno.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Blanco,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total recaudado", fontSize = 13.sp, color = Gris500)
                            Text(
                                formatPrecio(totalTurno),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = Verde,
                            )
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = VerdeClaro) {
                            Icon(
                                Icons.Default.TrendingUp,
                                null,
                                tint = Verde,
                                modifier = Modifier.padding(10.dp).size(24.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Diálogo registrar venta con stock real
    if (mostrarDialogoVenta) {
        DialogoRegistrarVentaEmpleado(
            vm = vm,
            onDismiss = {
                vm.limpiarCarrito()
                mostrarDialogoVenta = false
            },
            onExito = { mostrarDialogoVenta = false },
        )
    }

    // Error dialog
    if (estadoVenta is EstadoVentaEmpleado.Error) {
        AlertDialog(
            onDismissRequest = { vm.estadoVenta.value = EstadoVentaEmpleado.Idle },
            title = { Text("Error") },
            text = { Text((estadoVenta as EstadoVentaEmpleado.Error).mensaje) },
            confirmButton = {
                TextButton(onClick = { vm.estadoVenta.value = EstadoVentaEmpleado.Idle }) { Text("OK") }
            },
        )
    }
}

// ── Diálogo registrar venta con stock real ────────────────────────────────────

@Composable
private fun DialogoRegistrarVentaEmpleado(
    vm: PanelEmpleadoViewModel,
    onDismiss: () -> Unit,
    onExito: () -> Unit,
) {
    val stock by vm.stockFiltrado.collectAsStateWithLifecycle()
    val carrito by vm.carrito.collectAsStateWithLifecycle()
    val totalCarrito by vm.totalCarrito.collectAsStateWithLifecycle()
    val estadoVenta by vm.estadoVenta.collectAsStateWithLifecycle()
    val query by vm.queryStock.collectAsStateWithLifecycle()

    LaunchedEffect(estadoVenta) {
        if (estadoVenta is EstadoVentaEmpleado.Exito) onExito()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Blanco,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxHeight(0.85f),
        title = {
            Column {
                Text("Registrar venta", fontWeight = FontWeight.Bold, color = AzulNavy)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.queryStock.value = it },
                    placeholder = { Text("Buscar producto...", color = Gris500) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Gris500) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Azul,
                        unfocusedBorderColor = Gris200,
                        focusedTextColor = AzulNavy,
                        unfocusedTextColor = AzulNavy,
                        cursorColor = Azul,
                    ),
                )
            }
        },
        text = {
            Column {
                // Lista de stock
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (stock.isEmpty()) {
                        item {
                            Text(
                                "Sin productos disponibles",
                                color = Gris500,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    items(stock) { item ->
                        val enCarrito = carrito[item.productoId]?.cantidad ?: 0
                        StockItemRow(
                            item = item,
                            enCarrito = enCarrito,
                            onAgregar = { vm.agregarAlCarrito(item) },
                            onQuitar = { vm.quitarDelCarrito(item.productoId) },
                        )
                    }
                }

                // Resumen carrito
                if (carrito.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${carrito.values.sumOf { it.cantidad }} productos", fontSize = 12.sp, color = Gris500)
                            Text(
                                "Total: ${formatPrecio(totalCarrito)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Verde,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.registrarVenta() },
                enabled = carrito.isNotEmpty() && estadoVenta !is EstadoVentaEmpleado.Cargando,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rojo, contentColor = Blanco),
            ) {
                if (estadoVenta is EstadoVentaEmpleado.Cargando) {
                    CircularProgressIndicator(color = Blanco, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirmar venta", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Gris500) }
        },
    )
}

@Composable
private fun StockItemRow(
    item: StockItem,
    enCarrito: Int,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(10.dp), color = AzulFondo) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.nombre, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AzulNavy)
                Text(
                    "${formatPrecio(item.precio)} · Stock: ${item.cantidad}",
                    fontSize = 11.sp,
                    color = Gris500,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (enCarrito > 0) {
                    IconButton(onClick = onQuitar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, null, tint = Rojo, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        enCarrito.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = AzulNavy,
                        modifier = Modifier.widthIn(min = 20.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(
                    onClick = onAgregar,
                    modifier = Modifier.size(32.dp),
                    enabled = item.cantidad > enCarrito,
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = if (item.cantidad > enCarrito) Azul else Gris500,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun TurnoCard(titulo: String, valor: String, icono: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = Blanco.copy(alpha = 0.12f), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, null, tint = color, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(titulo, fontSize = 11.sp, color = Blanco.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(6.dp))
            Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Blanco)
        }
    }
}

@Composable
private fun AccionEmpleadoCard(icono: ImageVector, etiqueta: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(etiqueta, fontSize = 10.sp, color = AzulNavy, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun VentaItem(venta: Venta) {
    Surface(shape = RoundedCornerShape(12.dp), color = Blanco, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AzulFondo),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.ShoppingCart, null, tint = Azul, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${venta.items.size} producto${if (venta.items.size != 1) "s" else ""}",
                    fontWeight = FontWeight.SemiBold,
                    color = AzulNavy,
                    fontSize = 13.sp,
                )
                Text(
                    venta.items.take(2).joinToString(", ") { it.nombre },
                    fontSize = 11.sp,
                    color = Gris500,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatPrecio(venta.total), fontWeight = FontWeight.ExtraBold, color = Verde, fontSize = 15.sp)
                venta.fecha?.toDate()?.let {
                    Text(fmtHora.format(it), fontSize = 11.sp, color = Gris500)
                }
            }
        }
    }
}
