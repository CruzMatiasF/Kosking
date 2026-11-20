package com.kosking.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.kosking.app.datos.PreferenciasRepository
import com.kosking.app.navegacion.KoskingNavHost
import com.kosking.app.ajustes.PantallaBloqueo
import com.kosking.app.notificaciones.NotificacionDeudaWorker
import com.kosking.app.notificaciones.NotificacionAcuerdosWorker
import com.kosking.app.notificaciones.ResumenDiaWorker
import com.kosking.app.ui.theme.KoskingTheme
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val pedirPermisoNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* si acepta, las notificaciones ya funcionan automáticamente */ }

    private val _accionWidget = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _accionWidget.value = intent.getStringExtra("accion")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        habilitarPersistenciaOffline()
        pedirPermisoNotificacionSiNecesario()
        programarNotificaciones()
        _accionWidget.value = intent?.getStringExtra("accion")
        setContent {
            val context = this
            var tema by remember { mutableStateOf(PreferenciasRepository.getTema(context)) }
            val esDark = when (tema) {
                "claro" -> false
                "oscuro" -> true
                else -> isSystemInDarkTheme()
            }
            // Bloqueo por PIN: se activa al iniciar y al volver desde segundo plano
            var bloqueado by remember {
                mutableStateOf(PreferenciasRepository.isPinHabilitado(context))
            }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP &&
                        PreferenciasRepository.isPinHabilitado(context)) {
                        bloqueado = true
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            val accion by _accionWidget
            KoskingTheme(darkTheme = esDark) {
                if (bloqueado) {
                    PantallaBloqueo(onDesbloqueado = { bloqueado = false })
                } else {
                    KoskingNavHost(
                        onTemaChanged = { tema = it },
                        abrirEscaner = accion == "escaner",
                        onEscanerConsumido = { _accionWidget.value = null }
                    )
                }
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

    private fun programarNotificaciones() {
        val wm = WorkManager.getInstance(this)
        wm.enqueueUniquePeriodicWork(
            "recordatorio_deuda",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NotificacionDeudaWorker>(2, TimeUnit.DAYS).build()
        )
        wm.enqueueUniquePeriodicWork(
            "chequeo_acuerdos_diario",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NotificacionAcuerdosWorker>(1, TimeUnit.DAYS).build()
        )
        wm.enqueueUniquePeriodicWork(
            "resumen_dia",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ResumenDiaWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayHasta21h(), TimeUnit.MILLISECONDS)
                .build()
        )
    }

    private fun delayHasta21h(): Long {
        val cal = Calendar.getInstance()
        val ahora = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 21)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= ahora) cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis - ahora
    }
}
