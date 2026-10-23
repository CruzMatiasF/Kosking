package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*
import com.kosking.app.util.estaAbierto
import com.kosking.app.util.textoHorario

// ── Datos de ejemplo ──────────────────────────────────────────────────────────

private data class VentaReciente(
    val producto: String,
    val emoji: String,
    val precio: String,
    val hora: String,
)

private data class AccionRapida(
    val icono: ImageVector,
    val etiqueta: String,
    val color: Color,
    val ruta: String,
)

private val ventasRecientes = listOf(
    VentaReciente("Coca-Cola 500ml", "🥤", "$1.000", "Hace 5 min"),
    VentaReciente("Oreo 124g", "🍪", "$1.200", "Hace 18 min"),
    VentaReciente("Lays Clásicas 150g", "🍟", "$950", "Hace 32 min"),
    VentaReciente("Jugo Cepita 1L", "🧃", "$1.100", "Hace 1 hora"),
    VentaReciente("Leche La Serenísima 1L", "🥛", "$1.800", "Hace 2 horas"),
)

private val accionesRapidas = listOf(
    AccionRapida(Icons.Default.Add, "Agregar\nproducto", Azul, Rutas.AGREGAR_PRODUCTO),
    AccionRapida(Icons.Default.Inventory, "Ver\ninventario", Verde, Rutas.INVENTARIO),
    AccionRapida(Icons.Default.LocalOffer, "Mis\nofertas", Rojo, Rutas.MIS_OFERTAS),
    AccionRapida(Icons.Default.Settings, "Config\nlocal", Naranja, Rutas.CONFIG_LOCAL),
)

