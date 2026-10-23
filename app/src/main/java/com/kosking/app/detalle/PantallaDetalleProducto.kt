package com.kosking.app.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleProducto(
    productoId: String,
    navController: NavController,
) {
    val vm: DetalleProductoViewModel = viewModel(factory = DetalleProductoViewModel.factory(productoId))
    val producto by vm.producto.collectAsStateWithLifecycle()
    val tiendas by vm.tiendas.collectAsStateWithLifecycle()
    val cargando by vm.cargando.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulNavy),
            )
        },
    ) { padding ->
        if (cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Rojo)
            }
            return@Scaffold
        }

        if (producto == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Gris500, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Producto no encontrado", color = AzulNavy, fontWeight = FontWeight.Bold)
                }
            }
            return@Scaffold
        }

        val prod = producto!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Hero ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient)
                    .padding(top = 8.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Blanco.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Inventory, null, tint = Blanco, modifier = Modifier.size(52.dp))
                    }
                    if (prod.categoria.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = RojoClaro) {
                            Text(
                                prod.categoria,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = Rojo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // ── Info principal ────────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-20).dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = AzulFondo,
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {

                    Text(prod.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AzulNavy)
                    if (prod.marca.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(prod.marca, fontSize = 14.sp, color = Gris500)
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = Gris200)
                    Spacer(Modifier.height(24.dp))

                    // ── Kioscos con este producto ─────────────────────────────────
                    Text(
                        "Disponible en",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AzulNavy,
                    )
                    Spacer(Modifier.height(12.dp))

                    if (tiendas.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.StoreMallDirectory, null, tint = Gris500, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Ningún kiosco tiene este producto en stock", fontSize = 13.sp, color = Gris500)
                            }
                        }
                    } else {
                        tiendas.forEach { tienda ->
                            TiendaDisponibleItem(
                                tienda = tienda,
                                onClick = { navController.navigate(Rutas.detalleLocal(tienda.tiendaId)) },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TiendaDisponibleItem(tienda: TiendaConPrecio, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(Azul),
            )
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AzulClaro),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Store, null, tint = Azul, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(tienda.nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 14.sp)
                    if (tienda.direccion.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(tienda.direccion, fontSize = 12.sp, color = Gris500)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (tienda.precio > 0) {
                        Text(
                            "$%.0f".format(tienda.precio),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Rojo,
                        )
                    }
                    val stockColor = if (tienda.cantidad > 5) Verde else Naranja
                    Surface(shape = RoundedCornerShape(8.dp), color = stockColor.copy(alpha = 0.12f)) {
                        Text(
                            "Stock: ${tienda.cantidad}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = stockColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
