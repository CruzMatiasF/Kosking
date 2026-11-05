package com.kosking.app.explorar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Producto

private data class CategoriaProducto(
    val nombre: String,
    val icono: ImageVector,
    val palabrasClave: List<String>
)

private val CATEGORIAS_PRODUCTOS = listOf(
    CategoriaProducto("Gaseosas", Icons.Default.LocalDrink, listOf("coca", "pepsi", "sprite", "fanta", "7up", "manaos", "gaseosa", "soda", "torino", "paso de los toros")),
    CategoriaProducto("Agua", Icons.Default.WaterDrop, listOf("agua", "villavicencio", "ser ", "eco de los andes")),
    CategoriaProducto("Snacks", Icons.Default.SetMeal, listOf("papas", "chips", "maní", "palitos", "nachos", "pringles", "snack", "cheetos")),
    CategoriaProducto("Galletitas", Icons.Default.Cookie, listOf("galletita", "oreo", "cofler", "bagley", "terrabusi", "criollitas", "surtido")),
    CategoriaProducto("Lácteos", Icons.Default.Egg, listOf("leche", "yogur", "manteca", "crema", "queso", "lácteo", "lacteo")),
    CategoriaProducto("Energéticas", Icons.Default.Bolt, listOf("monster", "red bull", "energizante", "speed", "burn", "volt")),
    CategoriaProducto("Cigarrillos", Icons.Default.SmokingRooms, listOf("cigarrillo", "marlboro", "camel", "derby", "particulares", "jockey")),
    CategoriaProducto("Dulces", Icons.Default.Cake, listOf("dulce", "chocolate", "alfajor", "caramelo", "gomita", "chicle", "turron", "oblea")),
    CategoriaProducto("Bebidas", Icons.Default.SportsBar, listOf("cerveza", "vino", "sidra", "fernet", "whisky", "vodka", "heineken", "corona")),
    CategoriaProducto("Frutas y verduras", Icons.Default.Eco, listOf("fruta", "verdura", "manzana", "naranja", "papa ", "tomate", "lechuga", "banana", "limón", "uva")),
    CategoriaProducto("Panificados", Icons.Default.BakeryDining, listOf("pan ", "medialunas", "tostadas", "pan lactal", "baguette")),
    CategoriaProducto("Limpieza", Icons.Default.CleaningServices, listOf("detergente", "lavandina", "jabón", "limpiador", "desengrasante", "suavizante")),
)

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun TabTiendas(
    todosLocales: List<Local>,
    todosProductos: List<Producto>,
    onVerLocal: (String) -> Unit,
    isRefrescando: Boolean = false,
    onRefrescar: () -> Unit = {}
) {
    var categoriaSeleccionada by remember { mutableStateOf<CategoriaProducto?>(null) }
    val localMap = remember(todosLocales) { todosLocales.associateBy { it.id } }

    val localesConProductosPorCategoria = remember(todosProductos, todosLocales) {
        CATEGORIAS_PRODUCTOS.associate { cat ->
            cat.nombre to todosLocales.filter { local ->
                todosProductos.any { prod ->
                    prod.localId == local.id &&
                    cat.palabrasClave.any { kw -> prod.nombre.contains(kw, ignoreCase = true) }
                }
            }
        }
    }

    val localesFiltrados = remember(categoriaSeleccionada, todosLocales, localesConProductosPorCategoria) {
        if (categoriaSeleccionada == null) todosLocales
        else localesConProductosPorCategoria[categoriaSeleccionada!!.nombre] ?: emptyList()
    }

    val productosFiltrados = remember(categoriaSeleccionada, todosProductos) {
        if (categoriaSeleccionada == null) todosProductos
        else todosProductos.filter { prod ->
            categoriaSeleccionada!!.palabrasClave.any { kw -> prod.nombre.contains(kw, ignoreCase = true) }
        }
    }

    val localesAbiertos = remember(localesFiltrados) { localesFiltrados.filter { estaAbiertoAhora(it) } }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefrescando,
        onRefresh = onRefrescar,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = categoriaSeleccionada == null,
                            onClick = { categoriaSeleccionada = null },
                            label = { Text("Todos", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Apps, null, modifier = Modifier.size(20.dp)) }
                        )
                    }
                    items(CATEGORIAS_PRODUCTOS) { cat ->
                        FilterChip(
                            selected = categoriaSeleccionada == cat,
                            onClick = { categoriaSeleccionada = if (categoriaSeleccionada == cat) null else cat },
                            label = { Text(cat.nombre, style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(cat.icono, null, modifier = Modifier.size(20.dp)) }
                        )
                    }
                }
            }

            if (localesAbiertos.isNotEmpty()) {
                item {
                    Column {
                        Text("Abiertos ahora", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(localesAbiertos, key = { "abierto_${it.id}" }) { local ->
                                TarjetaLocalDestacado(local = local, onClick = { onVerLocal(local.id) })
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "${localesFiltrados.size} local${if (localesFiltrados.size != 1) "es" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (localesFiltrados.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.StoreMallDirectory, null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant)
                            Text("No hay locales en esta categoría",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(localesFiltrados, key = { it.id }) { local ->
                    TarjetaLocalTiendas(local = local, onClick = { onVerLocal(local.id) })
                }
            }

            if (productosFiltrados.isNotEmpty()) {
                item {
                    SeccionLabel(
                        texto = "Productos",
                        conteo = "${productosFiltrados.size} publicado${if (productosFiltrados.size != 1) "s" else ""}"
                    )
                }
                val pares = productosFiltrados.chunked(2)
                items(pares) { par ->
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

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TarjetaLocalDestacado(local: Local, onClick: () -> Unit) {
    Card(modifier = Modifier.width(140.dp), shape = RoundedCornerShape(16.dp), onClick = onClick) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (local.imagenUrl.isNotBlank()) {
                    AsyncImage(model = local.imagenUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(iconParaTipo(local.tipo), null, modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(local.nombre, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TarjetaLocalTiendas(local: Local, onClick: () -> Unit) {
    val abierto = estaAbiertoAhora(local)
    val horarioTexto = textoHorarioHoy(local)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (local.imagenUrl.isNotBlank()) {
                    AsyncImage(model = local.imagenUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(iconParaTipo(local.tipo), null, modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(local.nombre, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(local.tipo, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (local.direccion.isNotBlank()) {
                    Text(local.direccion, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (horarioTexto.isNotBlank()) {
                    Text(horarioTexto, style = MaterialTheme.typography.labelSmall,
                        color = if (abierto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(
                        if (abierto) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(if (abierto) "Abierto" else "Cerrado",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (abierto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }
}
