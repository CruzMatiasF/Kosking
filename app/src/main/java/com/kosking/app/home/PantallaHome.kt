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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.modelo.TIPOS_DE_LOCAL
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*
import com.kosking.app.util.estaAbierto



private data class ProductoPopular(val emoji: String, val nombre: String, val precio: String, val tienda: String)

// ── Pantalla ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHome(navController: NavController, vm: HomeViewModel = viewModel()) {
    val kioscosFirestore by vm.kioscos.collectAsStateWithLifecycle()
    val productosFirestore by vm.productosPopulares.collectAsStateWithLifecycle()
    var tabSeleccionado by remember { mutableIntStateOf(0) }
    val user = FirebaseAuth.getInstance().currentUser
    val nombreUsuario = user?.displayName ?: user?.email?.substringBefore("@") ?: "Vos"

    Scaffold(
        containerColor = AzulFondo,
        bottomBar = {
            Column {
                HorizontalDivider(color = Gris200, thickness = 1.dp)
                NavigationBar(containerColor = Blanco, tonalElevation = 0.dp) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Rojo, selectedTextColor = Rojo,
                        indicatorColor = RojoClaro,
                        unselectedIconColor = Gris500, unselectedTextColor = Gris500,
                    )
                    NavigationBarItem(
                        selected = tabSeleccionado == 0,
                        onClick = { tabSeleccionado = 0 },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Inicio") },
                        colors = navColors,
                    )
                    NavigationBarItem(
                        selected = tabSeleccionado == 1,
                        onClick = { navController.navigate(Rutas.MAPA) },
                        icon = { Icon(Icons.Default.LocationOn, null) },
                        label = { Text("Mapa") },
                        colors = navColors,
                    )
                    NavigationBarItem(
                        selected = tabSeleccionado == 2,
                        onClick = { navController.navigate(Rutas.FAVORITOS) },
                        icon = { Icon(Icons.Default.Favorite, null) },
                        label = { Text("Favoritos") },
                        colors = navColors,
                    )
                    NavigationBarItem(
                        selected = tabSeleccionado == 3,
                        onClick = { navController.navigate(Rutas.OFERTAS) },
                        icon = { Icon(Icons.Default.LocalOffer, null) },
                        label = { Text("Ofertas") },
                        colors = navColors,
                    )
                    NavigationBarItem(
                        selected = tabSeleccionado == 4,
                        onClick = { navController.navigate(Rutas.MAS) },
                        icon = { Icon(Icons.Default.MoreHoriz, null) },
                        label = { Text("Más") },
                        colors = navColors,
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
                    .background(HeaderGradient)
                    .padding(top = 48.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
            ) {
                Column {
                    // Avatar + nombre + notificaciones
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Rojo.copy(alpha = 0.85f))
                                .clickable { navController.navigate(Rutas.PERFIL) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                nombreUsuario.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Blanco,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Hola, $nombreUsuario 👋",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Blanco,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Blanco.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(2.dp))
                                Text("Palpalá, Jujuy", fontSize = 12.sp, color = Blanco.copy(alpha = 0.6f))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, null, tint = Blanco.copy(alpha = 0.85f))
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
                            modifier = Modifier.clickable { navController.navigate(Rutas.OFERTAS) },
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

            // ── Tipos de local ────────────────────────────────────────────────
            Text(
                "¿Qué buscás?",
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
                items(TIPOS_DE_LOCAL) { tipo ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            navController.navigate(Rutas.resultados("tipo:${tipo.nombre}"))
                        },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(AzulClaro),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(tipo.emoji, fontSize = 26.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(tipo.nombre, fontSize = 12.sp, color = AzulNavy, fontWeight = FontWeight.Medium)
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

            val prodMostrar = if (productosFirestore.isNotEmpty()) productosFirestore.map {
                ProductoPopular(it.marca.ifBlank { "🛒" }, it.nombre, "$%.0f".format(0.0), it.marca)
            } else emptyList()

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(prodMostrar) { prod ->
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

            if (kioscosFirestore.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No hay kioscos registrados aún", color = Gris500, fontSize = 14.sp)
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    kioscosFirestore.forEach { tienda ->
                        KioscoItem(tienda.nombre, tienda.direccion, estaAbierto(tienda)) {
                            navController.navigate(Rutas.detalleLocal(tienda.id))
                        }
                    }
                }
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
private fun KioscoItem(nombre: String, direccion: String, abierto: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
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
                    Icon(Icons.Default.Store, null, tint = Azul, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 14.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(direccion, fontSize = 12.sp, color = Gris500, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (abierto) Verde.copy(alpha = 0.12f) else Gris200,
                ) {
                    Text(
                        if (abierto) "Abierto" else "Cerrado",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = if (abierto) Verde else Gris500,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
