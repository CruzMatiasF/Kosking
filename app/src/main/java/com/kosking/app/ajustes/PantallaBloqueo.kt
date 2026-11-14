package com.kosking.app.ajustes

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kosking.app.datos.PreferenciasRepository

@Composable
fun PantallaBloqueo(onDesbloqueado: () -> Unit) {
    val context = LocalContext.current
    val biometricoHabilitado = remember { PreferenciasRepository.isBiometricoHabilitado(context) }
    val biometricoDisponible = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    var digitos by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun verificar() {
        if (PreferenciasRepository.verificarPin(context, digitos)) {
            onDesbloqueado()
        } else {
            errorMsg = "PIN incorrecto"
            digitos = ""
        }
    }

    fun lanzarBiometrico() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onDesbloqueado()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Kosking")
            .setSubtitle("Verificá tu identidad para continuar")
            .setNegativeButtonText("Usar PIN")
            .build()
        prompt.authenticate(info)
    }

    // Auto-trigger biométrico al entrar
    if (biometricoHabilitado && biometricoDisponible) {
        LaunchedEffect(Unit) { lanzarBiometrico() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Text("Kosking", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Text("Ingresá tu PIN", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Indicador de dígitos
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { i ->
                    val lleno = i < digitos.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (lleno) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            } else {
                Spacer(Modifier.height(16.dp))
            }

            // Teclado numérico
            Column(verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("bio", "0", "del")
                ).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        fila.forEach { tecla ->
                            when (tecla) {
                                "del" -> TeclaPin(
                                    onClick = { if (digitos.isNotEmpty()) digitos = digitos.dropLast(1); errorMsg = null }
                                ) {
                                    Icon(Icons.Default.Backspace, null, Modifier.size(22.dp))
                                }
                                "bio" -> if (biometricoHabilitado && biometricoDisponible) {
                                    TeclaPin(onClick = { lanzarBiometrico() }) {
                                        Icon(Icons.Default.Fingerprint, null, Modifier.size(22.dp))
                                    }
                                } else {
                                    Spacer(Modifier.size(72.dp))
                                }
                                else -> TeclaPin(
                                    onClick = {
                                        if (digitos.length < 4) {
                                            digitos += tecla
                                            errorMsg = null
                                            if (digitos.length == 4) verificar()
                                        }
                                    }
                                ) {
                                    Text(tecla, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeclaPin(onClick: () -> Unit, content: @Composable () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape
    ) { content() }
}
