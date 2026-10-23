package com.kosking.app.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
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
import coil.compose.AsyncImage
import com.kosking.app.ui.theme.*
import com.kosking.app.util.estaAbierto
import com.kosking.app.util.textoHorario

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

    val nombre = tiendaFirestore?.nombre ?: ""
    val direccion = tiendaFirestore?.direccion ?: ""
    val horario = tiendaFirestore?.let { textoHorario(it) } ?: ""
    val abierto = tiendaFirestore?.let { estaAbierto(it) } ?: false
    val esFavorito by vm.esFavorito.collectAsStateWithLifecycle()
    var tabSeleccionado by remember { mutableIntStateOf(0) }

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
                    IconButton(onClick = { vm.toggleFavorito() }) {
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
                        onClick = {
                            val t = tiendaFirestore
                            if (t != null && (t.latitud != 0.0 || t.longitud != 0.0))
                                navController.navigate(Rutas.mapa(t.latitud, t.longitud))
                            else
                                navController.navigate(Rutas.MAPA_SIMPLE)
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Azul),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Azul),
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ver en mapa", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { vm.toggleFavorito() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (esFavorito) Verde else Rojo,
                            contentColor = Blanco,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    ) {
                        Icon(
                            if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (esFavorito) "Guardado" else "Guardar",
                            fontWeight = FontWeight.SemiBold,
                        )
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
                        val fotoUrl = tiendaFirestore?.fotoUrl ?: ""
                        if (fotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = fotoUrl,
                                contentDescription = nombre,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                            )
                        } else {
                            Text("🏪", fontSize = 80.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(nombre, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Blanco)
                        if (tiendaFirestore?.categoria?.isNotBlank() == true) {
                            Spacer(Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(20.dp), color = Blanco.copy(alpha = 0.15f)) {
                                Text(
                                    tiendaFirestore!!.categoria,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Blanco,
                                    fontSize = 13.sp,
                                )
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
                            StatChip("${stockFirestore.size}", "Productos", Azul, modifier = Modifier.weight(1f))
                            StatChip("${ofertasFirestore.size}", "Ofertas", Rojo, modifier = Modifier.weight(1f))
                            StatChip(if (abierto) "Abierto" else "Cerrado", "Estado", if (abierto) Verde else Gris500, modifier = Modifier.weight(1f))
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
                                        "Stock (${stockFirestore.size})",
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
                                        "Ofertas (${ofertasFirestore.size})",
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
                0 -> {
                    if (stockFirestore.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Sin productos en stock", color = Gris500)
                            }
                        }
                    } else {
                        items(stockFirestore, key = { it.productoId }) { item ->
                            StockItem(
                                nombre = item.nombre,
                                precio = "$%.0f".format(item.precio),
                                stock = item.cantidad,
                            ) { navController.navigate(Rutas.detalleProducto(item.productoId)) }
                        }
                    }
                }
                1 -> {
                    if (ofertasFirestore.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No hay ofertas activas", color = Gris500)
                            }
                        }
                    } else {
                        items(ofertasFirestore, key = { it.id }) { oferta ->
                            OfertaItem(
                                nombre = oferta.nombreProducto,
                                precioNormal = "$%.0f".format(oferta.precioNormal),
                                precioOferta = "$%.0f".format(oferta.precioOferta),
                            )
                        }
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
private fun StockItem(nombre: String, precio: String, stock: Int, onClick: () -> Unit) {
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
                Icon(Icons.Default.Inventory2, null, tint = Azul, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                nombre,
                fontWeight = FontWeight.SemiBold,
                color = AzulNavy,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(precio, fontWeight = FontWeight.ExtraBold, color = Rojo, fontSize = 15.sp)
                val stockColor = if (stock > 5) Verde else Naranja
                Text("Stock: $stock", fontSize = 11.sp, color = stockColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun OfertaItem(nombre: String, precioNormal: String, precioOferta: String) {
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
                    Icon(Icons.Default.LocalOffer, null, tint = Rojo, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(precioNormal, fontSize = 12.sp, color = Gris500,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                        Surface(shape = RoundedCornerShape(6.dp), color = RojoClaro) {
                            Text(
                                precioOferta,
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
