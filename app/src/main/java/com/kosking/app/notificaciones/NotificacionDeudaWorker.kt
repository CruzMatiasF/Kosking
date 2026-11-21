package com.kosking.app.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.MainActivity
import com.kosking.app.R
import com.kosking.app.modelo.Cliente
import kotlinx.coroutines.tasks.await

class NotificacionDeudaWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val prefs = ctx.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE)
        val localId = prefs.getString("local_id", "") ?: ""
        if (localId.isBlank()) return Result.success()

        return try {
            val snap = FirebaseFirestore.getInstance()
                .collection("clientes")
                .whereEqualTo("localId", localId)
                .limit(500)
                .get()
                .await()

            val clientes = snap.toObjects(Cliente::class.java)
            val conDeuda = clientes.filter { it.deudaTotal > 0 }
            if (conDeuda.isEmpty()) return Result.success()

            val totalDeuda = conDeuda.sumOf { it.deudaTotal }
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel("kosking_recordatorio", "Recordatorios Kosking", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Recordatorio de deudas pendientes" }
            )

            val totalFmt = "%,.0f".format(totalDeuda)
            val texto = "${conDeuda.size} cliente${if (conDeuda.size != 1) "s" else ""} te deben \$$totalFmt en total"

            val notif = NotificationCompat.Builder(ctx, "kosking_recordatorio")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Deuda pendiente de cobrar 💰")
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        ctx, 1002,
                        Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP },
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            manager.notify(1002, notif)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
