package com.kosking.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.datos.ProductoRepository
import com.kosking.app.modelo.Producto
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    val kioscos: StateFlow<List<Tienda>> = KioscoRepository.kioscosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var productosPopulares = kotlinx.coroutines.flow.MutableStateFlow<List<Producto>>(emptyList())
        private set

    var cargando = androidx.compose.runtime.mutableStateOf(false)
        private set

    init {
        cargarProductosPopulares()
    }

    private fun cargarProductosPopulares() {
        viewModelScope.launch {
            cargando.value = true
            try {
                productosPopulares.value = ProductoRepository.productosPopulares()
            } catch (_: Exception) { }
            cargando.value = false
        }
    }
}
