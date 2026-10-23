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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.modelo.Tienda
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*
import com.kosking.app.util.estaAbierto

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A)),
    endY = 320f,
)

@Composable
fun PantallaResultados(
    queryInicial: String,
    navController: NavController,
    vm: BusquedaViewModel = viewModel(),
) {
    var query by remember { mutableStateOf(queryInicial) }
    var tabSeleccionado by remember { mutableIntStateOf(1) } // Kioscos por defecto
    val focusRequester = remember { FocusRequester() }
    val kioscos = vm.kioscos.value
    val cargando = vm.cargando.value

    LaunchedEffect(Unit) { if (queryInicial.isEmpty()) focusRequester.requestFocus() }
    LaunchedEffect(queryInicial) { if (queryInicial.isNotBlank()) vm.buscar(queryInicial) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; vm.buscar(it) },
                        placeholder = { Text("Buscar kioscos...", color = Blanco.copy(alpha = 0.45f), fontSize = 15.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.buscar(query) }),
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

                Spacer(Modifier.height(12.dp))

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
                                "Productos",
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
                                "Kioscos (${kioscos.size})",
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

        when {
            cargando -> {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Rojo)
                }
            }
            query.isBlank() -> {
                MensajeVacio(
                    icono = Icons.Default.Search,
                    titulo = "Buscá un kiosco",
                    subtitulo = "Escribí el nombre o categoría",
                )
            }
            tabSeleccionado == 1 -> {
                if (kioscos.isEmpty()) {
                    MensajeVacio(
                        icono = Icons.Default.StoreMallDirectory,
                        titulo = "Sin resultados",
                        subtitulo = "No hay kioscos con ese nombre",
                    )
                } else {
                    ListaKioscos(kioscos) { tiendaId ->
                        navController.navigate(Rutas.detalleLocal(tiendaId))
                    }
                }
            }
            else -> {
                MensajeVacio(
                    icono = Icons.Default.Inventory,
                    titulo = "Próximamente",
                    subtitulo = "La búsqueda de productos estará disponible pronto",
                )
            }
        }
    }
}

@Composable
private fun MensajeVacio(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, subtitulo: String) {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icono, null, tint = Gris500, modifier = Modifier.size(48.dp))
            Text(titulo, fontWeight = FontWeight.Bold, color = AzulNavy, fontSize = 16.sp, textAlign = TextAlign.Center)
            Text(subtitulo, color = Gris500, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ListaKioscos(kioscos: List<Tienda>, onKioscoClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(kioscos, key = { it.id }) { kiosco ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Blanco,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().clickable { onKioscoClick(kiosco.id) },
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
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AzulClaro),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Store, null, tint = Azul, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(kiosco.nombre, fontWeight = FontWeight.Bold, color = AzulNavy, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(kiosco.direccion, fontSize = 12.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(shape = RoundedCornerShape(6.dp), color = AzulClaro) {
                                    Text(
                                        kiosco.categoria,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        color = Azul,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                if (estaAbierto(kiosco)) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Verde.copy(alpha = 0.12f)) {
                                        Text(
                                            "Abierto",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            color = Verde,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                } else {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Gris200) {
                                        Text(
                                            "Cerrado",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            color = Gris500,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RojoClaro,
                            modifier = Modifier.clickable { onKioscoClick(kiosco.id) },
                        ) {
                            Text(
                                "Ver",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                color = Rojo,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
