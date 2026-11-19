package com.kosking.app.autenticacion

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PantallaDescargandoRubro(
    tipo: String,
    onListo: () -> Unit
) {
    val emoji = when (tipo.lowercase()) {
        "kiosco"     -> "🏪"
        "almacén", "almacen" -> "🛒"
        "peluquería", "peluqueria" -> "✂️"
        "farmacia"   -> "💊"
        else         -> "🏪"
    }

    var progreso by remember { mutableFloatStateOf(0f) }
    var mensaje by remember { mutableStateOf("Preparando tu panel...") }

    LaunchedEffect(Unit) {
        delay(300)
        progreso = 0.3f
        mensaje = "Cargando herramientas de $tipo..."
        delay(600)
        progreso = 0.65f
        mensaje = "Configurando tu local..."
        delay(600)
        progreso = 0.9f
        mensaje = "Casi listo..."
        delay(500)
        progreso = 1f
        delay(300)
        onListo()
    }

    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(durationMillis = 400, easing = EaseInOut),
        label = "progreso"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 72.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            tipo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { progresoAnimado },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "${(progresoAnimado * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
