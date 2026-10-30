package com.example.practicafinal.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.practicafinal.MainActivity
import com.example.practicafinal.DetallePublicacionActivity
import com.example.practicafinal.R

object NotificacionHelper {

    private const val CHANNEL_ID = "avistamientos"
    private const val CHANNEL_NAME = "Avistamientos"
    private const val CHANNEL_DESC = "Notificaciones de avistamientos de mascotas perdidas"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun mostrarPush(
        context: Context,
        notificationId: Int,
        titulo: String,
        mensaje: String,
        publicacionId: Long,
        latitud: Double? = null,
        longitud: Double? = null,
        tipo: String = "avistamiento"
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val destino = if (tipo == "caso_resuelto") DetallePublicacionActivity::class.java
            else MainActivity::class.java
        val intent = Intent(context, destino).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (tipo == "caso_resuelto") {
                putExtra(DetallePublicacionActivity.EXTRA_PUBLICACION_ID, publicacionId)
            } else if (latitud != null && longitud != null) {
                putExtra("centrar_lat", latitud)
                putExtra("centrar_lng", longitud)
            } else {
                putExtra("mostrar_alerta", publicacionId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pin_rojo)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
