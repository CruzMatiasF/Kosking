package com.kosking.app.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kosking.app.MainActivity
import com.kosking.app.R

class NotificacionDeudaWorker(
    private val ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {

    override fun doWork(): Result {
        val prefs = ctx.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE)
        val totalDeuda = prefs.getFloat("total_deuda", 0f)
        val clientesConDeuda = prefs.getInt("clientes_con_deuda", 0)
        val ultimaActualizacion = prefs.getLong("prefs_timestamp", 0L)

        if (totalDeuda <= 0 || clientesConDeuda <= 0) return Result.success()

        // Si los datos tienen más de 30 días, probablemente estén desactualizados
        val treintaDiasMs = 30L * 24 * 60 * 60 * 1000
        if (ultimaActualizacion > 0 && System.currentTimeMillis() - ultimaActualizacion > treintaDiasMs) {
            return Result.success()
        }

        val channelId = "kosking_recordatorio"
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId, "Recordatorios Kosking", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Recordatorio semanal de deudas pendientes" }
        manager.createNotificationChannel(channel)

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val totalFmt = "%.0f".format(totalDeuda)
        val texto = "$clientesConDeuda cliente${if (clientesConDeuda != 1) "s" else ""} te deben \$$totalFmt en total"

        val notif = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Deuda pendiente de cobrar 💰")
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(1002, notif)
        return Result.success()
    }
}
