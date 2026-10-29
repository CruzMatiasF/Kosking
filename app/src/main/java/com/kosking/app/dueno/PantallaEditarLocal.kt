package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val tiposLocalesEditar = listOf(
    "Kiosco", "Almacén", "Maxikiosco", "Ropa", "Corralón", "Farmacia", "Verdulería", "Otro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarLocal(
    onVolver: () -> Unit,
    vm: DuenoViewModel = viewModel()
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    val nombreLocalActual by vm.nombreLocal.collectAsState()
    val localActual by vm.localData.collectAsState()

    var nombre by remember(nombreLocalActual) { mutableStateOf(nombreLocalActual) }
    var tipo by remember(localActual) { mutableStateOf(localActual?.tipo ?: "Kiosco") }
    var direccion by remember(localActual) { mutableStateOf(localActual?.direccion ?: "") }
    var telefono by remember(localActual) { mutableStateOf(localActual?.telefono ?: "") }
    var descripcion by remember(localActual) { mutableStateOf(localActual?.descripcion ?: "") }
    var abierto by remember(localActual) { mutableStateOf(localActual?.abierto ?: true) }
    var expandirTipo by remember { mutableStateOf(false) }

    var latGuardada by remember(localActual) { mutableStateOf(localActual?.lat ?: 0.0) }
    var lngGuardada by remember(localActual) { mutableStateOf(localActual?.lng ?: 0.0) }
    var errorUbicacion by remember { mutableStateOf<String?>(null) }
    var obteniendoUbicacion by remember { mutableStateOf(false) }

    val lanzarUbicacion = rememberUbicacionLauncher(
        onUbicacion = { lat, lng ->
            latGuardada = lat
            lngGuardada = lng
            obteniendoUbicacion = false
            errorUbicacion = null
        },
        onError = { msg ->
            errorUbicacion = msg
            obteniendoUbicacion = false
        }
    )

    LaunchedEffect(Unit) { vm.cargarDatosLocal() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar local") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Esta info aparece en la vidriera pública que ven tus clientes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del local *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = expandirTipo,
                onExpandedChange = { expandirTipo = it }
            ) {
                OutlinedTextField(
                    value = tipo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de local") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirTipo) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandirTipo,
                    onDismissRequest = { expandirTipo = false }
                ) {
                    tiposLocalesEditar.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = { tipo = t; expandirTipo = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono / WhatsApp") },
                placeholder = { Text("Ej: 3515551234") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección") },
                placeholder = { Text("Ej: Av. Corrientes 1234") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción (opcional)") },
                placeholder = { Text("Ej: Atendemos de 8 a 22hs. Delivery disponible.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Sección de ubicación GPS
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ubicación en el mapa", style = MaterialTheme.typography.titleSmall)
                    if (latGuardada != 0.0 && lngGuardada != 0.0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "%.5f, %.5f".format(latGuardada, lngGuardada),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            "Sin ubicación cargada — tu local no aparecerá en el mapa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    errorUbicacion?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { obteniendoUbicacion = true; lanzarUbicacion() },
                        enabled = !obteniendoUbicacion,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (obteniendoUbicacion) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Obteniendo ubicación...")
                        } else {
                            Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (latGuardada != 0.0) "Actualizar ubicación" else "Usar mi ubicación actual")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Local abierto") },
                    supportingContent = { Text(if (abierto) "Visible como abierto en la vidriera" else "Visible como cerrado") },
                    trailingContent = {
                        Switch(checked = abierto, onCheckedChange = { abierto = it })
                    }
                )
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    vm.actualizarLocal(
                        nombre = nombre.trim(),
                        tipo = tipo,
                        direccion = direccion.trim(),
                        telefono = telefono.trim(),
                        descripcion = descripcion.trim(),
                        abierto = abierto,
                        lat = latGuardada.takeIf { it != 0.0 },
                        lng = lngGuardada.takeIf { it != 0.0 },
                        onExito = onVolver
                    )
                },
                enabled = !cargando && nombre.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Guardar cambios")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
