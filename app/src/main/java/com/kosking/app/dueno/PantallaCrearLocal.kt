package com.kosking.app.dueno

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kosking.app.modelo.HorarioDia
import java.util.Locale

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
    val context = LocalContext.current
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    var nombre by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Kiosco") }
    var direccion by remember { mutableStateOf("") }
    var expandirTipo by remember { mutableStateOf(false) }
    val horarios = remember { mutableStateMapOf<String, HorarioDia>() }

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
            label = { Text("Dirección") },
            placeholder = { Text("Ej: Av. Corrientes 1234, Buenos Aires") },
            supportingText = { Text("Tus clientes verán esto para encontrarte. También se usa para ubicarte en el mapa.") },
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

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                // Geocodificar la dirección para obtener lat/lng
                var lat = 0.0
                var lng = 0.0
                if (direccion.isNotBlank()) {
                    try {
                        @Suppress("DEPRECATION")
                        val results = Geocoder(context, Locale("es", "AR"))
                            .getFromLocationName(direccion, 1)
                        val addr = results?.firstOrNull()
                        if (addr != null) {
                            lat = addr.latitude
                            lng = addr.longitude
                        }
                    } catch (_: Exception) {}
                }
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
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
