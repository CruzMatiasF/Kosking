package com.kosking.app.ofertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.modelo.Oferta
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class OfertaConTienda(
    val oferta: Oferta,
    val tiendaId: String,
    val tiendaNombre: String,
    val tiendaDireccion: String,
)

class OfertasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    val kioscos: StateFlow<List<Tienda>> = KioscoRepository.kioscosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ofertas = MutableStateFlow<List<OfertaConTienda>>(emptyList())
    val cargando = MutableStateFlow(false)

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            cargando.value = true
            try {
                val snap = db.collectionGroup("ofertas")
                    .whereEqualTo("activa", true)
                    .get().await()

                val tiendaMap = kioscos.value.associateBy { it.id }

                ofertas.value = snap.documents.mapNotNull { doc ->
                    val oferta = doc.toObject<Oferta>()?.copy(id = doc.id) ?: return@mapNotNull null
                    // path: tiendas/{tiendaId}/ofertas/{ofertaId}
                    val tiendaId = doc.reference.parent.parent?.id ?: return@mapNotNull null
                    val tienda = tiendaMap[tiendaId]
                    OfertaConTienda(
                        oferta = oferta,
                        tiendaId = tiendaId,
                        tiendaNombre = tienda?.nombre ?: "Local",
                        tiendaDireccion = tienda?.direccion ?: "",
                    )
                }
            } catch (_: Exception) { }
            cargando.value = false
        }
    }
}
