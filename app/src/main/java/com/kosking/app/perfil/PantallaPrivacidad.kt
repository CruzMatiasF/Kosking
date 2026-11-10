package com.kosking.app.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrivacidad(onVolver: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Política de privacidad") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Última actualización: Noviembre 2025",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { SeccionPrivacidad("1. Información que recopilamos") {
                "Kosking recopila la información que vos mismo ingresás: nombre de tu local, datos de tus clientes (nombre, teléfono, domicilio), movimientos de fiados y pagos, y datos de tu cuenta (email).\n\nNo recopilamos información sin tu consentimiento."
            }}
            item { SeccionPrivacidad("2. Cómo usamos tu información") {
                "Usamos tu información exclusivamente para brindarte el servicio de gestión de fiaditos. Nunca vendemos ni compartimos tus datos con terceros.\n\nLos datos de tus clientes son privados y solo vos y tus empleados autorizados pueden verlos."
            }}
            item { SeccionPrivacidad("3. Almacenamiento y seguridad") {
                "Tus datos se almacenan en Firebase (Google Cloud), con encriptación en tránsito y en reposo. Tomamos medidas razonables para proteger tu información.\n\nKosking soporta uso offline: si no tenés internet, tus datos se sincronizan automáticamente cuando se restablece la conexión."
            }}
            item { SeccionPrivacidad("4. Datos de tus clientes") {
                "Vos sos el responsable de los datos que cargás sobre tus clientes. Kosking actúa como procesador de datos en tu nombre.\n\nSolo cargá datos de personas que te dieron su consentimiento para gestionar su cuenta de fiaditos."
            }}
            item { SeccionPrivacidad("5. Notificaciones") {
                "Con tu permiso, Kosking puede enviarte notificaciones locales recordándote deudas pendientes de cobrar. Podés deshabilitar esto desde la configuración de tu dispositivo en cualquier momento."
            }}
            item { SeccionPrivacidad("6. Eliminación de datos") {
                "Podés eliminar tu cuenta y todos tus datos contactándonos por email. Los datos se eliminan permanentemente en un plazo de 30 días."
            }}
            item { SeccionPrivacidad("7. Contacto") {
                "Si tenés preguntas sobre esta política, escribinos a: matiascf04@gmail.com"
            }}
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SeccionPrivacidad(titulo: String, contenido: () -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            contenido(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
        )
    }
}
