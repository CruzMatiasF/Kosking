package com.kosking.app.busqueda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kosking.app.ui.theme.*

// ── Datos de ejemplo ──────────────────────────────────────────────────────────

private data class ResultadoProducto(
    val emoji: String,
    val nombre: String,
    val precio: String,
    val tienda: String,
    val distancia: String,
    val stock: Int,
)

private data class ResultadoKiosco(
    val nombre: String,
    val distancia: String,
    val rating: String,
    val productos: Int,
    val categoria: String,
)

private val productosEjemplo = listOf(
    ResultadoProducto("🥤", "Coca-Cola 500ml", "$1.000", "Kiosco El Cielo", "3 cuadras", 12),
    ResultadoProducto("🥤", "Coca-Cola 1.5L", "$1.800", "Kiosco La Esquina", "5 cuadras", 6),
    ResultadoProducto("🥤", "Coca-Cola Zero 500ml", "$1.050", "Maxikiosco 24", "7 cuadras", 20),
    ResultadoProducto("🍪", "Oreo Original 124g", "$1.200", "Kiosco El Cielo", "3 cuadras", 8),
    ResultadoProducto("🍟", "Lays Clásicas 150g", "$950", "Kiosco Don Juan", "4 cuadras", 15),
    ResultadoProducto("🧃", "Jugo Cepita Naranja 1L", "$1.100", "Kiosco La Esquina", "5 cuadras", 4),
)

private val kioscosEjemplo = listOf(
    ResultadoKiosco("Kiosco El Cielo", "3 cuadras", "4.8", 245, "Kiosco"),
    ResultadoKiosco("Kiosco La Esquina", "5 cuadras", "4.6", 180, "Kiosco"),
    ResultadoKiosco("Maxikiosco 24", "7 cuadras", "4.4", 310, "Maxikiosco"),
    ResultadoKiosco("Kiosco Don Juan", "4 cuadras", "4.2", 120, "Kiosco"),
)

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A)),
    endY = 320f,
)

// ── Pantalla ──────────────────────────────────────────────────────────────────

@Composable
fun PantallaResultados(
    queryInicial: String,
    navController: NavController,
) {
    // navegar al detalle pasando el id del producto
    var query by remember { mutableStateOf(queryInicial) }
    var tabSeleccionado by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { if (queryInicial.isEmpty()) focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().background(AzulFondo)) {

        // ── Header azul con búsqueda activa ───────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(BG)
                .padding(top = 48.dp, bottom = 0.dp, start = 16.dp, end = 16.dp),
        ) {
            Column {
                // Barra de búsqueda activa
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Buscar productos o kioscos...", color = Blanco.copy(alpha = 0.45f), fontSize = 15.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { /* filtrar */ }),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, null, tint = Blanco.copy(alpha = 0.6f))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Blanco,
                            unfocusedTextColor = Blanco,
                            focusedContainerColor = Blanco.copy(alpha = 0.12f),
                            unfocusedContainerColor = Blanco.copy(alpha = 0.08f),
                            focusedBorderColor = Rojo.copy(alpha = 0.7f),
                            unfocusedBorderColor = Blanco.copy(alpha = 0.25f),
                            cursorColor = Rojo,
                        ),
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Tabs Productos | Kioscos
                TabRow(
                    selectedTabIndex = tabSeleccionado,
                    containerColor = Color.Transparent,
                    contentColor = Blanco,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabSeleccionado]),
                            color = Rojo,
                            height = 3.dp,
                        )
                    },
                    divider = {},
                ) {
                    Tab(
                        selected = tabSeleccionado == 0,
                        onClick = { tabSeleccionado = 0 },
                        text = {
                            Text(
                                "Productos (${productosEjemplo.size})",
                                fontWeight = if (tabSeleccionado == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                            )
                        },
                        selectedContentColor = Blanco,
                        unselectedContentColor = Blanco.copy(alpha = 0.5f),
                    )
                    Tab(
                        selected = tabSeleccionado == 1,
                        onClick = { tabSeleccionado = 1 },
                        text = {
                            Text(
                                "Kioscos (${kioscosEjemplo.size})",
                                fontWeight = if (tabSeleccionado == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                            )
                        },
                        selectedContentColor = Blanco,
                        unselectedContentColor = Blanco.copy(alpha = 0.5f),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Resultados ────────────────────────────────────────────────────────
        when (tabSeleccionado) {
            0 -> ListaProductos(productosEjemplo) { navController.navigate(com.kosking.app.navegacion.Rutas.detalleProducto(it)) }
            1 -> ListaKioscos(kioscosEjemplo)
        }
    }
}

// ── Lista de productos ────────────────────────────────────────────────────────

@Composable
private fun ListaProductos(productos: List<ResultadoProducto>, onProductoClick: (String) -> Unit = {}) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(productos) { prod ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Blanco,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().clickable { onProductoClick(prod.nombre) },
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    // Acento rojo lateral
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
                        // Imagen del producto
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AzulFondo),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(prod.emoji, fontSize = 32.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(prod.tienda, fontSize = 12.sp, color = Gris500)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Azul, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(prod.distancia, fontSize = 11.sp, color = Azul)
                                Spacer(Modifier.width(10.dp))
                                // Badge stock
                                val stockColor = if (prod.stock > 5) Verde else Naranja
                                Surface(shape = RoundedCornerShape(4.dp), color = stockColor.copy(alpha = 0.12f)) {
                                    Text(
                                        "Stock: ${prod.stock}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        color = stockColor,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(prod.precio, fontWeight = FontWeight.ExtraBold, color = Rojo, fontSize = 16.sp)
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AzulClaro,
                                modifier = Modifier.clickable { },
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    null,
                                    tint = Azul,
                                    modifier = Modifier.padding(6.dp).size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Lista de kioscos ──────────────────────────────────────────────────────────

@Composable
private fun ListaKioscos(kioscos: List<ResultadoKiosco>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(kioscos) { kiosco ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Blanco,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().clickable { },
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    // Acento azul lateral
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
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AzulClaro),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🏪", fontSize = 28.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(kiosco.nombre, fontWeight = FontWeight.Bold, color = AzulNavy, fontSize = 15.sp)
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("A ${kiosco.distancia}", fontSize = 12.sp, color = Gris500)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text("${kiosco.productos} productos", fontSize = 12.sp, color = Azul, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = AzulClaro) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Star, null, tint = Azul, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(kiosco.rating, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AzulNavy)
                                }
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = RojoClaro) {
                                Text(
                                    "Ver local",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Rojo,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
