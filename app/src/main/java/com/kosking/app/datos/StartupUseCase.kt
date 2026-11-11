package com.kosking.app.datos

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

sealed class StartupDestino {
    object PanelEmpleado : StartupDestino()
    object PanelDueno : StartupDestino()
    data class PanelDuenoConTab(val tab: Int) : StartupDestino()
}

object StartupUseCase {

    suspend fun resolver(
        context: Context,
        empleadoTieneSession: Boolean,
        duenoLocalIdFlow: Flow<String>
    ): StartupDestino? {
        return when {
            empleadoTieneSession -> StartupDestino.PanelEmpleado
            else -> {
                val localId = resolverLocalId(duenoLocalIdFlow)
                if (localId != null) StartupDestino.PanelDueno else null
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
