package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Wrapper para el flujo de dueño — lee del ViewModel y delega al composable unificado. */
@Composable
fun PantallaAgregarFiado(
    clienteId: String,
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    vm: DuenoViewModel,
    tipoInicial: String = "cargo"
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val cliente = clientes.find { it.id == clienteId }

    PantallaAgregarFiadoUI(
        tipoInicial = tipoInicial,
        onVolver = onVolver,
        cargando = cargando,
        error = error,
        deudaActual = cliente?.deudaTotal ?: 0.0,
        limiteCredito = cliente?.limiteCredito ?: 0.0,
        nombreCliente = cliente?.nombre ?: "",
        notaInterna = cliente?.notaInterna ?: "",
        mostrarVisitante = true,
        onConfirmar = { items, tipo, visitante ->
            vm.agregarMovimientos(clienteId, items, tipo, visitante, onGuardado)
        }
    )
}

/** Versión para el flujo de empleado — recibe datos y callback desde Navegacion. */
@Composable
fun PantallaAgregarFiadoConLambda(
    onVolver: () -> Unit,
    onConfirmar: (items: List<Triple<String, Int, Double>>, tipo: String, visitante: String) -> Unit,
    cargando: Boolean = false,
    error: String? = null,
    deudaActual: Double = 0.0,
    limiteCredito: Double = 0.0,
    nombreCliente: String = "",
    nombreEmpleado: String = "",
) {
    PantallaAgregarFiadoUI(
        onVolver = onVolver,
        cargando = cargando,
        error = error,
        deudaActual = deudaActual,
        limiteCredito = limiteCredito,
        nombreCliente = nombreCliente,
        nombreEmpleado = nombreEmpleado,
        mostrarVisitante = false,
        onConfirmar = { items, tipo, visitante -> onConfirmar(items, tipo, visitante) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantallaAgregarFiadoUI(
    tipoInicial: String = "cargo",
    onVolver: (() -> Unit)? = null,
    cargando: Boolean = false,
    error: String? = null,
    deudaActual: Double = 0.0,
    limiteCredito: Double = 0.0,
    nombreCliente: String = "",
    nombreEmpleado: String = "",
    notaInterna: String = "",
    mostrarVisitante: Boolean = false,
    onConfirmar: (items: List<Triple<String, Int, Double>>, tipo: String, visitante: String) -> Unit
) {
    val tipo = tipoInicial
    var monto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var visitante by remember { mutableStateOf("") }
    val montoFocus = remember { FocusRequester() }
    val descripcionFocus = remember { FocusRequester() }
    val visitanteFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { montoFocus.requestFocus() }

    val montoDouble = monto.toDoubleOrNull() ?: 0.0
    val deudaFinal = if (tipo == "cargo") deudaActual + montoDouble
                     else deudaActual - montoDouble
    val superaLimite = limiteCredito > 0 && tipo == "cargo" && deudaFinal > limiteCredito

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (tipo == "cargo") "Anotar fiado" else "Registrar pago")
                        if (nombreCliente.isNotBlank())
                            Text(nombreCliente, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (montoDouble > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Va a quedar debiendo", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$${"%.0f".format(deudaFinal.coerceAtLeast(0.0))}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (deudaFinal > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                        }
                        if (superaLimite) {
                            Text("⚠ Va a superar el límite de crédito",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            onConfirmar(
                                listOf(Triple(descripcion.trim(), 1, montoDouble)),
                                tipo,
                                visitante.trim()
                            )
                        },
                        enabled = !cargando && !superaLimite && montoDouble > 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(
                            if (tipo == "cargo") "Confirmar fiado $${"%.0f".format(montoDouble)}"
                            else "Registrar pago $${"%.0f".format(montoDouble)}"
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (notaInterna.isNotBlank()) {
                NotaInternaCard(notaInterna)
            }
            if (deudaActual > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Deuda actual", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${"%.0f".format(deudaActual)}", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
                HorizontalDivider()
            }

            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Monto $") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = if (tipo == "cargo") ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { if (tipo == "cargo") descripcionFocus.requestFocus() },
                    onDone = { if (mostrarVisitante) visitanteFocus.requestFocus() }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(montoFocus),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (tipo == "cargo") MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                )
            )

            if (tipo == "cargo") {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    placeholder = { Text("Ej: pan, leche, aceite...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (mostrarVisitante) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { visitanteFocus.requestFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(descripcionFocus)
                )
            }

            if (nombreEmpleado.isNotBlank()) {
                Text("Recibido por: $nombreEmpleado", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (mostrarVisitante) {
                OutlinedTextField(
                    value = visitante,
                    onValueChange = { visitante = it },
                    label = { Text("¿Quién vino? (opcional)") },
                    placeholder = { Text("Ej: María, la hija") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().focusRequester(visitanteFocus),
                    trailingIcon = if (visitante.isNotBlank()) {
                        { IconButton(onClick = { visitante = "" }) { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)) } }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun NotaInternaCard(nota: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = Color(0xFFF57F17))
            Column {
                Text("Nota del cliente", style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF57F17), fontWeight = FontWeight.SemiBold)
                Text(nota, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037))
            }
        }
    }
}
