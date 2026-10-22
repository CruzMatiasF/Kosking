package com.kosking.app.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kosking.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleProducto(
    productoId: String,
    navController: NavController,
) {
    // Datos de ejemplo — reemplazar con Firestore
    val nombre = "Coca-Cola 500ml"
    val emoji = "🥤"
    val precio = "$1.000"
    val descripcion = "Bebida gaseosa cola. Presentación 500ml. Ideal para consumo personal."
    val categoria = "Bebidas"
    var esFavorito by remember { mutableStateOf(false) }

    // Kioscos que tienen este producto
    val kioscos = listOf(
        Triple("Kiosco El Cielo", "3 cuadras", 12),
        Triple("Kiosco La Esquina", "5 cuadras", 6),
        Triple("Maxikiosco 24", "7 cuadras", 20),
    )

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                },
                actions = {
                    IconButton(onClick = { esFavorito = !esFavorito }) {
                        Icon(
                            if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null,
                            tint = if (esFavorito) Rojo else Blanco,
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Share, null, tint = Blanco)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B3E)),
            )
        },
        bottomBar = {
            Surface(color = Blanco, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { navController.navigate("mapa") },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Azul),
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ver en mapa", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { esFavorito = true },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Rojo, contentColor = Blanco),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    ) {
                        Icon(Icons.Default.Favorite, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar", fontWeight = FontWeight.SemiBold)
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
            // ── Hero: imagen + precio ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF1A3A8A), AzulFondo))
                    )
                    .padding(top = 0.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 100.sp)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RojoClaro,
                    ) {
                        Text(
                            categoria,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Rojo,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // ── Info principal ────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-20).dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = AzulFondo,
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nombre, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AzulNavy)
                            Spacer(Modifier.height(4.dp))
                            Text(descripcion, fontSize = 13.sp, color = Gris500, lineHeight = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(precio, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Rojo)
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = Gris200)
                    Spacer(Modifier.height(24.dp))

                    // ── Kioscos que tienen este producto ──────────────────────
                    Text("Disponible en", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AzulNavy)
                    Spacer(Modifier.height(12.dp))

                    kioscos.forEach { (nombreKiosco, distancia, stock) ->
                        KioscoDisponibleItem(nombreKiosco, distancia, stock)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun KioscoDisponibleItem(nombre: String, distancia: String, stock: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(Azul),
            )
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AzulClaro),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🏪", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 14.sp)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("A $distancia", fontSize = 12.sp, color = Gris500)
                    }
                }
                val stockColor = if (stock > 5) Verde else Naranja
                Surface(shape = RoundedCornerShape(8.dp), color = stockColor.copy(alpha = 0.12f)) {
                    Text(
                        "Stock: $stock",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        color = stockColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
