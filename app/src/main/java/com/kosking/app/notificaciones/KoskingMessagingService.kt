package com.kosking.app.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kosking.app.MainActivity
import com.kosking.app.R

class KoskingMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        guardarToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val titulo = message.notification?.title ?: message.data["titulo"] ?: "Kosking"
        val cuerpo = message.notification?.body ?: message.data["cuerpo"] ?: ""
        mostrarNotificacion(titulo, cuerpo)
    }

    private fun mostrarNotificacion(titulo: String, cuerpo: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val canal = NotificationChannel(
            CANAL_ID,
            "Notificaciones Kosking",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Avisos de kioscos y ofertas" }
        manager.createNotificationChannel(canal)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notif)
    }

    companion object {
        const val CANAL_ID = "kosking_general"

        fun guardarToken(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .update("fcmToken", token)
                .addOnFailureListener {
                    // Si el doc no existe todavía, lo creamos con set merge
                    FirebaseFirestore.getInstance()
                        .collection("usuarios").document(uid)
                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                }
        }
    }
}
