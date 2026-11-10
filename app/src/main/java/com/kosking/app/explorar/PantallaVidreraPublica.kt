package com.kosking.app.explorar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kosking.app.modelo.HorarioDia
import com.kosking.app.modelo.Producto
import com.kosking.app.modelo.Publicacion
import java.util.Calendar
import java.util.concurrent.TimeUnit

private val HeaderBg = Color(0xFF0F0F0F)
private val HeaderText = Color.White
private val HeaderMuted = Color.White.copy(alpha = 0.5f)
private val HeaderChipBg = Color.White.copy(alpha = 0.12f)

private val DIAS_KEYS = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
private val DIAS_COMPLETOS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

private fun diaHoyKey(): String {
    return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
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
        if (abierto) "Abierto · Cierra ${hoy.cierre}" else "Cerrado · Abre ${hoy.apertura}"
    } else {
        if (abierto) "Abierto" else "Cerrado"
    }
}

private fun tiempoRelativo(pub: Publicacion): String {
    val ahoraMs = System.currentTimeMillis()
    val pubMs = pub.fecha.toDate().time
    val diffMs = ahoraMs - pubMs
    val minutos = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val horas = TimeUnit.MILLISECONDS.toHours(diffMs)
    val dias = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutos < 2 -> "Ahora"
        minutos < 60 -> "Hace $minutos min"
        horas < 24 -> "Hace $horas h"
        dias == 1L -> "Ayer"
        dias < 7 -> "Hace $dias días"
        else -> {
            val cal = Calendar.getInstance().apply { timeInMillis = pubMs }
            "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}"
        }
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
    val publicaciones by remember(localId) { vm.publicacionesFlow(localId) }.collectAsState()
    val productosPublicos by remember(localId) { vm.productosPublicosFlow(localId) }.collectAsState()

    var mostrarHorarios by remember { mutableStateOf(false) }

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

                Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp)) {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = HeaderText, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    val loc = local
                    if (loc != null) {
                        Text(loc.tipo.uppercase(), color = HeaderMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(loc.nombre, color = HeaderText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

                        if (loc.descripcion.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(loc.descripcion, color = HeaderMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val estaAbierto = calcularAbierto(loc.horarios, loc.abierto)
                            val chipColor = if (estaAbierto) Color(0xFF4ADE80) else Color(0xFFFF6B6B)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipColor.copy(alpha = 0.15f))
                                    .clickable { mostrarHorarios = !mostrarHorarios }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(chipColor))
                                    Text(textoHorarioHoy(loc.horarios, estaAbierto), color = chipColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                    if (loc.horarios.isNotEmpty()) {
                                        Icon(if (mostrarHorarios) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = chipColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        if (mostrarHorarios && loc.horarios.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Column(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(HeaderChipBg).padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val hoyKey = diaHoyKey()
                                DIAS_KEYS.forEachIndexed { i, key ->
                                    val h = loc.horarios[key]
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(DIAS_COMPLETOS[i], color = if (key == hoyKey) Color(0xFF4ADE80) else HeaderMuted, style = MaterialTheme.typography.labelSmall, fontWeight = if (key == hoyKey) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(80.dp))
                                        Text(if (h != null && h.tieneCarga) "${h.apertura} – ${h.cierre}" else "Cerrado", color = if (key == hoyKey) HeaderText else HeaderMuted, style = MaterialTheme.typography.labelSmall, fontWeight = if (key == hoyKey) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }

                        if (loc.telefono.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${loc.telefono}"))) },
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
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/54$numero")))
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

                        if (loc.direccion.isNotBlank() || (loc.lat != 0.0 && loc.lng != 0.0)) {
                            Spacer(Modifier.height(14.dp))
                            val tieneCoords = loc.lat != 0.0 && loc.lng != 0.0
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                    val uri = if (tieneCoords) Uri.parse("geo:${loc.lat},${loc.lng}?q=${loc.lat},${loc.lng}(${Uri.encode(loc.nombre)})")
                                    else Uri.parse("geo:0,0?q=${Uri.encode(loc.direccion)}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = HeaderMuted, modifier = Modifier.size(16.dp))
                                Text(loc.direccion.ifBlank { "Ver en el mapa" }, color = HeaderText, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Sección precios públicos
        if (productosPublicos.isNotEmpty()) {
            item {
                Text(
                    "Precios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
            val porCategoria = productosPublicos
                .sortedBy { it.nombre }
                .groupBy { it.categoria.ifBlank { "General" } }
                .toSortedMap()
            porCategoria.forEach { (categoria, items) ->
                item(key = "pcat_$categoria") {
                    Text(
                        categoria,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                items(items, key = { "pp_${it.id}" }) { p ->
                    TarjetaPrecioPublico(p)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // Título sección publicaciones
        item {
            Text(
                "Publicaciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        // Feed de publicaciones
        if (publicaciones.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Campaign, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Text("Este local todavía no publicó nada", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(publicaciones, key = { it.id }) { pub ->
                TarjetaPublicacion(pub = pub)
            }
        }

        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun TarjetaPrecioPublico(producto: Producto) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(producto.nombre, fontWeight = FontWeight.Medium)
                if (producto.esOferta) {
                    Surface(shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer) {
                        Text("OFERTA", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
        },
        supportingContent = if (producto.descripcion.isNotBlank()) ({
            Text(producto.descripcion, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }) else null,
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${"%.0f".format(producto.precio)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (producto.esOferta) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )
                if (producto.esOferta && producto.precioAnterior > 0) {
                    Text(
                        "$${"%.0f".format(producto.precioAnterior)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )
                }
            }
        }
    )
}

@Composable
private fun TarjetaPublicacion(pub: Publicacion) {
    val (etiquetaTexto, etiquetaColor) = when (pub.etiqueta) {
        "promo" -> "Promo" to MaterialTheme.colorScheme.tertiary
        "aviso" -> "Aviso" to MaterialTheme.colorScheme.primary
        else -> "Novedad" to MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            if (pub.imagenUrl.isNotBlank()) {
                AsyncImage(
                    model = pub.imagenUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        pub.texto,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = etiquetaColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            etiquetaTexto,
                            color = etiquetaColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(tiempoRelativo(pub), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
