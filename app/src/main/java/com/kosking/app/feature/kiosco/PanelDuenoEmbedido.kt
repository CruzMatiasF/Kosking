package com.kosking.app.feature.kiosco

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.feature.kiosco.tabs.TabPanelDueno
import com.kosking.app.modelo.Cliente
import com.kosking.app.ui.components.BannerOffline
import com.kosking.app.util.WhatsAppUtil
import com.kosking.app.widget.KoskingWidget
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelDuenoEmbedido(
    vm: DuenoViewModel,
    onVerClientes: () -> Unit = {},
    onAbrirAgregarCliente: () -> Unit = {},
    onAbrirDetalleCliente: (String) -> Unit = {},
    onAbrirDetalleMovimiento: (String) -> Unit = {},
    onAbrirProductos: () -> Unit = {},
    esEmpleado: Boolean = false,
    puedeVerDeuda: Boolean = true,
    onCerrarSesionEmpleado: () -> Unit = {},
    onAbrirPerfil: () -> Unit = {},
    onAbrirNotificaciones: () -> Unit = {}
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
    var clientePreseleccionado by remember { mutableStateOf<Cliente?>(null) }
    var tipoAnotar by remember { mutableStateOf("cargo") }
    var mostrarRecordarTodos by remember { mutableStateOf(false) }

    // Solo para dueños: el widget no debe exponer datos del teléfono del empleado
    if (!esEmpleado) {
        LaunchedEffect(totalDeuda, clientesConDeuda.size, localId, localData?.nombre) {
            context.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE).edit()
                .putFloat("total_deuda", totalDeuda.toFloat())
                .putInt("clientes_con_deuda", clientesConDeuda.size)
                .putLong("prefs_timestamp", System.currentTimeMillis())
                .also { if (localId.isNotBlank()) it.putString("local_id", localId) }
                .also { localData?.nombre?.let { n -> if (n.isNotBlank()) it.putString("nombre_local", n) } }
                .apply()
            KoskingWidget.actualizarTodos(context)
        }
    }

    var clienteWhatsApp by remember { mutableStateOf<Cliente?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    data class UltimoAnotado(val cliente: Cliente, val monto: Double, val descripcion: String, val tipo: String, val items: List<Triple<String, Double, Int>> = emptyList())
    var ultimoAnotado by remember { mutableStateOf<UltimoAnotado?>(null) }

    val vmError by vm.error.collectAsState()
    LaunchedEffect(vmError) {
        val msg = vmError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.clearError()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
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
        }
    ) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
            BannerOffline()
            TabPanelDueno(
                clientes = clientes,
                movimientosLocal = movimientosLocal,
                totalDeuda = totalDeuda,
                clientesConDeuda = clientesConDeuda,
                localData = localData,
                disputados = disputados,
                sobreLimite = sobreLimite,
                onVerClientes = onVerClientes,
                onAbrirDetalleCliente = onAbrirDetalleCliente,
                onAbrirDetalleMovimiento = onAbrirDetalleMovimiento,
                onAnotar = { tipoAnotar = "cargo"; mostrarAnotar = true },
                onRegistrarPago = { cliente ->
                    clientePreseleccionado = cliente; tipoAnotar = "pago"; mostrarAnotar = true
                },
                puedeVerDeuda = puedeVerDeuda,
                onRecordarTodos = { if (!esEmpleado) mostrarRecordarTodos = true },
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
                onAbrirProductos = onAbrirProductos,
                esEmpleado = esEmpleado,
                onAbrirPerfil = onAbrirPerfil,
                onAbrirNotificaciones = onAbrirNotificaciones
            )
        }
    }

    // Sheet anotar rápido
    if (mostrarAnotar) {
        AnotarRapidoSheet(
            clientes = clientes,
            movimientos = movimientosLocal,
            cargando = cargando,
            clienteInicial = clientePreseleccionado,
            productos = productos,
            tipoInicial = tipoAnotar,
            onBuscarGlobal = { query -> vm.buscarProductosGlobales(query) },
            onGuardarProducto = { nombre, precio, onExito -> vm.guardarProducto(nombre, precio, null, "", null, onExito) },
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
                            ultimoAnotado = UltimoAnotado(cliente, montoTotal, descripcionItems, tipo, items)
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
                                        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                            Date()
                                        )
                                        WhatsAppUtil.abrirWhatsApp(
                                            context = context,
                                            telefono = ua.cliente.telefono,
                                            mensaje = WhatsAppUtil.ticketVisita(
                                                nombreCliente = ua.cliente.nombre,
                                                nombreLocal = localData?.nombre ?: "el local",
                                                items = ua.items,
                                                tipo = ua.tipo,
                                                hora = hora
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
        val nombreLocalStr = localData?.nombre ?: "el local"
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
                    nombreLocal = nombreLocalStr,
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
        val nombreLocalStr = localData?.nombre ?: "el local"
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
                                            nombreLocal = nombreLocalStr,
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
