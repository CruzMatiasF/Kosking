package com.kosking.app.explorar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.kosking.app.modelo.Local
import kotlinx.coroutines.launch

@Composable
internal fun TabMapa(locales: List<Local>, onVerLocal: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localesConCoordenadas = remember(locales) { locales.filter { it.lat != 0.0 && it.lng != 0.0 } }
    var localSeleccionado by remember { mutableStateOf<Local?>(null) }
    var soloAbiertos by remember { mutableStateOf(false) }
    var ubicacionUsuario by remember { mutableStateOf<LatLng?>(null) }
    var permisoUbicacionDenegado by remember { mutableStateOf(false) }

    val localesFiltrados = remember(localesConCoordenadas, soloAbiertos) {
        if (soloAbiertos) localesConCoordenadas.filter { estaAbiertoAhora(it) }
        else localesConCoordenadas
    }

    val camaraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-34.6037, -58.3816), 11f)
    }

    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val ok = permisos[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                 permisos[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            try {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) ubicacionUsuario = LatLng(loc.latitude, loc.longitude)
                    }
            } catch (_: SecurityException) {}
        } else {
            permisoUbicacionDenegado = true
        }
    }

    LaunchedEffect(Unit) {
        permLauncher.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    LaunchedEffect(ubicacionUsuario) {
        ubicacionUsuario?.let { pos ->
            camaraState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pos, 13f)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camaraState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            properties = MapProperties(isMyLocationEnabled = ubicacionUsuario != null)
        ) {
            localesFiltrados.forEach { local ->
                val abierto = estaAbiertoAhora(local)
                val markerState = remember(local.lat, local.lng) { MarkerState(position = LatLng(local.lat, local.lng)) }
                Marker(
                    state = markerState,
                    title = local.nombre,
                    snippet = buildString {
                        append(local.tipo)
                        val horarioTexto = textoHorarioHoy(local)
                        if (horarioTexto.isNotBlank()) append(" · $horarioTexto")
                    },
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (abierto) BitmapDescriptorFactory.HUE_GREEN
                        else BitmapDescriptorFactory.HUE_RED
                    ),
                    onClick = { localSeleccionado = local; false }
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = soloAbiertos,
                onClick = { soloAbiertos = !soloAbiertos; localSeleccionado = null },
                label = { Text("Solo abiertos") },
                leadingIcon = if (soloAbiertos) ({
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                }) else null
            )
        }

        FloatingActionButton(
            onClick = {
                val pos = ubicacionUsuario
                if (pos != null) {
                    scope.launch {
                        camaraState.animate(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pos, 14f)
                        )
                    }
                } else {
                    permLauncher.launch(arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (localSeleccionado != null) 160.dp else 16.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.MyLocation, "Mi ubicación")
        }

        localSeleccionado?.let { local ->
            val abierto = estaAbiertoAhora(local)
            val horarioTexto = textoHorarioHoy(local)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 72.dp, bottom = 16.dp)
                    .fillMaxWidth()
            ) {
                Card(elevation = CardDefaults.cardElevation(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text(local.nombre, fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Column {
                                    Text(buildString {
                                        append(local.tipo)
                                        if (local.direccion.isNotBlank()) append(" · ${local.direccion}")
                                    })
                                    if (horarioTexto.isNotBlank()) {
                                        Text(horarioTexto,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (abierto) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                        .background(
                                            if (abierto) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(iconParaTipo(local.tipo), null,
                                        tint = if (abierto) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp))
                                }
                            },
                            trailingContent = {
                                Badge(
                                    containerColor = if (abierto) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(if (abierto) "Abierto" else "Cerrado",
                                        color = if (abierto) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { onVerLocal(local.id) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Store, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ver local")
                            }
                            if (local.lat != 0.0 && local.lng != 0.0) {
                                Button(
                                    onClick = {
                                        val uri = "geo:${local.lat},${local.lng}?q=${local.lat},${local.lng}(${Uri.encode(local.nombre)})".toUri()
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Directions, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cómo llegar")
                                }
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { localSeleccionado = null },
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                ) {
                    Icon(Icons.Default.Close, "Cerrar", modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (localesConCoordenadas.isEmpty() && locales.isNotEmpty()) {
            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp, start = 12.dp, end = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ningún local cargó su ubicación todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
