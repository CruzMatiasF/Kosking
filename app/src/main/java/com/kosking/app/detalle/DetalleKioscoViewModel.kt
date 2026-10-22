package com.kosking.app.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.modelo.Oferta
import com.kosking.app.modelo.StockItem
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetalleKioscoViewModel(private val tiendaId: String) : ViewModel() {

    val stock: StateFlow<List<StockItem>> = KioscoRepository.stockFlow(tiendaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ofertas: StateFlow<List<Oferta>> = KioscoRepository.ofertasFlow(tiendaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _tienda = MutableStateFlow<Tienda?>(null)
    val tienda: StateFlow<Tienda?> = _tienda

    init {
        viewModelScope.launch {
            KioscoRepository.kioscosFlow().collect { lista ->
                _tienda.value = lista.find { it.id == tiendaId }
            }
        }
    }

    class Factory(private val tiendaId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DetalleKioscoViewModel(tiendaId) as T
        }
    }
}
