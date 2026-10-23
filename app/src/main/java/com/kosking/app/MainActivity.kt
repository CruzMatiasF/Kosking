package com.kosking.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.kosking.app.datos.HistorialRepository
import com.kosking.app.navegacion.KoskingNavHost
import com.kosking.app.navegacion.Rutas
import com.kosking.app.notificaciones.KoskingMessagingService
import com.kosking.app.ui.theme.KoskingTheme

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* el usuario decidió — no forzamos nada */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        HistorialRepository.init(this)
        pedirPermisoNotificaciones()
        guardarTokenFCM()
        enableEdgeToEdge()

        val pantallaInicial = if (FirebaseAuth.getInstance().currentUser != null) {
            Rutas.HOME
        } else {
            Rutas.LOGIN
        }

        setContent {
            KoskingTheme {
                KoskingNavHost(inicio = pantallaInicial)
            }
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun guardarTokenFCM() {
        if (FirebaseAuth.getInstance().currentUser == null) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            KoskingMessagingService.guardarToken(token)
        }
    }
}
