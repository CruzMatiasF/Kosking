package com.kosking.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

@Composable
fun PantallaElegirRol(navController: NavController) {
    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(HeaderGradient).statusBarsPadding()) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Text("Mi local", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Blanco)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.weight(0.4f))

            Text("🏪", fontSize = 64.sp)
            Text(
                "¿Cuál es tu rol?",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = AzulNavy,
                textAlign = TextAlign.Center,
            )
            Text(
                "Elegí cómo vas a usar Kosking",
                fontSize = 14.sp,
                color = Gris500,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            // Tarjeta Dueño
            RolCard(
                emoji = "👑",
                titulo = "Soy dueño",
                descripcion = "Creá tu kiosco, gestioná el stock, las ofertas y las ventas",
                color = Rojo,
                onClick = { navController.navigate(Rutas.CREAR_LOCAL) },
            )

            // Tarjeta Empleado
            RolCard(
                emoji = "🤝",
                titulo = "Soy empleado",
                descripcion = "Ingresá el código que te dio el dueño para unirte a su local",
                color = Azul,
                onClick = { navController.navigate(Rutas.INGRESAR_CODIGO) },
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun RolCard(emoji: String, titulo: String, descripcion: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Blanco,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 30.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = AzulNavy)
                Spacer(Modifier.height(4.dp))
                Text(descripcion, fontSize = 13.sp, color = Gris700, lineHeight = 18.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = color, modifier = Modifier.size(22.dp))
        }
    }
}
