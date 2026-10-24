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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.datos.HistorialRepository
import com.kosking.app.modelo.StockItemConTienda
import com.kosking.app.modelo.Tienda
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*
import com.kosking.app.util.estaAbierto

@Composable
fun PantallaResultados(
    queryInicial: String,
    navController: NavController,
    vm: BusquedaViewModel = viewModel(),
) {
    val esTipo = vm.esFiltroTipo(queryInicial)
    val tipoNombre = if (esTipo) vm.tipoDeQuery(queryInicial) else ""
    var query by remember { mutableStateOf(if (esTipo) "" else queryInicial) }
    val focusRequester = remember { FocusRequester() }
    val kioscos = vm.kioscos.value
    val stockItems = vm.stockItems.value
    val cargando = vm.cargando.value

    LaunchedEffect(Unit) {
        if (queryInicial.isNotBlank()) vm.buscar(queryInicial)
        else focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize().background(AzulFondo)) {

        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(HeaderGradient)
                .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                }
                Spacer(Modifier.width(4.dp))
                if (esTipo) {
                    Text(
                        tipoNombre,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Blanco,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; vm.buscar(it) },
                        placeholder = { Text("Buscar productos o kioscos...", color = Blanco.copy(alpha = 0.45f), fontSize = 15.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            vm.buscar(query)
                            if (query.isNotBlank()) HistorialRepository.guardar(query)
                        }),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; vm.buscar("") }) {
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
            }
        }

        // ── Resultados ────────────────────────────────────────────────────────
        when {
            cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Rojo)
                }
            }
            !esTipo && query.isBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Search, null, tint = Gris500, modifier = Modifier.size(48.dp))
                        Text("Buscá un kiosco o producto", fontWeight = FontWeight.Bold, color = AzulNavy, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Text("Escribí el nombre, marca o categoría", color = Gris500, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            kioscos.isEmpty() && stockItems.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SearchOff, null, tint = Gris500, modifier = Modifier.size(48.dp))
                        Text("Sin resultados", fontWeight = FontWeight.Bold, color = AzulNavy, fontSize = 16.sp)
                        Text("Probá con otro término", color = Gris500, fontSize = 13.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (kioscos.isNotEmpty()) {
                        item {
                            Text(
                                "Kioscos (${kioscos.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AzulNavy,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        items(kioscos, key = { "k_${it.id}" }) { kiosco ->
                            KioscoResultItem(kiosco) {
                                navController.navigate(Rutas.detalleLocal(kiosco.id))
                            }
                        }
                    }
                    if (stockItems.isNotEmpty()) {
                        item {
                            Text(
                                "Productos (${stockItems.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AzulNavy,
                                modifier = Modifier.padding(top = if (kioscos.isNotEmpty()) 12.dp else 4.dp, bottom = 4.dp),
                            )
                        }
                        items(stockItems, key = { "p_${it.tiendaId}_${it.item.productoId}" }) { stock ->
                            StockResultItem(stock) {
                                navController.navigate(Rutas.detalleLocal(stock.tiendaId))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun KioscoResultItem(kiosco: Tienda, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AzulClaro),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Store, null, tint = Azul, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(kiosco.nombre, fontWeight = FontWeight.Bold, color = AzulNavy, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(kiosco.direccion.ifBlank { "Sin dirección" }, fontSize = 12.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (kiosco.categoria.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(6.dp), color = AzulClaro) {
                                Text(kiosco.categoria, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = Azul, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        val ab = estaAbierto(kiosco)
                        Surface(shape = RoundedCornerShape(6.dp), color = if (ab) Verde.copy(alpha = 0.12f) else Gris200) {
                            Text(if (ab) "Abierto" else "Cerrado", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = if (ab) Verde else Gris500, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockResultItem(stock: StockItemConTienda, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                Icon(Icons.Default.Inventory, null, tint = Azul, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stock.item.nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (stock.item.marca.isNotBlank()) {
                    Text(stock.item.marca, fontSize = 12.sp, color = Gris500, maxLines = 1)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, null, tint = Gris500, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(stock.nombreTienda, fontSize = 12.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("$${"%,.0f".format(stock.item.precio)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Rojo)
                if (stock.item.cantidad in 1..2) {
                    Text("Últimas uds", fontSize = 10.sp, color = Naranja, fontWeight = FontWeight.SemiBold)
                } else if (stock.item.cantidad == 0) {
                    Text("Agotado", fontSize = 10.sp, color = Rojo, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
