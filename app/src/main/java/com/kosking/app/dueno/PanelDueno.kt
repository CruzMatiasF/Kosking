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
private fun SeccionCard(
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

// â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

// â”€â”€ Panel embebido para usar dentro de PantallaExplorar (TabMÃ¡s) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    val nombreLocal by vm.nombreLocal.collectAsState()
    val codigoLocal by vm.codigoLocal.collectAsState()
    val totalDeuda by vm.totalDeuda.collectAsState()
    val fiadoHoy by vm.resumenHoy.collectAsState()
    val disputados by vm.disputados.collectAsState()
    val sobreLimite by vm.sobreLimite.collectAsState()
    val localData by vm.localData.collectAsState()

    BackHandler(enabled = tabSeleccionado != 0) { onTabChange(0) }

    // tabSeleccionado == 0 â†’ dashboard, 1..4 â†’ secciÃ³n
    if (tabSeleccionado != 0) {
        when (tabSeleccionado) {
            1 -> TabClientes(
                clientes = clientes,
                movimientos = movimientosLocal,
                onVerCliente = onAbrirDetalleCliente,
                onAgregarCliente = onAbrirAgregarCliente
            )
            2 -> TabProductos(vm = vm)
            3 -> TabEquipo(
                empleados = empleados,
                movimientos = movimientosLocal,
                onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                vm = vm
            )
        }
        return
    }

    var editandoDireccion by remember { mutableStateOf(false) }
    var editandoHorarios by remember { mutableStateOf(false) }
    var editandoDatos by remember { mutableStateOf(false) }

    val fotoLocalLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.subirFotoLocal(uri) {}
    }

    // Dashboard principal
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }
    val alertaTotal = disputados.size + sobreLimite.size
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val movimientosRecientes = remember(movimientosLocal) {
        movimientosLocal.filter { !it.anulado }.take(20)
    }
    val visitasEmb = remember(movimientosRecientes, clienteMap) {
        agruparVisitas(movimientosRecientes, clienteMap, fmtHora)
    }
    var mostrarTodasVisitas by remember { mutableStateOf(false) }

    if (mostrarTodasVisitas) {
        BackHandler { mostrarTodasVisitas = false }
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
                        IconButton(onClick = { mostrarTodasVisitas = false }) {
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

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Hero con foto del local
        item {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
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
                            startY = 60f
                        )
                    )
                )
                // Botones arriba a la derecha: horarios + cÃ¡mara
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { editandoHorarios = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(Icons.Default.Schedule, "Horarios", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { fotoLocalLauncher.launch("image/*") },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(Icons.Default.CameraAlt, "Cambiar foto", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        nombreLocal.ifBlank { "Mi local" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (codigoLocal.isNotBlank()) {
                        Text(
                            "CÃ³digo para clientes: $codigoLocal",
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
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
                            onClick = { editandoDireccion = true },
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
                    Text(
                        "Hoy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "+$${"%.0f".format(fiadoHoyVal)} fiado",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (fiadoHoyVal > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "âˆ’$${"%.0f".format(cobradoHoyVal)} cobrado",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cobradoHoyVal > 0) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Card de estado de hoy (excepciÃ³n)
        item {
            val abiertoAhora = localData?.let { com.kosking.app.explorar.estaAbiertoAhora(it) } ?: false
            var mostrarTimePicker by remember { mutableStateOf("") } // "antes" o "mas"

            if (mostrarTimePicker.isNotEmpty()) {
                val horarioCierreBase = localData?.let {
                    com.kosking.app.explorar.cierreEfectivoHoy(it)
                } ?: "20:00"
                val partes = horarioCierreBase.split(":")
                val hBase = partes.getOrNull(0)?.toIntOrNull() ?: 20
                val mBase = partes.getOrNull(1)?.toIntOrNull() ?: 0
                val timeState = rememberTimePickerState(
                    initialHour = if (mostrarTimePicker == "antes") maxOf(hBase - 1, 0) else minOf(hBase + 1, 23),
                    initialMinute = mBase,
                    is24Hour = true
                )
                AlertDialog(
                    onDismissRequest = { mostrarTimePicker = "" },
                    title = {
                        Text(if (mostrarTimePicker == "antes") "Â¿Hasta quÃ© hora cerrÃ¡s hoy?" else "Â¿Hasta quÃ© hora seguÃ­s abierto?")
                    },
                    text = { TimeInput(state = timeState) },
                    confirmButton = {
                        TextButton(onClick = {
                            val hora = "%02d:%02d".format(timeState.hour, timeState.minute)
                            vm.actualizarCierreHoy(hora)
                            mostrarTimePicker = ""
                        }) { Text("Confirmar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarTimePicker = "" }) { Text("Cancelar") }
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (abiertoAhora) Color(0xFF1B5E20).copy(alpha = 0.15f)
                                     else Color(0xFF7F0000).copy(alpha = 0.12f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape)
                                .background(if (abiertoAhora) Color(0xFF4ADE80) else Color(0xFFFF6B6B))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (abiertoAhora) "Abierto ahora" else "Cerrado ahora",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        }
                    Spacer(Modifier.height(2.dp))
                    val textoHorario = localData?.let { com.kosking.app.explorar.textoHorarioHoy(it) } ?: ""
                    if (textoHorario.isNotEmpty()) {
                        Text(textoHorario, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { mostrarTimePicker = "antes" },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cerrar antes", style = MaterialTheme.typography.labelMedium) }
                        OutlinedButton(
                            onClick = { mostrarTimePicker = "mas" },
                            modifier = Modifier.weight(1f)
                        ) { Text("Quedarme mÃ¡s", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }

        // BotÃ³n editar datos del local
        item {
            OutlinedButton(
                onClick = { editandoDatos = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Editar datos del local")
            }
        }

        // Alertas
        if (alertaTotal > 0) {
            items(disputados, key = { "disp_${it.id}" }) { mov ->
                val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: "Cliente"
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onAbrirDetalleMovimiento(mov.id) }
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
                        .clickable { onTabChange(1) }
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeccionCard(
                    icon = Icons.Default.Person,
                    label = "Clientes",
                    modifier = Modifier.weight(1f),
                    onClick = { onTabChange(1) }
                )
                SeccionCard(
                    icon = Icons.Default.ShoppingCart,
                    label = "Productos",
                    modifier = Modifier.weight(1f),
                    onClick = { onTabChange(2) }
                )
                SeccionCard(
                    icon = Icons.Default.People,
                    label = "Equipo",
                    modifier = Modifier.weight(1f),
                    onClick = { onTabChange(3) }
                )
            }
        }

        // Movimientos recientes agrupados por visita
        if (visitasEmb.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
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
                        onClick = { mostrarTodasVisitas = true },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Ver todo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            items(visitasEmb.take(4), key = { "visita_${it.items.firstOrNull()?.grupoId?.ifBlank { it.items.firstOrNull()?.id } ?: it.hora}" }) { visita ->
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

    if (editandoDatos) {
        val sheetDatos = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val tiposLocales = listOf("Kiosco", "AlmacÃ©n", "Maxikiosco", "Ropa", "CorralÃ³n", "Farmacia", "VerdulerÃ­a", "Otro")
        var nombreEdit by remember { mutableStateOf(localData?.nombre.orEmpty()) }
        var tipoEdit by remember { mutableStateOf(localData?.tipo ?: "Kiosco") }
        var telefonoEdit by remember { mutableStateOf(localData?.telefono.orEmpty()) }
        var descripcionEdit by remember { mutableStateOf(localData?.descripcion.orEmpty()) }
        var expandirTipo by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { editandoDatos = false },
            sheetState = sheetDatos
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Datos del local",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = nombreEdit,
                    onValueChange = { nombreEdit = it },
                    label = { Text("Nombre del local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = expandirTipo,
                    onExpandedChange = { expandirTipo = it }
                ) {
                    OutlinedTextField(
                        value = tipoEdit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de local") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirTipo) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandirTipo,
                        onDismissRequest = { expandirTipo = false }
                    ) {
                        tiposLocales.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo) },
                                onClick = { tipoEdit = tipo; expandirTipo = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = telefonoEdit,
                    onValueChange = { telefonoEdit = it },
                    label = { Text("TelÃ©fono") },
                    placeholder = { Text("Ej: 3884123456") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = descripcionEdit,
                    onValueChange = { descripcionEdit = it },
                    label = { Text("DescripciÃ³n") },
                    placeholder = { Text("Ej: Kiosco 24hs, delivery al barrio") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val local = localData ?: return@Button
                        vm.actualizarLocal(
                            nombre = nombreEdit.trim(),
                            tipo = tipoEdit,
                            direccion = local.direccion,
                            telefono = telefonoEdit.trim(),
                            descripcion = descripcionEdit.trim(),
                            abierto = local.abierto,
                            lat = local.lat?.takeIf { it != 0.0 },
                            lng = local.lng?.takeIf { it != 0.0 },
                            onExito = {}
                        )
                        editandoDatos = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar")
                }
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
                vm.actualizarLocal(
                    nombre = local.nombre, tipo = local.tipo, direccion = dir,
                    telefono = local.telefono, descripcion = local.descripcion,
                    abierto = local.abierto, lat = lat, lng = lng, onExito = {}
                )
                editandoDireccion = false
            },
            onDismiss = { editandoDireccion = false }
        )
    }

    if (editandoHorarios) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val diasClaves = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
        val diasEtiquetas = listOf("Lunes", "Martes", "MiÃ©rcoles", "Jueves", "Viernes", "SÃ¡bado", "Domingo")
        var horariosEdit by remember {
            mutableStateOf(
                diasClaves.associateWith { clave ->
                    localData?.horarios?.get(clave) ?: HorarioDia()
                }.toMutableMap()
            )
        }

        ModalBottomSheet(
            onDismissRequest = { editandoHorarios = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Horarios del local",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                diasClaves.forEachIndexed { i, clave ->
                    val h = horariosEdit[clave] ?: HorarioDia()
                    FilaHorarioPanelDueno(
                        dia = diasEtiquetas[i],
                        horario = h,
                        onCambio = { nuevo ->
                            horariosEdit = horariosEdit.toMutableMap().also { it[clave] = nuevo }
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val local = localData ?: return@Button
                        vm.actualizarLocal(
                            nombre = local.nombre,
                            tipo = local.tipo,
                            direccion = local.direccion,
                            telefono = local.telefono,
                            descripcion = local.descripcion,
                            abierto = local.abierto,
                            horarios = horariosEdit,
                            onExito = {}
                        )
                        editandoHorarios = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar horarios")
                }
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
