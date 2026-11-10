package com.kosking.app.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class SlideOnboarding(
    val emoji: String,
    val titulo: String,
    val descripcion: String
)

private val slides = listOf(
    SlideOnboarding(
        emoji = "📒",
        titulo = "¿Cuánto te deben hoy?",
        descripcion = "Con el cuaderno nunca sabés exactamente. Con Kosking ves la deuda total en segundos, siempre actualizada."
    ),
    SlideOnboarding(
        emoji = "🏷️",
        titulo = "\"¿A cuánto estaba el aceite?\"",
        descripcion = "Cargá tus precios una vez. Tu empleado y vos siempre saben cuánto cobrar, sin adivinar."
    ),
    SlideOnboarding(
        emoji = "👥",
        titulo = "¿Tu empleado anotó mal?",
        descripcion = "Cada movimiento queda registrado con nombre y hora. Sabés quién anotó qué, desde tu celu."
    ),
    SlideOnboarding(
        emoji = "💬",
        titulo = "Cobrar sin incomodar",
        descripcion = "Mandá el resumen de deuda por WhatsApp con un toque. El cliente lo ve y paga. Sin vueltas."
    ),
    SlideOnboarding(
        emoji = "🚀",
        titulo = "Empezá gratis ahora",
        descripcion = "Sin tarjeta. Sin complicaciones. En 2 minutos tenés tu kiosco organizado."
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PantallaOnboarding(
    onTerminar: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val esUltimaSlide = pagerState.currentPage == slides.size - 1

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pagina ->
            SlideContent(slide = slides[pagina])
        }

        // Saltar — arriba a la derecha
        if (!esUltimaSlide) {
            TextButton(
                onClick = onTerminar,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)
            ) {
                Text("Saltar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Dots + botón — abajo
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Dots indicador
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(slides.size) { i ->
                    val color by animateColorAsState(
                        if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        label = "dot_color"
                    )
                    val width = if (i == pagerState.currentPage) 24.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Botón acción
            Button(
                onClick = {
                    if (esUltimaSlide) {
                        onTerminar()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (esUltimaSlide) "Empezar" else "Siguiente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SlideContent(slide: SlideOnboarding) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            slide.emoji,
            fontSize = 80.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Text(
            slide.titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            slide.descripcion,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
    }
}
