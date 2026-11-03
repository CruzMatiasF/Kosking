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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
        val tipoInicial: String = "cargo",
        val desdeResumen: Boolean = false
    ) : SubPantalla()
    data class SeleccionarCliente(val tipo: String) : SubPantalla()
    data class DetalleMovimiento(val movimientoId: String, val clienteId: String? = null) : SubPantalla()
    object Clientes : SubPantalla()
    object Productos : SubPantalla()
    object Equipo : SubPantalla()
    object MiLocal : SubPantalla()
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
                onGuardado = {
                    subPantalla = if (sp.desdeResumen) null
                                 else SubPantalla.DetalleCliente(sp.clienteId)
                },
                onVolver = {
                    subPantalla = if (sp.desdeResumen) SubPantalla.SeleccionarCliente(sp.tipoInicial)
                                 else SubPantalla.DetalleCliente(sp.clienteId)
                },
                vm = vm,
                tipoInicial = sp.tipoInicial
            )
            return
        }
        is SubPantalla.SeleccionarCliente -> {
            PantallaSeleccionarCliente(
                tipo = sp.tipo,
                vm = vm,
                onSeleccionado = { clienteId ->
                    subPantalla = SubPantalla.AgregarFiado(clienteId, sp.tipo, desdeResumen = true)
                },
                onVolver = { subPantalla = null }
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
                    TabProductos(vm = vm, movimientos = movimientosLocal)
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
        is SubPantalla.MiLocal -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Mi local") },
                        navigationIcon = {
                            IconButton(onClick = { subPantalla = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    TabMiLocal(vm = vm)
                }
            }
            return
        }
        is SubPantalla.TodasLasVisitas -> {
            val fmtFecha = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            val fmtDia = remember { SimpleDateFormat("dd 'de' MMMM", Locale("es", "AR")) }
            val clienteMapTodas = remember(clientes) { clientes.associateBy { it.id } }
            val todasVisitas = remember(movimientosLocal, clienteMapTodas) {
                agruparVisitas(movimientosLocal.filter { !it.anulado }, clienteMapTodas, fmtFecha)
            }
            val visitasPorFecha = remember(todasVisitas) {
                todasVisitas.groupBy { v ->
                    val mov = movimientosLocal.firstOrNull { m -> m.clienteId == v.clienteId && fmtFecha.format(m.fecha.toDate()) == v.hora }
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
                IconButton(
                    onClick = { vm.cerrarSesion(); onCerrarSesion() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(Icons.Default.Logout, "Cerrar sesión", tint = Color.White)
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
                            "Código para tus clientes: $codigoLocal",
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
                    if (clientesConDeuda > 0) {
                        TextButton(
                            onClick = { subPantalla = SubPantalla.Clientes },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "$clientesConDeuda cliente${if (clientesConDeuda != 1) "s" else ""} con deuda  →",
                                style = MaterialTheme.typography.labelMedium
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
                            "−$${"%.0f".format(cobradoHoyVal)} cobrado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Botones de acción
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { subPantalla = SubPantalla.SeleccionarCliente("cargo") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Anotar fiado")
                }
                OutlinedButton(
                    onClick = { subPantalla = SubPantalla.SeleccionarCliente("pago") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cobrar")
                }
            }
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
                            Text("${cliente.nombre} superó su límite", fontWeight = FontWeight.Medium)
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
                SeccionCard(
                    icon = Icons.Default.Store,
                    label = "Mi local",
                    modifier = Modifier.weight(1f),
                    onClick = { subPantalla = SubPantalla.MiLocal }
                )
            }
        }

        // Últimas visitas (máx 4)
        if (visitas.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ÚLTIMAS VISITAS",
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
                    Text("Todavía no hay movimientos",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
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
                        if (esSoloPago) "−" else "+",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (esSoloPago) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    visita.nombreCliente.ifBlank { "—" },
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
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (esSoloPago) "Pago" else "Fiado",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (esSoloPago) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${visita.items.size} ítem${if (visita.items.size != 1) "s" else ""}",
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
                        "−$${"%.0f".format(visita.totalPagos)}",
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
                // Encabezado: nombre izquierda, hora + quien anotó derecha
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

                // Lista de ítems agrupados por nombre de producto
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
                            "${if (item.esCargo) "+" else "−"}$${"%.0f".format(item.monto)}",
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
                            "${visita.items.size} ítem${if (visita.items.size != 1) "s" else ""}",
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

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun resolverNombreAnotador(anotadoPorNombre: String, nombreLocal: String, nombreUsuario: String): String {
    val nombre = anotadoPorNombre.trim()
    return when {
        nombre.isBlank() -> nombreUsuario.ifBlank { "Vos" }
        nombre.equals(nombreLocal, ignoreCase = true) -> nombreUsuario.ifBlank { "Vos" }
        else -> nombre
    }
}

// ── Tab Resumen ───────────────────────────────────────────────────────────────

@Composable
private fun TabResumen(
    clientes: List<Cliente>,
    movimientos: List<Movimiento>,
    movimientosHoy: List<Movimiento> = emptyList(),
    onIrAClientes: () -> Unit,
    onVerMovimiento: (String) -> Unit,
    onAnotarFiado: () -> Unit = {},
    onCobrar: () -> Unit = {},
    nombreLocal: String = "",
    nombreUsuario: String = "",
    totalDeuda: Double = 0.0,
    clientesConDeuda: Int = 0,
    fiadoHoy: Double = 0.0,
    cobradoHoy: Double = 0.0,
    disputados: List<Movimiento> = emptyList(),
    sobreLimite: List<Cliente> = emptyList()
) {
    val clienteMap = remember(clientes) { clientes.associateBy { it.id } }
    val alertaTotal = disputados.size + sobreLimite.size

    val ultimoMov = movimientos.firstOrNull { !it.anulado }
    val fmtHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tarjeta total
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Te deben en total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$${"%.0f".format(totalDeuda)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (clientesConDeuda > 0) {
                        TextButton(onClick = onIrAClientes, contentPadding = PaddingValues(0.dp)) {
                            Text("$clientesConDeuda cliente${if (clientesConDeuda != 1) "s" else ""} con deuda  →",
                                style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Fiado hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+$${"%.0f".format(fiadoHoy)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cobrado hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-$${"%.0f".format(cobradoHoy)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }

        // Acciones rápidas
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAnotarFiado, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Anotar fiado")
                }
                OutlinedButton(onClick = onCobrar, modifier = Modifier.weight(1f)) {
                    Text("Cobrar")
                }
            }
        }


        // Alertas
        if (alertaTotal > 0) {
            item {
                Text("PARA REVISAR · $alertaTotal",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(disputados, key = { "disp_${it.id}" }) { mov ->
                val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: "Cliente"
                Card(modifier = Modifier.fillMaxWidth().clickable { onVerMovimiento(mov.id) }) {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        },
                        headlineContent = {
                            Text("$nombreCliente no reconoce un fiado de $${"%.0f".format(mov.monto)}",
                                fontWeight = FontWeight.Medium)
                        },
                        supportingContent = {
                            Text("anotó ${resolverNombreAnotador(mov.anotadoPorNombre, nombreLocal, nombreUsuario)} · ${fmtHora.format(mov.fecha.toDate())}")
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                    )
                }
            }
            items(sobreLimite, key = { "lim_${it.id}" }) { cliente ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onIrAClientes() }) {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
                        },
                        headlineContent = {
                            Text("${cliente.nombre} superó su límite", fontWeight = FontWeight.Medium)
                        },
                        supportingContent = {
                            Text("Debe $${"%.0f".format(cliente.deudaTotal)} · límite $${"%.0f".format(cliente.limiteCredito)}")
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                    )
                }
            }
        }

        // Movimientos de hoy / último movimiento
        val seccionLabel = if (movimientosHoy.isNotEmpty()) "MOVIMIENTOS DE HOY · ${movimientosHoy.size}"
                           else if (ultimoMov != null) "ÚLTIMO MOVIMIENTO"
                           else null

        if (seccionLabel != null) {
            item {
                Text(seccionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val movsAMostrar = if (movimientosHoy.isNotEmpty()) movimientosHoy
                           else if (ultimoMov != null) listOf(ultimoMov)
                           else emptyList()

        if (movsAMostrar.isNotEmpty()) {
            items(movsAMostrar, key = { "res_${it.id}" }) { mov ->
                val esCargo = mov.tipo == "cargo"
                val nombreCliente = clienteMap[mov.clienteId]?.nombre ?: ""
                Card(modifier = Modifier.fillMaxWidth().clickable { onVerMovimiento(mov.id) }) {
                    ListItem(
                        leadingContent = {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (esCargo) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        if (esCargo) "+" else "−",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (esCargo) MaterialTheme.colorScheme.onErrorContainer
                                                else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        },
                        headlineContent = {
                            Text(
                                "${mov.producto.ifBlank { if (esCargo) "Fiado" else "Pago" }} · $nombreCliente",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            Text("${fmtHora.format(mov.fecha.toDate())} · ${resolverNombreAnotador(mov.anotadoPorNombre, nombreLocal, nombreUsuario)}")
                        },
                        trailingContent = {
                            Text(
                                "${if (esCargo) "+" else "−"}$${"%.0f".format(mov.monto)}",
                                fontWeight = FontWeight.SemiBold,
                                color = if (esCargo) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                        }
                    )
                }
            }
        } else {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Todavía no hay movimientos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Tab Clientes ──────────────────────────────────────────────────────────────

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
            StatMiniCard("Al día", "$alDia", isSuccess = true, modifier = Modifier.weight(1f))
        }

        // Búsqueda + orden + agregar
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
                    DropdownMenuItem(text = { Text("Última visita") }, onClick = { orden = "visita"; mostrarOrden = false },
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
                Seg("aldia", alDia, "Al día"),
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
                    if (clientes.isEmpty()) "Todavía no hay clientes\nTocá + para agregar el primero"
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
        diasSinVenir < 7 -> "Hace $diasSinVenir días"
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
                // Última visita con punto de color
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
                // Barra de límite de crédito (solo si tiene límite)
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
        diasSinVenir < 7 -> "Hace $diasSinVenir días"
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
        // Franja lateral — se estira al alto del contenido
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
            // Nombre + badges + barra límite
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
                        if (superado) "Superó el límite de $${"%.0f".format(cliente.limiteCredito)}"
                        else "${"%.0f".format(progreso * 100)}% del límite",
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
                        Text("Al día", style = MaterialTheme.typography.bodySmall,
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

// ── Tab Productos ─────────────────────────────────────────────────────────────

@Composable
private fun TabProductos(vm: DuenoViewModel, movimientos: List<Movimiento> = emptyList()) {
    val productos by vm.productos.collectAsState()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    var productoEditarPrecio by remember { mutableStateOf<Producto?>(null) }
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
    var filtro by remember { mutableStateOf("todos") }

    // Conteo de veces que cada producto fue fiado
    val conteoFiados = remember(movimientos) {
        movimientos
            .filter { it.tipo == "cargo" && !it.anulado && it.producto.isNotBlank() }
            .groupBy { it.producto.trim().lowercase() }
            .mapValues { it.value.size }
    }

    val enOferta = remember(productos) { productos.count { it.esOferta && it.habilitado } }
    val ocultos = remember(productos) { productos.count { !it.habilitado } }

    val productosFiltrados = remember(productos, filtro, busqueda, conteoFiados) {
        productos
            .filter { p ->
                when (filtro) {
                    "oferta" -> p.esOferta && p.habilitado
                    "oculto" -> !p.habilitado
                    else -> true
                }
            }
            .filter { p -> busqueda.isBlank() || p.nombre.contains(busqueda, ignoreCase = true) }
            .sortedByDescending { p -> conteoFiados[p.nombre.trim().lowercase()] ?: 0 }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Búsqueda + nuevo
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar producto") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) }
                )
                Button(onClick = { productoEditar = null; mostrarDialogo = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nuevo")
                }
            }

            // Filtros con conteo
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filtro == "todos",
                        onClick = { filtro = "todos" },
                        label = { Text("Todos · ${productos.size}") }
                    )
                }
                item {
                    FilterChip(
                        selected = filtro == "oferta",
                        onClick = { filtro = "oferta" },
                        label = { Text("En oferta · $enOferta") }
                    )
                }
                item {
                    FilterChip(
                        selected = filtro == "oculto",
                        onClick = { filtro = "oculto" },
                        label = { Text("Ocultos · $ocultos") }
                    )
                }
            }

            if (productosFiltrados.isEmpty() && busqueda.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No hay productos cargados",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Tocá Nuevo para agregar precios",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val mostrarOrdenLabel = conteoFiados.isNotEmpty() && busqueda.isBlank() && filtro == "todos"
                if (mostrarOrdenLabel) {
                    Text(
                        "ORDENADOS POR LOS MÁS FIADOS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(productosFiltrados, key = { it.id }) { prod ->
                        val vecesFiado = conteoFiados[prod.nombre.trim().lowercase()] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { productoEditar = prod; mostrarDialogo = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        productoParaFoto = prod
                                        fotoProductoLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
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
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // overlay cámara
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt, null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(prod.nombre, fontWeight = FontWeight.Medium)
                                    if (prod.esOferta) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                            Text("OFERTA",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    if (!prod.habilitado) {
                                        Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                            Text("OCULTO",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                val subtitulo = buildString {
                                    if (prod.descripcion.isNotBlank()) append(prod.descripcion)
                                    if (vecesFiado > 0) {
                                        if (isNotEmpty()) append(" · ")
                                        append("fiado $vecesFiado ve${if (vecesFiado == 1) "z" else "ces"}")
                                    }
                                }
                                if (subtitulo.isNotBlank()) {
                                    Text(subtitulo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            // Precio como botón tappable para edición rápida
                            OutlinedCard(onClick = { productoEditarPrecio = prod }) {
                                Text(
                                    "$${"%.0f".format(prod.precio)}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                    item {
                        Text(
                            "Tocá el precio para cambiarlo rápido.\nTocá el producto para editar todo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Diálogo de edición rápida de precio
    productoEditarPrecio?.let { prod ->
        var nuevoPrecio by remember(prod.id) { mutableStateOf("%.0f".format(prod.precio)) }
        AlertDialog(
            onDismissRequest = { productoEditarPrecio = null },
            title = { Text(prod.nombre) },
            text = {
                OutlinedTextField(
                    value = nuevoPrecio,
                    onValueChange = { nuevoPrecio = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Precio") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val precio = nuevoPrecio.toDoubleOrNull()
                        if (precio != null && precio >= 0) {
                            vm.actualizarProducto(prod.copy(precio = precio))
                            productoEditarPrecio = null
                        }
                    },
                    enabled = nuevoPrecio.toDoubleOrNull() != null
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { productoEditarPrecio = null }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDialogo) {
        DialogProducto(
            productoInicial = productoEditar,
            productosExistentes = if (productoEditar == null) productos else emptyList(),
            onGuardar = { nombre, precio, descripcion, habilitado, esOferta, precioAnterior, agotado ->
                if (productoEditar != null) {
                    vm.actualizarProducto(productoEditar!!.copy(
                        nombre = nombre, precio = precio, descripcion = descripcion,
                        habilitado = habilitado, esOferta = esOferta, precioAnterior = precioAnterior,
                        agotado = agotado
                    ))
                } else {
                    vm.agregarProducto(nombre, precio, descripcion, habilitado, esOferta, precioAnterior, agotado)
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
            text = { Text("¿Eliminar ${prod.nombre} de la lista?") },
            confirmButton = {
                TextButton(onClick = { vm.eliminarProducto(prod.id); productoAEliminar = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { productoAEliminar = null }) { Text("Cancelar") } }
        )
    }
}

// ── Tab Equipo ─────────────────────────────────────────────────────────────────

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
    var mostrarDialogNombre by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<Pair<String, String>?>(null) }
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

    // Caja de hoy: stats por persona (uid → nombre, anotó N, cobró $X)
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

    // Último movimiento por uid (sin anulados)
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
                            Text("Quién",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                            Text("Anotó",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp))
                            Text("Cobró",
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
                Text("EMPLEADOS · ${empleados.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { mostrarDialogNombre = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Sumar")
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
                        "Los empleados pueden anotar fiados y registrar pagos. Controlá los permisos por cliente desde el detalle de cada uno.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (empleados.isEmpty()) {
            item {
                Text("Sin empleados. Tocá + Sumar para generar un código.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(empleados, key = { it.id }) { emp ->
                val ultimoMov = ultimoMovPorUid[emp.uid]
                val reclamos = disputasPorUid[emp.uid] ?: 0
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        leadingContent = {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        emp.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        headlineContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(emp.nombre, fontWeight = FontWeight.Medium)
                                if (reclamos > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                        Text("$reclamos reclamo${if (reclamos != 1) "s" else ""}",
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        },
                        supportingContent = {
                            if (ultimoMov != null) {
                                val accion = if (ultimoMov.tipo == "cargo") "Anotó fiado" else "Cobró"
                                val esHoy = ultimoMov.fecha.toDate().time >= inicioDia
                                val fechaStr = if (esHoy) "hoy ${fmtHora.format(ultimoMov.fecha.toDate())}"
                                              else fmtFecha.format(ultimoMov.fecha.toDate())
                                Text("Último: $fechaStr · $accion",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { empleadoAEliminar = emp }) {
                                Icon(Icons.Default.Delete, "Eliminar",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }
    }

    if (mostrarDialogNombre) {
        DialogNombreEmpleado(
            cargando = vmCargando,
            error = vmError,
            onGuardar = { nombre, whatsapp ->
                vm.generarCodigoEmpleado(nombre) { codigo ->
                    mostrarDialogNombre = false
                    codigoGenerado = codigo to whatsapp
                }
            },
            onDismiss = { mostrarDialogNombre = false }
        )
    }

    empleadoAEliminar?.let { emp ->
        AlertDialog(
            onDismissRequest = { empleadoAEliminar = null },
            title = { Text("Eliminar empleado") },
            text = { Text("¿Eliminar a ${emp.nombre}? Se va a desconectar del local.") },
            confirmButton = {
                TextButton(onClick = { onEliminarEmpleado(emp.id); empleadoAEliminar = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { empleadoAEliminar = null }) { Text("Cancelar") } }
        )
    }

    codigoGenerado?.let { (codigo, whatsapp) ->
        AlertDialog(
            onDismissRequest = { codigoGenerado = null },
            title = { Text("Código generado") },
            text = {
                Column {
                    Text("Compartí este código con el empleado:")
                    Spacer(Modifier.height(12.dp))
                    Text(codigo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("El empleado lo ingresa en la app para acceder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                if (whatsapp.isNotBlank()) {
                    TextButton(onClick = {
                        val numero = whatsapp.replace(Regex("[^0-9]"), "")
                        val mensaje = "Tu código de empleado para Kosking es: $codigo — Descargá la app y usá este código para conectarte al local."
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        codigoGenerado = null
                    }) { Text("Enviar por WhatsApp") }
                } else {
                    TextButton(onClick = { codigoGenerado = null }) { Text("Listo") }
                }
            },
            dismissButton = if (whatsapp.isNotBlank()) {
                { TextButton(onClick = { codigoGenerado = null }) { Text("Cerrar") } }
            } else null
        )
    }
}

// ── Panel embebido para usar dentro de PantallaExplorar (TabMás) ──────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelDuenoEmbedido(
    vm: DuenoViewModel,
    tabSeleccionado: Int = 0,
    onTabChange: (Int) -> Unit = {},
    onAbrirAgregarCliente: () -> Unit,
    onAbrirDetalleCliente: (String) -> Unit,
    onAbrirAgregarFiado: (clienteId: String, tipo: String) -> Unit,
    onAbrirDetalleMovimiento: (String) -> Unit,
    onAbrirSeleccionarCliente: (tipo: String) -> Unit = {}
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

    // tabSeleccionado == 0 → dashboard, 1..4 → sección
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
            2 -> TabProductos(vm = vm, movimientos = movimientosLocal)
            3 -> TabEquipo(
                empleados = empleados,
                movimientos = movimientosLocal,
                onEliminarEmpleado = { vm.eliminarEmpleado(it) },
                vm = vm
            )
            4 -> TabMiLocal(vm = vm)
        }
        return
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
        val fmtFecha = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
        val fmtDia = remember { SimpleDateFormat("dd 'de' MMMM", Locale("es", "AR")) }
        val clienteMapTodas = remember(clientes) { clientes.associateBy { it.id } }
        val todasVisitas = remember(movimientosLocal, clienteMapTodas) {
            agruparVisitas(movimientosLocal.filter { !it.anulado }, clienteMapTodas, fmtFecha)
        }
        val visitasPorFecha = remember(todasVisitas) {
            todasVisitas.groupBy { v ->
                val mov = movimientosLocal.firstOrNull { m -> m.clienteId == v.clienteId && fmtFecha.format(m.fecha.toDate()) == v.hora }
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
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        nombreLocal.ifBlank { "Mi local" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (codigoLocal.isNotBlank()) {
                        Text(
                            "Código para clientes: $codigoLocal",
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
                    if (clientesConDeuda > 0) {
                        Text(
                            "$clientesConDeuda cliente${if (clientesConDeuda != 1) "s" else ""} con deuda  →",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onTabChange(1) }
                        )
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
                        "−$${"%.0f".format(cobradoHoyVal)} cobrado",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cobradoHoyVal > 0) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Botones de acción
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onAbrirSeleccionarCliente("cargo") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Anotar fiado")
                }
                OutlinedButton(
                    onClick = { onAbrirSeleccionarCliente("pago") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cobrar")
                }
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
                            Text("${cliente.nombre} superó su límite", fontWeight = FontWeight.Medium)
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
                SeccionCard(
                    icon = Icons.Default.Store,
                    label = "Mi local",
                    modifier = Modifier.weight(1f),
                    onClick = { onTabChange(4) }
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
                        "ÚLTIMAS VISITAS",
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
                    Text("Todavía no hay movimientos",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Pantalla Seleccionar Cliente (acción rápida desde Resumen) ────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeleccionarCliente(
    tipo: String,
    vm: DuenoViewModel,
    onSeleccionado: (clienteId: String) -> Unit,
    onVolver: () -> Unit
) {
    val clientes by vm.clientes.collectAsState()
    var busqueda by remember { mutableStateOf("") }

    val titulo = if (tipo == "cargo") "¿A quién le anotás?" else "¿Quién está pagando?"
    val clientesFiltrados = remember(clientes, busqueda) {
        if (busqueda.isBlank()) clientes
        else clientes.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titulo)
                        Text(
                            "Seleccioná un cliente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Buscador
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.text.BasicTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box {
                            if (busqueda.isEmpty()) {
                                Text("Buscar cliente...", style = MaterialTheme.typography.bodyMedium,
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

            if (clientesFiltrados.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (clientes.isEmpty()) "No tenés clientes aún" else "Sin resultados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn {
                    items(clientesFiltrados, key = { it.id }) { cliente ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeleccionado(cliente.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cliente.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                cliente.nombre,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ChevronRight, null,
                                Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 70.dp))
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// ── Tab Mi Local ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabMiLocal(vm: DuenoViewModel) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    val localActual by vm.localData.collectAsState()

    var nombre by remember(localActual) { mutableStateOf(localActual?.nombre ?: "") }
    var tipo by remember(localActual) { mutableStateOf(localActual?.tipo ?: "Kiosco") }
    var direccion by remember(localActual) { mutableStateOf(localActual?.direccion ?: "") }
    var telefono by remember(localActual) { mutableStateOf(localActual?.telefono ?: "") }
    var descripcion by remember(localActual) { mutableStateOf(localActual?.descripcion ?: "") }
    var abierto by remember(localActual) { mutableStateOf(localActual?.abierto ?: true) }
    var expandirTipo by remember { mutableStateOf(false) }

    var latGuardada by remember(localActual) { mutableStateOf(localActual?.lat ?: 0.0) }
    var lngGuardada by remember(localActual) { mutableStateOf(localActual?.lng ?: 0.0) }
    var obteniendoUbicacion by remember { mutableStateOf(false) }
    var errorUbicacion by remember { mutableStateOf<String?>(null) }
    var guardado by remember { mutableStateOf(false) }

    val horarios = remember(localActual) {
        mutableStateMapOf<String, HorarioDia>().also { map ->
            localActual?.horarios?.forEach { (k, v) -> map[k] = v }
        }
    }

    val fotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.subirFotoLocal(uri) {}
    }

    val lanzarUbicacion = rememberUbicacionLauncher(
        onUbicacion = { lat, lng -> latGuardada = lat; lngGuardada = lng; obteniendoUbicacion = false; errorUbicacion = null },
        onError = { msg -> errorUbicacion = msg; obteniendoUbicacion = false }
    )

    if (localActual == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Foto del local
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Foto del local", style = MaterialTheme.typography.titleSmall)
                    if (localActual?.imagenUrl?.isNotBlank() == true) {
                        AsyncImage(
                            model = localActual!!.imagenUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }
                    OutlinedButton(
                        onClick = { fotoLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (localActual?.imagenUrl?.isNotBlank() == true) "Cambiar foto" else "Agregar foto")
                    }
                }
            }
        }

        // Datos basicos
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Datos del local", style = MaterialTheme.typography.titleSmall)

                    OutlinedTextField(value = nombre, onValueChange = { nombre = it; guardado = false },
                        label = { Text("Nombre *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    ExposedDropdownMenuBox(expanded = expandirTipo, onExpandedChange = { expandirTipo = it }) {
                        OutlinedTextField(value = tipo, onValueChange = {}, readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirTipo) },
                            modifier = Modifier.fillMaxWidth().menuAnchor())
                        ExposedDropdownMenu(expanded = expandirTipo, onDismissRequest = { expandirTipo = false }) {
                            listOf("Kiosco","Almacen","Maxikiosco","Ropa","Corralon","Farmacia","Verduleria","Otro").forEach { t ->
                                DropdownMenuItem(text = { Text(t) }, onClick = { tipo = t; expandirTipo = false; guardado = false })
                            }
                        }
                    }

                    OutlinedTextField(value = telefono, onValueChange = { telefono = it; guardado = false },
                        label = { Text("Telefono / WhatsApp") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    OutlinedTextField(value = direccion, onValueChange = { direccion = it; guardado = false },
                        label = { Text("Direccion") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    OutlinedTextField(value = descripcion, onValueChange = { descripcion = it; guardado = false },
                        label = { Text("Descripcion (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                }
            }
        }

        // Horarios
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Horarios de atencion", style = MaterialTheme.typography.titleSmall)
                    Text("El mapa muestra automaticamente si estas abierto ahora.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("lunes" to "Lunes","martes" to "Martes","miercoles" to "Miercoles",
                        "jueves" to "Jueves","viernes" to "Viernes","sabado" to "Sabado","domingo" to "Domingo"
                    ).forEach { (clave, etiqueta) ->
                        val h = horarios[clave] ?: HorarioDia()
                        FilaHorarioPanelDueno(dia = etiqueta, horario = h, onCambio = { nuevo -> horarios[clave] = nuevo; guardado = false })
                    }
                }
            }
        }

        // Ubicacion GPS con mapa interactivo
        item {
            UbicacionCard(
                lat = latGuardada,
                lng = lngGuardada,
                obteniendo = obteniendoUbicacion,
                error = errorUbicacion,
                onUsarGps = { obteniendoUbicacion = true; guardado = false; lanzarUbicacion() },
                onPosicionCambiada = { nuevaLat, nuevaLng ->
                    latGuardada = nuevaLat
                    lngGuardada = nuevaLng
                    guardado = false
                }
            )
        }

        // Estado abierto/cerrado
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Local abierto ahora") },
                    supportingContent = { Text(if (abierto) "Visible como abierto" else "Visible como cerrado") },
                    trailingContent = { Switch(checked = abierto, onCheckedChange = { abierto = it; guardado = false }) }
                )
            }
        }

        // Error y boton guardar
        item {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (guardado) {
                Text("Cambios guardados", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    vm.actualizarLocal(
                        nombre = nombre.trim(), tipo = tipo, direccion = direccion.trim(),
                        telefono = telefono.trim(), descripcion = descripcion.trim(), abierto = abierto,
                        lat = latGuardada.takeIf { it != 0.0 }, lng = lngGuardada.takeIf { it != 0.0 },
                        horarios = horarios.toMap().ifEmpty { null },
                        onExito = { guardado = true }
                    )
                },
                enabled = !cargando && nombre.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Guardar cambios")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UbicacionCard(
    lat: Double,
    lng: Double,
    obteniendo: Boolean,
    error: String?,
    onUsarGps: () -> Unit,
    onPosicionCambiada: (Double, Double) -> Unit
) {
    val tieneUbicacion = lat != 0.0 && lng != 0.0
    val camaraState = rememberCameraPositionState {
        position = if (tieneUbicacion)
            CameraPosition.fromLatLngZoom(LatLng(lat, lng), 16f)
        else
            CameraPosition.fromLatLngZoom(LatLng(-34.6037, -58.3816), 11f)
    }
    val markerState = remember(lat, lng) { MarkerState(LatLng(lat, lng)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(lat, lng) {
        if (lat != 0.0 && lng != 0.0) {
            camaraState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16f))
        }
    }

    // Detectar cuando el marker termina de ser arrastrado
    val markerDragging = markerState.dragState == com.google.maps.android.compose.DragState.END
    LaunchedEffect(markerDragging) {
        if (markerDragging) {
            onPosicionCambiada(markerState.position.latitude, markerState.position.longitude)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ubicación en el mapa", style = MaterialTheme.typography.titleSmall)

            if (tieneUbicacion) {
                Text(
                    "Tocá el mapa para mover el pin al lugar exacto, o arrastrá el marcador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = camaraState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                        onMapClick = { latlng ->
                            markerState.position = latlng
                            onPosicionCambiada(latlng.latitude, latlng.longitude)
                        }
                    ) {
                        Marker(
                            state = markerState,
                            draggable = true
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        "%.5f, %.5f".format(markerState.position.latitude, markerState.position.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "Sin ubicación — tu local no aparece en el mapa de la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            OutlinedButton(
                onClick = onUsarGps,
                enabled = !obteniendo,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (obteniendo) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Obteniendo ubicación...")
                } else {
                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (tieneUbicacion) "Actualizar con GPS" else "Usar mi ubicación actual")
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
