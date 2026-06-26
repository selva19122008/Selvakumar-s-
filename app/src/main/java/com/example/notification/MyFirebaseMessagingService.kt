package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.db.AppDatabase
import com.example.db.NotificationEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        
        // Save token locally in SharedPreferences for later upload on user login
        val sharedPrefs = getSharedPreferences("payment_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("fcm_token", token).apply()

        // If the user is currently logged in, update the token in Firestore immediately
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            updateTokenInFirestore(currentUser.uid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "BattleZone Announcement"
        val messageBody = data["message"] ?: remoteMessage.notification?.body ?: "New update available!"
        val type = data["type"] ?: "GENERAL"
        val tournamentIdString = data["tournamentId"]
        val tournamentId = tournamentIdString?.toIntOrNull()

        Log.d(TAG, "Message Notification Title: $title")
        Log.d(TAG, "Message Notification Body: $messageBody")
        Log.d(TAG, "Message Notification Type: $type")

        // Display standard system notification
        sendNotification(title, messageBody)

        // Also save to in-app notification center database so users see it in their inbox!
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "GLOBAL"
        scope.launch {
            try {
                db.notificationDao().insertNotification(
                    NotificationEntity(
                        userId = currentUserId,
                        title = title,
                        message = messageBody,
                        type = type,
                        tournamentId = tournamentId
                    )
                )
                Log.d(TAG, "Saved FCM notification to Room local database successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save FCM notification to database", e)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "battlezone_fcm_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BattleZone FCM Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts, tournament registrations and match countdowns"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun updateTokenInFirestore(userId: String, token: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "User FCM Token updated successfully in Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating User FCM Token in Firestore", e)
            }
    }

    companion object {
        private const val TAG = "BattleZoneFCM"
    }
}
