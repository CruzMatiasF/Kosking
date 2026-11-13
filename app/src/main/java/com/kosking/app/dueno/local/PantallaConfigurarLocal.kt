package com.kosking.app.dueno.local

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.HorarioDia
import com.kosking.app.modelo.Local

private val DIAS = listOf(
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
fun PantallaConfigurarLocal(
    localData: Local,
    cargando: Boolean = false,
    onGuardar: (nombre: String, direccion: String, telefono: String, horarios: Map<String, HorarioDia>) -> Unit,
    onColaboradores: () -> Unit = {},
    onVolver: () -> Unit
) {
    var nombre by remember(localData) { mutableStateOf(localData.nombre) }
    var direccion by remember(localData) { mutableStateOf(localData.direccion) }
    var telefono by remember(localData) { mutableStateOf(localData.telefono) }

    val horariosMutable = remember(localData) {
        mutableStateMapOf<String, HorarioDia>().also { map ->
            DIAS.forEach { (key, _) ->
                map[key] = localData.horarios[key] ?: HorarioDia()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi local") },
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Datos básicos
            Text(
                "DATOS DEL LOCAL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del local") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Colaboradores
            ListItem(
                headlineContent = { Text("Colaboradores", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("Empleados de tu local", style = MaterialTheme.typography.bodySmall) },
                leadingContent = { Icon(Icons.Default.Group, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.clickable(onClick = onColaboradores)
            )

            HorizontalDivider()

            // Horarios
            Text(
                "HORARIOS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                "Configurá qué días y en qué horario abre tu local.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DIAS.forEach { (key, label) ->
                val horario = horariosMutable[key] ?: HorarioDia()
                HorarioDiaRow(
                    label = label,
                    horario = horario,
                    onCambiar = { horariosMutable[key] = it }
                )
            }

            HorizontalDivider()

            Button(
                onClick = {
                    onGuardar(
                        nombre.trim(),
                        direccion.trim(),
                        telefono.trim(),
                        horariosMutable.toMap()
                    )
                },
                enabled = !cargando && nombre.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Guardar cambios")
            }
        }
    }
}

@Composable
private fun HorarioDiaRow(
    label: String,
    horario: HorarioDia,
    onCambiar: (HorarioDia) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (horario.abierto) "Abierto" else "Cerrado",
                style = MaterialTheme.typography.bodySmall,
                color = if (horario.abierto) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = horario.abierto,
                onCheckedChange = { onCambiar(horario.copy(abierto = it)) }
            )
        }
        if (horario.abierto) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = horario.horaApertura,
                    onValueChange = { v ->
                        if (v.length <= 5) onCambiar(horario.copy(horaApertura = v))
                    },
                    label = { Text("Apertura") },
                    placeholder = { Text("09:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Text("–", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = horario.horaCierre,
                    onValueChange = { v ->
                        if (v.length <= 5) onCambiar(horario.copy(horaCierre = v))
                    },
                    label = { Text("Cierre") },
                    placeholder = { Text("20:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}
