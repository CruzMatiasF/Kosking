package com.kosking.app.autenticacion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {

    var cargando by mutableStateOf(false)
        private set

    var error by mutableStateOf("")
        private set

    fun limpiarError() { error = "" }

    fun login(email: String, password: String, alExito: () -> Unit) {
        if (!validarCampos(email, password)) return
        cargando = true
        error = ""
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { alExito() }
            .addOnFailureListener { cargando = false; error = traducirError(it.message) }
    }

    fun registrar(nombre: String, email: String, password: String, alExito: () -> Unit) {
        if (nombre.isBlank()) { error = "Ingresá tu nombre"; return }
        if (!validarCampos(email, password)) return
        cargando = true
        error = ""
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user!!.uid
                FirebaseFirestore.getInstance()
                    .collection("usuarios").document(uid)
                    .set(mapOf(
                        "nombre" to nombre.trim(),
                        "email" to email.trim(),
                        "creadoAt" to FieldValue.serverTimestamp()
                    ))
                    .addOnSuccessListener { alExito() }
                    .addOnFailureListener { cargando = false; error = traducirError(it.message) }
            }
            .addOnFailureListener { cargando = false; error = traducirError(it.message) }
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
