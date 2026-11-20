package com.kosking.app.scanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfirmarAnotacion(
    items: List<ItemAnotado>,
    clientes: List<Cliente>,
    movimientos: List<Movimiento>,
    cargando: Boolean,
    onConfirmar: (Cliente) -> Unit,
    onCrearYConfirmar: (nombre: String, onClienteCreado: (Cliente) -> Unit) -> Unit = { _, _ -> },
    onVolver: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val clientesOrdenados = remember(clientes, movimientos) {
        clientes.sortedByDescending { cliente ->
            movimientos.filter { it.clienteId == cliente.id }
                .maxOfOrNull { it.fecha.seconds } ?: 0L
        }
    }

    val filtrados = remember(busqueda, clientesOrdenados) {
        if (busqueda.isBlank()) clientesOrdenados
        else clientesOrdenados.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    fun tiempoRelativo(clienteId: String): String {
        val ultimo = movimientos.filter { it.clienteId == clienteId }
            .maxOfOrNull { it.fecha.seconds } ?: return ""
        val diff = System.currentTimeMillis() / 1000 - ultimo
        return when {
            diff < 3600 -> "hace ${diff / 60} min"
            diff < 86400 -> "hoy"
            diff < 172800 -> "ayer"
            diff < 604800 -> "hace ${diff / 86400} días"
            else -> ""
        }
    }

    val total = items.sumOf { it.precio * it.cantidad }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    var mostrarItems by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("¿A quién anotamos?", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Último cliente — acceso rápido con 1 tap
            if (busqueda.isBlank() && clientesOrdenados.isNotEmpty()) {
                val ultimo = clientesOrdenados.first()
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConfirmar(ultimo) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                Icons.Default.History,
                                null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "¿De nuevo para ${ultimo.nombre}?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (ultimo.deudaTotal > 0) {
                                    Text(
                                        "Debe \$${ultimo.deudaTotal.toInt()} actualmente",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider()
            }

            // Buscador de clientes
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar cliente...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            // Crear cliente nuevo si no hay resultados
            if (busqueda.isNotBlank() && filtrados.isEmpty() && !cargando) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCrearYConfirmar(busqueda.trim()) { clienteNuevo ->
                                onConfirmar(clienteNuevo)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                "Crear \"${busqueda.trim()}\" y anotar",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Se agrega como cliente nuevo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            // Lista de clientes (ocupa el espacio disponible)
            if (cargando) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtrados) { cliente ->
                        val tiempo = tiempoRelativo(cliente.id)
                        ListItem(
                            headlineContent = {
                                Text(cliente.nombre, fontWeight = FontWeight.Medium)
                            },
                            supportingContent = when {
                                cliente.deudaTotal > 0 -> { {
                                    Text(
                                        "Debe $${cliente.deudaTotal.toInt()}",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } }
                                tiempo.isNotEmpty() -> { {
                                    Text(
                                        tiempo,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } }
                                else -> null
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable { onConfirmar(cliente) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Resumen de items fijo abajo (expandible hacia arriba)
            HorizontalDivider()
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Lista detallada (expandida hacia arriba)
                    if (mostrarItems) {
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    if (item.cantidad > 1) {
                                        Text(
                                            "x${item.cantidad}  ·  $${item.precio.toInt()} c/u",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    "$${(item.precio * item.cantidad).toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }

                    // Fila resumen / toggle (siempre visible)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarItems = !mostrarItems }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (mostrarItems) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "${items.sumOf { it.cantidad }} producto${if (items.sumOf { it.cantidad } != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Total  $${total.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
