package com.kosking.app.explorar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VidreraPublicaViewModel : ViewModel() {
    val local = MutableStateFlow<Local?>(null)
    val cargando = MutableStateFlow(true)

    fun cargar(localId: String) {
        viewModelScope.launch {
            cargando.value = true
            local.value = FiadoRepository.obtenerLocal(localId)
            cargando.value = false
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
    val ofertas = productosHabilitados.filter { it.esOferta }
    val resto = productosHabilitados.filter { !it.esOferta }

    if (cargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
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

        // Sección ofertas
        if (ofertas.isNotEmpty()) {
            item {
                SeccionVidreraLabel(
                    texto = "Ofertas",
                    conteo = "${ofertas.size} disponible${if (ofertas.size != 1) "s" else ""}"
                )
            }
            val paresOfertas = ofertas.chunked(2)
            items(paresOfertas) { par ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    par.forEach { prod ->
                        TarjetaVidreraProducto(
                            producto = prod,
                            esOferta = true,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    if (par.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        // Sección productos regulares
        if (resto.isNotEmpty()) {
            item {
                SeccionVidreraLabel(
                    texto = "Precios",
                    conteo = "${resto.size} producto${if (resto.size != 1) "s" else ""}"
                )
            }
            val paresResto = resto.chunked(2)
            items(paresResto) { par ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    par.forEach { prod ->
                        TarjetaVidreraProducto(
                            producto = prod,
                            esOferta = false,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    if (par.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
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

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Imagen o ícono
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Badge oferta
            if (descuento > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "−$descuento%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            // Nombre
            Text(
                producto.nombre,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )

            // Descripción como subtítulo
            if (producto.descripcion.isNotBlank()) {
                Text(
                    producto.descripcion,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))

            // Precio
            Text(
                "$${"%.0f".format(producto.precio)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold
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
