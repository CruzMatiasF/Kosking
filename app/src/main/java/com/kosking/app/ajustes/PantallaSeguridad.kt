package com.kosking.app.ajustes

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.datos.PreferenciasRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeguridad(onVolver: () -> Unit) {
    val context = LocalContext.current
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""

    var pinHabilitado by remember { mutableStateOf(PreferenciasRepository.isPinHabilitado(context)) }
    var biometricoHabilitado by remember { mutableStateOf(PreferenciasRepository.isBiometricoHabilitado(context)) }

    // Dialogs
    var mostrarDialogSetPin by remember { mutableStateOf(false) }
    var mostrarDialogQuitarPin by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }

    // Biométrico disponible en este dispositivo
    val biometricoDisponible = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    // ── Diálogo configurar PIN ──────────────────────────────────────────────
    if (mostrarDialogSetPin) {
        DialogConfigurarPin(
            onConfirmar = { pin ->
                PreferenciasRepository.setPinHash(context, pin)
                pinHabilitado = true
                mostrarDialogSetPin = false
                esError = false
                mensaje = "PIN activado. La próxima vez que abras la app tendrás que ingresarlo."
            },
            onCancelar = { mostrarDialogSetPin = false }
        )
    }

    // ── Diálogo confirmar quitar PIN ────────────────────────────────────────
    if (mostrarDialogQuitarPin) {
        AlertDialog(
            onDismissRequest = { mostrarDialogQuitarPin = false },
            icon = { Icon(Icons.Default.LockOpen, null) },
            title = { Text("¿Desactivar PIN?") },
            text = { Text("La app se abrirá sin pedir contraseña.") },
            confirmButton = {
                Button(
                    onClick = {
                        PreferenciasRepository.quitarPin(context)
                        pinHabilitado = false
                        biometricoHabilitado = false
                        mostrarDialogQuitarPin = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Desactivar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogQuitarPin = false }) { Text("Cancelar") }
            }
        )
    }

    mensaje?.let {
        AlertDialog(
            onDismissRequest = { mensaje = null },
            icon = {
                Icon(
                    if (esError) Icons.Default.Error else Icons.Default.CheckCircle, null,
                    tint = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(if (esError) "Error" else "Listo") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { mensaje = null }) { Text("OK") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguridad") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Acceso con PIN ───────────────────────────────────────────
            Text(
                "BLOQUEO DE APP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            ListItem(
                headlineContent = { Text("PIN de seguridad", fontWeight = FontWeight.Medium) },
                supportingContent = {
                    Text(if (pinHabilitado) "La app pide PIN al abrirse" else "La app se abre sin contraseña")
                },
                leadingContent = {
                    Icon(Icons.Default.Pin, null, Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Switch(
                        checked = pinHabilitado,
                        onCheckedChange = { activar ->
                            if (activar) mostrarDialogSetPin = true
                            else mostrarDialogQuitarPin = true
                        }
                    )
                }
            )

            if (pinHabilitado) {
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                ListItem(
                    headlineContent = { Text("Cambiar PIN", fontWeight = FontWeight.Medium) },
                    leadingContent = {
                        Icon(Icons.Default.Edit, null, Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier.clickable { mostrarDialogSetPin = true }.fillMaxWidth()
                )
                // Biométrico — solo si el hardware lo soporta
                if (biometricoDisponible) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    ListItem(
                        headlineContent = {
                            Text(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                                    "Huella o reconocimiento facial"
                                else "Huella digital",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = { Text("Desbloquear con biometría además del PIN") },
                        leadingContent = {
                            Icon(Icons.Default.Fingerprint, null, Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Switch(
                                checked = biometricoHabilitado,
                                onCheckedChange = { valor ->
                                    PreferenciasRepository.setBiometricoHabilitado(context, valor)
                                    biometricoHabilitado = valor
                                }
                            )
                        }
                    )
                }
            }

            // ── Contraseña de cuenta ─────────────────────────────────────
            HorizontalDivider()
            Text(
                "CUENTA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            ListItem(
                headlineContent = { Text("Cambiar contraseña", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("Te enviamos un email para resetear tu contraseña") },
                leadingContent = {
                    Icon(Icons.Default.Lock, null, Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable {
                    if (email.isNotBlank()) {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnSuccessListener { esError = false; mensaje = "Te enviamos un email a $email para cambiar tu contraseña." }
                            .addOnFailureListener { esError = true; mensaje = "No se pudo enviar el email. Intentá de nuevo." }
                    }
                }.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DialogConfigurarPin(
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var paso by remember { mutableStateOf(1) } // 1=ingresar, 2=confirmar

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Default.Pin, null) },
        title = { Text(if (paso == 1) "Crear PIN" else "Confirmar PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (paso == 1) "Ingresá un PIN de 4 dígitos"
                    else "Volvé a ingresar el PIN para confirmar"
                )
                OutlinedTextField(
                    value = if (paso == 1) pin else pinConfirm,
                    onValueChange = { v ->
                        if (v.length <= 4 && v.all { it.isDigit() }) {
                            if (paso == 1) pin = v else pinConfirm = v
                            errorMsg = null
                        }
                    },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (paso == 1) {
                        if (pin.length < 4) { errorMsg = "El PIN debe tener 4 dígitos"; return@Button }
                        paso = 2
                    } else {
                        if (pinConfirm != pin) { errorMsg = "Los PINs no coinciden"; pinConfirm = ""; return@Button }
                        onConfirmar(pin)
                    }
                }
            ) { Text(if (paso == 1) "Siguiente" else "Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
