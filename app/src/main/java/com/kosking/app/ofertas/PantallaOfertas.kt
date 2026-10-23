package com.kosking.app.ofertas

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

@Composable
fun PantallaOfertas(navController: NavController, vm: OfertasViewModel = viewModel()) {
    val ofertas by vm.ofertas.collectAsStateWithLifecycle()
    val cargando by vm.cargando.collectAsStateWithLifecycle()

    // Recarga cuando llegan los kioscos (para tener los nombres)
    val kioscos by vm.kioscos.collectAsStateWithLifecycle()
    LaunchedEffect(kioscos) { if (kioscos.isNotEmpty() && ofertas.isEmpty()) vm.cargar() }

    Column(modifier = Modifier.fillMaxSize().background(AzulFondo)) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderGradient)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Column {
                Text(
                    "Ofertas",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Blanco,
                )
                Text(
                    if (ofertas.isEmpty() && !cargando) "No hay ofertas activas"
                    else "${ofertas.size} ofertas activas",
                    fontSize = 13.sp,
                    color = Blanco.copy(alpha = 0.65f),
                )
            }
        }

        when {
            cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Rojo)
                }
            }
            ofertas.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.LocalOffer, null, tint = Gris500, modifier = Modifier.size(64.dp))
                        Text("Sin ofertas activas", fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 16.sp)
                        Text("Cuando haya descuentos aparecen acá", fontSize = 13.sp, color = Gris500, textAlign = TextAlign.Center)
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(ofertas, key = { it.oferta.id }) { item ->
                        OfertaCard(item) {
                            navController.navigate(Rutas.detalleLocal(item.tiendaId))
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun OfertaCard(item: OfertaConTienda, onClick: () -> Unit) {
    val descuento = if (item.oferta.precioNormal > 0) {
        ((1 - item.oferta.precioOferta / item.oferta.precioNormal) * 100).toInt()
    } else 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral roja
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(Rojo),
            )
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Ícono producto
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RojoClaro),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.LocalOffer, null, tint = Rojo, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.oferta.nombreProducto,
                        fontWeight = FontWeight.Bold,
                        color = AzulNavy,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, null, tint = Gris500, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            item.tiendaNombre,
                            fontSize = 12.sp,
                            color = Gris500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (item.tiendaDireccion.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                item.tiendaDireccion,
                                fontSize = 11.sp,
                                color = Gris500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "$%.0f".format(item.oferta.precioNormal),
                            fontSize = 13.sp,
                            color = Gris500,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.LineThrough,
                            ),
                        )
                        Surface(shape = RoundedCornerShape(8.dp), color = RojoClaro) {
                            Text(
                                "$%.0f".format(item.oferta.precioOferta),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Rojo,
                                fontSize = 16.sp,
                            )
                        }
                        if (descuento > 0) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Verde.copy(alpha = 0.12f)) {
                                Text(
                                    "-$descuento%",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Verde,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
