package com.kosking.app.configuracion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.ui.components.SeccionCard
import com.kosking.app.ui.components.SeccionLabel
import com.kosking.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PantallaConfiguracion(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: ""

    // Notificaciones (locales, sin backend aún)
    var notifAbiertos by remember { mutableStateOf(true) }
    var notifOfertas by remember { mutableStateOf(true) }

    // Cambiar contraseña
    var mostrarCambioPass by remember { mutableStateOf(false) }
    var passActual by remember { mutableStateOf("") }
    var passNueva by remember { mutableStateOf("") }
    var passConfirm by remember { mutableStateOf("") }
    var cambiandoPass by remember { mutableStateOf(false) }

    // Eliminar cuenta
    var mostrarEliminarCuenta by remember { mutableStateOf(false) }

    val snackState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
        containerColor = AzulFondo,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Configuración",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Blanco,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Notificaciones ──────────────────────────────────────────────
            SeccionLabel("Notificaciones")
            SeccionCard {
                SwitchItem(
                    icono = Icons.Default.Storefront,
                    iconColor = Verde,
                    etiqueta = "Kioscos abiertos cerca",
                    subtitulo = "Avisame cuando un local cercano abre",
                    checked = notifAbiertos,
                    onCheckedChange = { notifAbiertos = it },
                )
                HorizontalDivider(color = Gris200, thickness = 0.5.dp, modifier = Modifier.padding(start = 66.dp))
                SwitchItem(
                    icono = Icons.Default.LocalOffer,
                    iconColor = Rojo,
                    etiqueta = "Nuevas ofertas",
                    subtitulo = "Avisame cuando haya descuentos nuevos",
                    checked = notifOfertas,
                    onCheckedChange = { notifOfertas = it },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Seguridad ───────────────────────────────────────────────────
            SeccionLabel("Seguridad")
            SeccionCard {
                InfoItem(
                    icono = Icons.Default.Email,
                    iconColor = Azul,
                    etiqueta = "Email",
                    valor = email,
                )
                HorizontalDivider(color = Gris200, thickness = 0.5.dp, modifier = Modifier.padding(start = 66.dp))
                ActionItem(
                    icono = Icons.Default.Lock,
                    iconColor = Naranja,
                    etiqueta = "Cambiar contraseña",
                    onClick = { mostrarCambioPass = true },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Zona peligrosa ───────────────────────────────────────────────
            SeccionLabel("Zona peligrosa")
            SeccionCard {
                ActionItem(
                    icono = Icons.Default.DeleteForever,
                    iconColor = Rojo,
                    etiqueta = "Eliminar cuenta",
                    onClick = { mostrarEliminarCuenta = true },
                    textColor = Rojo,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Dialog cambiar contraseña ────────────────────────────────────────────
    if (mostrarCambioPass) {
        AlertDialog(
            onDismissRequest = { mostrarCambioPass = false },
            title = { Text("Cambiar contraseña", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = passActual,
                        onValueChange = { passActual = it },
                        label = { Text("Contraseña actual") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                    OutlinedTextField(
                        value = passNueva,
                        onValueChange = { passNueva = it },
                        label = { Text("Nueva contraseña") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                    OutlinedTextField(
                        value = passConfirm,
                        onValueChange = { passConfirm = it },
                        label = { Text("Confirmar nueva contraseña") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passNueva != passConfirm) {
                            snackMsg = "Las contraseñas no coinciden"
                            return@Button
                        }
                        if (passNueva.length < 6) {
                            snackMsg = "Mínimo 6 caracteres"
                            return@Button
                        }
                        cambiandoPass = true
                        scope.launch {
                            runCatching {
                                val cred = EmailAuthProvider.getCredential(email, passActual)
                                user?.reauthenticate(cred)?.await()
                                user?.updatePassword(passNueva)?.await()
                            }.onSuccess {
                                snackMsg = "Contraseña actualizada"
                                mostrarCambioPass = false
                                passActual = ""; passNueva = ""; passConfirm = ""
                            }.onFailure {
                                snackMsg = "Error: ${it.message?.take(60)}"
                            }
                            cambiandoPass = false
                        }
                    },
                    enabled = !cambiandoPass,
                    colors = ButtonDefaults.buttonColors(containerColor = Azul),
                ) {
                    if (cambiandoPass) CircularProgressIndicator(Modifier.size(16.dp), color = Blanco, strokeWidth = 2.dp)
                    else Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarCambioPass = false
                    passActual = ""; passNueva = ""; passConfirm = ""
                }) { Text("Cancelar") }
            },
        )
    }

    // ── Dialog eliminar cuenta ───────────────────────────────────────────────
    if (mostrarEliminarCuenta) {
        AlertDialog(
            onDismissRequest = { mostrarEliminarCuenta = false },
            title = { Text("Eliminar cuenta", fontWeight = FontWeight.Bold, color = Rojo) },
            text = { Text("Esta acción es permanente. Se eliminará tu sesión y no podrás recuperar tu cuenta. ¿Estás seguro?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                user?.delete()?.await()
                            }.onSuccess {
                                navController.navigate("login") { popUpTo(0) { inclusive = true } }
                            }.onFailure {
                                snackMsg = "Necesitás volver a iniciar sesión para eliminar la cuenta"
                                mostrarEliminarCuenta = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rojo),
                ) { Text("Sí, eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarEliminarCuenta = false }) { Text("Cancelar") }
            },
        )
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun SwitchItem(
    icono: ImageVector,
    iconColor: Color,
    etiqueta: String,
    subtitulo: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(etiqueta, fontSize = 15.sp, color = AzulNavy, fontWeight = FontWeight.SemiBold)
            Text(subtitulo, fontSize = 11.sp, color = Gris500)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Blanco, checkedTrackColor = Verde),
        )
    }
}

@Composable
private fun InfoItem(icono: ImageVector, iconColor: Color, etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(etiqueta, fontSize = 11.sp, color = Gris500)
            Text(valor, fontSize = 15.sp, color = AzulNavy, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActionItem(
    icono: ImageVector,
    iconColor: Color,
    etiqueta: String,
    onClick: () -> Unit,
    textColor: Color = AzulNavy,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(etiqueta, fontSize = 15.sp, color = textColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Gris500, modifier = Modifier.size(18.dp))
    }
}

