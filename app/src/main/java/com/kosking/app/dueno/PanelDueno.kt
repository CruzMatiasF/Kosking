package com.kosking.app.dueno

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.HorarioDia
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
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
    val nombreUsuario by vm.nombreUsuario.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val movimientosLocal by vm.movimientosLocal.collectAsState()
    val empleados by vm.empleados.collectAsState()
    val totalDeuda by vm.totalDeuda.collectAsState()
    val clientesConDeuda by vm.clientesConDeuda.collectAsState()
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
                        onAgregarCliente = { subPantalla = SubPantalla.AgregarCliente },
                        onAnotar = { clienteId -> subPantalla = SubPantalla.AgregarFiado(clienteId, "cargo") },
                        onCobrar = { clienteId -> subPantalla = SubPantalla.AgregarFiado(clienteId, "pago") }
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

private fun resolverNombreAnotador(anotadoPorNombre: String, nombreLocal: String, nombreUsuario: String): String {
    val nombre = anotadoPorNombre.trim()
    return when {
        nombre.isBlank() -> nombreUsuario.ifBlank { "Vos" }
        nombre.equals(nombreLocal, ignoreCase = true) -> nombreUsuario.ifBlank { "Vos" }
        else -> nombre
    }
}

// â”€â”€ Tab Clientes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun TabClientes(
    clientes: List<Cliente>,
    movimientos: List<Movimiento> = emptyList(),
    onVerCliente: (String) -> Unit,
    onAgregarCliente: () -> Unit,
    onAnotar: (clienteId: String) -> Unit = {},
    onCobrar: (clienteId: String) -> Unit = {}
) {
    var filtro by remember { mutableStateOf("deuda") }
    var busqueda by remember { mutableStateOf("") }
    var orden by remember { mutableStateOf("deuda") }
    var mostrarOrden by remember { mutableStateOf(false) }

    val conDeuda = remember(clientes) { clientes.count { it.deudaTotal > 0 && it.habilitado } }
    val alDia = remember(clientes) { clientes.count { it.deudaTotal <= 0 && it.habilitado } }
    val deudaTotal = remember(clientes) { clientes.sumOf { it.deudaTotal } }

    val ultimaVisita = remember(movimientos) {
        movimientos.filter { !it.anulado }
            .groupBy { it.clienteId }
            .mapValues { (_, movs) -> movs.maxByOrNull { it.fecha.toDate() }?.fecha?.toDate() }
    }

    val ahora = remember { System.currentTimeMillis() }

    val clientesFiltrados = remember(clientes, filtro, busqueda, orden, ultimaVisita) {
        clientes
            .filter { c ->
                when (filtro) {
                    "deuda" -> c.deudaTotal > 0 && c.habilitado
                    "aldia" -> c.deudaTotal <= 0 && c.habilitado
                    "bloqueado" -> !c.habilitado
                    else -> true
                }
            }
            .filter { c -> busqueda.isBlank() || c.nombre.contains(busqueda, ignoreCase = true) }
            .sortedWith(when (orden) {
                "nombre" -> compareBy { it.nombre }
                "visita" -> compareByDescending { ultimaVisita[it.id]?.time ?: 0L }
                else -> compareByDescending { it.deudaTotal }
            })
    }

    Column(Modifier.fillMaxSize()) {
        // Stats
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatMiniCard("Deuda total", "$${"%.0f".format(deudaTotal)}", isError = deudaTotal > 0, modifier = Modifier.weight(1f))
            StatMiniCard("Deben", "$conDeuda", isError = conDeuda > 0, modifier = Modifier.weight(1f))
            StatMiniCard("Al dÃ­a", "$alDia", isSuccess = true, modifier = Modifier.weight(1f))
        }

        // BÃºsqueda + orden + agregar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Buscador compacto
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box {
                            if (busqueda.isEmpty()) {
                                Text("Buscar cliente", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    }
                )
                if (busqueda.isNotEmpty()) {
                    Icon(Icons.Default.Close, "Limpiar", Modifier.size(16.dp).clickable { busqueda = "" },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Ordenar
            Box {
                IconButton(
                    onClick = { mostrarOrden = true },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) { Icon(Icons.Default.SwapVert, "Ordenar", Modifier.size(20.dp)) }
                DropdownMenu(expanded = mostrarOrden, onDismissRequest = { mostrarOrden = false }) {
                    DropdownMenuItem(text = { Text("Por deuda") }, onClick = { orden = "deuda"; mostrarOrden = false },
                        leadingIcon = { if (orden == "deuda") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) })
                    DropdownMenuItem(text = { Text("Por nombre") }, onClick = { orden = "nombre"; mostrarOrden = false },
                        leadingIcon = { if (orden == "nombre") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) })
                    DropdownMenuItem(text = { Text("Ãšltima visita") }, onClick = { orden = "visita"; mostrarOrden = false },
                        leadingIcon = { if (orden == "visita") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) })
                }
            }
            // Agregar cliente
            IconButton(
                onClick = onAgregarCliente,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) { Icon(Icons.Default.PersonAdd, "Agregar cliente", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        }

        // Segmented control
        val bloqueados = clientes.count { !it.habilitado }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            data class Seg(val key: String, val num: Int, val label: String)
            val segs = listOf(
                Seg("deuda", conDeuda, "Deben"),
                Seg("aldia", alDia, "Al dÃ­a"),
                Seg("bloqueado", bloqueados, "Bloqueados"),
                Seg("todos", clientes.size, "Todos")
            )
            segs.forEach { seg ->
                val selected = filtro == seg.key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (selected) Modifier.background(MaterialTheme.colorScheme.surface)
                                .then(Modifier.padding(1.dp))
                            else Modifier
                        )
                        .clickable { filtro = seg.key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            seg.num.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            seg.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (clientesFiltrados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (clientes.isEmpty()) "TodavÃ­a no hay clientes\nTocÃ¡ + para agregar el primero"
                    else "Sin resultados para este filtro",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clientesFiltrados, key = { it.id }) { cliente ->
                    CardCliente(
                        cliente = cliente,
                        ultimaVisitaDate = ultimaVisita[cliente.id],
                        ahoraMs = ahora,
                        onVerCliente = onVerCliente
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    label: String,
    valor: String,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    isError -> MaterialTheme.colorScheme.error
                    isSuccess -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun CardCliente(
    cliente: Cliente,
    ultimaVisitaDate: java.util.Date?,
    ahoraMs: Long,
    onVerCliente: (String) -> Unit
) {
    val diasSinVenir = remember(ultimaVisitaDate, ahoraMs) {
        if (ultimaVisitaDate == null) null
        else ((ahoraMs - ultimaVisitaDate.time) / (1000 * 60 * 60 * 24)).toInt()
    }

    val accentColor = when {
        !cliente.habilitado -> MaterialTheme.colorScheme.outlineVariant
        cliente.limiteCredito > 0 && cliente.deudaTotal > cliente.limiteCredito -> MaterialTheme.colorScheme.error
        cliente.deudaTotal > 300 -> MaterialTheme.colorScheme.error
        cliente.deudaTotal > 50 -> Color(0xFFE65100)
        cliente.deudaTotal > 0 -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val visitaTexto = when {
        diasSinVenir == null -> "Nuevo"
        diasSinVenir == 0 -> "Hoy"
        diasSinVenir == 1 -> "Ayer"
        diasSinVenir < 7 -> "Hace $diasSinVenir dÃ­as"
        else -> "Hace ${diasSinVenir / 7} sem."
    }
    val visitaEsAlerta = diasSinVenir != null && diasSinVenir >= 7 && cliente.deudaTotal > 0
    val visitaEsWarning = diasSinVenir != null && diasSinVenir in 3..6 && cliente.deudaTotal > 0

    val dotColor = when {
        visitaEsAlerta -> MaterialTheme.colorScheme.error
        visitaEsWarning -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVerCliente(cliente.id) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Barra de color superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor)
            )
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar circular
                val avatarBg = when {
                    !cliente.habilitado -> MaterialTheme.colorScheme.surfaceVariant
                    cliente.deudaTotal > 0 -> accentColor.copy(alpha = 0.18f)
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        cliente.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (cliente.deudaTotal > 0 && cliente.habilitado) accentColor
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Nombre
                Text(
                    cliente.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                // Monto de deuda (si debe)
                if (cliente.deudaTotal > 0 && cliente.habilitado) {
                    Text(
                        "$${"%.0f".format(cliente.deudaTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                // Ãšltima visita con punto de color
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        visitaTexto,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Barra de lÃ­mite de crÃ©dito (solo si tiene lÃ­mite)
                if (cliente.limiteCredito > 0) {
                    val progreso = (cliente.deudaTotal / cliente.limiteCredito).coerceIn(0.0, 1.0).toFloat()
                    val superado = cliente.deudaTotal > cliente.limiteCredito
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = if (superado) MaterialTheme.colorScheme.error else accentColor,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaCliente(
    cliente: Cliente,
    ultimaVisitaDate: java.util.Date?,
    ahoraMs: Long,
    onVerCliente: (String) -> Unit,
    onAnotar: (String) -> Unit,
    onCobrar: (String) -> Unit = {}
) {
    val diasSinVenir = remember(ultimaVisitaDate, ahoraMs) {
        if (ultimaVisitaDate == null) null
        else ((ahoraMs - ultimaVisitaDate.time) / (1000 * 60 * 60 * 24)).toInt()
    }

    val accentColor = when {
        !cliente.habilitado -> MaterialTheme.colorScheme.outlineVariant
        cliente.limiteCredito > 0 && cliente.deudaTotal > cliente.limiteCredito -> MaterialTheme.colorScheme.error
        cliente.deudaTotal > 300 -> MaterialTheme.colorScheme.error
        cliente.deudaTotal > 50 -> Color(0xFFE65100)
        cliente.deudaTotal > 0 -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val visitaBadge = when {
        diasSinVenir == null -> null
        diasSinVenir == 0 -> "Hoy"
        diasSinVenir == 1 -> "Ayer"
        diasSinVenir < 7 -> "Hace $diasSinVenir dÃ­as"
        else -> "Hace ${diasSinVenir / 7} sem."
    }
    val visitaEsAlerta = diasSinVenir != null && diasSinVenir >= 7 && cliente.deudaTotal > 0
    val visitaEsWarning = diasSinVenir != null && diasSinVenir in 3..6 && cliente.deudaTotal > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onVerCliente(cliente.id) }
    ) {
        // Franja lateral â€” se estira al alto del contenido
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(accentColor))
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (cliente.deudaTotal > 0) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        cliente.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Nombre + badges + barra lÃ­mite
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(cliente.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!cliente.habilitado) BadgeChip("Bloqueado", isError = true)
                    val hoyMs = remember { java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis }
                    val fechaPagoMs = cliente.fechaPago?.toDate()?.time
                    val pagoVencido = fechaPagoMs != null && fechaPagoMs < hoyMs && cliente.deudaTotal > 0
                    val pagoHoy = fechaPagoMs != null && fechaPagoMs in hoyMs..(hoyMs + 86_400_000L - 1) && cliente.deudaTotal > 0
                    when {
                        pagoVencido -> BadgeChip("Pago vencido", isError = true)
                        pagoHoy -> BadgeChip("Paga hoy", isWarning = true)
                        visitaBadge != null -> BadgeChip(visitaBadge, isError = visitaEsAlerta, isWarning = visitaEsWarning)
                        ultimaVisitaDate == null -> BadgeChip("Nuevo")
                    }
                }
                if (cliente.limiteCredito > 0 && cliente.deudaTotal > 0) {
                    val progreso = (cliente.deudaTotal / cliente.limiteCredito).coerceIn(0.0, 1.0).toFloat()
                    val superado = cliente.deudaTotal > cliente.limiteCredito
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = if (superado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        if (superado) "SuperÃ³ el lÃ­mite de $${"%.0f".format(cliente.limiteCredito)}"
                        else "${"%.0f".format(progreso * 100)}% del lÃ­mite",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (superado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Monto + acciones + chevron
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cliente.deudaTotal > 0) {
                        Text("$${"%.0f".format(cliente.deudaTotal)}", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Al dÃ­a", style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    }
                    if (cliente.habilitado) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { onAnotar(cliente.id) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("+ Anotar", style = MaterialTheme.typography.labelSmall)
                            }
                            if (cliente.deudaTotal > 0) {
                                TextButton(
                                    onClick = { onCobrar(cliente.id) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Cobrar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 3.dp))
}

@Composable
private fun BadgeChip(text: String, isError: Boolean = false, isWarning: Boolean = false) {
    val bg = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isWarning -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        isError -> MaterialTheme.colorScheme.error
        isWarning -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.extraSmall, color = bg) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

// â”€â”€ Tab Productos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun TabProductos(vm: DuenoViewModel) {
    val productos by vm.productos.collectAsState()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    var productoParaFoto by remember { mutableStateOf<Producto?>(null) }

    val fotoProductoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val prod = productoParaFoto
        if (uri != null && prod != null) {
            vm.subirFotoProducto(prod, uri) {}
        }
        productoParaFoto = null
    }
    var busqueda by remember { mutableStateOf("") }

    // Secciones: categorÃ­as ordenadas alfabÃ©ticamente, sin categorÃ­a al final
    val secciones = remember(productos) {
        val cats = productos.map { it.categoria.trim() }
            .filter { it.isNotBlank() }.distinct().sorted()
        val sinCat = productos.any { it.categoria.isBlank() }
        if (sinCat) cats + listOf("") else cats
    }

    val productosBusqueda = remember(productos, busqueda) {
        if (busqueda.isBlank()) emptyList()
        else productos.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        // Buscador compacto + botÃ³n nuevo
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box {
                            if (busqueda.isEmpty()) Text("Buscar...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    }
                )
                if (busqueda.isNotEmpty()) {
                    Icon(Icons.Default.Close, "Limpiar",
                        Modifier.size(14.dp).clickable { busqueda = "" },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(
                onClick = { productoEditar = null; mostrarDialogo = true },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(Icons.Default.Add, "Nuevo producto", Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        // Contenido
        when {
            productos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No hay productos cargados",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("TocÃ¡ + para agregar el primero",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            busqueda.isNotBlank() -> {
                // BÃºsqueda activa: grid 3 columnas
                if (productosBusqueda.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin resultados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(productosBusqueda, key = { it.id }) { prod ->
                            CardProducto(
                                prod = prod,
                                onEditar = { productoEditar = prod; mostrarDialogo = true },
                                onFoto = { productoParaFoto = prod; fotoProductoLauncher.launch("image/*") }
                            )
                        }
                        item(span = { GridItemSpan(3) }) { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
            secciones.isEmpty() -> {
                // Productos sin categorÃ­a, mostrar directamente como grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(productos, key = { it.id }) { prod ->
                        CardProducto(
                            prod = prod,
                            onEditar = { productoEditar = prod; mostrarDialogo = true },
                            onFoto = { productoParaFoto = prod; fotoProductoLauncher.launch("image/*") }
                        )
                    }
                    item(span = { GridItemSpan(3) }) { Spacer(Modifier.height(80.dp)) }
                }
            }
            else -> {
                // Secciones por categorÃ­a con scroll horizontal (como Rappi/PedidosYa)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    secciones.forEach { cat ->
                        val prods = productos.filter {
                            if (cat.isBlank()) it.categoria.isBlank()
                            else it.categoria.trim() == cat
                        }
                        if (prods.isEmpty()) return@forEach
                        item(key = "header_$cat") {
                            Row(
                                Modifier.fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (cat.isBlank()) "Sin categorÃ­a" else cat,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${prods.size} productos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        item(key = "row_$cat") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(prods, key = { it.id }) { prod ->
                                    Box(Modifier.width(110.dp)) {
                                        CardProducto(
                                            prod = prod,
                                            onEditar = { productoEditar = prod; mostrarDialogo = true },
                                            onFoto = { productoParaFoto = prod; fotoProductoLauncher.launch("image/*") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogProducto(
            productoInicial = productoEditar,
            productosExistentes = if (productoEditar == null) productos else emptyList(),
            onGuardar = { nombre, precio, descripcion, habilitado, esOferta, precioAnterior, agotado, estadoStock, categoria ->
                if (productoEditar != null) {
                    vm.actualizarProducto(productoEditar!!.copy(
                        nombre = nombre, precio = precio, descripcion = descripcion,
                        habilitado = habilitado, esOferta = esOferta, precioAnterior = precioAnterior,
                        agotado = agotado, estadoStock = estadoStock, categoria = categoria
                    ))
                } else {
                    vm.agregarProducto(nombre, precio, descripcion, habilitado, esOferta, precioAnterior, agotado, estadoStock, categoria)
                }
                mostrarDialogo = false
            },
            onDismiss = { mostrarDialogo = false }
        )
    }

    productoAEliminar?.let { prod ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("Â¿Eliminar ${prod.nombre} de la lista?") },
            confirmButton = {
                TextButton(onClick = { vm.eliminarProducto(prod.id); productoAEliminar = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { productoAEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CardProducto(
    prod: Producto,
    onEditar: () -> Unit,
    onFoto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEditar),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Imagen / inicial + overlay cÃ¡mara + precio en esquina
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(if (prod.imagenUrl.isBlank()) Modifier.clickable(onClick = onFoto) else Modifier)
            ) {
                if (prod.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = prod.imagenUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        prod.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // Overlay cÃ¡mara (solo sin imagen)
                if (prod.imagenUrl.isBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onFoto),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                // Badges
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (prod.esOferta) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("OFERTA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    if (!prod.habilitado) {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("OCULTO", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    if (prod.estadoStock == "agotado" || (prod.estadoStock.isBlank() && prod.agotado)) {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer) {
                            Text("AGOTADO", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    } else if (prod.estadoStock == "pocos") {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("POCOS", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            // Info + precio
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    prod.nombre,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$${"%.0f".format(prod.precio)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (prod.esOferta && prod.precioAnterior > 0) {
                        Text(
                            "$${"%.0f".format(prod.precioAnterior)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€ Tab Equipo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun TabEquipo(
    empleados: List<Empleado>,
    movimientos: List<Movimiento>,
    onEliminarEmpleado: (String) -> Unit,
    vm: DuenoViewModel
) {
    val context = LocalContext.current
    val vmCargando by vm.cargando.collectAsState()
    val vmError by vm.error.collectAsState()
    val nombreLocal by vm.nombreLocal.collectAsState()
    var mostrarDialogInvitar by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<String?>(null) }
    var empleadoAEliminar by remember { mutableStateOf<Empleado?>(null) }

    val duenoUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    val inicioDia = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val movsHoy = remember(movimientos, inicioDia) {
        movimientos.filter { !it.anulado && it.fecha.toDate().time >= inicioDia }
    }

    // Caja de hoy: stats por persona (uid â†’ nombre, anotÃ³ N, cobrÃ³ $X)
    data class StatPersona(val uid: String, val nombre: String, val anoto: Int, val cobro: Double)

    val statsHoy = remember(movsHoy, duenoUid) {
        val map = mutableMapOf<String, StatPersona>()
        movsHoy.forEach { mov ->
            val uid = mov.anotadoPorUid.ifBlank { duenoUid }
            val nombre = if (uid == duenoUid) "Vos"
                         else mov.anotadoPorNombre.ifBlank { "Empleado" }
            val actual = map[uid] ?: StatPersona(uid, nombre, 0, 0.0)
            map[uid] = actual.copy(
                anoto = actual.anoto + 1,
                cobro = actual.cobro + if (mov.tipo == "pago") mov.monto else 0.0
            )
        }
        map.values.sortedByDescending { if (it.uid == duenoUid) Int.MAX_VALUE else it.anoto }
    }

    val totalCobradoHoy = remember(statsHoy) { statsHoy.sumOf { it.cobro } }

    // Ãšltimo movimiento por uid (sin anulados)
    val fmtFecha = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val ultimoMovPorUid = remember(movimientos) {
        movimientos
            .filter { !it.anulado }
            .groupBy { it.anotadoPorUid }
            .mapValues { it.value.first() }
    }

    // Disputas por uid
    val disputasPorUid = remember(movimientos) {
        movimientos.filter { it.disputado && !it.anulado }
            .groupBy { it.anotadoPorUid }
            .mapValues { it.value.size }
    }

    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Caja de hoy
        if (statsHoy.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Caja de hoy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(
                                "Cobrado $${"%.0f".format(totalCobradoHoy)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        // Encabezado tabla
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("QuiÃ©n",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                            Text("AnotÃ³",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp))
                            Text("CobrÃ³",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(72.dp))
                        }
                        HorizontalDivider()
                        statsHoy.forEach { stat ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stat.nombre,
                                    fontWeight = if (stat.uid == duenoUid) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f))
                                Text("${stat.anoto}",
                                    modifier = Modifier.width(48.dp),
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "$${"%.0f".format(stat.cobro)}",
                                    modifier = Modifier.width(72.dp),
                                    color = if (stat.cobro > 0) Color(0xFF2E7D32)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (stat.cobro > 0) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Encabezado empleados
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EMPLEADOS Â· ${empleados.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(
                    onClick = { mostrarDialogInvitar = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sumar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Info permisos
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null,
                        Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        "Los empleados pueden anotar fiados y registrar pagos. ControlÃ¡ los permisos por cliente desde el detalle de cada uno.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (empleados.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Groups, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("Sin empleados todavÃ­a",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("TocÃ¡ + Sumar para invitar a alguien",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        } else {
            items(empleados, key = { it.id }) { emp ->
                val ultimoMov = ultimoMovPorUid[emp.uid]
                val reclamos = disputasPorUid[emp.uid] ?: 0
                val esActivoHoy = ultimoMov != null && ultimoMov.fecha.toDate().time >= inicioDia

                val avatarColors = listOf(
                    MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
                )
                val (avatarBg, avatarFg) = avatarColors[Math.floorMod(emp.nombre.hashCode(), 3)]

                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(
                            start = 12.dp, end = 4.dp, top = 10.dp,
                            bottom = if (esActivoHoy || reclamos > 0) 8.dp else 10.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(avatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    emp.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = avatarFg
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(emp.nombre,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyLarge)
                                if (ultimoMov != null) {
                                    val accion = if (ultimoMov.tipo == "cargo") "AnotÃ³ fiado" else "CobrÃ³"
                                    val esHoy = ultimoMov.fecha.toDate().time >= inicioDia
                                    val fechaStr = if (esHoy) "hoy ${fmtHora.format(ultimoMov.fecha.toDate())}"
                                                  else fmtFecha.format(ultimoMov.fecha.toDate())
                                    Text("Ãšltimo: $fechaStr Â· $accion",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { empleadoAEliminar = emp }) {
                                Icon(Icons.Default.Delete, "Desvincular",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (esActivoHoy || reclamos > 0) {
                            Row(
                                Modifier.padding(start = 56.dp, top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (esActivoHoy) {
                                    Surface(shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("Activo hoy",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                if (reclamos > 0) {
                                    Surface(shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.errorContainer) {
                                        Text("$reclamos reclamo${if (reclamos != 1) "s" else ""}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogInvitar) {
        val localContext = LocalContext.current
        DialogInvitarEmpleado(
            cargando = vmCargando,
            error = vmError,
            onGenerar = { whatsapp, email ->
                vm.generarCodigoEmpleado { codigo ->
                    mostrarDialogInvitar = false
                    val local = nombreLocal.ifBlank { "Kosking" }
                    val mensaje = "ðŸ‘‹ Te invitan a unirte como empleado a *$local* en Kosking.\n\nSeguÃ­ estos pasos:\n1ï¸âƒ£ DescargÃ¡ la app Kosking desde Play Store\n2ï¸âƒ£ Registrate o iniciÃ¡ sesiÃ³n\n3ï¸âƒ£ TocÃ¡ \"Soy empleado\" e ingresÃ¡ el cÃ³digo:\n\n*$codigo*\n\nEl cÃ³digo es de un solo uso."
                    if (whatsapp.isNotBlank()) {
                        val numero = whatsapp.replace(Regex("[^0-9]"), "")
                        try {
                            localContext.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
                            })
                        } catch (_: Exception) {}
                    } else if (email.isNotBlank()) {
                        try {
                            localContext.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                                putExtra(Intent.EXTRA_SUBJECT, "Tu cÃ³digo de empleado - Kosking")
                                putExtra(Intent.EXTRA_TEXT, mensaje)
                            })
                        } catch (_: Exception) {}
                    } else {
                        codigoGenerado = codigo
                    }
                }
            },
            onDismiss = { mostrarDialogInvitar = false }
        )
    }

    empleadoAEliminar?.let { emp ->
        AlertDialog(
            onDismissRequest = { empleadoAEliminar = null },
            title = { Text("Eliminar empleado") },
            text = { Text("Â¿Eliminar a ${emp.nombre}? Se va a desconectar del local.") },
            confirmButton = {
                TextButton(onClick = { onEliminarEmpleado(emp.id); empleadoAEliminar = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { empleadoAEliminar = null }) { Text("Cancelar") } }
        )
    }

    codigoGenerado?.let { codigo ->
        AlertDialog(
            onDismissRequest = { codigoGenerado = null },
            title = { Text("CÃ³digo de invitaciÃ³n") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CompartÃ­ este cÃ³digo con el empleado:")
                    Text(codigo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("El empleado lo ingresa en la app para conectarse al local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { codigoGenerado = null }) { Text("Listo") }
            }
        )
    }
}

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
    val clientesConDeuda by vm.clientesConDeuda.collectAsState()
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
                onAgregarCliente = onAbrirAgregarCliente,
                onAnotar = { clienteId -> onAbrirAgregarFiado(clienteId, "cargo") },
                onCobrar = { clienteId -> onAbrirAgregarFiado(clienteId, "pago") }
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
