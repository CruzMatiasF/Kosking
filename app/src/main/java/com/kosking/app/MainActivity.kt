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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.kosking.app.navegacion.KoskingNavHost
import com.kosking.app.notificaciones.NotificacionDeudaWorker
import com.kosking.app.notificaciones.NotificacionAcuerdosWorker
import com.kosking.app.ui.theme.KoskingTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val pedirPermisoNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* si acepta, las notificaciones ya funcionan automáticamente */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        habilitarPersistenciaOffline()
        pedirPermisoNotificacionSiNecesario()
        programarRecordatorioSemanal()
        setContent {
            KoskingTheme {
                KoskingNavHost()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun habilitarPersistenciaOffline() {
        // setPersistenceEnabled es deprecated en Firestore 25+ pero sigue funcionando
        // La API nueva (memoryCacheSettings / persistentCacheSettings) requiere SDK 24.4+
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    private fun pedirPermisoNotificacionSiNecesario() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                pedirPermisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun programarRecordatorioSemanal() {
        val wm = WorkManager.getInstance(this)
        wm.enqueueUniquePeriodicWork(
            "recordatorio_deuda_semanal",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NotificacionDeudaWorker>(7, TimeUnit.DAYS).build()
        )
        wm.enqueueUniquePeriodicWork(
            "chequeo_acuerdos_diario",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NotificacionAcuerdosWorker>(1, TimeUnit.DAYS).build()
        )
    }
}
