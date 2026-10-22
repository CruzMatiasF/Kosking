package com.kosking.app.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

// ── Datos de ejemplo ──────────────────────────────────────────────────────────

private data class ProductoEnStock(
    val emoji: String,
    val nombre: String,
    val precio: String,
    val stock: Int,
)

private data class OfertaKiosco(
    val emoji: String,
    val nombre: String,
    val precioNormal: String,
    val precioOferta: String,
)

private val stockEjemplo = listOf(
    ProductoEnStock("🥤", "Coca-Cola 500ml", "$1.000", 12),
    ProductoEnStock("🥤", "Coca-Cola 1.5L", "$1.800", 6),
    ProductoEnStock("🍪", "Oreo 124g", "$1.200", 8),
    ProductoEnStock("🍟", "Lays Clásicas 150g", "$950", 15),
    ProductoEnStock("🧃", "Jugo Cepita 1L", "$1.100", 4),
    ProductoEnStock("🥛", "Leche La Serenísima 1L", "$1.800", 10),
)

private val ofertasEjemplo = listOf(
    OfertaKiosco("🥤", "Coca-Cola 1.5L", "$1.800", "$1.500"),
    OfertaKiosco("🍪", "Oreo 124g", "$1.200", "$990"),
)

// ── Pantalla ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleKiosco(
    tiendaId: String,
    navController: NavController,
) {
    val vm: DetalleKioscoViewModel = viewModel(factory = DetalleKioscoViewModel.Factory(tiendaId))
    val tiendaFirestore by vm.tienda.collectAsStateWithLifecycle()
    val stockFirestore by vm.stock.collectAsStateWithLifecycle()
    val ofertasFirestore by vm.ofertas.collectAsStateWithLifecycle()

    // Usa datos de Firestore si están disponibles, si no muestra valores de ejemplo
    val nombre = tiendaFirestore?.nombre ?: "Kiosco El Cielo"
    val direccion = tiendaFirestore?.direccion ?: "San Martín 456, Palpalá"
    val distancia = "Cerca"
    val rating = "4.8"
    val reseñas = 0
    val horario = "Lun–Sáb 8:00–22:00 · Dom 9:00–20:00"
    var esFavorito by remember { mutableStateOf(false) }
    var tabSeleccionado by remember { mutableIntStateOf(0) }

    val stockMostrar = if (stockFirestore.isNotEmpty()) stockFirestore.map {
        ProductoEnStock("🛒", it.nombre, "$%.0f".format(it.precio), it.cantidad)
    } else stockEjemplo

    val ofertasMostrar = if (ofertasFirestore.isNotEmpty()) ofertasFirestore.map {
        OfertaKiosco("🏷️", it.nombreProducto, "$%.0f".format(it.precioNormal), "$%.0f".format(it.precioOferta))
    } else ofertasEjemplo

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
                        onClick = { navController.navigate(Rutas.MAPA) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // ── Hero ──────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0D1B3E), Color(0xFF1A3A8A), AzulFondo)
                            )
                        )
                        .padding(top = 8.dp, bottom = 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏪", fontSize = 80.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(nombre, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Blanco)
                        Spacer(Modifier.height(6.dp))
                        // Rating pill
                        Surface(shape = RoundedCornerShape(20.dp), color = Blanco.copy(alpha = 0.15f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Default.Star, null, tint = Amarillo, modifier = Modifier.size(16.dp))
                                Text(rating, fontWeight = FontWeight.Bold, color = Blanco, fontSize = 14.sp)
                                Text("($reseñas reseñas)", color = Blanco.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ── Info básica ───────────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = AzulFondo,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                        // Dirección y horario
                        InfoFila(Icons.Default.LocationOn, Azul, direccion)
                        Spacer(Modifier.height(10.dp))
                        InfoFila(Icons.Default.Schedule, Verde, horario)

                        Spacer(Modifier.height(20.dp))

                        // Stats rápidos
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatChip("${stockMostrar.size}", "Productos", Azul, modifier = Modifier.weight(1f))
                            StatChip("${ofertasMostrar.size}", "Ofertas", Rojo, modifier = Modifier.weight(1f))
                            StatChip(distancia, "Distancia", Verde, modifier = Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── Tabs Stock | Ofertas ──────────────────────────────
                        TabRow(
                            selectedTabIndex = tabSeleccionado,
                            containerColor = Color.Transparent,
                            contentColor = AzulNavy,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabSeleccionado]),
                                    color = Rojo,
                                    height = 3.dp,
                                )
                            },
                            divider = { HorizontalDivider(color = Gris200) },
                        ) {
                            Tab(
                                selected = tabSeleccionado == 0,
                                onClick = { tabSeleccionado = 0 },
                                text = {
                                    Text(
                                        "Stock (${stockMostrar.size})",
                                        fontWeight = if (tabSeleccionado == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                    )
                                },
                                selectedContentColor = AzulNavy,
                                unselectedContentColor = Gris500,
                            )
                            Tab(
                                selected = tabSeleccionado == 1,
                                onClick = { tabSeleccionado = 1 },
                                text = {
                                    Text(
                                        "Ofertas (${ofertasMostrar.size})",
                                        fontWeight = if (tabSeleccionado == 1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                    )
                                },
                                selectedContentColor = AzulNavy,
                                unselectedContentColor = Gris500,
                            )
                        }
                    }
                }
            }

            // ── Contenido del tab ─────────────────────────────────────────────
            when (tabSeleccionado) {
                0 -> items(stockMostrar) { prod ->
                    StockItem(prod) {
                        navController.navigate(Rutas.detalleProducto(prod.nombre))
                    }
                }
                1 -> {
                    if (ofertasMostrar.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No hay ofertas activas", color = Gris500)
                            }
                        }
                    } else {
                        items(ofertasMostrar) { oferta -> OfertaItem(oferta) }
                    }
                }
            }
        }
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun InfoFila(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 13.sp, color = Gris700)
    }
}

@Composable
private fun StatChip(valor: String, etiqueta: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = Blanco, shadowElevation = 1.dp, modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
            Text(etiqueta, fontSize = 11.sp, color = Gris500)
        }
    }
}

@Composable
private fun StockItem(prod: ProductoEnStock, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
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
                Text(prod.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                prod.nombre,
                fontWeight = FontWeight.SemiBold,
                color = AzulNavy,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(prod.precio, fontWeight = FontWeight.ExtraBold, color = Rojo, fontSize = 15.sp)
                val stockColor = if (prod.stock > 5) Verde else Naranja
                Text("Stock: ${prod.stock}", fontSize = 11.sp, color = stockColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun OfertaItem(oferta: OfertaKiosco) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(Rojo),
            )
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RojoClaro),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(oferta.emoji, fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(oferta.nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(oferta.precioNormal, fontSize = 12.sp, color = Gris500,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                        Surface(shape = RoundedCornerShape(6.dp), color = RojoClaro) {
                            Text(
                                oferta.precioOferta,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Rojo,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
