package com.kosking.app.dueno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.modelo.Oferta
import com.kosking.app.modelo.StockItem
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class EstadoOferta {
    object Idle : EstadoOferta()
    object Guardando : EstadoOferta()
    object Exito : EstadoOferta()
    data class Error(val mensaje: String) : EstadoOferta()
}

@OptIn(ExperimentalCoroutinesApi::class)
class MisOfertasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val tienda: StateFlow<Tienda?> = KioscoRepository.kioscosFlow()
        .map { lista -> lista.find { it.duenioUid == uid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val tiendaId: StateFlow<String> = tienda
        .map { it?.id ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Todas las ofertas (activas e inactivas)
    val ofertas: StateFlow<List<Oferta>> = tiendaId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList())
            else callbackFlow {
                val sub = db.collection("tiendas").document(id)
                    .collection("ofertas")
                    .orderBy("creadoAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snap, _ ->
                        val lista = snap?.documents?.mapNotNull { doc ->
                            doc.toObject(Oferta::class.java)?.copy(id = doc.id)
                        } ?: emptyList()
                        trySend(lista)
                    }
                awaitClose { sub.remove() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stock del kiosco para seleccionar al crear oferta
    val stock: StateFlow<List<StockItem>> = tiendaId
        .flatMapLatest { id -> if (id.isBlank()) flowOf(emptyList()) else KioscoRepository.stockFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val estado = MutableStateFlow<EstadoOferta>(EstadoOferta.Idle)

    fun crearOferta(nombreProducto: String, precioNormal: Double, precioOferta: Double, descripcion: String) {
        val id = tiendaId.value
        if (id.isBlank()) return
        viewModelScope.launch {
            estado.value = EstadoOferta.Guardando
            try {
                val oferta = hashMapOf(
                    "nombreProducto" to nombreProducto,
                    "precioNormal" to precioNormal,
                    "precioOferta" to precioOferta,
                    "descripcion" to descripcion.ifBlank { null },
                    "activa" to true,
                    "creadoAt" to Timestamp.now(),
                )
                db.collection("tiendas").document(id)
                    .collection("ofertas")
                    .add(oferta).await()
                estado.value = EstadoOferta.Exito
            } catch (e: Exception) {
                estado.value = EstadoOferta.Error(e.message ?: "Error al crear oferta")
            }
        }
    }

    fun toggleActiva(oferta: Oferta) {
        val id = tiendaId.value
        if (id.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("tiendas").document(id)
                    .collection("ofertas").document(oferta.id)
                    .update("activa", !oferta.activa).await()
            } catch (_: Exception) { }
        }
    }

    fun eliminarOferta(ofertaId: String) {
        val id = tiendaId.value
        if (id.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("tiendas").document(id)
                    .collection("ofertas").document(ofertaId)
                    .delete().await()
            } catch (_: Exception) { }
        }
    }
}
