package com.kosking.app.cliente

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PantallaVerFiado(
    onVolver: () -> Unit,
    vm: ClienteViewModel = viewModel()
) {
    val localesGuardados by vm.localesGuardados.collectAsState()
    val deudasPorLocal by vm.deudasPorLocal.collectAsState()
    val movimientos by vm.movimientos.collectAsState()
    val cliente by vm.cliente.collectAsState()
    val productos by vm.productos.collectAsState()
    val localInfo by vm.localInfo.collectAsState()

    var localAbiertoId by remember { mutableStateOf<String?>(null) }
    var mostrarDialogConectar by remember { mutableStateOf(false) }
    val conectarCargando by vm.cargando.collectAsState()
    val conectarError by vm.error.collectAsState()

    LaunchedEffect(mostrarDialogConectar) {
        if (mostrarDialogConectar) vm.clearError()
    }

    val localAbierto = localesGuardados.firstOrNull { it.localId == localAbiertoId }

    BackHandler(enabled = localAbierto != null) {
        localAbiertoId = null
    }

    val refrescando by vm.refrescando.collectAsState()

    if (localAbierto != null) {
        LaunchedEffect(localAbierto.localId) { vm.seleccionarLocal(localAbierto.localId) }
        DetalleFiado(
            local = localAbierto,
            movimientos = movimientos,
            cliente = cliente,
            productos = productos,
            localInfo = localInfo,
            onVolver = { localAbiertoId = null },
            onMarcarDisputado = { vm.marcarDisputado(it) }
        )
    } else {
        ListaLocales(
            locales = localesGuardados,
            deudasPorLocal = deudasPorLocal,
            onSeleccionar = { localAbiertoId = it.localId },
            onQuitarLocal = { vm.quitarLocal(it.localId) },
            onVolver = onVolver,
            onConectarme = { mostrarDialogConectar = true },
            isRefrescando = refrescando,
            onRefrescar = { vm.refrescar() }
        )
    }

    if (mostrarDialogConectar) {
        DialogConectarComoCliente(
            cargando = conectarCargando,
            error = conectarError,
            onConectar = { codigo ->
                vm.verificarPorCodigoAcceso(codigo) { mostrarDialogConectar = false }
            },
            onDismiss = { mostrarDialogConectar = false }
        )
    }
}
