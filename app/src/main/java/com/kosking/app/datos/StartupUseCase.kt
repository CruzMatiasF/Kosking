package com.kosking.app.datos

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withTimeoutOrNull

sealed class StartupDestino {
    object PanelEmpleado : StartupDestino()
    object PanelDueno : StartupDestino()
}

object StartupUseCase {

    suspend fun resolver(
        context: Context,
        empleadoTieneSession: Boolean,
        duenoLocalIdFlow: Flow<String>,
        duenoCargandoInicialFlow: Flow<Boolean> = flowOf(false)
    ): StartupDestino? {
        return when {
            empleadoTieneSession -> StartupDestino.PanelEmpleado
            else -> {
                val localId = resolverLocalId(duenoLocalIdFlow) ?: return null
                // Esperar que los datos iniciales lleguen antes de mostrar el panel
                withTimeoutOrNull(8_000L) {
                    duenoCargandoInicialFlow.filter { !it }.first()
                }
                StartupDestino.PanelDueno
            }
        }
    }

    private suspend fun resolverLocalId(flow: Flow<String>): String? {
        if (FirebaseAuth.getInstance().currentUser == null) return null
        return withTimeoutOrNull(10_000L) {
            flow.filter { it.isNotBlank() }.first()
        }
    }
}
