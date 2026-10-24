package com.kosking.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.modelo.StockItemConTienda
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    val kioscos: StateFlow<List<Tienda>> = KioscoRepository.kioscosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var productosPopulares = MutableStateFlow<List<StockItemConTienda>>(emptyList())
        private set

    var isRefreshing = MutableStateFlow(false)
        private set

    init {
        viewModelScope.launch {
            val tiendas = kioscos.filter { it.isNotEmpty() }.first()
            try {
                productosPopulares.value = KioscoRepository.stockPopular(tiendas)
            } catch (_: Exception) { }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            val tiendas = kioscos.value.ifEmpty {
                kioscos.filter { it.isNotEmpty() }.first()
            }
            try {
                productosPopulares.value = KioscoRepository.stockPopular(tiendas)
            } catch (_: Exception) { }
            isRefreshing.value = false
        }
    }
}
