package com.kosking.app.dueno

import com.kosking.app.dueno.tabs.*
import com.kosking.app.dueno.detalle.*
import com.kosking.app.dueno.local.*

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.HorarioDia
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.*

private sealed class SubPantalla {
    object AgregarCliente : SubPantalla()
    data class DetalleCliente(val id: String) : SubPantalla()
    data class AgregarFiado(
        val clienteId: String,
        val tipoInicial: String = "cargo"
    ) : SubPantalla()
    data class DetalleMovimiento(val movimientoId: String, val clienteId: String? = null) : SubPantalla()
    object Clientes : SubPantalla()
    object Productos : SubPantalla()
    object Equipo : SubPantalla()
    object TodasLasVisitas : SubPantalla()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelDueno(
    onCerrarSesion: () -> Unit,
    onVolver: () -> Unit = {},
    vm: DuenoViewModel = viewModel()
) {
    val nombreLocal by vm.nombreLocal.collectAsState()
    val codigoLocal by vm.codigoLocal.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val movimientosLocal by vm.movimientosLocal.collectAsState()
    val empleados by vm.empleados.collectAsState()
    val totalDeuda by vm.totalDeuda.collectAsState()
    val fiadoHoy by vm.resumenHoy.collectAsState()
    val disputados by vm.disputados.collectAsState()
    val sobreLimite by vm.sobreLimite.collectAsState()
    val localData by vm.localData.collectAsState()

    var subPantalla by remember { mutableStateOf<SubPantalla?>(null) }
    var editandoDireccionStandalone by remember { mutableStateOf(false) }

    BackHandler(enabled = subPantalla != null) { subPantalla = null }

    when (val sp = subPantalla) {
        is SubPantalla.AgregarCliente -> {
            PantallaAgregarCliente(onVolver = { subPantalla = null }, vm = vm)
            return
        }
        is SubPantalla.DetalleCliente -> {
            PantallaDetalleCliente(
                clienteId = sp.id,
                onVolver = { subPantalla = null },
                onVerMovimiento = { movId ->
                    subPantalla = SubPantalla.DetalleMovimiento(movId, clienteId = sp.id)
                },
                onAnotarFiado = { clienteId -> subPantalla = SubPantalla.AgregarFiado(clienteId, "cargo") },
                onCobrar = { clienteId -> subPantalla = SubPantalla.AgregarFiado(clienteId, "pago") },
                vm = vm
            )
            return
        }
        is SubPantalla.AgregarFiado -> {
            PantallaAgregarFiado(
                clienteId = sp.clienteId,
                onGuardado = { subPantalla = SubPantalla.DetalleCliente(sp.clienteId) },
                onVolver = { subPantalla = SubPantalla.DetalleCliente(sp.clienteId) },
                vm = vm,
                tipoInicial = sp.tipoInicial
            )
            return
        }
        is SubPantalla.DetalleMovimiento -> {
            PantallaDetalleMovimiento(
                movimientoId = sp.movimientoId,
                onVolver = {
                    subPantalla = sp.clienteId?.let { SubPantalla.DetalleCliente(it) }
                },
                vm = vm
            )
            return
        }
        is SubPantalla.Clientes -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Clientes") },
                        navigationIcon = {
                            IconButton(onClick = { subPantalla = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    TabClientes(
                        clientes = clientes,
                        movimientos = movimientosLocal,
                        onVerCliente = { subPantalla = SubPantalla.DetalleCliente(it) },
                        onAgregarCliente = { subPantalla = SubPantalla.AgregarCliente }
                    )
                }
            }
            return
        }
        is SubPantalla.Productos -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Productos") },
                        navigationIcon = {
                            IconButton(onClick = { subPantalla = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    TabProductos(vm = vm)
                }
            }
            return
        }
        is SubPantalla.Equipo -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Equipo") },
                        navigationIcon = {
                            IconButton(onClick = { subPantalla = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    TabEquipo(
                        empleados = empleados,
                        movimientos = movimientosLocal,
                        onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                        vm = vm
                    )
                }
            }
            return
        }
        is SubPantalla.TodasLasVisitas -> {
            val fmtHoraTodas = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            val fmtDia = remember { SimpleDateFormat("dd 'de' MMMM", Locale("es", "AR")) }
            val clienteMapTodas = remember(clientes) { clientes.associateBy { it.id } }
            val todasVisitas = remember(movimientosLocal, clienteMapTodas) {
                agruparVisitas(movimientosLocal.filter { !it.anulado }, clienteMapTodas, fmtHoraTodas)
            }
            val visitasPorFecha = remember(todasVisitas) {
                todasVisitas.groupBy { v ->
                    val mov = movimientosLocal.firstOrNull { m -> m.clienteId == v.clienteId && fmtHoraTodas.format(m.fecha.toDate()) == v.hora }
                    mov?.fecha?.toDate()?.let {
                        val cal = java.util.Calendar.getInstance().apply { time = it }
                        Triple(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.DAY_OF_YEAR), fmtDia.format(it))
                    } ?: Triple(0, 0, "")
                }.entries.sortedByDescending { it.key.first * 1000 + it.key.second }
            }
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Actividad") },
                        navigationIcon = {
                            IconButton(onClick = { subPantalla = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    visitasPorFecha.forEach { (fechaKey, visitasDelDia) ->
                        item(key = "fecha_${fechaKey.first}_${fechaKey.second}") {
                            Text(
                                fechaKey.third,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(visitasDelDia, key = { "all_${it.items.firstOrNull()?.grupoId?.ifBlank { it.items.firstOrNull()?.id } ?: it.hora}" }) { visita ->
                            FilaVisitaDashboard(visita)
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
            return
        }
        null -> {}
    }

    // Dashboard principal
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }
    val alertaTotal = disputados.size + sobreLimite.size
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val movimientosRecientes = remember(movimientosLocal) {
        movimientosLocal.filter { !it.anulado }.take(20)
    }
    val visitas = remember(movimientosRecientes, clienteMap) {
        agruparVisitas(movimientosRecientes, clienteMap, fmtHora)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Hero con foto del local
        item {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                if (localData?.imagenUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = localData!!.imagenUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 80f
                        )
                    )
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        nombreLocal.ifBlank { "Mi local" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (codigoLocal.isNotBlank()) {
                        Text(
                            "CÃ³digo para tus clientes: $codigoLocal",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Deuda total
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Deuda total de clientes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$${"%.0f".format(totalDeuda)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totalDeuda > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                    val direccion = localData?.direccion.orEmpty()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            direccion.ifBlank { "Sin direcciÃ³n" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = { editandoDireccionStandalone = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit, null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    val fiadoHoyVal = fiadoHoy.second
                    val cobradoHoyVal = fiadoHoy.third
                    if (fiadoHoyVal > 0 || cobradoHoyVal > 0) {
                        Text(
                            "+$${"%.0f".format(fiadoHoyVal)} fiado hoy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "âˆ’$${"%.0f".format(cobradoHoyVal)} cobrado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Alertas
        if (alertaTotal > 0) {
            items(disputados, key = { "disp_${it.id}" }) { mov ->
                val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: "Cliente"
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { subPantalla = SubPantalla.DetalleMovimiento(mov.id) }
                ) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
                        headlineContent = {
                            Text(
                                "$nombreCliente no reconoce un fiado de $${"%.0f".format(mov.monto)}",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                    )
                }
            }
            items(sobreLimite, key = { "lim_${it.id}" }) { cliente ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { subPantalla = SubPantalla.Clientes }
                ) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) },
                        headlineContent = {
                            Text("${cliente.nombre} superÃ³ su lÃ­mite", fontWeight = FontWeight.Medium)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                    )
                }
            }
        }

        // Cards de secciones
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeccionCard(
                    icon = Icons.Default.Person,
                    label = "Clientes",
                    modifier = Modifier.weight(1f),
                    onClick = { subPantalla = SubPantalla.Clientes }
                )
                SeccionCard(
                    icon = Icons.Default.ShoppingCart,
                    label = "Productos",
                    modifier = Modifier.weight(1f),
                    onClick = { subPantalla = SubPantalla.Productos }
                )
                SeccionCard(
                    icon = Icons.Default.People,
                    label = "Equipo",
                    modifier = Modifier.weight(1f),
                    onClick = { subPantalla = SubPantalla.Equipo }
                )
            }
        }

        // Ãšltimas visitas (mÃ¡x 4)
        if (visitas.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ÃšLTIMAS VISITAS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { subPantalla = SubPantalla.TodasLasVisitas },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Ver todo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            items(visitas.take(4), key = { "visita_${it.items.firstOrNull()?.grupoId?.ifBlank { it.items.firstOrNull()?.id } ?: it.hora}" }) { visita ->
                FilaVisitaDashboard(visita)
            }
        } else {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("TodavÃ­a no hay movimientos",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (editandoDireccionStandalone) {
        DireccionBottomSheet(
            direccionActual = localData?.direccion.orEmpty(),
            latActual = localData?.lat,
            lngActual = localData?.lng,
            onGuardar = { dir, lat, lng ->
                val local = localData ?: return@DireccionBottomSheet
                vm.actualizarLocal(
                    nombre = local.nombre, tipo = local.tipo, direccion = dir,
                    telefono = local.telefono, descripcion = local.descripcion,
                    abierto = local.abierto, lat = lat, lng = lng, onExito = {}
                )
                editandoDireccionStandalone = false
            },
            onDismiss = { editandoDireccionStandalone = false }
        )
    }
}

internal data class VisitaResumen(
    val clienteId: String,
    val nombreCliente: String,
    val hora: String,
    val totalCargos: Double,
    val totalPagos: Double,
    val items: List<Movimiento>,
    val anotadoPor: String
)

internal fun agruparVisitas(
    movimientos: List<Movimiento>,
    clienteMap: Map<String, com.kosking.app.modelo.Cliente>,
    fmtHora: SimpleDateFormat,
    ventanaMinutos: Long = 30
): List<VisitaResumen> {
    val result = mutableListOf<VisitaResumen>()

    fun addVisita(grupo: List<Movimiento>) {
        if (grupo.isEmpty()) return
        val rep = grupo.minByOrNull { it.fecha.toDate().time }!!
        result.add(VisitaResumen(
            clienteId = rep.clienteId,
            nombreCliente = clienteMap[rep.clienteId]?.nombre ?: "",
            hora = fmtHora.format(rep.fecha.toDate()),
            totalCargos = grupo.filter { it.tipo == "cargo" }.sumOf { it.monto },
            totalPagos = grupo.filter { it.tipo != "cargo" }.sumOf { it.monto },
            items = grupo.sortedBy { it.fecha.toDate().time },
            anotadoPor = grupo.firstOrNull { it.anotadoPorNombre.isNotBlank() }?.anotadoPorNombre ?: ""
        ))
    }

    // Nuevos con grupoId: agrupar por clienteId + grupoId
    val conGrupo = movimientos.filter { it.grupoId.isNotBlank() }
    conGrupo.groupBy { it.clienteId to it.grupoId }.values.forEach { addVisita(it) }

    // Viejos sin grupoId: fallback ventana de tiempo por cliente
    val sinGrupo = movimientos.filter { it.grupoId.isBlank() }
    val usados = mutableSetOf<String>()
    val sorted = sinGrupo.sortedByDescending { it.fecha.toDate().time }
    for (mov in sorted) {
        if (mov.id in usados) continue
        val t0 = mov.fecha.toDate().time
        val grupo = sorted.filter { m ->
            m.id !in usados &&
            m.clienteId == mov.clienteId &&
            kotlin.math.abs(m.fecha.toDate().time - t0) <= ventanaMinutos * 60_000
        }
        grupo.forEach { usados.add(it.id) }
        addVisita(grupo)
    }

    return result.sortedByDescending { it.items.firstOrNull()?.fecha?.toDate()?.time ?: 0 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilaVisitaDashboard(visita: VisitaResumen) {
    var mostrarDetalle by remember { mutableStateOf(false) }
    val esSoloPago = visita.totalCargos == 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { mostrarDetalle = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (esSoloPago) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (esSoloPago) "âˆ’" else "+",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (esSoloPago) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    visita.nombreCliente.ifBlank { "â€”" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        visita.hora,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Â·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (esSoloPago) "Pago" else "Fiado",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (esSoloPago) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${visita.items.size} Ã­tem${if (visita.items.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (visita.totalCargos > 0) {
                    Text(
                        "+$${"%.0f".format(visita.totalCargos)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (visita.totalPagos > 0) {
                    Text(
                        "âˆ’$${"%.0f".format(visita.totalPagos)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }

    if (mostrarDetalle) {
        ModalBottomSheet(onDismissRequest = { mostrarDetalle = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Encabezado: nombre izquierda, hora + quien anotÃ³ derecha
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        visita.nombreCliente.ifBlank { "Cliente" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(visita.hora, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        if (visita.anotadoPor.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Person, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(visita.anotadoPor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Lista de Ã­tems agrupados por nombre de producto
                data class ItemAgrupado(val nombre: String, val cantidad: Int, val monto: Double, val esCargo: Boolean)
                val itemsAgrupados = visita.items
                    .groupBy { it.producto.ifBlank { if (it.tipo == "cargo") "Fiado" else "Pago" } }
                    .map { (nombre, movs) ->
                        ItemAgrupado(
                            nombre = nombre,
                            cantidad = movs.sumOf { it.cantidad.coerceAtLeast(1) },
                            monto = movs.sumOf { it.monto },
                            esCargo = movs.first().tipo == "cargo"
                        )
                    }
                itemsAgrupados.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.nombre,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (item.cantidad > 1) "x${item.cantidad}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp).widthIn(min = 28.dp)
                        )
                        Text(
                            "${if (item.esCargo) "+" else "âˆ’"}$${"%.0f".format(item.monto)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (item.esCargo) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                    }
                    HorizontalDivider()
                }

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${visita.items.size} Ã­tem${if (visita.items.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Total fiado", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        "+$${"%.0f".format(visita.totalCargos - visita.totalPagos)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
internal fun SeccionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(onClick = onClick, modifier = modifier.height(64.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            }
        }
    }
}

