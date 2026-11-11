package com.kosking.app.datos

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.datos.PreferenciasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

sealed class StartupDestino {
    object PanelEmpleado : StartupDestino()
    data class PanelEmpleadoConTab(val tab: Int) : StartupDestino()
    object PanelDueno : StartupDestino()
    data class PanelDuenoConTab(val tab: Int) : StartupDestino()
}

object StartupUseCase {

    suspend fun resolver(
        context: Context,
        empleadoTieneSession: Boolean,
        duenoLocalIdFlow: Flow<String>
    ): StartupDestino? {
        val pantalla = PreferenciasRepository.getPantallaInicio(context)
        return when (pantalla) {
            PreferenciasRepository.PANTALLA_EMPLEADO -> {
                if (empleadoTieneSession) StartupDestino.PanelEmpleado else null
            }
            PreferenciasRepository.PANTALLA_DUENO -> {
                val localId = resolverLocalId(duenoLocalIdFlow)
                if (localId != null) StartupDestino.PanelDueno else null
            }
            PreferenciasRepository.PANTALLA_PRODUCTOS -> {
                if (empleadoTieneSession) {
                    StartupDestino.PanelEmpleadoConTab(0)
                } else {
                    val localId = resolverLocalId(duenoLocalIdFlow)
                    if (localId != null) StartupDestino.PanelDuenoConTab(3) else null
                }
            }
            else -> {
                when {
                    empleadoTieneSession -> StartupDestino.PanelEmpleado
                    else -> {
                        val localId = resolverLocalId(duenoLocalIdFlow)
                        if (localId != null) StartupDestino.PanelDueno else null
                    }
                }
            }
        }
    }

    private suspend fun resolverLocalId(flow: Flow<String>): String? {
        if (FirebaseAuth.getInstance().currentUser == null) return null
        return withTimeoutOrNull(3000L) {
            flow.filter { it.isNotBlank() }.first()
        }
    }
}
