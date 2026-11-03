package com.kosking.app.explorar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.HorarioDia
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Producto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.Calendar

class VidreraPublicaViewModel : ViewModel() {
    val local = MutableStateFlow<Local?>(null)
    val cargando = MutableStateFlow(true)

    fun cargar(localId: String) {
        viewModelScope.launch {
            cargando.value = true
            val loc = FiadoRepository.obtenerLocal(localId)
            local.value = loc
            cargando.value = false
            // Registrar visita si hay sesión activa
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null && loc != null) {
                try { FiadoRepository.registrarVisitaLocal(uid, localId, loc.nombre) } catch (_: Exception) {}
            }
        }
    }

    fun productosFlow(localId: String): StateFlow<List<Producto>> =
        FiadoRepository.productosFlow(localId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

private val HeaderBg = Color(0xFF0F0F0F)
private val HeaderText = Color.White
private val HeaderMuted = Color.White.copy(alpha = 0.5f)
private val HeaderChipBg = Color.White.copy(alpha = 0.12f)

private val DIAS_KEYS = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
private val DIAS_NOMBRES = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
private val DIAS_COMPLETOS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

private fun diaHoyKey(): String {
    val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return when (dow) {
        Calendar.MONDAY -> "lunes"
        Calendar.TUESDAY -> "martes"
        Calendar.WEDNESDAY -> "miercoles"
        Calendar.THURSDAY -> "jueves"
        Calendar.FRIDAY -> "viernes"
        Calendar.SATURDAY -> "sabado"
        else -> "domingo"
    }
}

private fun parseMinutos(tiempo: String): Int? {
    val partes = tiempo.trim().split(":")
    if (partes.size != 2) return null
    val h = partes[0].toIntOrNull() ?: return null
    val m = partes[1].toIntOrNull() ?: return null
    return h * 60 + m
}

private fun calcularAbierto(horarios: Map<String, HorarioDia>, abiertoManual: Boolean): Boolean {
    val hoy = horarios[diaHoyKey()] ?: return abiertoManual
    if (!hoy.tieneCarga) return abiertoManual
    val ahora = Calendar.getInstance()
    val minActual = ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE)
    val minApertura = parseMinutos(hoy.apertura) ?: return abiertoManual
    val minCierre = parseMinutos(hoy.cierre) ?: return abiertoManual
    return minActual in minApertura until minCierre
}

private fun textoHorarioHoy(horarios: Map<String, HorarioDia>, abierto: Boolean): String {
    val hoy = horarios[diaHoyKey()]
    return if (hoy != null && hoy.tieneCarga) {
        if (abierto) "Abierto · Cierra ${hoy.cierre}"
        else "Cerrado · Abre ${hoy.apertura}"
    } else {
        if (abierto) "Abierto" else "Cerrado"
    }
}

