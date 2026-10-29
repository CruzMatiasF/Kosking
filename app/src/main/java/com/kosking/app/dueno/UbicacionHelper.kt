package com.kosking.app.dueno

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

// Pide permiso de ubicación y, si se otorga, llama onUbicacion(lat, lng).
// Retorna una lambda que el botón puede invocar para arrancar el flujo.
@SuppressLint("MissingPermission")
@Composable
fun rememberUbicacionLauncher(
    onUbicacion: (lat: Double, lng: Double) -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val ok = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                 permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            obtenerUbicacion(context, fusedClient, onUbicacion, onError)
        } else {
            onError("Permiso de ubicación denegado")
        }
    }

    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

@SuppressLint("MissingPermission")
private fun obtenerUbicacion(
    context: Context,
    fusedClient: com.google.android.gms.location.FusedLocationProviderClient,
    onUbicacion: (Double, Double) -> Unit,
    onError: (String) -> Unit
) {
    val cts = CancellationTokenSource()
    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
        .addOnSuccessListener { loc: Location? ->
            if (loc != null) onUbicacion(loc.latitude, loc.longitude)
            else onError("No se pudo obtener la ubicación")
        }
        .addOnFailureListener { onError("Error al obtener ubicación") }
}
