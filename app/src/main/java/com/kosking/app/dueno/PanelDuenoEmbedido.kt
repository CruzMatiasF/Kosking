package com.kosking.app.dueno

import com.kosking.app.dueno.tabs.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.kosking.app.util.BannerOffline
import com.kosking.app.util.WhatsAppUtil
import kotlinx.coroutines.launch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelDuenoEmbedido(
    vm: DuenoViewModel,
    tabSeleccionado: Int = 0,
    onTabChange: (Int) -> Unit = {},
    onAbrirAgregarCliente: () -> Unit,
    onAbrirDetalleCliente: (String) -> Unit,
    onAbrirAgregarFiado: (clienteId: String, tipo: String) -> Unit,
    onAbrirDetalleMovimiento: (String) -> Unit
) {
    val clientes by vm.clientes.collectAsState()
    val movimientosLocal by vm.movimientosLocal.collectAsState()
    val productos by vm.productos.collectAsState()
    val empleados by vm.empleados.collectAsState()
    val totalDeuda by vm.totalDeuda.collectAsState()
    val disputados by vm.disputados.collectAsState()
    val sobreLimite by vm.sobreLimite.collectAsState()
    val localData by vm.localData.collectAsState()
    val localId by vm.localId.collectAsState()
    val cargando by vm.cargando.collectAsState()

    val clientesConDeuda = remember(clientes) {
        clientes.filter { it.deudaTotal > 0 }.sortedByDescending { it.deudaTotal }
    }

    val context = LocalContext.current

    var mostrarAnotar by remember { mutableStateOf(false) }
    var clientePreseleccionado by remember { mutableStateOf<com.kosking.app.modelo.Cliente?>(null) }
    var tipoAnotar by remember { mutableStateOf("cargo") }
    var mostrarRecordarTodos by remember { mutableStateOf(false) }
    var mostrarEquipo by remember { mutableStateOf(false) }
    var mostrarProductos by remember { mutableStateOf(false) }
    var mostrarClientes by remember { mutableStateOf(false) }

    BackHandler(enabled = mostrarClientes) { mostrarClientes = false }
    BackHandler(enabled = mostrarProductos) { mostrarProductos = false }
    BackHandler(enabled = mostrarEquipo) { mostrarEquipo = false }

    // Actualiza las prefs que leen los workers de notificaciones
    LaunchedEffect(totalDeuda, clientesConDeuda.size, localId) {
        context.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE).edit()
            .putFloat("total_deuda", totalDeuda.toFloat())
            .putInt("clientes_con_deuda", clientesConDeuda.size)
            .putLong("prefs_timestamp", System.currentTimeMillis())
            .also { if (localId.isNotBlank()) it.putString("local_id", localId) }
            .apply()
    }

    var clienteWhatsApp by remember { mutableStateOf<com.kosking.app.modelo.Cliente?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    data class UltimoAnotado(val cliente: com.kosking.app.modelo.Cliente, val monto: Double, val descripcion: String, val tipo: String, val deudaNueva: Double)
    var ultimoAnotado by remember { mutableStateOf<UltimoAnotado?>(null) }

    val vmError by vm.error.collectAsState()
    LaunchedEffect(vmError) {
        val msg = vmError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.clearError()
    }

    Scaffold { scaffoldPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
            BannerOffline()
            Box(modifier = Modifier.weight(1f)) {
            TabInicio(
                clientes = clientes,
                movimientosLocal = movimientosLocal,
                totalDeuda = totalDeuda,
                clientesConDeuda = clientesConDeuda,
                localData = localData,
                empleados = empleados,
                disputados = disputados,
                sobreLimite = sobreLimite,
                onTabChange = { mostrarClientes = true },
                onAbrirDetalleCliente = onAbrirDetalleCliente,
                onAbrirDetalleMovimiento = onAbrirDetalleMovimiento,
                onAnotar = { tipoAnotar = "cargo"; mostrarAnotar = true },
                onRegistrarPago = { cliente -> clientePreseleccionado = cliente; tipoAnotar = "pago"; mostrarAnotar = true },
                onRecordarTodos = { mostrarRecordarTodos = true },
                onCompartirResumen = {
                    val resumen = vm.resumenHoy.value
                    val (movHoy, fiadoHoy, cobradoHoy) = resumen
                    val cantFiados = movHoy.count { it.tipo == "cargo" }
                    val cantPagos = movHoy.count { it.tipo == "pago" }
                    val texto = WhatsAppUtil.mensajeResumenDia(
                        nombreLocal = localData?.nombre ?: "Mi local",
                        fiadoHoy = fiadoHoy,
                        cobradoHoy = cobradoHoy,
                        cantFiados = cantFiados,
                        cantPagos = cantPagos
                    )
                    WhatsAppUtil.compartir(context, texto)
                },
                onWhatsAppCliente = { clienteWhatsApp = it },
                onGestionarEquipo = { mostrarEquipo = true },
                onAbrirProductos = { mostrarProductos = true }
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                snackbar = { data ->
                    Snackbar(
                        action = {
                            TextButton(onClick = { data.performAction() }) {
                                Text(data.visuals.actionLabel ?: "Avisar", color = MaterialTheme.colorScheme.inversePrimary)
                            }
                        },
                        dismissAction = {
                            TextButton(onClick = { data.dismiss() }) {
                                Text("No", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    ) { Text(data.visuals.message) }
                }
            )
            } // Box weight(1f)
            } // Column

            // Clientes como overlay
            if (mostrarClientes) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Clientes") },
                            navigationIcon = {
                                IconButton(onClick = { mostrarClientes = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                                }
                            }
                        )
                        TabClientes(
                            clientes = clientes,
                            movimientos = movimientosLocal,
                            onVerCliente = onAbrirDetalleCliente,
                            onAgregarCliente = onAbrirAgregarCliente,
                            onAnotarRapido = { cliente, tipo ->
                                clientePreseleccionado = cliente
                                tipoAnotar = tipo
                                mostrarAnotar = true
                            }
                        )
                    }
                }
            }

            // Pantalla productos como overlay dentro del panel (sin tab)
            if (mostrarProductos) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Productos") },
                            navigationIcon = {
                                IconButton(onClick = { mostrarProductos = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                                }
                            }
                        )
                        TabProductos(
                            productos = productos,
                            cargando = cargando,
                            onGuardar = { nombre, precio, productoId, onExito -> vm.guardarProducto(nombre, precio, productoId, onExito) },
                            onEliminar = { vm.eliminarProducto(it) }
                        )
                    }
                }
            }

            // Pantalla equipo como overlay dentro del panel (sin tab)
            if (mostrarEquipo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Equipo") },
                            navigationIcon = {
                                IconButton(onClick = { mostrarEquipo = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                                }
                            }
                        )
                        TabEquipo(
                            empleados = empleados,
                            movimientos = movimientosLocal,
                            clientes = clientes,
                            onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                            vm = vm
                        )
                    }
                }
            }
        }
    }

    // Sheet anotar rápido
    if (mostrarAnotar) {
        AnotarRapidoSheet(
            clientes = clientes,
            cargando = cargando,
            clienteInicial = clientePreseleccionado,
            productos = productos,
            tipoInicial = tipoAnotar,
            onGuardar = { clienteId, items, tipo ->
                vm.agregarMovimientos(
                    clienteId = clienteId,
                    items = items,
                    tipo = tipo,
                    onExito = {
                        mostrarAnotar = false
                        clientePreseleccionado = null
                        val cliente = clientes.find { it.id == clienteId }
                        if (cliente != null && cliente.telefono.isNotBlank()) {
                            val montoTotal = items.sumOf { it.second }
                            val descripcionItems = items.filter { it.first.isNotBlank() }.joinToString(", ") { it.first }
                            val deudaNueva = if (tipo == "cargo") cliente.deudaTotal + montoTotal
                                            else (cliente.deudaTotal - montoTotal).coerceAtLeast(0.0)
                            ultimoAnotado = UltimoAnotado(cliente, montoTotal, descripcionItems, tipo, deudaNueva)
                            val accion = if (tipo == "cargo") "anotado" else "pago"
                            val montoFmt = "%.0f".format(montoTotal)
                            scope.launch {
                                val resultado = snackbarHostState.showSnackbar(
                                    message = "Anotado $accion \$$montoFmt a ${cliente.nombre}",
                                    actionLabel = "Avisar por WA",
                                    duration = SnackbarDuration.Long
                                )
                                if (resultado == SnackbarResult.ActionPerformed) {
                                    ultimoAnotado?.let { ua ->
                                        val desc = if (ua.descripcion.isBlank()) accion else ua.descripcion
                                        WhatsAppUtil.abrirWhatsApp(
                                            context = context,
                                            telefono = ua.cliente.telefono,
                                            mensaje = WhatsAppUtil.mensajeDeuda(
                                                nombreCliente = ua.cliente.nombre,
                                                monto = ua.monto,
                                                descripcion = desc,
                                                nombreLocal = localData?.nombre ?: "el local",
                                                deudaTotal = ua.deudaNueva,
                                                tipo = ua.tipo
                                            )
                                        )
                                    }
                                }
                                ultimoAnotado = null
                            }
                        }
                    }
                )
            },
            onDismiss = { mostrarAnotar = false; clientePreseleccionado = null; tipoAnotar = "cargo" }
        )
    }

    // WhatsApp desde lista de clientes
    LaunchedEffect(clienteWhatsApp) {
        val cliente = clienteWhatsApp ?: return@LaunchedEffect
        val nombreLocal = localData?.nombre ?: "el local"
        val resultado = snackbarHostState.showSnackbar(
            message = "Deuda de ${cliente.nombre}: ${"$"}${"%.0f".format(cliente.deudaTotal)}",
            actionLabel = "Avisar por WA",
            duration = SnackbarDuration.Long
        )
        if (resultado == SnackbarResult.ActionPerformed) {
            WhatsAppUtil.abrirWhatsApp(
                context = context,
                telefono = cliente.telefono,
                mensaje = WhatsAppUtil.mensajeResumen(
                    nombreCliente = cliente.nombre,
                    nombreLocal = nombreLocal,
                    deudaTotal = cliente.deudaTotal
                )
            )
        }
        clienteWhatsApp = null
    }


    // BottomSheet recordar a todos
    if (mostrarRecordarTodos) {
        val clientesConFono = remember(clientesConDeuda) {
            clientesConDeuda.filter { it.telefono.isNotBlank() }
        }
        val nombreLocal = localData?.nombre ?: "el local"
        ModalBottomSheet(onDismissRequest = { mostrarRecordarTodos = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Recordar deuda por WhatsApp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "${clientesConFono.size} cliente${if (clientesConFono.size != 1) "s" else ""} con teléfono",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                clientesConFono.forEach { cliente ->
                    ListItem(
                        headlineContent = { Text(cliente.nombre, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(
                                "$${"%.0f".format(cliente.deudaTotal)} de deuda",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            OutlinedButton(
                                onClick = {
                                    WhatsAppUtil.abrirWhatsApp(
                                        context = context,
                                        telefono = cliente.telefono,
                                        mensaje = WhatsAppUtil.mensajeResumen(
                                            nombreCliente = cliente.nombre,
                                            nombreLocal = nombreLocal,
                                            deudaTotal = cliente.deudaTotal
                                        )
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("WA", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}
