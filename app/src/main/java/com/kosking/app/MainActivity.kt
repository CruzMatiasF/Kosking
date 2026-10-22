package com.kosking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kosking.app.navegacion.KoskingNavHost
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.KoskingTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // TODO: restaurar lógica de sesión cuando Home esté implementado
        val pantallaInicial = Rutas.LOGIN

        setContent {
            KoskingTheme {
                KoskingNavHost(inicio = pantallaInicial)
            }
        }
    }
}
