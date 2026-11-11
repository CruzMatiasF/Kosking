package com.kosking.app.dueno.local

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

private data class ResultadoDireccion(
    val descripcion: String,
    val lat: Double,
    val lng: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DireccionBottomSheet(
    direccionActual: String,
    onGuardar: (dir: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf(direccionActual) }
    var resultados by remember { mutableStateOf<List<ResultadoDireccion>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var sinResultados by remember { mutableStateOf(false) }

    var dirSelec by remember { mutableStateOf(direccionActual) }

    val tieneUbicacion = dirSelec.isNotBlank() && dirSelec == query


    fun buscar() {
        val texto = query.trim()
        if (texto.length < 3) return
        keyboard?.hide()
        scope.launch {
            cargando = true
            sinResultados = false
            resultados = emptyList()
            try {
                @Suppress("DEPRECATION")
                val lista = withContext(Dispatchers.IO) {
                    Geocoder(context, Locale("es", "AR"))
                        .getFromLocationName(texto, 5)
                        ?.mapNotNull { addr ->
                            val dir = buildString {
                                addr.thoroughfare?.let { append(it) }
                                addr.subThoroughfare?.let { append(" $it") }
                                addr.locality?.let { append(", $it") }
                                addr.adminArea?.let { append(", $it") }
                            }.ifBlank { addr.getAddressLine(0) ?: return@mapNotNull null }
                            ResultadoDireccion(dir, addr.latitude, addr.longitude)
                        } ?: emptyList()
                }
                resultados = lista
                sinResultados = lista.isEmpty()
            } catch (_: Exception) {
                sinResultados = true
            }
            cargando = false
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ubicación del local",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }

            HorizontalDivider()

            // Campo de búsqueda
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    // limpiar selección si el usuario modifica el texto
                    if (it != dirSelec) { resultados = emptyList(); sinResultados = false }
                },
                placeholder = { Text("Ej: Av. Corrientes 1234, Buenos Aires") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (cargando) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else if (query.isNotBlank()) {
                        IconButton(onClick = ::buscar) {
                            Icon(Icons.Default.Search, "Buscar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { buscar() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                "Escribí la dirección y tocá el ícono de búsqueda o la lupa del teclado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(4.dp))

            // Resultados
            if (resultados.isNotEmpty()) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(resultados) { res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dirSelec = res.descripcion
                                    query = res.descripcion
                                    resultados = emptyList()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationOn, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            val partes = res.descripcion.split(",", limit = 2)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(partes[0].trim(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (partes.size > 1)
                                    Text(partes[1].trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
                    }
                }
            } else if (sinResultados) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "No se encontró esa dirección.\nIntentá con más detalle (ciudad, provincia).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else if (tieneUbicacion) {
                // Mini mapa de confirmación
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            dirSelec,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Botón guardar
            Button(
                onClick = { onGuardar(dirSelec.ifBlank { query.trim() }) },
                enabled = query.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(if (tieneUbicacion) "Guardar ubicación" else "Guardar dirección")
            }
        }
    }
}
