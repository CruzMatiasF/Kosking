package com.kosking.app.dueno.local

import com.kosking.app.dueno.DuenoViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val tiposLocales = listOf(
    "Kiosco", "Almacén", "Maxikiosco", "Ropa", "Corralón", "Farmacia", "Verdulería", "Otro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearLocal(
    onLocalCreado: () -> Unit,
    onVolver: (() -> Unit)? = null,
    vm: DuenoViewModel = viewModel()
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    var nombre by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Kiosco") }
    var expandirTipo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (onVolver != null) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                )
            }
        }
    ) { padding ->
    Column(
        modifier = Modifier.padding(padding)
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏪", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))

        Text(
            "¿Cómo se llama tu local?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Solo necesitás el nombre para empezar.\nPodés agregar más datos después.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del local") },
            placeholder = { Text("Ej: Kiosco Don Pedro") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text("Dirección (opcional)") },
            placeholder = { Text("Ej: Av. San Martín 1234, Rosario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Tipo — chips horizontales
        Text(
            "Tipo de local",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tiposLocales.size) { i ->
                val t = tiposLocales[i]
                FilterChip(
                    selected = tipo == t,
                    onClick = { tipo = t },
                    label = { Text(t) }
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                vm.crearLocal(
                    nombre = nombre.trim(),
                    tipo = tipo,
                    direccion = direccion.trim(),
                    onExito = onLocalCreado
                )
            },
            enabled = !cargando && nombre.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (cargando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Crear mi local", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
    } // Scaffold
}