@Composable
fun PantallaVidreraPublica(
    localId: String,
    onVolver: () -> Unit,
    vm: VidreraPublicaViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(localId) { vm.cargar(localId) }

    val local by vm.local.collectAsState()
    val cargando by vm.cargando.collectAsState()
    val productos by remember(localId) { vm.productosFlow(localId) }.collectAsState()

    var busqueda by remember { mutableStateOf("") }
    var mostrarHorarios by remember { mutableStateOf(false) }

    val productosHabilitados = productos.filter { it.habilitado }
    val ofertas = productosHabilitados.filter { it.esOferta && !it.agotado }

    // Agrupar por categoría: productos con categoría agrupados, sin categoría van a "Otros"
    // Los agotados se muestran al final de cada sección, griseados
    val secciones: List<Pair<String, List<Producto>>> = remember(productosHabilitados) {
        val sinOferta = productosHabilitados.filter { !it.esOferta }
        val porCategoria = sinOferta.groupBy { it.categoria.ifBlank { "Productos" } }
        // Ordenar: categorías con nombre primero (alfabético), "Productos" al final
        porCategoria.entries
            .sortedWith(compareBy { if (it.key == "Productos") "ZZZ" else it.key })
            .map { (cat, prods) ->
                cat to prods.sortedWith(compareBy({ it.agotado }, { it.nombre }))
            }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Calcular índices de cada sección en la LazyColumn para scroll
    // items: 0=header, 1=chips(si hay secciones>1), 2=ofertas_label(si hay), 3=ofertas_grid(si hay), luego secciones
    val indicesSecciones = remember(ofertas, secciones) {
        var idx = 1 // header = 0
        if (secciones.size > 1) idx++ // chips
        if (ofertas.isNotEmpty()) idx += 2 // label + grid
        secciones.mapIndexed { i, _ ->
            val base = idx + i * 2 // label + grid por sección
            base
        }
    }

    if (cargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header oscuro
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderBg)
            ) {
                // Imagen de fondo si existe
                if (local?.imagenUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = local!!.imagenUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .align(Alignment.TopCenter)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    // Botón volver
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = HeaderText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    val loc = local
                    if (loc != null) {
                        // Tipo del local
                        Text(
                            loc.tipo.uppercase(),
                            color = HeaderMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
                        )
                        Spacer(Modifier.height(4.dp))

                        // Nombre
                        Text(
                            loc.nombre,
                            color = HeaderText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Descripción
                        if (loc.descripcion.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                loc.descripcion,
                                color = HeaderMuted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Chips de estado
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Abierto calculado desde horarios; fallback al booleano manual
                            val estaAbierto = calcularAbierto(loc.horarios, loc.abierto)
                            val chipColor = if (estaAbierto) Color(0xFF4ADE80) else Color(0xFFFF6B6B)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipColor.copy(alpha = 0.15f))
                                    .clickable { mostrarHorarios = !mostrarHorarios }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(chipColor)
                                    )
                                    Text(
                                        textoHorarioHoy(loc.horarios, estaAbierto),
                                        color = chipColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (loc.horarios.isNotEmpty()) {
                                        Icon(
                                            if (mostrarHorarios) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            null,
                                            tint = chipColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Horarios semanales expandibles
                        if (mostrarHorarios && loc.horarios.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HeaderChipBg)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val hoyKey = diaHoyKey()
                                DIAS_KEYS.forEachIndexed { i, key ->
                                    val h = loc.horarios[key]
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            DIAS_COMPLETOS[i],
                                            color = if (key == hoyKey) Color(0xFF4ADE80) else HeaderMuted,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (key == hoyKey) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.width(80.dp)
                                        )
                                        Text(
                                            if (h != null && h.tieneCarga) "${h.apertura} – ${h.cierre}" else "Cerrado",
                                            color = if (key == hoyKey) HeaderText else HeaderMuted,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (key == hoyKey) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Teléfono / WhatsApp
                        if (loc.telefono.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val uri = Uri.parse("tel:${loc.telefono}")
                                        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HeaderText),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Llamar", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val numero = loc.telefono.filter { it.isDigit() }
                                        val uri = Uri.parse("https://wa.me/54$numero")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Dirección clickeable para abrir mapa
                        if (loc.direccion.isNotBlank() || (loc.lat != 0.0 && loc.lng != 0.0)) {
                            Spacer(Modifier.height(14.dp))
                            val tieneCoords = loc.lat != 0.0 && loc.lng != 0.0
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val uri = if (tieneCoords)
                                            Uri.parse("geo:${loc.lat},${loc.lng}?q=${loc.lat},${loc.lng}(${Uri.encode(loc.nombre)})")
                                        else
                                            Uri.parse("geo:0,0?q=${Uri.encode(loc.direccion)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn, null,
                                    tint = HeaderMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    loc.direccion.ifBlank { "Ver en el mapa" },
                                    color = HeaderText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Barra de búsqueda compacta
        item {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar producto...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (busqueda.isNotBlank()) {
                        IconButton(onClick = { busqueda = "" }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .heightIn(max = 46.dp),
                shape = RoundedCornerShape(28.dp)
            )
        }

        // Resultados de búsqueda
        if (busqueda.isNotBlank()) {
            val query = busqueda.trim()
            val resultados = productosHabilitados.filter {
                it.nombre.contains(query, ignoreCase = true) ||
                it.descripcion.contains(query, ignoreCase = true) ||
                it.categoria.contains(query, ignoreCase = true)
            }
            if (resultados.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay productos que coincidan con \"$query\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                item {
                    SeccionVidreraLabel(
                        texto = "Resultados (${resultados.size})"
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(resultados) { prod ->
                            TarjetaVidreraProducto(producto = prod, esOferta = prod.esOferta)
                        }
                    }
                }
            }
            return@LazyColumn
        }

        // Grid de categorías destacadas
        if (secciones.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Categorías",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    val todasCats = buildList {
                        if (ofertas.isNotEmpty()) add(Pair("Ofertas", -1))
                        secciones.forEachIndexed { i, (cat, _) -> add(Pair(cat, i)) }
                    }
                    val filas = todasCats.chunked(5)
                    filas.forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fila.forEach { (cat, idx) ->
                                val targetIdx = if (idx == -1) 2 else indicesSecciones[idx]
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { scope.launch { listState.animateScrollToItem(targetIdx) } }
                                        .padding(bottom = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (cat == "Ofertas") Icons.Default.LocalOffer
                                            else Icons.Default.ShoppingBag,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            repeat(5 - fila.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // Sección ofertas
        if (ofertas.isNotEmpty()) {
            item { SeccionVidreraLabel(texto = "Ofertas") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ofertas) { prod ->
                        TarjetaVidreraProducto(producto = prod, esOferta = true)
                    }
                }
            }
        }

        // Secciones agrupadas por categoría
        secciones.forEach { (categoria, prods) ->
            item { SeccionVidreraLabel(texto = categoria) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(prods) { prod ->
                        TarjetaVidreraProducto(producto = prod, esOferta = false)
                    }
                }
            }
        }

        // Vacío
        if (productosHabilitados.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.StoreMallDirectory, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Este local todavía no cargó productos",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun ChipHeader(texto: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(HeaderChipBg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(texto, color = HeaderMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SeccionVidreraLabel(texto: String, conteo: String = "", onMostrarTodos: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { onMostrarTodos?.invoke() }) {
            Text(
                "Mostrar todos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaVidreraProducto(
    producto: Producto,
    esOferta: Boolean
) {
    val descuento = if (esOferta && producto.precioAnterior > 0)
        (((producto.precioAnterior - producto.precio) / producto.precioAnterior) * 100).toInt()
    else 0
    val agotado = producto.agotado

    Card(
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (agotado) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (agotado) MaterialTheme.colorScheme.surfaceVariant
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = if (agotado) Modifier.alpha(0.5f) else Modifier) {
            // Imagen grande
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (producto.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = producto.imagenUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.ShoppingBag, null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                // Badge de descuento sobre la imagen
                if (!agotado && descuento > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFD600))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("$descuento% OFF", style = MaterialTheme.typography.labelSmall,
                            color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                if (agotado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Agotado", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    producto.nombre,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                    color = if (agotado) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "$${"%.0f".format(producto.precio)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (agotado) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )
                if (esOferta && producto.precioAnterior > 0) {
                    Text(
                        "$${"%.0f".format(producto.precioAnterior)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }
    }
}
