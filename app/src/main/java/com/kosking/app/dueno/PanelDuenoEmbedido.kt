package com.kosking.app.dueno

import com.kosking.app.dueno.tabs.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kosking.app.datos.PreferenciasRepository
import com.kosking.app.util.BannerOffline
import com.kosking.app.util.WhatsAppUtil
import kotlinx.coroutines.launch

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
    val cargando by vm.cargando.collectAsState()

    BackHandler(enabled = tabSeleccionado != 0) { onTabChange(0) }

    val clientesConDeuda = remember(clientes) {
        clientes.filter { it.deudaTotal > 0 }.sortedByDescending { it.deudaTotal }
    }

    val context = LocalContext.current

    var mostrarAnotar by remember { mutableStateOf(false) }
    var clientePreseleccionado by remember { mutableStateOf<com.kosking.app.modelo.Cliente?>(null) }
    var tipoAnotar by remember { mutableStateOf("cargo") }
    var mostrarRecordarTodos by remember { mutableStateOf(false) }

    // Wizard primer uso: aparece solo si SharedPrefs dice que no fue completado
    // Y además no tiene ni clientes ni productos (reinstalación en teléfono nuevo)
    var wizardCompletado by remember { mutableStateOf(PreferenciasRepository.isWizardCompletado(context)) }
    val localCargado = localData != null
    // Una vez que llegan los datos de Firestore, si ya tiene clientes o productos lo marcamos completado
    LaunchedEffect(localCargado, clientes.size, productos.size) {
        if (localCargado && !wizardCompletado && (clientes.isNotEmpty() || productos.isNotEmpty())) {
            PreferenciasRepository.marcarWizardCompletado(context)
            wizardCompletado = true
        }
    }
    val mostrarWizard = localCargado && !wizardCompletado
    var clienteWhatsApp by remember { mutableStateOf<com.kosking.app.modelo.Cliente?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    data class UltimoAnotado(val cliente: com.kosking.app.modelo.Cliente, val monto: Double, val descripcion: String, val tipo: String, val deudaNueva: Double)
    var ultimoAnotado by remember { mutableStateOf<UltimoAnotado?>(null) }

    Scaffold(
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.tertiary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationBar {
                val alertasInicio = disputados.size + sobreLimite.size
                NavigationBarItem(
                    selected = tabSeleccionado == 0, onClick = { onTabChange(0) },
                    icon = {
                        BadgedBox(badge = {
                            if (alertasInicio > 0) Badge { Text("$alertasInicio") }
                        }) { Icon(Icons.Default.Home, null) }
                    },
                    label = { Text("Inicio") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 1, onClick = { onTabChange(1) },
                    icon = { Icon(Icons.Default.People, null) }, label = { Text("Clientes") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 2, onClick = { onTabChange(2) },
                    icon = { Icon(Icons.Default.Storefront, null) }, label = { Text("Productos") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 3, onClick = { onTabChange(3) },
                    icon = { Icon(Icons.Default.Badge, null) }, label = { Text("Equipo") },
                    colors = navColors
                )
            }
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
            BannerOffline()
            Box(modifier = Modifier.weight(1f)) {
            when (tabSeleccionado) {
                1 -> TabClientes(
                    clientes = clientes,
                    movimientos = movimientosLocal,
                    onVerCliente = onAbrirDetalleCliente,
                    onAgregarCliente = onAbrirAgregarCliente,
                    onAnotarRapido = { cliente ->
                        clientePreseleccionado = cliente
                        mostrarAnotar = true
                    }
                )
                2 -> TabProductos(
                    productos = productos,
                    cargando = cargando,
                    onGuardar = { nombre, precio, productoId -> vm.guardarProducto(nombre, precio, productoId) },
                    onEliminar = { vm.eliminarProducto(it) }
                )
                3 -> TabEquipo(
                    empleados = empleados,
                    movimientos = movimientosLocal,
                    clientes = clientes,
                    onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                    vm = vm
                )
                else -> TabInicio(
                    clientes = clientes,
                    movimientosLocal = movimientosLocal,
                    totalDeuda = totalDeuda,
                    clientesConDeuda = clientesConDeuda,
                    localData = localData,
                    disputados = disputados,
                    sobreLimite = sobreLimite,
                    onTabChange = onTabChange,
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
                    onWhatsAppCliente = { clienteWhatsApp = it }
                )
            }

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
                            val accion = if (tipo == "cargo") "fiado" else "pago"
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

    // Wizard primer uso
    if (mostrarWizard) {
        WizardPrimerUso(
            cargando = cargando,
            onGuardarProducto = { nombre, precio, onExito ->
                vm.guardarProducto(nombre, precio, null, onExito)
            },
            onGuardarCliente = { nombre, telefono, onExito ->
                vm.agregarCliente(nombre = nombre, telefono = telefono, domicilio = "", limiteCredito = 0.0, onExito)
            },
            onTerminar = {
                PreferenciasRepository.marcarWizardCompletado(context)
                wizardCompletado = true
            }
        )
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
