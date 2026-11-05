package com.kosking.app.explorar

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Producto
import kotlinx.coroutines.delay

@Composable
internal fun TabInicio(
    locales: List<Local>,
    todosLocales: List<Local>,
    productosFiltrados: List<Producto>,
    productosDestacados: List<Producto>,
    busqueda: String,
    localesGuardados: Boolean,
    hayMasProductos: Boolean = false,
    cargando: Boolean = false,
    onVerLocal: (String) -> Unit,
    onVerMisLocales: () -> Unit,
    onVerOfertas: () -> Unit = {},
    onCargarMas: () -> Unit = {},
    isRefrescando: Boolean = false,
    onRefrescar: () -> Unit = {}
) {
    var tipoFiltro by remember { mutableStateOf("Todos") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(listState) {
        var ultimoEstado = false
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 4 && info.totalItemsCount > 0
        }.collect { cercaDelFinal ->
            if (cercaDelFinal && !ultimoEstado) onCargarMas()
            ultimoEstado = cercaDelFinal
        }
    }

    LaunchedEffect(busqueda) {
        if (busqueda.isNotBlank()) tipoFiltro = "Todos"
    }

    val localMap = remember(todosLocales) { todosLocales.associateBy { it.id } }

    val localesFiltrados = remember(locales, tipoFiltro) {
        if (tipoFiltro == "Todos") locales else locales.filter { it.tipo == tipoFiltro }
    }

    val productosPorTipo = remember(productosFiltrados, tipoFiltro, todosLocales) {
        val base = productosFiltrados
        if (tipoFiltro == "Todos") base
        else base.filter { prod -> localMap[prod.localId]?.tipo == tipoFiltro }
    }

    if (cargando) {
        SkeletonInicio()
        return
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefrescando,
        onRefresh = onRefrescar,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

            if (busqueda.isNotBlank()) {
                if (locales.isNotEmpty()) {
                    item {
                        SeccionLabel(
                            texto = "Locales",
                            conteo = "${locales.size} encontrado${if (locales.size != 1) "s" else ""}"
                        )
                    }
                    items(locales, key = { it.id }) { local ->
                        FilaLocalPlana(local = local, onClick = { onVerLocal(local.id) })
                    }
                }

                if (productosFiltrados.isNotEmpty()) {
                    item {
                        SeccionLabel(
                            texto = "Productos",
                            conteo = "${productosFiltrados.size} resultado${if (productosFiltrados.size != 1) "s" else ""}"
                        )
                    }
                    val paresBusqueda = productosFiltrados.chunked(2)
                    items(paresBusqueda) { par ->
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            par.forEach { prod ->
                                TarjetaProducto(
                                    producto = prod,
                                    nombreLocal = localMap[prod.localId]?.nombre ?: "",
                                    onClick = { localMap[prod.localId]?.id?.let { onVerLocal(it) } },
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            if (par.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                if (productosFiltrados.isEmpty() && locales.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("Sin resultados para \"$busqueda\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

            } else {
                if (localesGuardados) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            onClick = onVerMisLocales
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Ver mi fiado", style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold)
                                    Text("Tocá para ver lo que debés", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    val slides = listOf(
                        Triple("Locales cerca tuyo", "Mirá qué kioscos y almacenes están abiertos ahora mismo.", Icons.Default.Storefront),
                        Triple("Poné tu kiosco", "Registrá tu local gratis y llegá a más clientes del barrio.", Icons.Default.AddBusiness),
                        Triple("Fiate con confianza", "Anotá lo que llevás y pagá cuando puedas con tu kiosquero.", Icons.Default.Handshake)
                    )
                    val pagerState = rememberPagerState(pageCount = { slides.size })
                    LaunchedEffect(pagerState) {
                        while (true) {
                            delay(3500)
                            val next = (pagerState.currentPage + 1) % slides.size
                            pagerState.animateScrollToPage(next)
                        }
                    }
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        HorizontalPager(state = pagerState) { page ->
                            val (titulo, subtitulo, icono) = slides[page]
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F))) {
                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                                    Icon(icono, null,
                                        modifier = Modifier.size(72.dp).align(Alignment.CenterEnd),
                                        tint = Color.White.copy(alpha = 0.07f))
                                    Column {
                                        Text("KOSKING", style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold,
                                            letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp))
                                        Spacer(Modifier.height(6.dp))
                                        Text(titulo, style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(Modifier.height(6.dp))
                                        Text(subtitulo, style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically) {
                            repeat(slides.size) { i ->
                                Box(
                                    modifier = Modifier.padding(horizontal = 3.dp)
                                        .size(if (pagerState.currentPage == i) 8.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == i) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                        )
                                )
                            }
                        }
                    }
                }

                item {
                    val tiposConIcono = listOf(
                        Triple("Todos",       Icons.Default.Apps,              MaterialTheme.colorScheme.primaryContainer),
                        Triple("Café",        Icons.Default.LocalCafe,         MaterialTheme.colorScheme.tertiaryContainer),
                        Triple("Carnicería",  Icons.Default.SetMeal,           MaterialTheme.colorScheme.errorContainer),
                        Triple("Heladería",   Icons.Default.Icecream,          MaterialTheme.colorScheme.secondaryContainer),
                        Triple("Veterinaria", Icons.Default.Pets,              MaterialTheme.colorScheme.primaryContainer),
                        Triple("Librería",    Icons.Default.MenuBook,          MaterialTheme.colorScheme.tertiaryContainer),
                        Triple("Farmacia",    Icons.Default.LocalPharmacy,     MaterialTheme.colorScheme.errorContainer),
                        Triple("Corralón",    Icons.Default.HomeRepairService, MaterialTheme.colorScheme.secondaryContainer),
                        Triple("Taller",      Icons.Default.Build,             MaterialTheme.colorScheme.surfaceVariant),
                        Triple("Gomería",     Icons.Default.DirectionsCar,     MaterialTheme.colorScheme.surfaceVariant),
                        Triple("Repuestos",   Icons.Default.Construction,      MaterialTheme.colorScheme.surfaceVariant),
                        Triple("Otro",        Icons.Default.Category,          MaterialTheme.colorScheme.surfaceVariant),
                    )
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(tiposConIcono) { (tipo, icono, color) ->
                            val sel = tipo == tipoFiltro
                            Column(modifier = Modifier.width(72.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                                        .background(if (sel) MaterialTheme.colorScheme.primary else color)
                                        .clickable { tipoFiltro = tipo },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icono, null, modifier = Modifier.size(28.dp),
                                        tint = if (sel) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(tipo, style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }

                if (localesFiltrados.isNotEmpty()) {
                    item {
                        SeccionLabel(
                            texto = "Locales",
                            conteo = "${localesFiltrados.size} registrado${if (localesFiltrados.size != 1) "s" else ""}"
                        )
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(localesFiltrados, key = { it.id }) { local ->
                                TarjetaLocalH(local = local, onClick = { onVerLocal(local.id) })
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (productosPorTipo.isNotEmpty()) {
                    item {
                        SeccionLabel(
                            texto = "Productos",
                            conteo = "${productosPorTipo.size} publicado${if (productosPorTipo.size != 1) "s" else ""}"
                        )
                    }
                    val paresProductos = productosPorTipo.chunked(2)
                    items(paresProductos) { par ->
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            par.forEach { prod ->
                                TarjetaProducto(
                                    producto = prod,
                                    nombreLocal = localMap[prod.localId]?.nombre ?: "",
                                    onClick = { localMap[prod.localId]?.id?.let { onVerLocal(it) } },
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            if (par.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                if (hayMasProductos && productosPorTipo.isNotEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }

                if (localesFiltrados.isEmpty() && productosPorTipo.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.StoreMallDirectory, null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(12.dp))
                                Text("No hay publicaciones todavía",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonInicio() {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.25f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    @Composable
    fun SkBox(modifier: Modifier) {
        Box(modifier.clip(RoundedCornerShape(8.dp)).background(skeletonColor))
    }

    Column(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { SkBox(Modifier.width(72.dp).height(32.dp)) }
        }
        SkBox(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp).width(80.dp).height(12.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { SkBox(Modifier.width(130.dp).height(130.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        SkBox(Modifier.padding(start = 16.dp, bottom = 8.dp).width(100.dp).height(12.dp))
        repeat(2) {
            Row(modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) { SkBox(Modifier.weight(1f).height(160.dp)) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TarjetaLocalH(local: Local, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(130.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (local.imagenUrl.isNotBlank()) {
                    AsyncImage(model = local.imagenUrl, contentDescription = local.nombre,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(iconParaTipo(local.tipo), null, modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(local.nombre, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(local.tipo, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .then(if (local.abierto) Modifier.background(MaterialTheme.colorScheme.onSurface)
                          else Modifier.background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(if (local.abierto) "Abierto" else "Cerrado",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (local.abierto) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FilaLocalPlana(local: Local, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconParaTipo(local.tipo), null, modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(local.nombre, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(local.tipo, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .then(if (local.abierto) Modifier.background(MaterialTheme.colorScheme.onSurface)
                          else Modifier.background(MaterialTheme.colorScheme.surfaceVariant))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(if (local.abierto) "Abierto" else "Cerrado",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (local.abierto) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
    }
}