// ── Pantalla ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPanelDueno(navController: NavController, vm: PanelDuenoViewModel = viewModel()) {
    val tienda by vm.tienda.collectAsStateWithLifecycle()
    val stock by vm.stock.collectAsStateWithLifecycle()
    val ofertasFirestore by vm.ofertas.collectAsStateWithLifecycle()
    val ventasFirestore by vm.ventas.collectAsStateWithLifecycle()
    val ventasHoyVal by vm.ventasHoy.collectAsStateWithLifecycle()
    val ventasSemanaVal by vm.ventasSemana.collectAsStateWithLifecycle()
    val stockBajoVal by vm.stockBajo.collectAsStateWithLifecycle()

    // Esperar hasta 3 segundos antes de concluir que el usuario no tiene local
    var yaEspero by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        yaEspero = true
    }

    if (tienda == null) {
        if (!yaEspero) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Rojo)
            }
            return
        }
        SinLocalPantalla(
            navController = navController,
            onVolver = { navController.popBackStack() },
        )
        return
    }

    val nombreLocal = tienda?.nombre ?: ""
    val ventasHoy = if (ventasHoyVal > 0) "$%.0f".format(ventasHoyVal) else "$0"
    val ventasSemana = if (ventasSemanaVal > 0) "$%.0f".format(ventasSemanaVal) else "$0"
    val productosActivos = stock.size
    val ofertasActivas = ofertasFirestore.size
    val stockBajo = stockBajoVal

    // Convierte ventas reales a VentaReciente para mostrar
    val ventasMostrar: List<VentaReciente> = if (ventasFirestore.isNotEmpty()) {
        ventasFirestore.take(5).map { venta ->
            val primerItem = venta.items.firstOrNull()
            VentaReciente(
                producto = primerItem?.nombre ?: "Venta",
                emoji = "🛍️",
                precio = "$%.0f".format(venta.total),
                hora = venta.fecha?.let {
                    val mins = ((System.currentTimeMillis() - it.toDate().time) / 60000).toInt()
                    when {
                        mins < 60 -> "Hace $mins min"
                        mins < 1440 -> "Hace ${mins / 60} horas"
                        else -> "Hace ${mins / 1440} días"
                    }
                } ?: ""
            )
        }
    } else ventasRecientes

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Kosking",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = Rojo,
                                letterSpacing = (-0.5).sp,
                            )
                            Text(
                                "Panel del dueño",
                                fontSize = 13.sp,
                                color = Blanco.copy(alpha = 0.65f),
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, null, tint = Blanco.copy(alpha = 0.85f))
                        }
                        IconButton(onClick = {
                            navController.navigate(Rutas.HOME) {
                                popUpTo(Rutas.PANEL_DUENO) { inclusive = true }
                            }
                        }) {
                            Icon(Icons.Default.Home, null, tint = Blanco.copy(alpha = 0.85f))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Nombre del local
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏪", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            nombreLocal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Blanco,
                        )
                        Spacer(Modifier.width(8.dp))
                        val abiertoAhora = tienda?.let { estaAbierto(it) } ?: false
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (abiertoAhora) Verde.copy(alpha = 0.2f) else Gris500.copy(alpha = 0.2f),
                        ) {
                            Text(
                                if (abiertoAhora) "Abierto" else "Cerrado",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                color = if (abiertoAhora) Verde else Blanco.copy(alpha = 0.6f),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Tarjetas de resumen
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ResumenCard(
                            titulo = "Ventas hoy",
                            valor = ventasHoy,
                            icono = Icons.Default.TrendingUp,
                            color = Verde,
                            modifier = Modifier.weight(1f),
                        )
                        ResumenCard(
                            titulo = "Esta semana",
                            valor = ventasSemana,
                            icono = Icons.Default.BarChart,
                            color = Azul,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Estado del inventario ─────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EstadoChip(
                    valor = productosActivos.toString(),
                    etiqueta = "Productos",
                    color = Azul,
                    modifier = Modifier.weight(1f),
                )
                EstadoChip(
                    valor = ofertasActivas.toString(),
                    etiqueta = "Ofertas activas",
                    color = Rojo,
                    modifier = Modifier.weight(1f),
                )
                EstadoChip(
                    valor = stockBajo.toString(),
                    etiqueta = "Stock bajo",
                    color = Naranja,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Estado del local (override) ───────────────────────────────────
            tienda?.let { t ->
                val abiertoAhora = estaAbierto(t)
                val tieneOverride = t.overrideAbierto != null &&
                    (t.overrideHasta?.toDate()?.time ?: 0L) > System.currentTimeMillis()

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Blanco,
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (abiertoAhora) Verde.copy(alpha = 0.12f) else Gris200),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (abiertoAhora) Icons.Default.LockOpen else Icons.Default.Lock,
                                    null,
                                    tint = if (abiertoAhora) Verde else Gris500,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (abiertoAhora) "Local abierto" else "Local cerrado",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AzulNavy,
                                )
                                Text(
                                    textoHorario(t),
                                    fontSize = 11.sp,
                                    color = Gris500,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (tieneOverride) {
                                OutlinedButton(
                                    onClick = { vm.limpiarOverride() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Gris500),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gris500),
                                ) {
                                    Text("Volver al horario", fontSize = 12.sp)
                                }
                            } else if (abiertoAhora) {
                                Button(
                                    onClick = { vm.setOverride(false) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Gris500),
                                ) {
                                    Text("Cerrar antes", fontSize = 12.sp, color = Blanco)
                                }
                            } else {
                                Button(
                                    onClick = { vm.setOverride(true) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Verde),
                                ) {
                                    Text("Quedarme más", fontSize = 12.sp, color = Blanco)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Acciones rápidas ──────────────────────────────────────────────
            Text(
                "Acciones rápidas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AzulNavy,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                accionesRapidas.forEach { accion ->
                    AccionCard(accion, modifier = Modifier.weight(1f)) {
                        navController.navigate(accion.ruta)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Ventas recientes ──────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Ventas recientes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AzulNavy,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {}) {
                    Text("Ver todas", color = Azul, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ventasMostrar.forEach { venta -> VentaItem(venta) }
            }

            Spacer(Modifier.height(24.dp))

            // ── Registrar venta manual ────────────────────────────────────────
            Button(
                onClick = { navController.navigate(Rutas.REGISTRAR_VENTA) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Azul, contentColor = Blanco),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registrar venta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun ResumenCard(
    titulo: String,
    valor: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco.copy(alpha = 0.12f),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(titulo, fontSize = 11.sp, color = Blanco.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(6.dp))
            Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Blanco)
        }
    }
}

@Composable
private fun EstadoChip(valor: String, etiqueta: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = Blanco, shadowElevation = 1.dp, modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            Spacer(Modifier.height(2.dp))
            Text(etiqueta, fontSize = 10.sp, color = Gris500, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AccionCard(accion: AccionRapida, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accion.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(accion.icono, null, tint = accion.color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                accion.etiqueta,
                fontSize = 11.sp,
                color = AzulNavy,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun VentaItem(venta: VentaReciente) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Blanco,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AzulFondo),
                contentAlignment = Alignment.Center,
            ) {
                Text(venta.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(venta.producto, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 13.sp)
                Spacer(Modifier.height(2.dp))
                Text(venta.hora, fontSize = 11.sp, color = Gris500)
            }
            Text(venta.precio, fontWeight = FontWeight.ExtraBold, color = Verde, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SinLocalPantalla(navController: NavController, onVolver: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AzulFondo),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text("🏪", fontSize = 64.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Todavía no tenés un local",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = AzulNavy,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Creá tu local para empezar a gestionar tu stock, ofertas y ventas.",
                fontSize = 14.sp,
                color = Gris500,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { navController.navigate(Rutas.CREAR_LOCAL) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rojo, contentColor = Blanco),
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Crear mi local", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onVolver,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Azul),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Azul),
            ) {
                Text("Volver", fontSize = 14.sp)
            }
        }
    }
}
