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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Producto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

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

    val productosHabilitados = productos.filter { it.habilitado }
    val ofertas = productosHabilitados.filter { it.esOferta && !it.agotado }

    // Agrupar por categoría: productos con categoría agrupados, sin categoría van a "Otros"
    // Los agotados se muestran al final de cada sección, griseados
    val secciones: List<Pair<String, List<Producto>>> = remember(productosHabilitados) {
        val sinOferta = productosHabilitados.filter { !it.esOferta }
        val porCategoria = sinOferta.groupBy { it.categoria.ifBlank { "Otros" } }
        // Ordenar: categorías con nombre primero (alfabético), "Otros" al final
        porCategoria.entries
            .sortedWith(compareBy { if (it.key == "Otros") "ZZZ" else it.key })
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
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(HeaderChipBg)
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
                                            .background(
                                                if (loc.abierto) Color(0xFF4ADE80)
                                                else Color(0xFFFF6B6B)
                                            )
                                    )
                                    Text(
                                        if (loc.abierto) "Abierto" else "Cerrado",
                                        color = HeaderText,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            if (productosHabilitados.isNotEmpty()) {
                                ChipHeader("${productosHabilitados.size} producto${if (productosHabilitados.size != 1) "s" else ""}")
                            }
                            if (ofertas.isNotEmpty()) {
                                ChipHeader("${ofertas.size} oferta${if (ofertas.size != 1) "s" else ""}")
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

                        // Dirección + botón mapa
                        if (loc.direccion.isNotBlank() || (loc.lat != 0.0 && loc.lng != 0.0)) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Ícono de ubicación
                                Icon(
                                    Icons.Default.LocationOn, null,
                                    tint = HeaderMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                // Texto de dirección
                                Text(
                                    loc.direccion.ifBlank { "Ver en el mapa" },
                                    color = HeaderText,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Botón "Ver en mapa"
                                val tieneCoords = loc.lat != 0.0 && loc.lng != 0.0
                                OutlinedButton(
                                    onClick = {
                                        val uri = if (tieneCoords)
                                            Uri.parse("geo:${loc.lat},${loc.lng}?q=${loc.lat},${loc.lng}(${Uri.encode(loc.nombre)})")
                                        else
                                            Uri.parse("geo:0,0?q=${Uri.encode(loc.direccion)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = HeaderText
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, Color.White.copy(alpha = 0.25f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Map, null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Mapa",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Chips de acceso rápido (solo si hay más de 1 sección)
        if (secciones.size > 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ofertas.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { scope.launch { listState.animateScrollToItem(if (secciones.size > 1) 2 else 1) } },
                                label = { Text("Ofertas") },
                                leadingIcon = { Icon(Icons.Default.LocalOffer, null, Modifier.size(14.dp)) }
                            )
                        }
                    }
                    items(secciones.size) { i ->
                        val (cat, _) = secciones[i]
                        FilterChip(
                            selected = false,
                            onClick = { scope.launch { listState.animateScrollToItem(indicesSecciones[i]) } },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        // Sección ofertas
        if (ofertas.isNotEmpty()) {
            item {
                SeccionVidreraLabel(
                    texto = "Ofertas",
                    conteo = "${ofertas.size} disponible${if (ofertas.size != 1) "s" else ""}"
                )
            }
            item {
                val tresColumnasOfertas = ofertas.chunked(3)
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tresColumnasOfertas.forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fila.forEach { prod ->
                                TarjetaVidreraProducto(
                                    producto = prod,
                                    esOferta = true,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        // Secciones agrupadas por categoría
        secciones.forEach { (categoria, prods) ->
            item {
                SeccionVidreraLabel(
                    texto = categoria,
                    conteo = "${prods.count { !it.agotado }} producto${if (prods.count { !it.agotado } != 1) "s" else ""}"
                )
            }
            item {
                val filas = prods.chunked(3)
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filas.forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fila.forEach { prod ->
                                TarjetaVidreraProducto(
                                    producto = prod,
                                    esOferta = false,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
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
private fun SeccionVidreraLabel(texto: String, conteo: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            texto.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (conteo.isNotBlank()) {
            Text(
                conteo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaVidreraProducto(
    producto: Producto,
    esOferta: Boolean,
    modifier: Modifier = Modifier
) {
    val descuento = if (esOferta && producto.precioAnterior > 0)
        (((producto.precioAnterior - producto.precio) / producto.precioAnterior) * 100).toInt()
    else 0
    val agotado = producto.agotado

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (agotado) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (agotado) MaterialTheme.colorScheme.surfaceVariant
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(10.dp).then(if (agotado) Modifier.alpha(0.5f) else Modifier)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (agotado) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text("Agotado", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(3.dp))
            } else if (descuento > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text("−$descuento%", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(3.dp))
            }

            Text(
                producto.nombre,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.labelMedium.lineHeight,
                color = if (agotado) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
            )

            if (producto.descripcion.isNotBlank()) {
                Text(
                    producto.descripcion,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "$${"%.0f".format(producto.precio)}",
                style = MaterialTheme.typography.bodySmall,
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
