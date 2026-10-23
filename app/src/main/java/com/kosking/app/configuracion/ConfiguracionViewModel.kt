package com.kosking.app.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class RolActual {
    object Cargando : RolActual()
    object Cliente : RolActual()
    data class Dueno(val tienda: Tienda) : RolActual()
    data class Empleado(val tiendaId: String, val tiendaNombre: String) : RolActual()
}

sealed class AccionRol {
    object Idle : AccionRol()
    object Procesando : AccionRol()
    object Exito : AccionRol()
    data class Error(val mensaje: String) : AccionRol()
}

class ConfiguracionViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Todos los kioscos en tiempo real
    private val kioscos: StateFlow<List<Tienda>> = KioscoRepository.kioscosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rolActual = MutableStateFlow<RolActual>(RolActual.Cargando)
    val accionRol = MutableStateFlow<AccionRol>(AccionRol.Idle)

    init {
        detectarRol()
    }

    private fun detectarRol() {
        viewModelScope.launch {
            if (uid.isBlank()) { rolActual.value = RolActual.Cliente; return@launch }
            try {
                // ¿Es dueño de algún local?
                kioscos.collect { lista ->
                    val tiendaPropia = lista.find { it.duenioUid == uid }
                    if (tiendaPropia != null) {
                        rolActual.value = RolActual.Dueno(tiendaPropia)
                        return@collect
                    }
                    // ¿Es empleado?
                    val doc = db.collection("usuarios").document(uid).get().await()
                    val empleadoEnId = doc.getString("empleadoEnTiendaId")
                    if (!empleadoEnId.isNullOrBlank()) {
                        val tiendaNombre = lista.find { it.id == empleadoEnId }?.nombre ?: "Local"
                        rolActual.value = RolActual.Empleado(empleadoEnId, tiendaNombre)
                    } else {
                        rolActual.value = RolActual.Cliente
                    }
                }
            } catch (_: Exception) {
                rolActual.value = RolActual.Cliente
            }
        }
    }

    fun desvincularseComoEmpleado() {
        viewModelScope.launch {
            accionRol.value = AccionRol.Procesando
            try {
                db.collection("usuarios").document(uid)
                    .update("empleadoEnTiendaId", null).await()
                rolActual.value = RolActual.Cliente
                accionRol.value = AccionRol.Exito
            } catch (e: Exception) {
                accionRol.value = AccionRol.Error("No se pudo desvincular: ${e.message?.take(60)}")
            }
        }
    }

    fun resetAccion() { accionRol.value = AccionRol.Idle }
}
