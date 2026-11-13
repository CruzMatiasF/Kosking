package com.kosking.app.dueno

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kosking.app.dueno.tabs.TabEquipo
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Movimiento

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaColaboradores(
    empleados: List<Empleado>,
    movimientos: List<Movimiento>,
    clientes: List<Cliente>,
    onEliminarEmpleado: (String) -> Unit,
    onVolver: () -> Unit,
    vm: DuenoViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Colaboradores") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TabEquipo(
                empleados = empleados,
                movimientos = movimientos,
                clientes = clientes,
                onEliminarEmpleado = onEliminarEmpleado,
                vm = vm
            )
        }
    }
}
