package com.kosking.app.cliente

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaIngresarPIN(
    localId: String,
    nombreLocal: String,
    onClienteVerificado: () -> Unit,
    onVolver: () -> Unit,
    vm: ClienteViewModel
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    var pin by remember { mutableStateOf("") }
    var shake by remember { mutableStateOf(false) }

    val offsetX by animateFloatAsState(
        targetValue = if (shake) 10f else 0f,
        animationSpec = if (shake) spring(stiffness = Spring.StiffnessHigh) else spring(),
        finishedListener = { shake = false },
        label = "shake"
    )

    LaunchedEffect(error) {
        if (error != null) {
            shake = true
            pin = ""
        }
    }

    // Auto-verificar al ingresar el 4° dígito
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            vm.verificarPIN(localId, nombreLocal, pin) {
                onClienteVerificado()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombreLocal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Ingresá el PIN que te mandó\n$nombreLocal",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            // Indicador de 4 puntos
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.offset(x = offsetX.dp)
            ) {
                repeat(4) { index ->
                    val lleno = index < pin.length
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (lleno) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(40.dp))

            // Teclado numérico
            TecladoNumeroPIN(
                onDigito = { d -> if (pin.length < 4 && !cargando) pin += d },
                onBorrar = { if (pin.isNotEmpty() && !cargando) pin = pin.dropLast(1) },
                cargando = cargando
            )
        }
    }
}

@Composable
private fun TecladoNumeroPIN(
    onDigito: (String) -> Unit,
    onBorrar: () -> Unit,
    cargando: Boolean
) {
    val filas = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        filas.forEach { fila ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                fila.forEach { tecla ->
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            tecla.isEmpty() -> {}
                            tecla == "⌫" -> OutlinedButton(
                                onClick = onBorrar,
                                enabled = !cargando,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) { Text(tecla, fontSize = 20.sp) }
                            else -> FilledTonalButton(
                                onClick = { onDigito(tecla) },
                                enabled = !cargando,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) { Text(tecla, fontSize = 22.sp) }
                        }
                    }
                }
            }
        }
    }
}
