package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.modelo.HorarioDia

private val tiposLocales = listOf(
    "Kiosco", "Almacén", "Maxikiosco", "Ropa", "Corralón", "Farmacia", "Verdulería", "Otro"
)

private val DIAS_SEMANA_CREAR = listOf(
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
fun PantallaCrearLocal(
    onLocalCreado: () -> Unit,
    vm: DuenoViewModel = viewModel()
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    var nombre by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Kiosco") }
    var direccion by remember { mutableStateOf("") }
    var expandirTipo by remember { mutableStateOf(false) }

    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }
    var obteniendoUbicacion by remember { mutableStateOf(false) }
    var errorUbicacion by remember { mutableStateOf<String?>(null) }

    val horarios = remember { mutableStateMapOf<String, HorarioDia>() }

    val lanzarUbicacion = rememberUbicacionLauncher(
        onUbicacion = { latObt, lngObt ->
            lat = latObt
            lng = lngObt
            obteniendoUbicacion = false
            errorUbicacion = null
        },
        onError = { msg ->
            errorUbicacion = msg
            obteniendoUbicacion = false
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Creá tu local", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Esto es lo que verán tus clientes al explorar la app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del local *") },
            modifier = Modifier.fillMaxWidth()
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
                tiposLocales.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t) },
                        onClick = { tipo = t; expandirTipo = false }
                    )
                }
            }
        }

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text("Dirección (opcional)") },
            placeholder = { Text("Ej: Av. Corrientes 1234") },
            modifier = Modifier.fillMaxWidth()
        )

        // ── Horarios ──────────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Horarios de atención", style = MaterialTheme.typography.titleSmall)
                Text(
                    "El mapa va a mostrar si tu local está abierto ahora según este horario.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DIAS_SEMANA_CREAR.forEach { (clave, etiqueta) ->
                    val h = horarios[clave] ?: HorarioDia()
                    FilaHorarioCrear(
                        dia = etiqueta,
                        horario = h,
                        onCambio = { nuevo -> horarios[clave] = nuevo }
                    )
                }
            }
        }

        // ── GPS ───────────────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ubicación en el mapa", style = MaterialTheme.typography.titleSmall)
                if (lat != 0.0 && lng != 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp))
                        Text("%.5f, %.5f".format(lat, lng),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Opcional — permite que aparezcas en el mapa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Obteniendo...")
                    } else {
                        Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (lat != 0.0) "Ubicación obtenida ✓" else "Usar mi ubicación")
                    }
                }
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                vm.crearLocal(
                    nombre = nombre,
                    tipo = tipo,
                    direccion = direccion,
                    lat = lat,
                    lng = lng,
                    horarios = horarios.toMap(),
                    onExito = onLocalCreado
                )
            },
            enabled = !cargando && nombre.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Crear local")
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FilaHorarioCrear(
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
                    onValueChange = { v -> apertura = v; onCambio(HorarioDia(v, cierre)) },
                    label = { Text("Abre") },
                    placeholder = { Text("08:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cierre,
                    onValueChange = { v -> cierre = v; onCambio(HorarioDia(apertura, v)) },
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
