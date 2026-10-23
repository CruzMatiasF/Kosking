package com.kosking.app.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.kosking.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PantallaPerfil(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val nombre = user?.displayName ?: user?.email?.substringBefore("@") ?: "Usuario"
    val email = user?.email ?: ""
    val inicial = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    var editandoNombre by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf(nombre) }
    var guardando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackState = remember { SnackbarHostState() }

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
                .padding(padding),
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
                        "Mi perfil",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Blanco,
                    )
                }
            }

            // Avatar grande centrado
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Rojo.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        inicial,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        color = Blanco,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(nombre, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Blanco)
                Text(email, fontSize = 13.sp, color = Blanco.copy(alpha = 0.6f))
            }

            Spacer(Modifier.height(24.dp))

            // Card con datos
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Blanco,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column {
                    // Nombre (editable)
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
                                .background(Azul.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Person, null, tint = Azul, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nombre", fontSize = 11.sp, color = Gris500)
                            if (editandoNombre) {
                                OutlinedTextField(
                                    value = nuevoNombre,
                                    onValueChange = { nuevoNombre = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = AzulNavy),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Azul,
                                        unfocusedBorderColor = Gris200,
                                    ),
                                )
                            } else {
                                Text(nombre, fontSize = 15.sp, color = AzulNavy, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        if (editandoNombre) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    nuevoNombre = nombre
                                    editandoNombre = false
                                }) { Text("Cancelar", fontSize = 12.sp) }
                                Button(
                                    onClick = {
                                        guardando = true
                                        scope.launch {
                                            runCatching {
                                                user?.updateProfile(userProfileChangeRequest { displayName = nuevoNombre })
                                            }.onSuccess {
                                                snackMsg = "Nombre actualizado"
                                                editandoNombre = false
                                            }.onFailure {
                                                snackMsg = "No se pudo actualizar"
                                            }
                                            guardando = false
                                        }
                                    },
                                    enabled = !guardando && nuevoNombre.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Azul),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp),
                                ) {
                                    if (guardando) CircularProgressIndicator(Modifier.size(14.dp), color = Blanco, strokeWidth = 2.dp)
                                    else Text("Guardar", fontSize = 12.sp)
                                }
                            }
                        } else {
                            IconButton(onClick = { editandoNombre = true }) {
                                Icon(Icons.Default.Edit, null, tint = Gris500, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = Gris200, thickness = 0.5.dp, modifier = Modifier.padding(start = 66.dp))

                    // Email (solo lectura)
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
                                .background(Verde.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Email, null, tint = Verde, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Email", fontSize = 11.sp, color = Gris500)
                            Text(email, fontSize = 15.sp, color = AzulNavy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
