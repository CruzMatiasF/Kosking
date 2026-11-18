package com.kosking.app.empleado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.util.CrashLogger
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Empleado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class)
class EmpleadoViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun clearError() { _error.value = null }
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _localId = MutableStateFlow("")
    val localId: StateFlow<String> = _localId

    private val _nombreLocal = MutableStateFlow("")
    val nombreLocal: StateFlow<String> = _nombreLocal

    private val _nombreEmpleado = MutableStateFlow("")
    val nombreEmpleado: StateFlow<String> = _nombreEmpleado

    val tieneSession: StateFlow<Boolean> = _localId
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uid = MutableStateFlow("")

    val puedeVerDeuda: StateFlow<Boolean> = combine(_uid, _localId) { uid, localId ->
        Pair(uid, localId)
    }.flatMapLatest { (uid, localId) ->
        if (uid.isNotBlank() && localId.isNotBlank())
            FiadoRepository.empleadoFlowPorUid(uid, localId).map { it?.puedeVerDeuda ?: false }
        else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        val user = fa.currentUser
        if (user != null && !user.isAnonymous) {
            viewModelScope.launch { cargarEmpleoDeFirestore(user.uid) }
        } else {
            _uid.value = ""
            _localId.value = ""
            _nombreLocal.value = ""
            _nombreEmpleado.value = ""
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    private suspend fun cargarEmpleoDeFirestore(uid: String) {
        try {
            _uid.value = uid
            val empleo = FiadoRepository.obtenerEmpleoDeUsuario(uid)
            if (empleo != null) {
                _localId.value = empleo.first
                _nombreLocal.value = empleo.second
            }
            val usuario = FiadoRepository.obtenerUsuario(uid)
            _nombreEmpleado.value = usuario?.nombre ?: auth.currentUser?.displayName ?: ""
        } catch (e: Exception) { CrashLogger.log(e, "cargarEmpleoDeFirestore") }
    }

    fun verificarCodigo(codigo: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val user = auth.currentUser
                if (user == null || user.isAnonymous) {
                    _error.value = "Necesitás iniciar sesión para ser empleado"
                    return@launch
                }
                val uid = user.uid

                val localId = FiadoRepository.verificarYUsarCodigoEmpleado(codigo, uid)
                if (localId == null) {
                    _error.value = "Código incorrecto o ya usado"
                    return@launch
                }

                val local = FiadoRepository.obtenerLocal(localId)
                val usuario = FiadoRepository.obtenerUsuario(uid)
                val nombreEmpleado = listOf(usuario?.nombre, user.displayName)
                    .firstOrNull { !it.isNullOrBlank() } ?: "Empleado"

                FiadoRepository.agregarEmpleado(Empleado(localId = localId, uid = uid, nombre = nombreEmpleado))
                FiadoRepository.guardarEmpleoEnUsuario(uid, localId, local?.nombre ?: "")

                _localId.value = localId
                _nombreLocal.value = local?.nombre ?: ""
                _nombreEmpleado.value = nombreEmpleado

                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "verificarCodigo empleado")
                _error.value = "No se pudo verificar el código. Intentá de nuevo"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) FiadoRepository.limpiarEmpleoEnUsuario(uid)
            } catch (e: Exception) { CrashLogger.log(e, "cerrarSesion") }
            _localId.value = ""
            _nombreLocal.value = ""
            _nombreEmpleado.value = ""
            auth.signOut()
        }
    }

    fun actualizarMiNombre(nombre: String) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                FiadoRepository.actualizarNombreEmpleadoPorUid(uid, _localId.value, nombre)
                _nombreEmpleado.value = nombre
            } catch (e: Exception) {
                CrashLogger.log(e, "actualizarMiNombre")
                _error.value = "No se pudo actualizar el nombre"
            }
        }
    }

    fun desvincularme(onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val localId = _localId.value
                FiadoRepository.eliminarEmpleadoPorUid(uid, localId)
                FiadoRepository.limpiarEmpleoEnUsuario(uid)
                _localId.value = ""
                _nombreLocal.value = ""
                _nombreEmpleado.value = ""
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "desvincularme")
                _error.value = "No se pudo desvincular"
            } finally {
                _cargando.value = false
            }
        }
    }

}
