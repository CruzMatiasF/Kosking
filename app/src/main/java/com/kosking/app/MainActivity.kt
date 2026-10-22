package com.kosking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kosking.app.ui.theme.KoskingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoskingTheme {
                var pantalla by remember { mutableStateOf("inicio") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (pantalla) {
                        "inicio" -> PantallaInicio(
                            modifier = Modifier.padding(innerPadding),
                            onDuenoClick = { pantalla = "auth_dueno" },
                            onEmpleadoClick = { pantalla = "auth_empleado" },
                            onClienteClick = { pantalla = "auth_cliente" }
                        )
                        "auth_dueno" -> AuthScreen(
                            rol = "dueno",
                            onVolver = { pantalla = "inicio" },
                            onLoginExitoso = { pantalla = "panel_dueno" }
                        )
                        "auth_empleado" -> AuthScreen(
                            rol = "empleado",
                            onVolver = { pantalla = "inicio" },
                            onLoginExitoso = { pantalla = "panel_empleado" }
                        )
                        "auth_cliente" -> AuthScreen(
                            rol = "cliente",
                            onVolver = { pantalla = "inicio" },
                            onLoginExitoso = { pantalla = "panel_cliente" }
                        )
                        "panel_dueno" -> PanelDueno(
                            modifier = Modifier.padding(innerPadding),
                            onCerrarSesion = { pantalla = "inicio" }
                        )
                        "panel_empleado" -> PanelEmpleado(
                            modifier = Modifier.padding(innerPadding),
                            onCerrarSesion = { pantalla = "inicio" }
                        )
                        "panel_cliente" -> PanelCliente(
                            modifier = Modifier.padding(innerPadding),
                            onCerrarSesion = { pantalla = "inicio" }
                        )
                    }
                }
            }
        }
    }
}
