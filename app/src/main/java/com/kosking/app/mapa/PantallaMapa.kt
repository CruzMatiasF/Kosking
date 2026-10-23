package com.kosking.app.mapa

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.home.HomeViewModel
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A)),
    endY = 280f,
)

@Composable
fun PantallaMapa(navController: NavController, vm: HomeViewModel = viewModel()) {
    val kioscos by vm.kioscos.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(AzulFondo)) {

        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(BG)
                .padding(top = 48.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Kioscos en Palpalá", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Blanco)
                }

                Spacer(Modifier.height(12.dp))

                // Placeholder mapa
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Blanco.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Default.Map, null, tint = Blanco.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                        Column {
                            Text("Mapa interactivo", fontWeight = FontWeight.Bold, color = Blanco, fontSize = 14.sp)
                            Text("Próximamente — los kioscos aparecerán en el mapa", color = Blanco.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Lista de kioscos ─────────────────────────────────────────────────
        if (kioscos.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.StoreMallDirectory, null, tint = Gris500, modifier = Modifier.size(48.dp))
                    Text("No hay kioscos registrados", fontWeight = FontWeight.Bold, color = AzulNavy)
                    Text("Sé el primero en agregar el tuyo", color = Gris500, fontSize = 13.sp)
                }
            }
        } else {
            Text(
                "${kioscos.size} kioscos registrados",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = AzulNavy,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(kioscos, key = { it.id }) { tienda ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Blanco,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().clickable {
                            navController.navigate(Rutas.detalleLocal(tienda.id))
                        },
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
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AzulClaro),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Store, null, tint = Azul, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tienda.nombre, fontWeight = FontWeight.Bold, color = AzulNavy, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, tint = Gris500, modifier = Modifier.size(11.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(tienda.direccion.ifBlank { "Sin dirección" }, fontSize = 12.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (tienda.horario.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, null, tint = Gris500, modifier = Modifier.size(11.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(tienda.horario, fontSize = 12.sp, color = Gris500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (tienda.abierto) Verde.copy(alpha = 0.12f) else Gris200,
                                ) {
                                    Text(
                                        if (tienda.abierto) "Abierto" else "Cerrado",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        color = if (tienda.abierto) Verde else Gris500,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
