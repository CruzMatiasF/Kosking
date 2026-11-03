package com.kosking.app.dueno

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.modelo.HorarioDia
import java.util.Locale

private val tiposLocalesEditar = listOf(
    "Kiosco", "Almacén", "Maxikiosco", "Ropa", "Corralón", "Farmacia", "Verdulería", "Otro"
)

private val DIAS_SEMANA = listOf(
    "lunes" to "Lunes",
    "martes" to "Martes",
    "miercoles" to "Miércoles",
    "jueves" to "Jueves",
    "viernes" to "Viernes",
    "sabado" to "Sábado",
    "domingo" to "Domingo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarLocal(
    onVolver: () -> Unit,
    vm: DuenoViewModel = viewModel()
) {
    val context = LocalContext.current
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

    // Horarios: mapa mutable día → HorarioDia
    val horarios = remember(localActual) {
        mutableStateMapOf<String, HorarioDia>().also { map ->
            localActual?.horarios?.forEach { (k, v) -> map[k] = v }
        }
    }

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
                placeholder = { Text("Ej: Av. Corrientes 1234, Buenos Aires") },
                supportingText = { Text("Tus clientes verán esto para encontrarte. También se usa para ubicarte en el mapa.") },
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

            // ── Horarios ──────────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Horarios de atención", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Si cargás horarios, el mapa muestra automáticamente si estás abierto ahora.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DIAS_SEMANA.forEach { (clave, etiqueta) ->
                        val h = horarios[clave] ?: HorarioDia()
                        FilaHorario(
                            dia = etiqueta,
                            horario = h,
                            onCambio = { nuevo -> horarios[clave] = nuevo }
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Local abierto") },
                    supportingContent = { Text(if (abierto) "Visible como abierto en la vidriera" else "Visible como cerrado") },
                    trailingContent = { Switch(checked = abierto, onCheckedChange = { abierto = it }) }
                )
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    var lat: Double? = localActual?.lat?.takeIf { it != 0.0 }
                    var lng: Double? = localActual?.lng?.takeIf { it != 0.0 }
                    val dirTrim = direccion.trim()
                    // Geocodificar si la dirección cambió o no hay coordenadas aún
                    if (dirTrim.isNotBlank() && dirTrim != (localActual?.direccion ?: "")) {
                        try {
                            @Suppress("DEPRECATION")
                            val results = Geocoder(context, Locale("es", "AR"))
                                .getFromLocationName(dirTrim, 1)
                            val addr = results?.firstOrNull()
                            if (addr != null) {
                                lat = addr.latitude
                                lng = addr.longitude
                            }
                        } catch (_: Exception) {}
                    }
                    vm.actualizarLocal(
                        nombre = nombre.trim(),
                        tipo = tipo,
                        direccion = dirTrim,
                        telefono = telefono.trim(),
                        descripcion = descripcion.trim(),
                        abierto = abierto,
                        lat = lat,
                        lng = lng,
                        horarios = horarios.toMap().ifEmpty { null },
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

// Fila de un día con toggle + campos de hora
@Composable
private fun FilaHorario(
    dia: String,
    horario: HorarioDia,
    onCambio: (HorarioDia) -> Unit
) {
    var activo by remember(horario) { mutableStateOf(horario.tieneCarga) }
    var apertura by remember(horario) { mutableStateOf(if (horario.tieneCarga) horario.apertura else "08:00") }
    var cierre by remember(horario) { mutableStateOf(if (horario.tieneCarga) horario.cierre else "20:00") }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dia, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(
                checked = activo,
                onCheckedChange = { nuevo ->
                    activo = nuevo
                    onCambio(if (nuevo) HorarioDia(apertura, cierre) else HorarioDia())
                }
            )
        }
        if (activo) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = apertura,
                    onValueChange = { v ->
                        apertura = v
                        onCambio(HorarioDia(v, cierre))
                    },
                    label = { Text("Abre") },
                    placeholder = { Text("08:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cierre,
                    onValueChange = { v ->
                        cierre = v
                        onCambio(HorarioDia(apertura, v))
                    },
                    label = { Text("Cierra") },
                    placeholder = { Text("20:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
