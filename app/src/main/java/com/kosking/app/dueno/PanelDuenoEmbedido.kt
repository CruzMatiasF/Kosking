package com.kosking.app.dueno

import com.kosking.app.dueno.tabs.*
import com.kosking.app.dueno.local.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.kosking.app.modelo.HorarioDia
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
    val empleados by vm.empleados.collectAsState()
    val totalDeuda by vm.totalDeuda.collectAsState()
    val disputados by vm.disputados.collectAsState()
    val sobreLimite by vm.sobreLimite.collectAsState()
    val localData by vm.localData.collectAsState()
    val localId by vm.localId.collectAsState()
    val productos by vm.productos.collectAsState()
    val cargando by vm.cargando.collectAsState()

    BackHandler(enabled = tabSeleccionado != 0) { onTabChange(0) }

    val clientesConDeuda = remember(clientes) {
        clientes.filter { it.deudaTotal > 0 }.sortedByDescending { it.deudaTotal }
    }

    val context = LocalContext.current

    LaunchedEffect(totalDeuda, clientesConDeuda) {
        val prefs = context.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("total_deuda", totalDeuda.toFloat())
            .putInt("clientes_con_deuda", clientesConDeuda.size)
            .putLong("prefs_timestamp", System.currentTimeMillis())
            .apply()
    }

    var mostrarAnotar by remember { mutableStateOf(false) }
    var clientePreseleccionado by remember { mutableStateOf<com.kosking.app.modelo.Cliente?>(null) }
    var mostrarTodasVisitas by remember { mutableStateOf(false) }
    var mostrarRecordarTodos by remember { mutableStateOf(false) }
    var mostrarQr by remember { mutableStateOf(false) }
    var clienteWhatsApp by remember { mutableStateOf<com.kosking.app.modelo.Cliente?>(null) }
    var editandoDatos by remember { mutableStateOf(false) }
    var editandoDireccion by remember { mutableStateOf(false) }
    var editandoHorarios by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    data class UltimoAnotado(val cliente: com.kosking.app.modelo.Cliente, val monto: Double, val descripcion: String, val tipo: String)
    var ultimoAnotado by remember { mutableStateOf<UltimoAnotado?>(null) }

    if (mostrarTodasVisitas) {
        BackHandler { mostrarTodasVisitas = false }
        PantallaTodasVisitas(
            movimientosLocal = movimientosLocal,
            clientes = clientes,
            onVolver = { mostrarTodasVisitas = false }
        )
        return
    }

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
                NavigationBarItem(
                    selected = tabSeleccionado == 0, onClick = { onTabChange(0) },
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 1, onClick = { onTabChange(1) },
                    icon = { Icon(Icons.Default.People, null) }, label = { Text("Clientes") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 2, onClick = { onTabChange(2) },
                    icon = { Icon(Icons.Default.Badge, null) }, label = { Text("Equipo") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tabSeleccionado == 3, onClick = { onTabChange(3) },
                    icon = { Icon(Icons.Default.Storefront, null) }, label = { Text("Precios") },
                    colors = navColors
                )
            }
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
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
                2 -> TabEquipo(
                    empleados = empleados,
                    movimientos = movimientosLocal,
                    onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                    vm = vm
                )
                3 -> TabPrecios(
                    productos = productos,
                    onAgregar = { nombre, precio, categoria, descripcion ->
                        vm.agregarProducto(nombre = nombre, precio = precio, descripcion = descripcion, categoria = categoria)
                    },
                    onActualizar = { vm.actualizarProducto(it) },
                    onEliminar = { vm.eliminarProducto(it) }
                )
                4 -> TabMiLocal(
                    localData = localData,
                    onEditarDatos = { editandoDatos = true },
                    onEditarDireccion = { editandoDireccion = true },
                    onEditarHorarios = { editandoHorarios = true },
                    onMostrarQr = { mostrarQr = true }
                )
                else -> TabInicio(
                    clientes = clientes,
                    movimientosLocal = movimientosLocal,
                    totalDeuda = totalDeuda,
                    clientesConDeuda = clientesConDeuda,
                    localData = localData,
                    onTabChange = onTabChange,
                    onAbrirDetalleCliente = onAbrirDetalleCliente,
                    onAnotar = { mostrarAnotar = true },
                    onMostrarQr = { mostrarQr = true },
                    onRecordarTodos = { mostrarRecordarTodos = true },
                    onVerTodasVisitas = { mostrarTodasVisitas = true },
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
        }
    }

    // Sheet anotar rápido
    if (mostrarAnotar) {
        AnotarRapidoSheet(
            clientes = clientes,
            productos = productos,
            cargando = cargando,
            clienteInicial = clientePreseleccionado,
            onGuardar = { clienteId, producto, monto, tipo ->
                vm.agregarMovimientos(
                    clienteId = clienteId,
                    items = listOf(Triple(producto, 1, monto)),
                    tipo = tipo,
                    onExito = {
                        mostrarAnotar = false
                        clientePreseleccionado = null
                        val cliente = clientes.find { it.id == clienteId }
                        if (cliente != null && cliente.telefono.isNotBlank()) {
                            ultimoAnotado = UltimoAnotado(cliente, monto, producto, tipo)
                            val accion = if (tipo == "cargo") "fiado" else "pago"
                            val montoFmt = "%.0f".format(monto)
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
                                                deudaTotal = ua.cliente.deudaTotal,
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
            onDismiss = { mostrarAnotar = false; clientePreseleccionado = null }
        )
    }

    // WhatsApp desde lista de clientes
    clienteWhatsApp?.let { cliente ->
        val nombreLocal = localData?.nombre ?: "el local"
        scope.launch {
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
    }

    // QR del local
    if (mostrarQr && localId.isNotBlank()) {
        QrLocalSheet(
            localId = localId,
            codigo = localData?.codigo ?: "",
            nombreLocal = localData?.nombre ?: "Mi local",
            onDismiss = { mostrarQr = false }
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
                                Icon(Icons.Default.Send, null, Modifier.size(14.dp))
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

    // Sheet editar datos
    if (editandoDatos) {
        val tiposLocales = listOf("Kiosco", "Almacén", "Maxikiosco", "Ropa", "Corralón", "Farmacia", "Verdulería", "Otro")
        var nombreEdit by remember { mutableStateOf(localData?.nombre.orEmpty()) }
        var tipoEdit by remember { mutableStateOf(localData?.tipo ?: "Kiosco") }
        var telefonoEdit by remember { mutableStateOf(localData?.telefono.orEmpty()) }
        var descripcionEdit by remember { mutableStateOf(localData?.descripcion.orEmpty()) }
        var expandirTipo by remember { mutableStateOf(false) }

        ModalBottomSheet(onDismissRequest = { editandoDatos = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Datos del local", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(value = nombreEdit, onValueChange = { nombreEdit = it }, label = { Text("Nombre del local") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                ExposedDropdownMenuBox(expanded = expandirTipo, onExpandedChange = { expandirTipo = it }) {
                    OutlinedTextField(value = tipoEdit, onValueChange = {}, readOnly = true, label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirTipo) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expandirTipo, onDismissRequest = { expandirTipo = false }) {
                        tiposLocales.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { tipoEdit = t; expandirTipo = false }) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = telefonoEdit, onValueChange = { telefonoEdit = it }, label = { Text("Teléfono") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = descripcionEdit, onValueChange = { descripcionEdit = it }, label = { Text("Descripción") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val local = localData ?: return@Button
                        vm.actualizarLocal(nombre = nombreEdit.trim(), tipo = tipoEdit, direccion = local.direccion,
                            telefono = telefonoEdit.trim(), descripcion = descripcionEdit.trim(), abierto = local.abierto,
                            lat = local.lat.takeIf { it != 0.0 }, lng = local.lng.takeIf { it != 0.0 }, onExito = {})
                        editandoDatos = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar") }
            }
        }
    }

    if (editandoDireccion) {
        DireccionBottomSheet(
            direccionActual = localData?.direccion.orEmpty(),
            latActual = localData?.lat,
            lngActual = localData?.lng,
            onGuardar = { dir, lat, lng ->
                val local = localData ?: return@DireccionBottomSheet
                vm.actualizarLocal(nombre = local.nombre, tipo = local.tipo, direccion = dir,
                    telefono = local.telefono, descripcion = local.descripcion, abierto = local.abierto, lat = lat, lng = lng, onExito = {})
                editandoDireccion = false
            },
            onDismiss = { editandoDireccion = false }
        )
    }

    if (editandoHorarios) {
        val diasClaves = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
        val diasEtiquetas = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        var horariosEdit by remember {
            mutableStateOf(diasClaves.associateWith { clave -> localData?.horarios?.get(clave) ?: HorarioDia() }.toMutableMap())
        }
        ModalBottomSheet(onDismissRequest = { editandoHorarios = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
                Text("Horarios del local", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                diasClaves.forEachIndexed { i, clave ->
                    FilaHorarioPanelDueno(dia = diasEtiquetas[i], horario = horariosEdit[clave] ?: HorarioDia(), onCambio = { nuevo ->
                        horariosEdit = horariosEdit.toMutableMap().also { it[clave] = nuevo }
                    })
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val local = localData ?: return@Button
                        vm.actualizarLocal(nombre = local.nombre, tipo = local.tipo, direccion = local.direccion,
                            telefono = local.telefono, descripcion = local.descripcion, abierto = local.abierto, horarios = horariosEdit, onExito = {})
                        editandoHorarios = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar horarios") }
            }
        }
    }
}

@Composable
private fun FilaHorarioPanelDueno(dia: String, horario: HorarioDia, onCambio: (HorarioDia) -> Unit) {
    var activo by remember(horario) { mutableStateOf(horario.tieneCarga) }
    var apertura by remember(horario) { mutableStateOf(if (horario.tieneCarga) horario.apertura else "08:00") }
    var cierre by remember(horario) { mutableStateOf(if (horario.tieneCarga) horario.cierre else "20:00") }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dia, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = activo, onCheckedChange = { nuevo ->
                activo = nuevo
                onCambio(if (nuevo) HorarioDia(apertura, cierre) else HorarioDia())
            })
        }
        if (activo) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = apertura, onValueChange = { v -> apertura = v; onCambio(HorarioDia(v, cierre)) },
                    label = { Text("Abre") }, placeholder = { Text("08:00") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = cierre, onValueChange = { v -> cierre = v; onCambio(HorarioDia(apertura, v)) },
                    label = { Text("Cierra") }, placeholder = { Text("20:00") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
        }
    }
}
