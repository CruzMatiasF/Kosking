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
import com.kosking.app.modelo.Venta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class PanelDuenoViewModel : ViewModel() {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // La tienda del dueño logueado
    val tienda: StateFlow<Tienda?> = KioscoRepository.kioscosFlow()
        .map { lista -> lista.find { it.duenioUid == uid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // tiendaId derivado de tienda
    private val tiendaId: StateFlow<String> = tienda
        .map { it?.id ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val stock: StateFlow<List<StockItem>> = tiendaId
        .flatMapLatest { id -> if (id.isBlank()) flowOf(emptyList()) else KioscoRepository.stockFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ofertas: StateFlow<List<Oferta>> = tiendaId
        .flatMapLatest { id -> if (id.isBlank()) flowOf(emptyList()) else KioscoRepository.ofertasFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ventas: StateFlow<List<Venta>> = tiendaId
        .flatMapLatest { id -> if (id.isBlank()) flowOf(emptyList()) else KioscoRepository.ventasFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Suma del total de ventas de hoy
    val ventasHoy: StateFlow<Double> = ventas
        .map { lista ->
            val hoy = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            lista.filter { v -> v.fecha?.toDate()?.after(hoy) == true }.sumOf { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Suma del total de ventas de esta semana
    val ventasSemana: StateFlow<Double> = ventas
        .map { lista ->
            val semana = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -7)
            }.time
            lista.filter { v -> v.fecha?.toDate()?.after(semana) == true }.sumOf { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Stock con cantidad baja (≤ 3)
    val stockBajo: StateFlow<Int> = stock
        .map { lista -> lista.count { it.cantidad <= 3 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val db = FirebaseFirestore.getInstance()

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
                        "overrideHasta" to Timestamp(medianoche),
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
