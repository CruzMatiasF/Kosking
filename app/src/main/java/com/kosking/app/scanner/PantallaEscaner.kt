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
import com.kosking.app.modelo.Movimiento
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private enum class ModoEscaner { NINGUNO, ANOTAR, CARGAR_NUEVO }

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

    // Clientes ordenados por actividad más reciente
    val clientesOrdenados = remember(clientes, movimientos) {
        clientes.sortedByDescending { cliente ->
            movimientos.filter { it.clienteId == cliente.id }
                .maxOfOrNull { it.fecha.seconds } ?: 0L
        }
    }

    var permiso by remember { mutableStateOf(false) }
    var modo by remember { mutableStateOf(ModoEscaner.ANOTAR) }
    var itemsAnotados by remember { mutableStateOf(listOf<ItemAnotado>()) }
    var productoParaCargar by remember { mutableStateOf<ProductoDetectado?>(null) }
    var buscandoProducto by remember { mutableStateOf(false) }
    var queryBusqueda by remember { mutableStateOf("") }
    var precioCarga by remember { mutableStateOf("") }
    var nombreCarga by remember { mutableStateOf("") }
    var mostrarConfirmar by remember { mutableStateOf(false) }
    var mostrarDialogoCambioModo by remember { mutableStateOf(false) }
    val yaDetecto = remember { mutableStateOf(false) }
    // Rastrea el último código escaneado y cuándo, para ignorar solo ese código
    // sin bloquear el resto del escáner
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

    fun agregarALista(item: ItemAnotado) {
        val idx = itemsAnotados.indexOfFirst { it.productoId != null && it.productoId == item.productoId }
        itemsAnotados = if (idx >= 0)
            itemsAnotados.mapIndexed { i, it -> if (i == idx) it.copy(cantidad = it.cantidad + 1) else it }
        else itemsAnotados + item
        queryBusqueda = ""
    }

    fun onCodigo(codigo: String) {
        if (modo == ModoEscaner.NINGUNO) return
        // Ignorar si es el mismo código escaneado hace menos de 1.5 seg
        val ahora = System.currentTimeMillis()
        if (codigo == ultimoCodigo.value && ahora - ultimoCodigoTs.value < 1500L) return
        if (yaDetecto.value) return
        yaDetecto.value = true
        ultimoCodigo.value = codigo
        ultimoCodigoTs.value = ahora
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
                        // yaDetecto queda en true hasta que el usuario toque "escanear otro"
                    }
                    ModoEscaner.NINGUNO -> yaDetecto.value = false
                }
            } catch (_: Exception) {
                buscandoProducto = false
                yaDetecto.value = false
            }
        }
    }

    if (mostrarConfirmar) {
        PantallaConfirmarAnotacion(
            items = itemsAnotados,
            clientes = clientesOrdenados,
            movimientos = movimientos,
            cargando = cargandoVm,
            onConfirmar = { cliente ->
                vm.agregarMovimientos(
                    clienteId = cliente.id,
                    items = itemsAnotados.map { Triple(it.nombre, it.precio, it.cantidad) },
                    tipo = "cargo",
                    onExito = { onCerrar() }
                )
            },
            onCrearYConfirmar = { nombre, onClienteCreado ->
                vm.agregarClienteYObtener(nombre = nombre, onExito = onClienteCreado)
            },
            onVolver = { mostrarConfirmar = false }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Cámara (porción superior) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
        ) {
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
                        val corners = listOf(
                            androidx.compose.ui.geometry.Offset(0f, 0f),
                            androidx.compose.ui.geometry.Offset(size.width, 0f),
                            androidx.compose.ui.geometry.Offset(0f, size.height),
                            androidx.compose.ui.geometry.Offset(size.width, size.height)
                        )
                        corners.forEach { corner ->
                            val hx = if (corner.x == 0f) corner.x + s else corner.x - s
                            val vy = if (corner.y == 0f) corner.y + s else corner.y - s
                            drawLine(c, corner, androidx.compose.ui.geometry.Offset(hx, corner.y), w)
                            drawLine(c, corner, androidx.compose.ui.geometry.Offset(corner.x, vy), w)
                        }
                    }
                }

                // Indicador buscando
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

            // Back button
            IconButton(
                onClick = onCerrar,
                modifier = Modifier.statusBarsPadding().padding(4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
        }

        // ── Panel inferior ──
        Surface(
            modifier = Modifier.fillMaxWidth().weight(0.55f),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Tabs de modo (siempre visibles) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tab Anotar
                    val anotarSeleccionado = modo == ModoEscaner.ANOTAR
                    if (anotarSeleccionado) {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anotar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { modo = ModoEscaner.ANOTAR; yaDetecto.value = false },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anotar", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Tab Cargar nuevo
                    val cargarSeleccionado = modo == ModoEscaner.CARGAR_NUEVO
                    if (cargarSeleccionado) {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cargar nuevo", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (modo == ModoEscaner.ANOTAR && itemsAnotados.isNotEmpty()) {
                                    mostrarDialogoCambioModo = true
                                } else {
                                    modo = ModoEscaner.CARGAR_NUEVO
                                    yaDetecto.value = false
                                }
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cargar nuevo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                HorizontalDivider()

                // ── Contenido del modo ──
                when (modo) {

                // ── Modo Anotar ──
                ModoEscaner.ANOTAR -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Campo búsqueda arriba (teclado no lo tapa)
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                IconButton(
                                    onClick = { agregarALista(ItemAnotado(queryBusqueda.trim(), 0.0)) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Sugerencias de búsqueda — chips horizontales
                        if (sugerencias.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sugerencias) { prod ->
                                    ElevatedCard(
                                        onClick = { agregarALista(ItemAnotado(prod.nombre, prod.ultimoPrecio, prod.id)) },
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                prod.nombre,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                            if (prod.ultimoPrecio > 0) {
                                                Text(
                                                    "$${prod.ultimoPrecio.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider()
                        }

                        HorizontalDivider()

                        // Lista de items (scrolleable, ocupa el espacio restante)
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            if (itemsAnotados.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Escaneá un producto para empezar",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(itemsAnotados) { item ->
                                    ListItem(
                                        headlineContent = { Text(item.nombre, style = MaterialTheme.typography.bodyMedium) },
                                        supportingContent = {
                                            Text(
                                                "$${(item.precio * item.cantidad).toInt()}" +
                                                    if (item.cantidad > 1) "  ·  x${item.cantidad}" else "",
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
                                                ) {
                                                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                                                }
                                                Text("${item.cantidad}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                IconButton(
                                                    onClick = {
                                                        itemsAnotados = itemsAnotados.map { if (it === item) it.copy(cantidad = it.cantidad + 1) else it }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }

                        // Botón confirmar (fijo al fondo)
                        if (itemsAnotados.isNotEmpty()) {
                            val total = itemsAnotados.sumOf { it.precio * it.cantidad }
                            Button(
                                onClick = { mostrarConfirmar = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .navigationBarsPadding()
                                    .height(48.dp)
                            ) {
                                Text(
                                    "Elegir cliente  ·  $${total.toInt()}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(Modifier.navigationBarsPadding())
                        }
                    }
                }

                // ── Modo Cargar nuevo ──
                ModoEscaner.CARGAR_NUEVO -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val prod = productoParaCargar
                        if (prod == null) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Escaneá el código del producto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            if (prod.nombre.isBlank()) {
                                OutlinedTextField(
                                    value = nombreCarga,
                                    onValueChange = { nombreCarga = it },
                                    label = { Text("Nombre del producto") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                                )
                            } else {
                                Text(
                                    prod.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
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
                                    ) {
                                        productoParaCargar = null
                                        precioCarga = ""
                                        yaDetecto.value = false
                                    }
                                },
                                enabled = !cargandoVm && (prod.nombre.isNotBlank() || nombreCarga.isNotBlank()),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (cargandoVm) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("Guardar al local", fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { productoParaCargar = null; yaDetecto.value = false; precioCarga = ""; nombreCarga = "" },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Escanear otro") }
                        }

                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }

                ModoEscaner.NINGUNO -> {} // nunca ocurre
            } // end when modo
            } // end Column panel
        } // end Surface
    } // end Column root

    // ── Diálogo confirmación cambio de modo ──
    if (mostrarDialogoCambioModo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCambioModo = false },
            title = { Text("¿Cambiar de modo?") },
            text = { Text("Tenés ${itemsAnotados.size} producto${if (itemsAnotados.size != 1) "s" else ""} en la lista. Si cambiás a Cargar nuevo los perdés.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoCambioModo = false
                    itemsAnotados = emptyList()
                    modo = ModoEscaner.CARGAR_NUEVO
                    yaDetecto.value = false
                }) { Text("Sí, cambiar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCambioModo = false }) { Text("Cancelar") }
            }
        )
    }
}

