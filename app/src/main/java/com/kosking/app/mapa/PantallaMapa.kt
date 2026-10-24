package com.kosking.app.mapa

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.kosking.app.home.HomeViewModel
import com.kosking.app.modelo.TIPOS_DE_LOCAL
import com.kosking.app.modelo.Tienda
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*
import com.kosking.app.util.estaAbierto
import com.kosking.app.util.textoHorario

private val PALPALA = LatLng(-24.2597, -65.1219)

@Composable
private fun KioscoMarker(abierto: Boolean, categoria: String) {
    val emoji = TIPOS_DE_LOCAL.find { it.nombre == categoria }?.emoji
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(if (abierto) Verde else Gris500, CircleShape)
            .border(3.dp, Blanco, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = 20.sp)
        } else {
            Icon(Icons.Default.Store, contentDescription = null, tint = Blanco, modifier = Modifier.size(22.dp))
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun PantallaMapa(
    navController: NavController,
    vm: HomeViewModel = viewModel(),
    focusLat: Double = 0.0,
    focusLng: Double = 0.0,
) {
    val hasFocus = focusLat != 0.0 || focusLng != 0.0
    val kioscos by vm.kioscos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val initialPos = if (hasFocus) LatLng(focusLat, focusLng) else PALPALA
    val initialZoom = if (hasFocus) 17f else 14f
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, initialZoom)
    }
    var seleccionado by remember { mutableStateOf<Tienda?>(null) }
    var ubicacionHabilitada by remember { mutableStateOf(false) }
    var categoriaFiltro by remember { mutableStateOf<String?>(null) }

    val permisosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ubicacionHabilitada = concedido
        if (concedido && !hasFocus) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        permisosLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))
    }

    val kioscosFiltrados = if (categoriaFiltro != null)
        kioscos.filter { it.categoria == categoriaFiltro }
    else kioscos
    val conUbicacion = kioscosFiltrados.filter { it.latitud != 0.0 || it.longitud != 0.0 }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Mapa ──────────────────────────────────────────────────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
            ),
            properties = MapProperties(isMyLocationEnabled = ubicacionHabilitada),
            onMapClick = { seleccionado = null },
        ) {
            conUbicacion.forEach { tienda ->
                val abiertoAhora = estaAbierto(tienda)
                MarkerComposable(
                    keys = arrayOf(tienda.id, abiertoAhora),
                    state = MarkerState(position = LatLng(tienda.latitud, tienda.longitud)),
                    title = tienda.nombre,
                    onClick = { seleccionado = tienda; false },
                ) {
                    KioscoMarker(abierto = abiertoAhora, categoria = tienda.categoria)
                }
            }
        }

        // ── Panel superior: barra de búsqueda + chips ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 44.dp),
        ) {
            // Barra de búsqueda
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Blanco,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Rutas.resultados("")) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Search, null, tint = Rojo, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Buscar kioscos o productos...",
                        color = Gris500,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Chips de categoría
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Chip "Todos"
                item {
                    val sel = categoriaFiltro == null
                    Surface(
                        shape = CircleShape,
                        color = if (sel) AzulNavy else Blanco,
                        shadowElevation = 3.dp,
                        modifier = Modifier.clickable { categoriaFiltro = null; seleccionado = null },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Apps, null, tint = if (sel) Blanco else Gris500, modifier = Modifier.size(14.dp))
                            Text("Todos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Blanco else AzulNavy)
                        }
                    }
                }
                items(TIPOS_DE_LOCAL) { tipo ->
                    val sel = categoriaFiltro == tipo.nombre
                    Surface(
                        shape = CircleShape,
                        color = if (sel) AzulNavy else Blanco,
                        shadowElevation = 3.dp,
                        modifier = Modifier.clickable { categoriaFiltro = if (sel) null else tipo.nombre; seleccionado = null },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(tipo.emoji, fontSize = 13.sp)
                            Text(tipo.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Blanco else AzulNavy)
                        }
                    }
                }
            }
        }

        // ── Botón Home (abajo izquierda) ─────────────────────────────────────
        val fabBottomPadding = if (seleccionado != null) 230.dp else 96.dp
        SmallFloatingActionButton(
            onClick = {
                navController.navigate(Rutas.HOME) {
                    popUpTo(Rutas.MAPA_SIMPLE) { inclusive = true }
                }
            },
            modifier = Modifier
                .padding(bottom = fabBottomPadding, start = 16.dp)
                .align(Alignment.BottomStart),
            containerColor = Blanco,
            contentColor = AzulNavy,
            elevation = FloatingActionButtonDefaults.elevation(4.dp),
        ) {
            Icon(Icons.Default.Home, null)
        }

        // ── Botón mi ubicación (abajo derecha) ───────────────────────────────
        if (ubicacionHabilitada) {
            SmallFloatingActionButton(
                onClick = {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    fusedClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null) {
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
                        }
                    }
                },
                modifier = Modifier
                    .padding(bottom = fabBottomPadding, end = 16.dp)
                    .align(Alignment.BottomEnd),
                containerColor = Blanco,
                contentColor = Azul,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
            ) {
                Icon(Icons.Default.MyLocation, null)
            }
        }

        // ── Panel inferior: kiosco seleccionado ───────────────────────────────
        seleccionado?.let { tienda ->
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Blanco,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AzulClaro),
                            contentAlignment = Alignment.Center,
                        ) {
                            val emoji = TIPOS_DE_LOCAL.find { it.nombre == tienda.categoria }?.emoji
                            if (emoji != null) Text(emoji, fontSize = 22.sp)
                            else Icon(Icons.Default.Store, null, tint = Azul, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tienda.nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AzulNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(tienda.direccion.ifBlank { "Sin dirección" }, fontSize = 13.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (estaAbierto(tienda)) Verde.copy(alpha = 0.12f) else Gris200,
                        ) {
                            Text(
                                if (estaAbierto(tienda)) "Abierto" else "Cerrado",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 12.sp,
                                color = if (estaAbierto(tienda)) Verde else Gris500,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (tienda.categoria.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Schedule, null, tint = Gris500, modifier = Modifier.size(13.dp))
                            Text(textoHorario(tienda), fontSize = 12.sp, color = Gris500)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { seleccionado = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Gris500),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gris500),
                        ) {
                            Text("Cerrar")
                        }
                        Button(
                            onClick = { navController.navigate(Rutas.detalleLocal(tienda.id)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Rojo),
                        ) {
                            Text("Ver kiosco")
                        }
                    }
                }
            }
        }

        // ── Panel inferior: resumen cuando no hay seleccionado ────────────────
        if (seleccionado == null) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Blanco,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Store, null, tint = Azul, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (categoriaFiltro != null)
                            "${conUbicacion.size} ${categoriaFiltro}s en el mapa"
                        else
                            "${conUbicacion.size} kioscos en el mapa",
                        fontWeight = FontWeight.SemiBold,
                        color = AzulNavy,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (categoriaFiltro != null) {
                        TextButton(onClick = { categoriaFiltro = null }) {
                            Text("Ver todos", color = Azul, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
