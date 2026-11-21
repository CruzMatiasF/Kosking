package com.kosking.app.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.kosking.app.datos.OpenFoodFactsRepository
import com.kosking.app.modelo.ItemAnotado
import com.kosking.app.modelo.Movimiento
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.util.WhatsAppUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

// ksk_c_{clienteId} → QR de identificación de cliente
private const val PREFIJO_CLIENTE_QR = "ksk_c_"

// Solo dos modos: anotar (flujo principal) y cargar nuevo producto al catálogo
private enum class ModoEscaner { ANOTAR, CARGAR_NUEVO }

private data class ProductoDetectado(
    val nombre: String,
    val precio: Double,
    val productoId: String? = null,
    val codigoBarras: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEscaner(
    vm: DuenoViewModel,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val productos by vm.productos.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val movimientos by vm.movimientosLocal.collectAsState()
    val cargandoVm by vm.cargando.collectAsState()
    val nombreLocal by vm.nombreLocal.collectAsState()

    val clientesOrdenados = remember(clientes, movimientos) {
        clientes.sortedByDescending { c ->
            movimientos.filter { it.clienteId == c.id }.maxOfOrNull { it.fecha.seconds } ?: 0L
        }
    }

    var permiso by remember { mutableStateOf(false) }
    var modo by remember { mutableStateOf(ModoEscaner.ANOTAR) }

    // Cliente: se setea al escanear el QR o buscando manualmente — siempre opcional
    var clienteSeleccionado by remember { mutableStateOf<Cliente?>(null) }
    var mostrarBuscarCliente by remember { mutableStateOf(false) }
    var busquedaCliente by remember { mutableStateOf("") }

    var itemsAnotados by remember { mutableStateOf(listOf<ItemAnotado>()) }
    var productoParaCargar by remember { mutableStateOf<ProductoDetectado?>(null) }
    var buscandoProducto by remember { mutableStateOf(false) }
    var cargandoAnotacion by remember { mutableStateOf(false) }
    var queryBusqueda by remember { mutableStateOf("") }
    var precioCarga by remember { mutableStateOf("") }
    var nombreCarga by remember { mutableStateOf("") }
    var mostrarConfirmar by remember { mutableStateOf(false) }
    var anotacionExitosa by remember { mutableStateOf<Pair<Cliente, List<ItemAnotado>>?>(null) }
    var mostrarDialogoCambioModo by remember { mutableStateOf(false) }
    val yaDetecto = remember { mutableStateOf(false) }
    val ultimoCodigo = remember { mutableStateOf("") }
    val ultimoCodigoTs = remember { mutableStateOf(0L) }
    val focusBusqueda = remember { FocusRequester() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permiso = granted }

    LaunchedEffect(Unit) {
        permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!permiso) launcher.launch(Manifest.permission.CAMERA)
    }

    val sugerencias = remember(queryBusqueda, productos) {
        if (queryBusqueda.isBlank()) emptyList()
        else productos.filter { it.nombre.contains(queryBusqueda, ignoreCase = true) }.take(3)
    }

    val filtradosCliente = remember(busquedaCliente, clientesOrdenados) {
        if (busquedaCliente.isBlank()) clientesOrdenados
        else clientesOrdenados.filter { it.nombre.contains(busquedaCliente, ignoreCase = true) }
    }

    fun agregarALista(item: ItemAnotado) {
        val idx = itemsAnotados.indexOfFirst { it.productoId != null && it.productoId == item.productoId }
        itemsAnotados = if (idx >= 0)
            itemsAnotados.mapIndexed { i, it -> if (i == idx) it.copy(cantidad = it.cantidad + 1) else it }
        else itemsAnotados + item
        queryBusqueda = ""
    }

    fun confirmarAnotacion(cliente: Cliente) {
        val itemsSnap = itemsAnotados.toList()
        cargandoAnotacion = true
        vm.agregarMovimientos(
            clienteId = cliente.id,
            items = itemsSnap.map { Triple(it.nombre, it.precio, it.cantidad) },
            tipo = "cargo",
            onExito = {
                cargandoAnotacion = false
                mostrarConfirmar = false
                if (cliente.telefono.isNotBlank()) {
                    anotacionExitosa = Pair(cliente, itemsSnap)
                } else {
                    onCerrar()
                }
            }
        )
    }

    fun onCodigo(codigo: String) {
        val ahora = System.currentTimeMillis()
        if (codigo == ultimoCodigo.value && ahora - ultimoCodigoTs.value < 1500L) return
        if (yaDetecto.value) return
        yaDetecto.value = true
        ultimoCodigo.value = codigo
        ultimoCodigoTs.value = ahora

        // QR de cliente: lo setea silenciosamente sin interrumpir el escaneo
        if (codigo.startsWith(PREFIJO_CLIENTE_QR)) {
            val clienteId = codigo.removePrefix(PREFIJO_CLIENTE_QR)
            val cliente = clientes.find { it.id == clienteId }
            if (cliente != null) clienteSeleccionado = cliente
            yaDetecto.value = false
            return
        }

        // En modo cargar nuevo, el cliente no aplica
        buscandoProducto = true
        scope.launch {
            try {
                val enLocal = productos.firstOrNull { it.codigoBarras == codigo }
                val nombre: String
                val precio: Double
                val productoId: String?
                if (enLocal != null) {
                    nombre = enLocal.nombre; precio = enLocal.ultimoPrecio; productoId = enLocal.id
                } else {
                    val ext = OpenFoodFactsRepository.buscarPorCodigo(codigo)
                    nombre = ext?.nombre ?: ""; precio = 0.0; productoId = null
                }
                buscandoProducto = false
                when (modo) {
                    ModoEscaner.ANOTAR -> {
                        val item = ItemAnotado(nombre.ifBlank { "Código: $codigo" }, precio, productoId)
                        val idx = itemsAnotados.indexOfFirst { it.productoId != null && it.productoId == productoId }
                        itemsAnotados = if (idx >= 0)
                            itemsAnotados.mapIndexed { i, it -> if (i == idx) it.copy(cantidad = it.cantidad + 1) else it }
                        else itemsAnotados + item
                        delay(300)
                        yaDetecto.value = false
                    }
                    ModoEscaner.CARGAR_NUEVO -> {
                        productoParaCargar = ProductoDetectado(nombre, precio, productoId, codigo)
                        nombreCarga = nombre
                        precioCarga = if (precio > 0) precio.toInt().toString() else ""
                    }
                }
            } catch (_: Exception) {
                buscandoProducto = false
                yaDetecto.value = false
            }
        }
    }

    // ── Pantalla confirmar (sin cliente preseleccionado → elige al final) ──
    if (mostrarConfirmar) {
        PantallaConfirmarAnotacion(
            items = itemsAnotados,
            clientes = clientesOrdenados,
            movimientos = movimientos,
            cargando = cargandoVm,
            onConfirmar = { confirmarAnotacion(it) },
            onCrearYConfirmar = { nombre, onClienteCreado ->
                vm.agregarClienteYObtener(nombre = nombre, onExito = onClienteCreado)
            },
            onVolver = { mostrarConfirmar = false }
        )
        return
    }

    // ── Pantalla éxito ──
    anotacionExitosa?.let { (cliente, items) ->
        val hora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
        val ctx = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "Anotado para ${cliente.nombre}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${items.sumOf { it.cantidad }} producto${if (items.sumOf { it.cantidad } != 1) "s" else ""}  ·  $${items.sumOf { it.precio * it.cantidad }.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    WhatsAppUtil.abrirWhatsApp(
                        ctx, cliente.telefono,
                        WhatsAppUtil.ticketVisita(
                            nombreCliente = cliente.nombre,
                            nombreLocal = nombreLocal,
                            items = items.map { Triple(it.nombre, it.precio, it.cantidad) },
                            tipo = "cargo",
                            hora = hora
                        )
                    )
                    anotacionExitosa = null
                    onCerrar()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enviar ticket por WhatsApp", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { anotacionExitosa = null; onCerrar() }, modifier = Modifier.fillMaxWidth()) {
                Text("Listo, sin enviar")
            }
        }
        return
    }

    // ── Pantalla principal del escáner ──
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Cámara ──
        Box(modifier = Modifier.fillMaxWidth().weight(0.45f)) {
            if (permiso) {
                AndroidView(
                    factory = { ctx ->
                        val pv = PreviewView(ctx)
                        val exec = Executors.newSingleThreadExecutor()
                        val future = ProcessCameraProvider.getInstance(ctx)
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                            val scanner = BarcodeScanning.getClient()
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build().also { ia ->
                                    ia.setAnalyzer(exec) { proxy ->
                                        @androidx.camera.core.ExperimentalGetImage
                                        val media = proxy.image
                                        if (media != null && !yaDetecto.value) {
                                            val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                                            scanner.process(img)
                                                .addOnSuccessListener { codes ->
                                                    codes.firstOrNull { it.rawValue != null }
                                                        ?.rawValue?.let { onCodigo(it) }
                                                }
                                                .addOnCompleteListener { proxy.close() }
                                        } else proxy.close()
                                    }
                                }
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))
                        pv
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Marco de escaneo
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val c = MaterialTheme.colorScheme.primary
                    Canvas(Modifier.size(220.dp)) {
                        val s = 28.dp.toPx(); val w = 3.5.dp.toPx()
                        listOf(
                            androidx.compose.ui.geometry.Offset(0f, 0f),
                            androidx.compose.ui.geometry.Offset(size.width, 0f),
                            androidx.compose.ui.geometry.Offset(0f, size.height),
                            androidx.compose.ui.geometry.Offset(size.width, size.height)
                        ).forEach { corner ->
                            val hx = if (corner.x == 0f) corner.x + s else corner.x - s
                            val vy = if (corner.y == 0f) corner.y + s else corner.y - s
                            drawLine(c, corner, androidx.compose.ui.geometry.Offset(hx, corner.y), w)
                            drawLine(c, corner, androidx.compose.ui.geometry.Offset(corner.x, vy), w)
                        }
                    }
                }

                if (buscandoProducto) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Dar permiso de cámara")
                    }
                }
            }

            IconButton(onClick = onCerrar, modifier = Modifier.statusBarsPadding().padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
        }

        // ── Panel inferior ──
        Surface(modifier = Modifier.fillMaxWidth().weight(0.55f), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Tabs: Anotar / Cargar nuevo
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (modo == ModoEscaner.ANOTAR) {
                        Button(onClick = {}, modifier = Modifier.weight(1f).height(36.dp), shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anotar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(onClick = { modo = ModoEscaner.ANOTAR; yaDetecto.value = false }, modifier = Modifier.weight(1f).height(36.dp), shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anotar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (modo == ModoEscaner.CARGAR_NUEVO) {
                        Button(onClick = {}, modifier = Modifier.weight(1f).height(36.dp), shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cargar nuevo", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (modo == ModoEscaner.ANOTAR && itemsAnotados.isNotEmpty()) mostrarDialogoCambioModo = true
                                else { modo = ModoEscaner.CARGAR_NUEVO; yaDetecto.value = false }
                            },
                            modifier = Modifier.weight(1f).height(36.dp), shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cargar nuevo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                HorizontalDivider()

                when (modo) {

                // ── Modo Anotar ──
                ModoEscaner.ANOTAR -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Búsqueda manual de producto
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = queryBusqueda,
                                onValueChange = { queryBusqueda = it },
                                placeholder = { Text("Sin código? Escribí el nombre") },
                                modifier = Modifier.weight(1f).focusRequester(focusBusqueda),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                trailingIcon = {
                                    if (queryBusqueda.isNotBlank()) {
                                        IconButton(onClick = { queryBusqueda = "" }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            )
                            if (queryBusqueda.isNotBlank() && sugerencias.isEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                IconButton(onClick = { agregarALista(ItemAnotado(queryBusqueda.trim(), 0.0)) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        if (sugerencias.isNotEmpty()) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(sugerencias) { prod ->
                                    ElevatedCard(onClick = { agregarALista(ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id)) }, modifier = Modifier.height(56.dp)) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
                                            Text(prod.nombre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1)
                                            if (prod.ultimoPrecio > 0) Text("$${prod.ultimoPrecio.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider()
                        }

                        HorizontalDivider()

                        // Lista de productos escaneados
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            if (itemsAnotados.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Escaneá un producto para empezar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                items(itemsAnotados) { item ->
                                    ListItem(
                                        headlineContent = { Text(item.nombre, style = MaterialTheme.typography.bodyMedium) },
                                        supportingContent = {
                                            Text(
                                                "$${(item.precio * item.cantidad).toInt()}" + if (item.cantidad > 1) "  ·  x${item.cantidad}" else "",
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        itemsAnotados = if (item.cantidad > 1)
                                                            itemsAnotados.map { if (it === item) it.copy(cantidad = it.cantidad - 1) else it }
                                                        else itemsAnotados - item
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
                                                Text("${item.cantidad}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                IconButton(
                                                    onClick = { itemsAnotados = itemsAnotados.map { if (it === item) it.copy(cantidad = it.cantidad + 1) else it } },
                                                    modifier = Modifier.size(32.dp)
                                                ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }

                        // ── Sección cliente (fija al fondo, antes del botón) ──
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mostrarBuscarCliente = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (clienteSeleccionado != null) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (clienteSeleccionado != null) {
                                    Text(
                                        clienteSeleccionado!!.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        "Cliente (opcional) — escaneá QR o buscá",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (clienteSeleccionado != null) {
                                IconButton(
                                    onClick = { clienteSeleccionado = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Botón principal
                        if (itemsAnotados.isNotEmpty()) {
                            val total = itemsAnotados.sumOf { it.precio * it.cantidad }
                            val cliente = clienteSeleccionado
                            Button(
                                onClick = {
                                    if (cliente != null) confirmarAnotacion(cliente)
                                    else mostrarConfirmar = true
                                },
                                enabled = !cargandoAnotacion,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).navigationBarsPadding().height(48.dp)
                            ) {
                                if (cargandoAnotacion) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text(
                                        if (cliente != null) "Confirmar para ${cliente.nombre}  ·  $${total.toInt()}"
                                        else "Elegir cliente  ·  $${total.toInt()}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.navigationBarsPadding())
                        }
                    }
                }

                // ── Modo Cargar nuevo ──
                ModoEscaner.CARGAR_NUEVO -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val prod = productoParaCargar
                        if (prod == null) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Escaneá el código del producto", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            if (prod.nombre.isBlank()) {
                                OutlinedTextField(value = nombreCarga, onValueChange = { nombreCarga = it }, label = { Text("Nombre del producto") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                            } else {
                                Text(prod.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = precioCarga,
                                onValueChange = { precioCarga = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Precio en tu local") },
                                prefix = { Text("$") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            Button(
                                onClick = {
                                    vm.guardarProducto(
                                        nombre = nombreCarga.trim().ifBlank { prod.nombre.ifBlank { prod.codigoBarras } },
                                        precio = precioCarga.toDoubleOrNull() ?: 0.0,
                                        productoId = prod.productoId,
                                        categoria = "",
                                        imagenUri = null
                                    ) { productoParaCargar = null; precioCarga = ""; yaDetecto.value = false }
                                },
                                enabled = !cargandoVm && (prod.nombre.isNotBlank() || nombreCarga.isNotBlank()),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (cargandoVm) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("Guardar al local", fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { productoParaCargar = null; yaDetecto.value = false; precioCarga = ""; nombreCarga = "" }, modifier = Modifier.fillMaxWidth()) {
                                Text("Escanear otro")
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }
                } // end when
            }
        }
    }

    // ── Bottom sheet buscar cliente ──
    if (mostrarBuscarCliente) {
        ModalBottomSheet(onDismissRequest = { mostrarBuscarCliente = false; busquedaCliente = "" }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    "¿Para quién?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = busquedaCliente,
                    onValueChange = { busquedaCliente = it },
                    placeholder = { Text("Buscar cliente...") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (busquedaCliente.isNotEmpty()) {
                            IconButton(onClick = { busquedaCliente = "" }) { Icon(Icons.Default.Close, null) }
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                // Crear nuevo si no hay resultados
                if (busquedaCliente.isNotBlank() && filtradosCliente.isEmpty()) {
                    ListItem(
                        headlineContent = { Text("Crear \"${busquedaCliente.trim()}\"", fontWeight = FontWeight.SemiBold) },
                        leadingContent = { Icon(Icons.Default.PersonAdd, null) },
                        modifier = Modifier.clickable {
                            vm.agregarClienteYObtener(nombre = busquedaCliente.trim()) { nuevoCliente ->
                                clienteSeleccionado = nuevoCliente
                                mostrarBuscarCliente = false
                                busquedaCliente = ""
                            }
                        }
                    )
                    HorizontalDivider()
                }
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(filtradosCliente) { cliente ->
                        ListItem(
                            headlineContent = { Text(cliente.nombre, fontWeight = FontWeight.Medium) },
                            supportingContent = if (cliente.deudaTotal > 0) ({
                                Text("Debe $${cliente.deudaTotal.toInt()}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }) else null,
                            trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.clickable {
                                clienteSeleccionado = cliente
                                mostrarBuscarCliente = false
                                busquedaCliente = ""
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // ── Diálogo cambio de modo ──
    if (mostrarDialogoCambioModo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCambioModo = false },
            title = { Text("¿Cambiar de modo?") },
            text = { Text("Tenés ${itemsAnotados.size} producto${if (itemsAnotados.size != 1) "s" else ""} en la lista. Si cambiás a Cargar nuevo los perdés.") },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoCambioModo = false; itemsAnotados = emptyList(); modo = ModoEscaner.CARGAR_NUEVO; yaDetecto.value = false }) {
                    Text("Sí, cambiar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCambioModo = false }) { Text("Cancelar") }
            }
        )
    }
}
