package com.kosking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun PantallaInicio(
    modifier: Modifier = Modifier,
    onDuenoClick: () -> Unit,
    onEmpleadoClick: () -> Unit,
    onClienteClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Kosking",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE63946)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "El barrio en tu bolsillo",
            fontSize = 16.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "¿Cómo querés usar Kosking?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDuenoClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE63946)
            )
        ) {
            Text(
                text = "Soy dueño",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onEmpleadoClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE63946))
        ) {
            Text(
                text = "Soy empleado",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE63946)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onClienteClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE63946))
        ) {
            Text(
                text = "Soy cliente",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE63946)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaInicioPreview() {
    KoskingTheme {
        PantallaInicio(onDuenoClick = {}, onEmpleadoClick = {}, onClienteClick = {})
    }
}
