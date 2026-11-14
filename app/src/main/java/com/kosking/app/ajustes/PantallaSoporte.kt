package com.kosking.app.ajustes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val WHATSAPP_NUMERO = "5491100000000" // reemplazar con tu número real
private const val EMAIL_SOPORTE = "matiasfc.info@gmail.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSoporte(onVolver: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y soporte") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "CONTACTO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            SoporteCard(
                icono = Icons.Default.Chat,
                titulo = "WhatsApp",
                descripcion = "Escribinos directo, respondemos rápido",
                onClick = {
                    val url = "https://wa.me/$WHATSAPP_NUMERO?text=Hola, necesito ayuda con Kosking"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            )

            SoporteCard(
                icono = Icons.Default.Email,
                titulo = "Correo electrónico",
                descripcion = EMAIL_SOPORTE,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$EMAIL_SOPORTE")
                        putExtra(Intent.EXTRA_SUBJECT, "Soporte Kosking")
                    }
                    context.startActivity(Intent.createChooser(intent, "Enviar correo"))
                }
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "PREGUNTAS FRECUENTES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            FaqItem(
                pregunta = "¿Cómo agrego un empleado?",
                respuesta = "Andá a Configuración → Mi local → Colaboradores y compartí el código de acceso."
            )
            HorizontalDivider()
            FaqItem(
                pregunta = "¿Los datos se guardan si no hay internet?",
                respuesta = "Sí, Kosking funciona offline y sincroniza automáticamente cuando vuelve la conexión."
            )
            HorizontalDivider()
            FaqItem(
                pregunta = "¿Cómo elimino un movimiento?",
                respuesta = "Entrá al detalle del movimiento y usá la opción Anular. Los movimientos anulados no se borran, quedan como registro."
            )
        }
    }
}

@Composable
private fun SoporteCard(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(titulo, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(descripcion, style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                Icon(icono, null, Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary)
            },
            trailingContent = {
                Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )
    }
}

@Composable
private fun FaqItem(pregunta: String, respuesta: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(pregunta, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(respuesta, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
