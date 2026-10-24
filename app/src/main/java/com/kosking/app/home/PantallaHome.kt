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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.kosking.app.modelo.StockItemConTienda
import com.kosking.app.modelo.TIPOS_DE_LOCAL
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*
import com.kosking.app.util.estaAbierto


// ── Pantalla ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHome(navController: NavController, vm: HomeViewModel = viewModel()) {
    val kioscosFirestore by vm.kioscos.collectAsStateWithLifecycle()
    val productosFirestore: List<StockItemConTienda> by vm.productosPopulares.collectAsStateWithLifecycle()
    val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    var tabSeleccionado by remember { mutableIntStateOf(0) }
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }
    var cantKioscosVer by remember { mutableIntStateOf(4) }
    var mostrarPerfil by remember { mutableStateOf(false) }
    val user = FirebaseAuth.getInstance().currentUser
    val nombreUsuario = user?.displayName ?: user?.email?.substringBefore("@") ?: "Vos"

    Scaffold(
        containerColor = AzulFondo,
        bottomBar = {
            Column {
                HorizontalDivider(color = Gris200, thickness = 1.dp)
                NavigationBar(containerColor = AzulNavy, tonalElevation = 0.dp) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Blanco,
                        selectedTextColor = Blanco,
                        indicatorColor = Rojo.copy(alpha = 0.35f),
                        unselectedIconColor = Blanco.copy(alpha = 0.5f),
                        unselectedTextColor = Blanco.copy(alpha = 0.5f),
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
                        onClick = { navController.navigate(Rutas.FAVORITOS) },
                        icon = { Icon(Icons.Default.Favorite, null) },
                        label = { Text("Favoritos") },
                        colors = navColors,
                    )
                    NavigationBarItem(
                        selected = tabSeleccionado == 2,
                        onClick = { navController.navigate(Rutas.MAPA_SIMPLE) },
                        icon = { Icon(Icons.Default.LocationOn, null) },
                        label = { Text("Mapa") },
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Header ───────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(HeaderGradient)
                        .padding(top = 4.dp, bottom = 10.dp, start = 12.dp, end = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logo placeholder — reemplazar con Image() cuando haya logo
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Blanco.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("K", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Blanco)
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Blanco.copy(alpha = 0.95f),
                            shadowElevation = 2.dp,
                            modifier = Modifier.weight(1f).clickable {
                                navController.navigate(Rutas.resultados(""))
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Rojo, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Buscar productos o kioscos...", color = Gris500, fontSize = 13.sp)
                            }
                        }
                        IconButton(onClick = { navController.navigate(Rutas.NOTIFICACIONES) }) {
                            Icon(Icons.Default.Notifications, null, tint = Blanco.copy(alpha = 0.85f))
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Rojo.copy(alpha = 0.85f))
                                .clickable { mostrarPerfil = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                nombreUsuario.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Blanco,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Banner ofertas ────────────────────────────────────────────
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

                Spacer(Modifier.height(20.dp))

                // ── Tipos de local ────────────────────────────────────────────
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(TIPOS_DE_LOCAL) { tipo ->
                        val seleccionado = categoriaSeleccionada == tipo.nombre
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                categoriaSeleccionada = if (seleccionado) null else tipo.nombre
                                cantKioscosVer = 4
                            },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(if (seleccionado) Rojo else AzulClaro),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(tipo.emoji, fontSize = 26.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                tipo.nombre,
                                fontSize = 12.sp,
                                color = if (seleccionado) Rojo else AzulNavy,
                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Kioscos disponibles (horizontal scroll) ───────────────────
                val kioscosFiltrados = if (categoriaSeleccionada != null)
                    kioscosFirestore.filter { it.categoria == categoriaSeleccionada }
                else kioscosFirestore

                Spacer(Modifier.height(8.dp))

                if (kioscosFiltrados.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (categoriaSeleccionada != null) "No hay locales de tipo \"$categoriaSeleccionada\""
                            else "No hay kioscos registrados aún",
                            color = Gris500,
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    val prodCount = productosFirestore.groupBy { it.tiendaId }.mapValues { it.value.size }
                    val kioscosVer = kioscosFiltrados.take(cantKioscosVer)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(kioscosVer, key = { it.id }) { tienda ->
                            KioscoCard(
                                nombre = tienda.nombre,
                                direccion = tienda.direccion,
                                abierto = estaAbierto(tienda),
                                cantProductos = prodCount[tienda.id],
                            ) { navController.navigate(Rutas.detalleLocal(tienda.id)) }
                        }
                        if (cantKioscosVer < kioscosFiltrados.size) {
                            item {
                                val restantes = kioscosFiltrados.size - cantKioscosVer
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = AzulClaro,
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clickable { cantKioscosVer += 4 },
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Add, null, tint = Azul, modifier = Modifier.size(28.dp))
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                "Ver $restantes\nmás",
                                                fontSize = 13.sp,
                                                color = Azul,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Productos disponibles (lista vertical) ────────────────────
                if (productosFirestore.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        productosFirestore.forEach { prod ->
                            ProductoItem(prod) { navController.navigate(Rutas.detalleLocal(prod.tiendaId)) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ── Diálogo de perfil básico ──────────────────────────────────────────────
    if (mostrarPerfil) {
        AlertDialog(
            onDismissRequest = { mostrarPerfil = false },
            containerColor = Blanco,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Rojo.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            nombreUsuario.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Blanco,
                        )
                    }
                    Column {
                        Text(nombreUsuario, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AzulNavy)
                        if (!user?.email.isNullOrBlank()) {
                            Text(user!!.email!!, fontSize = 12.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            text = null,
            confirmButton = {
                Button(
                    onClick = { mostrarPerfil = false; navController.navigate(Rutas.MAS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Rojo),
                ) {
                    Text("Ver perfil completo")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPerfil = false }) {
                    Text("Cerrar", color = Gris500)
                }
            },
        )
    }
}

@Composable
private fun KioscoCard(nombre: String, direccion: String, abierto: Boolean, cantProductos: Int?, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AzulClaro),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Store, null, tint = Azul, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                nombre,
                fontWeight = FontWeight.SemiBold,
                color = AzulNavy,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (direccion.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    direccion,
                    fontSize = 11.sp,
                    color = Gris500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (abierto) Verde.copy(alpha = 0.12f) else Gris200,
            ) {
                Text(
                    if (abierto) "Abierto" else "Cerrado",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    color = if (abierto) Verde else Gris500,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (cantProductos != null && cantProductos > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "$cantProductos en stock",
                    fontSize = 11.sp,
                    color = Azul,
                )
            }
        }
    }
}

@Composable
private fun ProductoItem(prod: StockItemConTienda, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AzulClaro),
                contentAlignment = Alignment.Center,
            ) {
                Text("🛒", fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    prod.item.nombre,
                    fontWeight = FontWeight.SemiBold,
                    color = AzulNavy,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (prod.item.marca.isNotBlank()) {
                    Text(prod.item.marca, fontSize = 12.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, null, tint = Gris500, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        prod.nombreTienda,
                        fontSize = 12.sp,
                        color = Gris500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "$${"%,.0f".format(prod.item.precio)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Rojo,
            )
        }
    }
}
