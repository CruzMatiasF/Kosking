package com.kosking.app.autenticacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    val error = MutableStateFlow<String?>(null)
    val cargando = MutableStateFlow(false)

    fun login(email: String, password: String, onExito: () -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                onExito()
            } catch (e: Exception) {
                error.value = "Email o contraseña incorrectos"
            } finally {
                cargando.value = false
            }
        }
    }

    fun registrar(nombre: String, email: String, password: String, onExito: () -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: return@launch
                val usuario = Usuario(uid = uid, nombre = nombre, rol = "dueno")
                FiadoRepository.guardarUsuario(usuario)
                onExito()
            } catch (e: Exception) {
                error.value = "No se pudo crear la cuenta"
            } finally {
                cargando.value = false
            }
        }
    }
}
