package com.kosking.app.mapa

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.kosking.app.modelo.Tienda
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*
import com.kosking.app.util.estaAbierto
import com.kosking.app.util.textoHorario

// Centro de Palpalá, Jujuy (fallback si no hay permisos)
private val PALPALA = LatLng(-24.2597, -65.1219)

@Composable
private fun KioscoMarker(abierto: Boolean) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(if (abierto) Verde else Gris500, CircleShape)
            .border(3.dp, Blanco, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Store,
            contentDescription = null,
            tint = Blanco,
            modifier = Modifier.size(22.dp),
        )
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
                    val pos = LatLng(loc.latitude, loc.longitude)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(pos, 15f))
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
        ) {
            kioscos.filter { it.latitud != 0.0 || it.longitud != 0.0 }.forEach { tienda ->
                val abiertoAhora = estaAbierto(tienda)
                MarkerComposable(
                    keys = arrayOf(tienda.id, abiertoAhora),
                    state = MarkerState(position = LatLng(tienda.latitud, tienda.longitud)),
                    title = tienda.nombre,
                    snippet = tienda.direccion,
                    onClick = { seleccionado = tienda; false },
                ) {
                    KioscoMarker(abierto = abiertoAhora)
                }
            }
        }

        // ── Botón volver ──────────────────────────────────────────────────────
        FloatingActionButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 48.dp, start = 16.dp).align(Alignment.TopStart),
            containerColor = Blanco,
            contentColor = AzulNavy,
            elevation = FloatingActionButtonDefaults.elevation(4.dp),
        ) {
            Icon(Icons.Default.ArrowBack, null)
        }

        // ── Botón mi ubicación ────────────────────────────────────────────────
        if (ubicacionHabilitada) {
            FloatingActionButton(
                onClick = {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    fusedClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null) {
                            val pos = LatLng(loc.latitude, loc.longitude)
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 24.dp, end = 16.dp).align(Alignment.BottomEnd),
                containerColor = Blanco,
                contentColor = Azul,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
            ) {
                Icon(Icons.Default.MyLocation, null)
            }
        }

        // ── Contador kioscos ──────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AzulNavy.copy(alpha = 0.9f),
            modifier = Modifier
                .padding(top = 56.dp, end = 16.dp)
                .align(Alignment.TopEnd),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Store, null, tint = Blanco, modifier = Modifier.size(14.dp))
                Text("${kioscos.size} kioscos", color = Blanco, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Card kiosco seleccionado ──────────────────────────────────────────
        seleccionado?.let { tienda ->
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Blanco,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tienda.nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AzulNavy)
                            Spacer(Modifier.height(4.dp))
                            Text(tienda.direccion.ifBlank { "Sin dirección" }, fontSize = 13.sp, color = Gris500)
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
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { seleccionado = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
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
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Mensaje si no hay kioscos con ubicación ───────────────────────────
        val conUbicacion = kioscos.count { it.latitud != 0.0 || it.longitud != 0.0 }
        if (kioscos.isNotEmpty() && conUbicacion == 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AzulNavy.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    "Los kioscos aún no tienen ubicación configurada. El dueño puede agregarla desde Configurar local.",
                    modifier = Modifier.padding(14.dp),
                    color = Blanco,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
