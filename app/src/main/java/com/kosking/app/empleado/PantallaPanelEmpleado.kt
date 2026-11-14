package com.kosking.app.empleado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.AnotarRapidoSheet
import com.kosking.app.modelo.Cliente
import com.kosking.app.ui.components.BannerOffline
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPanelEmpleado(
    onVerCliente: (String) -> Unit,
    onCerrarSesion: () -> Unit,
    onVolver: () -> Unit = {},
    vm: EmpleadoViewModel
) {
    val nombreLocal by vm.nombreLocal.collectAsState()
    val nombreEmpleado by vm.nombreEmpleado.collectAsState()
    val clientes by vm.clientesHabilitados.collectAsState()
    val movimientos by vm.movimientosLocal.collectAsState()
    val productos by vm.productos.collectAsState()
    var mostrarMenu by remember { mutableStateOf(false) }
    var mostrarConfirmDesvincularse by remember { mutableStateOf(false) }
    var mostrarEditarNombre by remember { mutableStateOf(false) }
    var mostrarPrecios by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var clienteConAlerta by remember { mutableStateOf<Cliente?>(null) }
    var clienteAnotar by remember { mutableStateOf<Cliente?>(null) }
    var mostrarAgregarCliente by remember { mutableStateOf(false) }
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()

    val inicioHoy = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val movsHoy = remember(movimientos) {
        movimientos.filter { it.fecha.toDate().time >= inicioHoy && !it.anulado }
    }
    val fiadosHoy = remember(movsHoy) { movsHoy.count { it.tipo == "cargo" } }
    val cobradoHoy = remember(movsHoy) { movsHoy.filter { it.tipo == "pago" }.sumOf { it.monto } }

    val clientesFiltrados = remember(clientes, busqueda) {
        if (busqueda.isBlank()) clientes
        else clientes.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombreLocal.ifBlank { "Kosking" }) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarPrecios = true }) {
                        Icon(Icons.Default.Sell, "Lista de precios")
                    }
                    Box {
                        IconButton(onClick = { mostrarMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Opciones")
                        }
                        DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Editar mi nombre") },
                                onClick = { mostrarMenu = false; mostrarEditarNombre = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Desvincularme del local") },
                                onClick = { mostrarMenu = false; mostrarConfirmDesvincularse = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                onClick = { mostrarMenu = false; vm.cerrarSesion(); onCerrarSesion() }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            BannerOffline()

            MiniStatsEmpleado(fiadosHoy = fiadosHoy, cobradoHoy = cobradoHoy)

            // Barra de búsqueda + botón nuevo cliente
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            Box {
                                if (busqueda.isEmpty()) Text("Buscar cliente...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                        }
                    )
                    if (busqueda.isNotEmpty()) {
                        Icon(Icons.Default.Close, "Limpiar", Modifier.size(18.dp).clickable { busqueda = "" },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                FilledTonalButton(
                    onClick = { mostrarAgregarCliente = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nuevo", style = MaterialTheme.typography.labelMedium)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (clientesFiltrados.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (clientes.isEmpty()) "No hay clientes todavía" else "Sin resultados",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(clientesFiltrados, key = { it.id }) { cliente ->
                        ClienteItemEmpleado(
                            cliente = cliente,
                            onClick = if (cliente.habilitado) ({
                                if (cliente.alertaEmpleado) clienteConAlerta = cliente
                                else onVerCliente(cliente.id)
                            }) else null,
                            onAnotarRapido = if (cliente.habilitado) ({
                                if (cliente.alertaEmpleado) clienteConAlerta = cliente
                                else clienteAnotar = cliente
                            }) else null
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.clearError() }) { Text("Cerrar") } }
        )
    }

    clienteConAlerta?.let { c ->
        AlertDialog(
            onDismissRequest = { clienteConAlerta = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFE65100)) },
            title = { Text("Atención") },
            text = {
                Text(
                    if (c.notaInterna.isNotBlank()) c.notaInterna
                    else "El dueño marcó a ${c.nombre} con una alerta. Revisá antes de anotar."
                )
            },
            confirmButton = {
                TextButton(onClick = { clienteConAlerta = null; onVerCliente(c.id) }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { clienteConAlerta = null }) { Text("Cancelar") }
            }
        )
    }

    clienteAnotar?.let { c ->
        AnotarRapidoSheet(
            clientes = clientes,
            cargando = cargando,
            clienteInicial = c,
            productos = productos,
            onGuardar = { clienteId, items, tipo ->
                vm.agregarMovimientos(
                    clienteId = clienteId,
                    items = items,
                    tipo = tipo,
                    onExito = { clienteAnotar = null }
                )
            },
            onDismiss = { clienteAnotar = null }
        )
    }

    var nombreEditar by remember(nombreEmpleado) { mutableStateOf(nombreEmpleado) }

    if (mostrarEditarNombre) {
        AlertDialog(
            onDismissRequest = { mostrarEditarNombre = false },
            title = { Text("Editar mi nombre") },
            text = {
                OutlinedTextField(
                    value = nombreEditar,
                    onValueChange = { nombreEditar = it },
                    label = { Text("Tu nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { if (nombreEditar.isNotBlank()) { vm.actualizarMiNombre(nombreEditar.trim()); mostrarEditarNombre = false } },
                    enabled = nombreEditar.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { mostrarEditarNombre = false }) { Text("Cancelar") } }
        )
    }

    if (mostrarPrecios) {
        ListaPreciosSheet(productos = productos, onDismiss = { mostrarPrecios = false })
    }

    if (mostrarAgregarCliente) {
        var nombreNuevo by remember { mutableStateOf("") }
        var telefonoNuevo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { mostrarAgregarCliente = false },
            icon = { Icon(Icons.Default.PersonAdd, null) },
            title = { Text("Nuevo cliente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nombreNuevo,
                        onValueChange = { nombreNuevo = it },
                        label = { Text("Nombre *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = telefonoNuevo,
                        onValueChange = { telefonoNuevo = it },
                        label = { Text("Teléfono (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.agregarCliente(nombreNuevo.trim(), telefonoNuevo.trim()) {
                            mostrarAgregarCliente = false
                        }
                    },
                    enabled = nombreNuevo.isNotBlank() && !cargando
                ) {
                    if (cargando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAgregarCliente = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarConfirmDesvincularse) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmDesvincularse = false },
            title = { Text("Desvincularme del local") },
            text = { Text("¿Querés dejar de trabajar en ${nombreLocal.ifBlank { "este local" }}? Vas a perder el acceso.") },
            confirmButton = {
                TextButton(onClick = { mostrarConfirmDesvincularse = false; vm.desvincularme { onCerrarSesion() } }) {
                    Text("Desvincularme", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmDesvincularse = false }) { Text("Cancelar") }
            }
        )
    }
}

