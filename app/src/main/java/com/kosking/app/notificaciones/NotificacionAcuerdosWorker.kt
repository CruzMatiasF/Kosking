package com.kosking.app.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.MainActivity
import com.kosking.app.R
import com.kosking.app.modelo.Cliente

class NotificacionAcuerdosWorker(
    private val ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {

    override fun doWork(): Result {
        val prefs = ctx.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE)
        val localId = prefs.getString("local_id", "") ?: ""
        if (localId.isBlank()) return Result.success()

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        crearCanales(manager)

        val ahora = Timestamp.now()

        return try {
            val snap = FirebaseFirestore.getInstance()
                .collection("clientes")
                .whereEqualTo("localId", localId)
                .get()
                .result ?: return Result.retry()

            val clientes = snap.toObjects(Cliente::class.java)

            // Acuerdos vencidos
            val vencidos = clientes.filter { c ->
                c.fechaCompromiso != null && c.fechaCompromiso.seconds < ahora.seconds && c.deudaTotal > 0
            }
            if (vencidos.isNotEmpty()) {
                val texto = if (vencidos.size == 1) {
                    val deuda = "%.0f".format(vencidos[0].deudaTotal)
                    "${vencidos[0].nombre} no pagó su acuerdo — debe $$deuda"
                } else {
                    "${vencidos.size} clientes no cumplieron su acuerdo de pago"
                }
                manager.notify(2001, buildNotif(
                    channelId = "kosking_acuerdos",
                    titulo = "Acuerdo vencido ⚠️",
                    texto = texto
                ))
            }

            // Sobre el límite de crédito
            val sobreLimite = clientes.filter { c ->
                c.limiteCredito > 0 && c.deudaTotal > c.limiteCredito
            }
            if (sobreLimite.isNotEmpty()) {
                val texto = if (sobreLimite.size == 1) {
                    "${sobreLimite[0].nombre} superó su límite de crédito"
                } else {
                    "${sobreLimite.size} clientes superaron su límite de crédito"
                }
                manager.notify(2002, buildNotif(
                    channelId = "kosking_limites",
                    titulo = "Límite superado 🔴",
                    texto = texto
                ))
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun crearCanales(manager: NotificationManager) {
        manager.createNotificationChannel(
            NotificationChannel("kosking_acuerdos", "Acuerdos vencidos", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Clientes que no cumplieron su acuerdo de pago" }
        )
        manager.createNotificationChannel(
            NotificationChannel("kosking_limites", "Límites de crédito", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Clientes que superaron su límite de crédito" }
        )
    }

    private fun buildNotif(channelId: String, titulo: String, texto: String) =
        NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    ctx, channelId.hashCode(),
                    Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
}
