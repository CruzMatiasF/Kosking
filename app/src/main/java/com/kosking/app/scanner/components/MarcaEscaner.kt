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

    Canvas(modifier) {
        val s = 28.dp.toPx()
        val w = 3.5.dp.toPx()
        val cy = size.height / 2f
        val extra = 48.dp.toPx()

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

        drawLine(
            color = colorMarco.copy(alpha = 0.5f),
            start = Offset(-extra, cy),
            end = Offset(size.width + extra, cy),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
internal fun IconoCodigoBarras(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val barWidths = listOf(1f, 2.5f, 1f, 1f, 3f, 1f, 2f, 1f, 1f, 2.5f, 1f, 1f, 3f, 1f)
        val totalWidth = barWidths.sum() * (size.width / barWidths.sum())
        val gap = size.width / barWidths.sum()
        var x = 0f
        barWidths.forEachIndexed { i, bw ->
            val pw = bw * gap
            if (i % 2 == 0) {
                drawLine(color, Offset(x + pw / 2f, 0f), Offset(x + pw / 2f, size.height), pw)
            }
            x += pw
        }
        // dos barras largas en los extremos (marcadores de inicio/fin típicos del código EAN)
        drawLine(color, Offset(0f + 0.5.dp.toPx(), 0f), Offset(0f + 0.5.dp.toPx(), size.height), 1.dp.toPx())
        drawLine(color, Offset(size.width - 0.5.dp.toPx(), 0f), Offset(size.width - 0.5.dp.toPx(), size.height), 1.dp.toPx())
    }
}
