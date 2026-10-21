package com.kosking.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

val KoskingRed = Color(0xFFE63946)

@Composable
fun AuthScreen(
    rol: String,
    onVolver: () -> Unit,
    onLoginExitoso: () -> Unit
) {
    var tabSeleccionado by remember { mutableIntStateOf(0) }
    val tabs = listOf("Iniciar sesión", "Registrarse")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        IconButton(onClick = onVolver) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = KoskingRed
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Kosking",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = KoskingRed
        )

        Spacer(modifier = Modifier.height(24.dp))

        TabRow(
            selectedTabIndex = tabSeleccionado,
            containerColor = Color.White,
            contentColor = KoskingRed
        ) {
            tabs.forEachIndexed { index, titulo ->
                Tab(
                    selected = tabSeleccionado == index,
                    onClick = { tabSeleccionado = index },
                    text = {
                        Text(
                            text = titulo,
                            fontWeight = if (tabSeleccionado == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (tabSeleccionado == 0) {
            FormularioLogin(onLoginExitoso = onLoginExitoso)
        } else {
            FormularioRegistro(rol = rol, onRegistroExitoso = onLoginExitoso)
        }
    }
}

@Composable
fun FormularioLogin(onLoginExitoso: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KoskingRed,
                focusedLabelColor = KoskingRed,
                cursorColor = KoskingRed
            )
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KoskingRed,
                focusedLabelColor = KoskingRed,
                cursorColor = KoskingRed
            )
        )

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    error = "Completá todos los campos"
                    return@Button
                }
                cargando = true
                error = ""
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { onLoginExitoso() }
                    .addOnFailureListener {
                        cargando = false
                        error = "Email o contraseña incorrectos"
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KoskingRed),
            enabled = !cargando
        ) {
            if (cargando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        TextButton(onClick = { }) {
            Text("¿Olvidaste tu contraseña?", color = KoskingRed)
        }
    }
}

@Composable
fun FormularioRegistro(rol: String, onRegistroExitoso: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KoskingRed,
                focusedLabelColor = KoskingRed,
                cursorColor = KoskingRed
            )
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KoskingRed,
                focusedLabelColor = KoskingRed,
                cursorColor = KoskingRed
            )
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KoskingRed,
                focusedLabelColor = KoskingRed,
                cursorColor = KoskingRed
            )
        )

        OutlinedTextField(
            value = confirmarPassword,
            onValueChange = { confirmarPassword = it },
            label = { Text("Confirmar contraseña") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KoskingRed,
                focusedLabelColor = KoskingRed,
                cursorColor = KoskingRed
            )
        )

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                when {
                    nombre.isBlank() || email.isBlank() || password.isBlank() ->
                        error = "Completá todos los campos"
                    password != confirmarPassword ->
                        error = "Las contraseñas no coinciden"
                    password.length < 6 ->
                        error = "La contraseña debe tener al menos 6 caracteres"
                    else -> {
                        cargando = true
                        error = ""
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { result ->
                                val uid = result.user?.uid ?: return@addOnSuccessListener
                                val usuario = hashMapOf(
                                    "nombre" to nombre.trim(),
                                    "rol" to rol,
                                    "creadoEn" to com.google.firebase.Timestamp.now()
                                )
                                db.collection("usuarios").document(uid)
                                    .set(usuario)
                                    .addOnSuccessListener { onRegistroExitoso() }
                                    .addOnFailureListener {
                                        cargando = false
                                        error = "Error al guardar el usuario"
                                    }
                            }
                            .addOnFailureListener {
                                cargando = false
                                error = "Este email ya está registrado"
                            }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KoskingRed),
            enabled = !cargando
        ) {
            if (cargando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Crear cuenta", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
