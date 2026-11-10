package com.kosking.app.autenticacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.kosking.app.util.CrashLogger
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun clearError() { _error.value = null }

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    fun login(email: String, password: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid
                if (uid != null) guardarTokenFcm(uid)
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "login")
                _error.value = "Email o contraseña incorrectos"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun registrar(nombre: String, email: String, password: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: return@launch
                val usuario = Usuario(uid = uid, nombre = nombre, rol = "dueno")
                FiadoRepository.guardarUsuario(usuario)
                guardarTokenFcm(uid)
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "registrar")
                _error.value = "No se pudo crear la cuenta"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun loginAnonimo(onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                auth.signInAnonymously().await()
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "loginAnonimo")
                _error.value = "No se pudo conectar. Revisá tu conexión"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun recuperarPassword(email: String, onExito: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "recuperarPassword")
                onError("No encontramos una cuenta con ese email")
            }
        }
    }

    private suspend fun guardarTokenFcm(uid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            FiadoRepository.guardarFcmToken(uid, token)
        } catch (e: Exception) { CrashLogger.log(e, "guardarTokenFcm") }
    }
}
