package com.kosking.app.empleado

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaIngresarCodigo(navController: NavController, vm: IngresarCodigoViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var codigo by remember { mutableStateOf("") }

    val exito = estado as? EstadoCodigo.Exito

    // Si ya se unió, navegar al panel de empleado
    LaunchedEffect(exito) {
        if (exito != null) {
            kotlinx.coroutines.delay(2000)
            navController.navigate(Rutas.PANEL_EMPLEADO) {
                popUpTo(Rutas.HOME) { inclusive = false }
            }
        }
    }

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(BG).statusBarsPadding()) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Unirme a un local", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Blanco)
                        Text("Ingresá el código que te dio el dueño", fontSize = 12.sp, color = Blanco.copy(alpha = 0.65f))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.weight(0.3f))

            // Ilustración
            Text("🔑", fontSize = 64.sp)
            Text(
                "Código de invitación",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = AzulNavy,
                textAlign = TextAlign.Center,
            )
            Text(
                "El dueño del local te compartió un código de 6 caracteres para que puedas unirte.",
                fontSize = 14.sp,
                color = Gris700,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            // Campo del código
            Surface(shape = RoundedCornerShape(16.dp), color = Blanco, shadowElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { if (it.length <= 6) codigo = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                        label = { Text("Código del local") },
                        placeholder = { Text("Ej: AB3X7K", color = Gris500) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = Azul) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            letterSpacing = 6.sp,
                            textAlign = TextAlign.Center,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Azul,
                            focusedLabelColor = Azul,
                        ),
                        isError = estado is EstadoCodigo.CodigoInvalido,
                        supportingText = when (estado) {
                            is EstadoCodigo.CodigoInvalido -> ({ Text("Código no encontrado. Verificá con el dueño.", color = Rojo) })
                            else -> null
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${codigo.length}/6 caracteres", fontSize = 11.sp, color = Gris500)
                }
            }

            // Estado de éxito: muestra el local encontrado
            if (exito != null) {
                Surface(shape = RoundedCornerShape(14.dp), color = Verde.copy(alpha = 0.08f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("🏪", fontSize = 32.sp)
                        Column {
                            Text("¡Código válido!", fontWeight = FontWeight.Bold, color = Verde, fontSize = 14.sp)
                            Text(exito.tienda.nombre, fontWeight = FontWeight.SemiBold, color = AzulNavy, fontSize = 16.sp)
                            Text(exito.tienda.direccion, fontSize = 12.sp, color = Gris500)
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Azul.copy(alpha = 0.08f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(color = Azul, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Entrando al panel de empleado...", fontSize = 13.sp, color = Azul)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { vm.unirseConCodigo(codigo) },
                enabled = codigo.length == 6 && estado !is EstadoCodigo.Buscando && exito == null,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Azul, contentColor = Blanco,
                    disabledContainerColor = Gris200, disabledContentColor = Gris500,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                if (estado is EstadoCodigo.Buscando) {
                    CircularProgressIndicator(color = Blanco, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Verificando...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Login, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unirme al local", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (estado is EstadoCodigo.Error) {
        AlertDialog(
            onDismissRequest = { vm.estado.value = EstadoCodigo.Idle },
            title = { Text("Error") },
            text = { Text((estado as EstadoCodigo.Error).mensaje) },
            confirmButton = { TextButton(onClick = { vm.estado.value = EstadoCodigo.Idle }) { Text("OK") } },
        )
    }
}
