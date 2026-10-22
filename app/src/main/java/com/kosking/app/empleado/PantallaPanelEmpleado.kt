package com.kosking.app.empleado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

// ── Datos de ejemplo ──────────────────────────────────────────────────────────

private data class VentaTurno(
    val emoji: String,
    val nombre: String,
    val cantidad: Int,
    val precio: String,
    val hora: String,
)

private val ventasTurno = listOf(
    VentaTurno("🥤", "Coca-Cola 500ml", 2, "$2.000", "10:15"),
    VentaTurno("🍪", "Oreo 124g", 1, "$1.200", "10:42"),
    VentaTurno("🍟", "Lays Clásicas 150g", 3, "$2.850", "11:05"),
    VentaTurno("🧃", "Jugo Cepita 1L", 1, "$1.100", "11:28"),
)

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A)),
)

// ── Pantalla ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPanelEmpleado(navController: NavController) {
    val nombreEmpleado = "Lucía"
    val nombreLocal = "Kiosco El Cielo"
    var mostrarDialogoVenta by remember { mutableStateOf(false) }

    val totalTurno = "$7.150"
    val ventasCantidad = ventasTurno.size

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
                                "Hola, $nombreEmpleado 👋",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Blanco,
                            )
                            Text(
                                nombreLocal,
                                fontSize = 13.sp,
                                color = Blanco.copy(alpha = 0.65f),
                            )
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

                    // Resumen del turno
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TurnoCard(
                            titulo = "Total del turno",
                            valor = totalTurno,
                            icono = Icons.Default.AttachMoney,
                            color = Verde,
                            modifier = Modifier.weight(1f),
                        )
                        TurnoCard(
                            titulo = "Ventas realizadas",
                            valor = ventasCantidad.toString(),
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
                    OutlinedButton(
                        onClick = { navController.navigate(Rutas.INGRESAR_CODIGO) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Azul),
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Escanear", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { mostrarDialogoVenta = true },
                        modifier = Modifier.weight(1f).height(52.dp),
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

            // ── Acciones rápidas ──────────────────────────────────────────────
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
                    icono = Icons.Default.QrCodeScanner,
                    etiqueta = "Escanear\ncódigo",
                    color = Azul,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Rutas.INGRESAR_CODIGO) },
                )
                AccionEmpleadoCard(
                    icono = Icons.Default.Inventory,
                    etiqueta = "Ver\nstock",
                    color = Verde,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
                AccionEmpleadoCard(
                    icono = Icons.Default.LocalOffer,
                    etiqueta = "Ver\nofertas",
                    color = Naranja,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Ventas del turno ──────────────────────────────────────────────
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
                Text(
                    "${ventasTurno.size} items",
                    fontSize = 13.sp,
                    color = Gris500,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (ventasTurno.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
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
                    ventasTurno.forEach { venta -> VentaTurnoItem(venta) }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Total del turno ───────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Blanco,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total recaudado", fontSize = 13.sp, color = Gris500)
                        Text(totalTurno, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Verde)
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

    // ── Diálogo registrar venta ───────────────────────────────────────────────
    if (mostrarDialogoVenta) {
        DialogoRegistrarVenta(
            onDismiss = { mostrarDialogoVenta = false },
            onConfirmar = { mostrarDialogoVenta = false },
        )
    }
}

// ── Diálogo rápido de venta ───────────────────────────────────────────────────

@Composable
private fun DialogoRegistrarVenta(onDismiss: () -> Unit, onConfirmar: () -> Unit) {
    var producto by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var precio by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Blanco,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Registrar venta", fontWeight = FontWeight.Bold, color = AzulNavy)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = producto,
                    onValueChange = { producto = it },
                    label = { Text("Producto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Azul,
                        focusedLabelColor = Azul,
                        cursorColor = Azul,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it },
                        label = { Text("Cantidad") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Azul,
                            focusedLabelColor = Azul,
                            cursorColor = Azul,
                        ),
                    )
                    OutlinedTextField(
                        value = precio,
                        onValueChange = { precio = it },
                        label = { Text("Precio $") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Azul,
                            focusedLabelColor = Azul,
                            cursorColor = Azul,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rojo, contentColor = Blanco),
            ) {
                Text("Registrar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Gris500)
            }
        },
    )
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun TurnoCard(
    titulo: String,
    valor: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco.copy(alpha = 0.12f),
        modifier = modifier,
    ) {
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
private fun AccionEmpleadoCard(
    icono: ImageVector,
    etiqueta: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                etiqueta,
                fontSize = 10.sp,
                color = AzulNavy,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
        }
    }
}

@Composable
private fun VentaTurnoItem(venta: VentaTurno) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AzulFondo),
                contentAlignment = Alignment.Center,
            ) {
                Text(venta.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(venta.nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 13.sp)
                Spacer(Modifier.height(2.dp))
                Text("x${venta.cantidad} · ${venta.hora}", fontSize = 11.sp, color = Gris500)
            }
            Text(venta.precio, fontWeight = FontWeight.ExtraBold, color = Verde, fontSize = 15.sp)
        }
    }
}
