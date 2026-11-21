package com.kosking.app.scanner.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun MarcaEscaner(escaneoExitoso: Boolean, modifier: Modifier = Modifier) {
    val colorMarco by animateColorAsState(
        targetValue = if (escaneoExitoso) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 200),
        label = "marcoColor"
    )
    Canvas(modifier.then(Modifier).also { }) {
        val s = 28.dp.toPx()
        val w = 3.5.dp.toPx()
        listOf(
            Offset(0f, 0f),
            Offset(size.width, 0f),
            Offset(0f, size.height),
            Offset(size.width, size.height)
        ).forEach { corner ->
            val hx = if (corner.x == 0f) corner.x + s else corner.x - s
            val vy = if (corner.y == 0f) corner.y + s else corner.y - s
            drawLine(colorMarco, corner, Offset(hx, corner.y), w)
            drawLine(colorMarco, corner, Offset(corner.x, vy), w)
        }
    }
}
