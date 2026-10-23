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

sealed class EstadoAgregar {
    object Idle : EstadoAgregar()
    object Guardando : EstadoAgregar()
    object Exito : EstadoAgregar()
    data class Error(val mensaje: String) : EstadoAgregar()
}

class AgregarProductoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val tienda: StateFlow<Tienda?> = KioscoRepository.kioscosFlow()
        .map { lista -> lista.find { it.duenioUid == uid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val estado = MutableStateFlow<EstadoAgregar>(EstadoAgregar.Idle)

    fun agregar(nombre: String, marca: String, precio: Double, cantidad: Int, categoria: String) {
        val id = tienda.value?.id ?: return
        viewModelScope.launch {
            estado.value = EstadoAgregar.Guardando
            try {
                val productoId = nombre.trim().lowercase().replace(" ", "_") +
                    "_" + marca.trim().lowercase().replace(" ", "_")
                val stockItem = hashMapOf(
                    "nombre" to nombre.trim(),
                    "marca" to marca.trim(),
                    "nombreBusqueda" to "${nombre.trim().lowercase()} ${marca.trim().lowercase()}",
                    "precio" to precio,
                    "cantidad" to cantidad,
                    "categoria" to categoria,
                    "updatedAt" to com.google.firebase.Timestamp.now(),
                )
                db.collection("tiendas").document(id)
                    .collection("stock").document(productoId)
                    .set(stockItem).await()
                estado.value = EstadoAgregar.Exito
            } catch (e: Exception) {
                estado.value = EstadoAgregar.Error(e.message ?: "Error al agregar")
            }
        }
    }
}
