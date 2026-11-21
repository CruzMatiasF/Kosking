package com.kosking.app.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.MainActivity
import com.kosking.app.R
import com.kosking.app.modelo.Movimiento
import java.util.Calendar

class ResumenDiaWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val prefs = ctx.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE)
        val localId = prefs.getString("local_id", "") ?: ""
        if (localId.isBlank()) return Result.success()

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel("kosking_resumen_dia", "Resumen del día", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Resumen diario de actividad del local" }
        )

        val inicioDia = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return try {
            val snap = FirebaseFirestore.getInstance()
                .collection("movimientos")
                .whereEqualTo("localId", localId)
                .whereEqualTo("anulado", false)
                .whereGreaterThanOrEqualTo("fecha", Timestamp(inicioDia / 1000, 0))
                .get()
                .await()

            val movimientos = snap.toObjects(Movimiento::class.java)
            if (movimientos.isEmpty()) return Result.success()

            var totalFiado = 0.0; var totalCobrado = 0.0
            movimientos.forEach { if (it.tipo == "cargo") totalFiado += it.monto else totalCobrado += it.monto }
            val totalDeuda = prefs.getFloat("total_deuda", 0f).toDouble()
            val clientesConDeuda = prefs.getInt("clientes_con_deuda", 0)

            val partes = mutableListOf<String>()
            if (totalFiado > 0) partes.add("Fiaste \$${"%,.0f".format(totalFiado)}")
            if (totalCobrado > 0) partes.add("cobraste \$${"%,.0f".format(totalCobrado)}")

            if (partes.isEmpty()) return Result.success()

            val resumen = partes.joinToString(" · ")
            val deudaTexto = if (totalDeuda > 0 && clientesConDeuda > 0)
                "\nDeuda total: \$${"%,.0f".format(totalDeuda)} ($clientesConDeuda cliente${if (clientesConDeuda != 1) "s" else ""})"
            else ""

            val texto = "$resumen$deudaTexto"

            val notif = NotificationCompat.Builder(ctx, "kosking_resumen_dia")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Resumen de hoy 📊")
                .setContentText(resumen)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        ctx, 3001,
                        Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP },
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            manager.notify(3001, notif)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
