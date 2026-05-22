package com.urvoice.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.urvoice.app.MainActivity

class UrVoiceMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.d("UrVoice", "onNewToken: no signed-in user, token will be saved on next sign-in")
            return
        }
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("UrVoice", "FCM token refreshed for user: $uid")
            }
            .addOnFailureListener { e ->
                Log.w("UrVoice", "Failed to save FCM token: $e")
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type       = message.data["type"]
        val title      = message.notification?.title
        val body       = message.notification?.body

        val tapIntent  = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when (type) {
            "CALL_STARTED" -> {
                val notification = NotificationCompat.Builder(this, "urvoice_calls")
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setContentTitle(title ?: "Incoming Call")
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setAutoCancel(false)
                    .setContentIntent(pendingIntent)
                    .build()
                notificationManager().notify(1001, notification)
            }

            "CALL_ENDED" -> {
                val notification = NotificationCompat.Builder(this, "urvoice_updates")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setContentTitle(title ?: "Call Completed")
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
                notificationManager().notify(1002, notification)
            }
        }
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
