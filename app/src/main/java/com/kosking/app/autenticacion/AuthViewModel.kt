package com.kosking.app.autenticacion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.messaging.FirebaseMessaging
import com.kosking.app.util.CrashLogger
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = FirebaseAuth.getInstance()
    private val prefs = app.getSharedPreferences("kosking_auth", android.content.Context.MODE_PRIVATE)

    val ultimoEmail: String get() = prefs.getString("ultimo_email", "") ?: ""

    private fun guardarEmail(email: String) {
        prefs.edit().putString("ultimo_email", email).apply()
    }

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
                guardarEmail(email.trim())
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "login")
                _error.value = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Contraseña incorrecta"
                    is FirebaseAuthInvalidUserException -> "No encontramos una cuenta con ese email"
                    else -> "No se pudo conectar. Revisá tu internet e intentá de nuevo"
                }
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
                _error.value = when (e) {
                    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese email. ¿Querés iniciar sesión?"
                    is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil. Usá al menos 6 caracteres"
                    is FirebaseAuthInvalidCredentialsException -> "El email no es válido"
                    else -> "No se pudo crear la cuenta. Revisá tu internet e intentá de nuevo"
                }
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
