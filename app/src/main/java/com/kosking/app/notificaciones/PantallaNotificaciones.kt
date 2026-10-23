package com.kosking.app.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ofertas.OfertaConTienda
import com.kosking.app.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun PantallaNotificaciones(navController: NavController, vm: NotificacionesViewModel = viewModel()) {
    val notificaciones by vm.notificaciones.collectAsStateWithLifecycle()
    val cargando by vm.cargando.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(AzulFondo)) {

        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderGradient)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .padding(bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notificaciones", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Blanco)
                    Text("Ofertas recientes en kioscos", fontSize = 12.sp, color = Blanco.copy(alpha = 0.65f))
                }
            }
        }

        when {
            cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Rojo)
                }
            }
            notificaciones.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, null, tint = Gris500, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Sin notificaciones", fontWeight = FontWeight.Bold, color = AzulNavy, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Cuando haya ofertas nuevas en los kioscos\naparecerán aquí.",
                            fontSize = 13.sp,
                            color = Gris500,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(notificaciones, key = { it.oferta.id }) { item ->
                        NotificacionCard(item) {
                            navController.navigate(Rutas.detalleLocal(item.tiendaId))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificacionCard(item: OfertaConTienda, onClick: () -> Unit) {
    val descuento = if (item.oferta.precioNormal > 0) {
        ((1 - item.oferta.precioOferta / item.oferta.precioNormal) * 100).toInt()
    } else 0

    val hace = item.oferta.creadoAt?.toDate()?.let { fecha ->
        val minutos = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - fecha.time)
        when {
            minutos < 60 -> "Hace ${minutos}min"
            minutos < 1440 -> "Hace ${minutos / 60}h"
            else -> "Hace ${minutos / 1440}d"
        }
    } ?: ""

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ícono
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RojoClaro,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalOffer, null, tint = Rojo, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.oferta.nombreProducto,
                    fontWeight = FontWeight.Bold,
                    color = AzulNavy,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(item.tiendaNombre, fontSize = 12.sp, color = Gris500)
                if (hace.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(hace, fontSize = 11.sp, color = Gris500)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$%.0f".format(item.oferta.precioOferta),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Rojo,
                )
                if (descuento > 0) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Verde.copy(alpha = 0.12f)) {
                        Text(
                            "-$descuento%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = Verde,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
