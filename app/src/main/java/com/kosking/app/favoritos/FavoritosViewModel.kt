package com.kosking.app.favoritos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kosking.app.datos.FavoritosRepository
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritosViewModel : ViewModel() {

    val favoritos: StateFlow<List<Tienda>> =
        combine(KioscoRepository.kioscosFlow(), FavoritosRepository.favoritosFlow()) { kioscos, ids ->
            kioscos.filter { it.id in ids }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun quitar(tiendaId: String) {
        viewModelScope.launch { FavoritosRepository.quitar(tiendaId) }
    }
}
