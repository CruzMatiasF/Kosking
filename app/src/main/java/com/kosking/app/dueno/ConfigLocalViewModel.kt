package com.kosking.app.dueno

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

sealed class EstadoConfig {
    object Idle : EstadoConfig()
    object Guardando : EstadoConfig()
    object Exito : EstadoConfig()
    data class Error(val mensaje: String) : EstadoConfig()
}

class ConfigLocalViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val tienda: StateFlow<Tienda?> = KioscoRepository.kioscosFlow()
        .map { lista -> lista.find { it.duenioUid == uid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val estado = MutableStateFlow<EstadoConfig>(EstadoConfig.Idle)

    fun guardar(
        nombre: String,
        direccion: String,
        horario: String,
        telefono: String,
        categoria: String,
        abierto: Boolean,
    ) {
        val id = tienda.value?.id ?: return
        viewModelScope.launch {
            estado.value = EstadoConfig.Guardando
            try {
                val nombreBusqueda = nombre.trim().lowercase()
                db.collection("tiendas").document(id).update(
                    mapOf(
                        "nombre" to nombre.trim(),
                        "nombreBusqueda" to nombreBusqueda,
                        "direccion" to direccion.trim(),
                        "horario" to horario.trim(),
                        "telefono" to telefono.trim(),
                        "categoria" to categoria.trim(),
                        "abierto" to abierto,
                    )
                ).await()
                estado.value = EstadoConfig.Exito
            } catch (e: Exception) {
                estado.value = EstadoConfig.Error(e.message ?: "Error al guardar")
            }
        }
    }
}
