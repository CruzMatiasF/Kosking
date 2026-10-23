package com.kosking.app.dueno

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    val subiendoFoto = MutableStateFlow(false)

    fun subirFoto(uri: Uri) {
        val id = tienda.value?.id ?: return
        viewModelScope.launch {
            subiendoFoto.value = true
            try {
                val ref = FirebaseStorage.getInstance().reference.child("kioscos/$id/foto.jpg")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                db.collection("tiendas").document(id).update("fotoUrl", url).await()
            } catch (_: Exception) { }
            subiendoFoto.value = false
        }
    }

    fun guardar(
        nombre: String,
        direccion: String,
        horarioApertura: String,
        horarioCierre: String,
        diasActivos: String,
        telefono: String,
        categoria: String,
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
                        "horarioApertura" to horarioApertura.trim(),
                        "horarioCierre" to horarioCierre.trim(),
                        "diasActivos" to diasActivos,
                        "telefono" to telefono.trim(),
                        "categoria" to categoria.trim(),
                    )
                ).await()
                estado.value = EstadoConfig.Exito
            } catch (e: Exception) {
                estado.value = EstadoConfig.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun guardarUbicacion(latitud: Double, longitud: Double) {
        val id = tienda.value?.id ?: return
        viewModelScope.launch {
            try {
                db.collection("tiendas").document(id).update(
                    mapOf("latitud" to latitud, "longitud" to longitud)
                ).await()
            } catch (_: Exception) { }
        }
    }

    fun setOverride(abierto: Boolean) {
        val id = tienda.value?.id ?: return
        viewModelScope.launch {
            try {
                val medianoche = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time
                db.collection("tiendas").document(id).update(
                    mapOf(
                        "overrideAbierto" to abierto,
                        "overrideHasta" to com.google.firebase.Timestamp(medianoche),
                    )
                ).await()
            } catch (_: Exception) { }
        }
    }

    fun limpiarOverride() {
        val id = tienda.value?.id ?: return
        viewModelScope.launch {
            try {
                db.collection("tiendas").document(id).update(
                    mapOf(
                        "overrideAbierto" to null,
                        "overrideHasta" to null,
                    )
                ).await()
            } catch (_: Exception) { }
        }
    }
}
