package com.kosking.app.autenticacion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    var cargando by mutableStateOf(false)
        private set

    var error by mutableStateOf("")
        private set

    private val _navegarAlHome = MutableSharedFlow<Unit>()
    val navegarAlHome = _navegarAlHome.asSharedFlow()

    fun limpiarError() { error = "" }

    fun login(email: String, password: String, alExito: () -> Unit) {
        if (!validarCampos(email, password)) return
        cargando = true
        error = ""
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email.trim(), password).await()
                cargando = false
                _navegarAlHome.emit(Unit)
            } catch (e: Exception) {
                cargando = false
                error = traducirError(e.message)
            }
        }
    }

    fun registrar(nombre: String, email: String, password: String, alExito: () -> Unit) {
        if (nombre.isBlank()) { error = "Ingresá tu nombre"; return }
        if (!validarCampos(email, password)) return
        cargando = true
        error = ""
        viewModelScope.launch {
            try {
                val resultado = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = resultado.user!!.uid
                // Fire-and-forget: no esperamos confirmación del servidor
                FirebaseFirestore.getInstance()
                    .collection("usuarios").document(uid)
                    .set(mapOf(
                        "nombre" to nombre.trim(),
                        "email" to email.trim(),
                        "creadoAt" to FieldValue.serverTimestamp()
                    ))
                cargando = false
                _navegarAlHome.emit(Unit)
            } catch (e: Exception) {
                cargando = false
                error = traducirError(e.message)
            }
        }
    }

    private fun validarCampos(email: String, password: String): Boolean {
        if (email.isBlank()) { error = "Ingresá tu email"; return false }
        if (password.length < 6) { error = "La contraseña debe tener al menos 6 caracteres"; return false }
        return true
    }

    private fun traducirError(msg: String?): String = when {
        msg == null -> "Ocurrió un error. Intentá de nuevo."
        "email address is already in use" in msg -> "Este email ya está registrado"
        "wrong-password" in msg || "invalid-credential" in msg -> "Email o contraseña incorrectos"
        "no user record" in msg -> "No existe una cuenta con ese email"
        "badly formatted" in msg -> "El email no es válido"
        "weak-password" in msg -> "La contraseña debe tener al menos 6 caracteres"
        "network" in msg -> "Sin conexión. Revisá tu internet."
        else -> "Ocurrió un error. Intentá de nuevo."
    }
}
