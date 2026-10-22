package com.kosking.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

// ── Datos de ejemplo ────────────────────────────────────────────────────────

private data class Categoria(val icono: String, val nombre: String)
private data class ProductoPopular(val emoji: String, val nombre: String, val precio: String, val tienda: String)

private val categorias = listOf(
    Categoria("🥤", "Bebidas"),
    Categoria("🍞", "Alimentos"),
    Categoria("🍪", "Snacks"),
    Categoria("🥛", "Lácteos"),
    Categoria("🧹", "Limpieza"),
    Categoria("💊", "Farmacia"),
)

private val productosPopulares = listOf(
    ProductoPopular("🥤", "Coca-Cola 500ml", "$1.000", "Kiosco El Cielo"),
    ProductoPopular("🍪", "Oreo 124g", "$1.200", "Kiosco La Esquina"),
    ProductoPopular("🍟", "Lays Clásicas", "$950", "Maxikiosco 24"),
    ProductoPopular("🥛", "Leche La Serenísima", "$1.800", "Kiosco Don Juan"),
    ProductoPopular("🧃", "Jugo Cepita 1L", "$1.100", "Kiosco El Cielo"),
)

// ── Pantalla ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHome(navController: NavController) {
    // navegar a búsqueda con query vacío abre la pantalla con teclado activo
    var tabSeleccionado by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = AzulFondo,
        bottomBar = {
            Column {
                HorizontalDivider(color = Gris200, thickness = 1.dp)
                NavigationBar(
                    containerColor = Blanco,
                    tonalElevation = 0.dp,
                ) {
                NavigationBarItem(
                    selected = tabSeleccionado == 0,
                    onClick = { tabSeleccionado = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Rojo,
                        selectedTextColor = Rojo,
                        indicatorColor = RojoClaro,
                        unselectedIconColor = Gris500,
                        unselectedTextColor = Gris500,
                    ),
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 1,
                    onClick = { tabSeleccionado = 1 },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Mapa") },
                    label = { Text("Mapa") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Rojo,
                        selectedTextColor = Rojo,
                        indicatorColor = RojoClaro,
                        unselectedIconColor = Gris500,
                        unselectedTextColor = Gris500,
                    ),
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 2,
                    onClick = { tabSeleccionado = 2 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritos") },
                    label = { Text("Favoritos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Rojo,
                        selectedTextColor = Rojo,
                        indicatorColor = RojoClaro,
                        unselectedIconColor = Gris500,
                        unselectedTextColor = Gris500,
                    ),
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 3,
                    onClick = { navController.navigate(Rutas.PANEL_DUENO) },
                    icon = { Icon(Icons.Default.Store, contentDescription = "Mi local") },
                    label = { Text("Mi local") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Rojo,
                        selectedTextColor = Rojo,
                        indicatorColor = RojoClaro,
                        unselectedIconColor = Gris500,
                        unselectedTextColor = Gris500,
                    ),
                )
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
            // ── Header con esquinas redondeadas abajo ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A))))
                    .padding(top = 48.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
            ) {
                Column {
                    // Logo + ubicación + notificaciones
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏪", fontSize = 26.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Kosking",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Rojo,
                                letterSpacing = (-0.5).sp,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Blanco.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "Palpalá, Jujuy",
                                    fontSize = 12.sp,
                                    color = Blanco.copy(alpha = 0.6f),
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Blanco.copy(alpha = 0.85f))
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Barra de búsqueda — navega a resultados al tocar
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Blanco,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth().clickable {
                            navController.navigate(Rutas.resultados(""))
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Rojo)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Buscar productos, marcas o kioscos...",
                                color = Gris500,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Banner ofertas ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Rojo, Color(0xFFFF6B6B))))
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ofertas cerca tuyo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Blanco)
                        Spacer(Modifier.height(4.dp))
                        Text("Descuentos en kioscos de tu barrio", fontSize = 13.sp, color = Blanco.copy(alpha = 0.9f))
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Blanco,
                            modifier = Modifier.clickable { },
                        ) {
                            Text(
                                "Ver promociones",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = Rojo,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    Text("🏷️", fontSize = 56.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Categorías ───────────────────────────────────────────────────
            Text(
                "Categorías",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AzulNavy,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(categorias) { cat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(AzulClaro),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(cat.icono, fontSize = 26.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(cat.nombre, fontSize = 12.sp, color = AzulNavy, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Productos populares ───────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Productos populares", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AzulNavy, modifier = Modifier.weight(1f))
                TextButton(onClick = {}) {
                    Text("Ver todos", color = Rojo, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(productosPopulares) { prod ->
                    ProductoCard(prod)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Kioscos cercanos ──────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Kioscos cercanos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AzulNavy, modifier = Modifier.weight(1f))
                TextButton(onClick = { tabSeleccionado = 1 }) {
                    Text("Ver mapa", color = Rojo, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KioscoItem("Kiosco El Cielo", "📍 A 3 cuadras", "4.8", 120) { navController.navigate(Rutas.detalleLocal(it)) }
                KioscoItem("Kiosco La Esquina", "📍 A 5 cuadras", "4.6", 87) { navController.navigate(Rutas.detalleLocal(it)) }
                KioscoItem("Maxikiosco 24", "📍 A 7 cuadras", "4.4", 53) { navController.navigate(Rutas.detalleLocal(it)) }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProductoCard(prod: ProductoPopular) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier.width(140.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Gris100),
                contentAlignment = Alignment.Center,
            ) {
                Text(prod.emoji, fontSize = 40.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(prod.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AzulNavy, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(prod.precio, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Rojo)
            Spacer(Modifier.height(2.dp))
            Text(prod.tienda, fontSize = 11.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun KioscoItem(nombre: String, distancia: String, rating: String, reseñas: Int, onClick: (String) -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick(nombre) },
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Acento azul lateral
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(Azul),
            )
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AzulClaro),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🏪", fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 14.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(distancia.removePrefix("📍 "), fontSize = 12.sp, color = Gris500)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AzulClaro,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Star, null, tint = Azul, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(rating, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AzulNavy)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("$reseñas reseñas", fontSize = 10.sp, color = Gris500)
                }
            }
        }
    }
}
