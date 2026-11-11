package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarCliente(
    onVolver: () -> Unit,
    vm: DuenoViewModel = viewModel()
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var domicilio by remember { mutableStateOf("") }
    var limiteTexto by remember { mutableStateOf("") }
    var usarLimite by remember { mutableStateOf(false) }
    var mostrarUpgrade by remember { mutableStateOf(false) }

    if (mostrarUpgrade) {
        AlertDialog(
            onDismissRequest = { mostrarUpgrade = false; vm.clearError() },
            icon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Límite del plan gratuito") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("El plan gratuito incluye hasta ${DuenoViewModel.LIMITE_CLIENTES_GRATIS} clientes.")
                    Text(
                        "Próximamente vas a poder desbloquear clientes ilimitados con Kosking Pro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarUpgrade = false; vm.clearError() }) { Text("Entendido") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nuevo cliente")
                        Text(
                            "Completá los datos básicos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre *") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            val telefonoValido = telefono.filter { it.isDigit() }.length >= 8
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono *") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    if (telefono.isNotBlank() && !telefonoValido)
                        Text("Mínimo 8 dígitos — ej: 1134567890", color = MaterialTheme.colorScheme.error)
                    else
                        Text("Ej: 1134567890 (sin 0 ni 15)")
                },
                isError = telefono.isNotBlank() && !telefonoValido
            )
            OutlinedTextField(
                value = domicilio,
                onValueChange = { domicilio = it },
                label = { Text("Domicilio (opcional)") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Límite de crédito
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Límite de crédito",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !usarLimite,
                        onClick = { usarLimite = false; limiteTexto = "" },
                        label = { Text("Sin límite") },
                        leadingIcon = if (!usarLimite) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = usarLimite,
                        onClick = { usarLimite = true },
                        label = { Text("Poner monto") },
                        leadingIcon = if (usarLimite) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (usarLimite) {
                    OutlinedTextField(
                        value = limiteTexto,
                        onValueChange = { limiteTexto = it },
                        label = { Text("Monto límite $") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // El error de límite se muestra como dialog; otros errores como texto
            LaunchedEffect(error) {
                if (error?.contains("Límite") == true) mostrarUpgrade = true
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val limite = if (usarLimite) limiteTexto.toDoubleOrNull() ?: 0.0 else 0.0
                    vm.agregarCliente(nombre, telefono, domicilio, limiteCredito = limite) { onVolver() }
                },
                enabled = !cargando && nombre.isNotBlank() && telefonoValido,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Crear cliente")
            }
        }
    }
}
