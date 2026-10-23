package com.kosking.app.historial

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kosking.app.datos.HistorialRepository
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A))
)

@Composable
fun PantallaHistorial(navController: NavController) {
    var historial by remember { mutableStateOf(HistorialRepository.obtener()) }

    fun recargar() { historial = HistorialRepository.obtener() }

    Scaffold(containerColor = AzulFondo) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BG)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Historial de búsquedas",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Blanco,
                        )
                        Text(
                            "${historial.size} búsquedas guardadas",
                            fontSize = 12.sp,
                            color = Blanco.copy(alpha = 0.65f),
                        )
                    }
                    if (historial.isNotEmpty()) {
                        TextButton(onClick = {
                            HistorialRepository.limpiar()
                            recargar()
                        }) {
                            Text("Limpiar", color = Blanco.copy(alpha = 0.75f), fontSize = 13.sp)
                        }
                    }
                }
            }

            if (historial.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            null,
                            tint = Gris500,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Todavía no buscaste nada",
                            fontWeight = FontWeight.SemiBold,
                            color = AzulNavy,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tus búsquedas aparecerán acá.",
                            fontSize = 13.sp,
                            color = Gris500,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(historial, key = { it }) { query ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Blanco,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(Rutas.resultados(query))
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    null,
                                    tint = Gris500,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    query,
                                    fontSize = 15.sp,
                                    color = AzulNavy,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        HistorialRepository.eliminar(query)
                                        recargar()
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        tint = Gris500,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
