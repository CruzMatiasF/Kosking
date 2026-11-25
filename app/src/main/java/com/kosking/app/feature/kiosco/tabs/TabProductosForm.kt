package com.kosking.app.feature.kiosco.tabs

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kosking.app.datos.CatalogoRepository
import com.kosking.app.datos.ItemCatalogo
import com.kosking.app.modelo.Producto
import com.kosking.app.scanner.components.CamaraEscaner
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DialogProducto(
    producto: Producto?,
    categoriasExistentes: List<String>,
    cargando: Boolean,
    onBuscarGlobal: suspend (String) -> List<String> = { emptyList() },
    onGuardar: (nombre: String, precio: Double, categoria: String, imagenUri: Uri?, presentacion: String, codigoBarras: String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        PantallaProducto(
            producto = producto,
            categoriasExistentes = categoriasExistentes,
            cargando = cargando,
            onBuscarGlobal = onBuscarGlobal,
            onGuardar = onGuardar,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PantallaProducto(
    producto: Producto?,
    categoriasExistentes: List<String>,
    cargando: Boolean,
    onBuscarGlobal: suspend (String) -> List<String>,
    onGuardar: (nombre: String, precio: Double, categoria: String, imagenUri: Uri?, presentacion: String, codigoBarras: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val esEdicion = producto != null

    var nombre by remember(producto) { mutableStateOf(producto?.nombre ?: "") }
    var precioTexto by remember(producto) { mutableStateOf(
        if (producto != null && producto.ultimoPrecio > 0) "%.0f".format(producto.ultimoPrecio) else ""
    ) }
    var categoria by remember(producto) { mutableStateOf(producto?.categoria ?: "") }
    var presentacion by remember(producto) { mutableStateOf(producto?.presentacion ?: "") }
    var codigoBarras by remember(producto) { mutableStateOf(producto?.codigoBarras ?: "") }
    var busqueda by remember { mutableStateOf(producto?.nombre ?: "") }
    var mostrarSugerencias by remember { mutableStateOf(false) }
    var sugerencias by remember { mutableStateOf(emptyList<ItemCatalogo>()) }
    var mostrarNuevaCategoria by remember { mutableStateOf(false) }
    var nuevaCategoria by remember { mutableStateOf("") }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarOpcionesImagen by remember { mutableStateOf(false) }
    var mostrarEscanerCodigo by remember { mutableStateOf(false) }
    val yaDetectoBarcode = remember { AtomicBoolean(false) }
    var permisoCamera by remember { mutableStateOf(false) }
    val launcherPermisoCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permisoCamera = granted
        if (granted) mostrarEscanerCodigo = true
    }
    LaunchedEffect(Unit) {
        permisoCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val pickerGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imagenUri = uri
    }
    val pickerCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            // Guardamos el bitmap como archivo temporal y obtenemos su Uri
            val file = File(context.cacheDir, "producto_foto_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            imagenUri = Uri.fromFile(file)
        }
    }

    LaunchedEffect(busqueda) {
        if (busqueda.length < 2) { sugerencias = emptyList(); return@LaunchedEffect }
        delay(300)
        val delCatalogo = CatalogoRepository.buscar(context, busqueda)
        val nombresYaCubiertos = delCatalogo.map { it.nombre.lowercase() }.toSet()
        val delGlobal = try {
            onBuscarGlobal(busqueda)
                .filter { it.lowercase() !in nombresYaCubiertos }
                .map { ItemCatalogo(it, "") }
        } catch (e: Exception) { emptyList() }
        sugerencias = (delCatalogo + delGlobal).take(8)
    }

    val precioDouble = precioTexto.toDoubleOrNull() ?: 0.0
    val puedeGuardar = nombre.isNotBlank() && !cargando

    val precioOriginal = if (producto != null && producto.ultimoPrecio > 0) "%.0f".format(producto.ultimoPrecio) else ""
    val hayCambios = nombre != (producto?.nombre ?: "") ||
        precioTexto != precioOriginal ||
        categoria != (producto?.categoria ?: "") ||
        presentacion != (producto?.presentacion ?: "") ||
        codigoBarras != (producto?.codigoBarras ?: "") ||
        imagenUri != null

    var mostrarDialogDescartar by remember { mutableStateOf(false) }

    BackHandler(enabled = hayCambios) { mostrarDialogDescartar = true }

    if (mostrarDialogDescartar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogDescartar = false },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Tenés cambios sin guardar.") },
            confirmButton = {
                Button(onClick = { onGuardar(nombre.trim(), precioDouble, categoria, imagenUri, presentacion.trim(), codigoBarras.trim()) }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogDescartar = false; onDismiss() }) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (mostrarOpcionesImagen) {
        AlertDialog(
            onDismissRequest = { mostrarOpcionesImagen = false },
            title = { Text("Imagen del producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListItem(
                        headlineContent = { Text("Elegir de galería") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                        modifier = Modifier.clickable { mostrarOpcionesImagen = false; pickerGaleria.launch("image/*") }
                    )
                    ListItem(
                        headlineContent = { Text("Tomar foto") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable { mostrarOpcionesImagen = false; pickerCamara.launch(null) }
                    )
                    if (imagenUri != null || producto?.imagenUrl?.isNotBlank() == true) {
                        ListItem(
                            headlineContent = { Text("Quitar imagen", color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable { mostrarOpcionesImagen = false; imagenUri = Uri.EMPTY }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarOpcionesImagen = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) "Editar producto" else "Nuevo producto") },
                navigationIcon = {
                    IconButton(onClick = { if (hayCambios) mostrarDialogDescartar = true else onDismiss() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancelar")
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Button(
                    onClick = { onGuardar(nombre.trim(), precioDouble, categoria, imagenUri, presentacion.trim(), codigoBarras.trim()) },
                    enabled = puedeGuardar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (cargando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Imagen ───────────────────────────────────────────────────
            val imagenActual = imagenUri
            val urlExistente = producto?.imagenUrl?.takeIf { it.isNotBlank() }
            val mostrarImagen = imagenActual != null && imagenActual != Uri.EMPTY
            val mostrarUrlExistente = !mostrarImagen && imagenActual != Uri.EMPTY && urlExistente != null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .clickable { mostrarOpcionesImagen = true },
                contentAlignment = Alignment.Center
            ) {
                when {
                    mostrarImagen -> AsyncImage(
                        model = ImageRequest.Builder(context).data(imagenActual).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    mostrarUrlExistente -> AsyncImage(
                        model = ImageRequest.Builder(context).data(urlExistente).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Agregar foto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (mostrarImagen || mostrarUrlExistente) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .size(28.dp).clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, "Cambiar foto", Modifier.size(14.dp))
                    }
                }
            }

            // ── Nombre ───────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                OutlinedTextField(
                    value = if (mostrarSugerencias) busqueda else nombre,
                    onValueChange = { v ->
                        busqueda = v
                        nombre = v
                        mostrarSugerencias = v.isNotBlank()
                    },
                    label = { Text("Nombre del producto") },
                    placeholder = { Text("Buscá o escribí el nombre") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (nombre.isNotBlank()) {
                            IconButton(onClick = { nombre = ""; busqueda = ""; mostrarSugerencias = false }) {
                                Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = mostrarSugerencias && sugerencias.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                    ) {
                        sugerencias.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        nombre = item.nombre
                                        busqueda = item.nombre
                                        if (item.categoria.isNotBlank()) categoria = item.categoria
                                        mostrarSugerencias = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(
                                    if (item.categoria.isNotBlank()) item.categoria else "otros locales",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.categoria.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            // ── Precio ───────────────────────────────────────────────────
            OutlinedTextField(
                value = precioTexto,
                onValueChange = { precioTexto = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Precio $") },
                placeholder = { Text("Ej: 500") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Presentación ─────────────────────────────────────────────
            OutlinedTextField(
                value = presentacion,
                onValueChange = { presentacion = it },
                label = { Text("Presentación") },
                placeholder = { Text("Ej: 500ml, 1L, 100gr, 1kg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Código de barras ─────────────────────────────────────────
            OutlinedTextField(
                value = codigoBarras,
                onValueChange = { codigoBarras = it },
                label = { Text("Código de barras") },
                placeholder = { Text("Escaneá o escribí el código") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        yaDetectoBarcode.set(false)
                        if (permisoCamera) mostrarEscanerCodigo = true
                        else launcherPermisoCamera.launch(Manifest.permission.CAMERA)
                    }) {
                        Icon(Icons.Default.QrCode, "Escanear código")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (mostrarEscanerCodigo) {
                Dialog(
                    onDismissRequest = { mostrarEscanerCodigo = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.Black, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        CamaraEscaner(
                            onCodigo = { code ->
                                codigoBarras = code
                                yaDetectoBarcode.set(true)
                                mostrarEscanerCodigo = false
                            },
                            isYaDetecto = { yaDetectoBarcode.get() },
                            buscandoProducto = false,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { mostrarEscanerCodigo = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        ) {
                            Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                        }
                        Text(
                            "Apuntá al código de barras o QR",
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // ── Categoría ────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Categoría", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (categoria.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(categoria) },
                        leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close, "Quitar",
                                Modifier.size(14.dp).clickable {
                                    categoria = ""; nuevaCategoria = ""; mostrarNuevaCategoria = false
                                }
                            )
                        }
                    )
                }
                val predeterminadas = listOf("Gaseosas", "Snacks", "Lácteos", "Fiambre", "Cigarrillos", "Panificados", "Almacén", "Limpieza", "Otros")
                val todasDisponibles = (predeterminadas + categoriasExistentes)
                    .distinct()
                    .filter { it != categoria }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(todasDisponibles) { cat ->
                        SuggestionChip(
                            onClick = { categoria = cat; nuevaCategoria = ""; mostrarNuevaCategoria = false },
                            label = { Text(cat, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
                AnimatedVisibility(visible = !mostrarNuevaCategoria) {
                    SuggestionChip(onClick = { mostrarNuevaCategoria = true }, label = { Text("+ Nueva categoría") })
                }
                AnimatedVisibility(visible = mostrarNuevaCategoria) {
                    OutlinedTextField(
                        value = nuevaCategoria,
                        onValueChange = { nuevaCategoria = it },
                        label = { Text("Nueva categoría") },
                        placeholder = { Text("Ej: Bebidas alcohólicas...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        trailingIcon = {
                            Row {
                                if (nuevaCategoria.isNotBlank()) {
                                    IconButton(onClick = {
                                        categoria = nuevaCategoria.trim().replaceFirstChar { it.uppercaseChar() }
                                        mostrarNuevaCategoria = false; nuevaCategoria = ""
                                    }) { Icon(Icons.Default.Check, "Confirmar", tint = MaterialTheme.colorScheme.primary) }
                                }
                                IconButton(onClick = { mostrarNuevaCategoria = false; nuevaCategoria = "" }) {
                                    Icon(Icons.Default.Close, "Cancelar")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
