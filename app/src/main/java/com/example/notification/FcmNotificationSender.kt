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
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object FcmNotificationSender {
    private const val TAG = "FcmNotificationSender"
    private val client = OkHttpClient()

    /**
     * Subscribes the current device/token to a specific tournament topic so that
     * they will receive notifications sent to that topic.
     */
    fun subscribeToTournamentTopic(tournamentId: Int) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("tournament_$tournamentId")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Successfully subscribed to topic: tournament_$tournamentId")
                    } else {
                        Log.e(TAG, "Failed to subscribe to topic: tournament_$tournamentId", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to topic", e)
        }
    }

    /**
     * Sends or simulates an FCM notification.
     * If fcmMockMode is true, it triggers a high-fidelity local simulation (system notification + database entry).
     * If fcmMockMode is false and a server key is provided, it fires a real HTTP request to the FCM gateway.
     */
    fun sendAlert(
        context: Context,
        userId: String,
        targetToken: String?,
        topic: String?,
        title: String,
        message: String,
        type: String, // "MATCH_START", "REGISTRATION_CONFIRMED"
        tournamentId: Int?,
        mockMode: Boolean,
        serverKey: String?
    ) {
        Log.d(TAG, "sendAlert: userId=$userId, title=$title, mockMode=$mockMode")

        // 1. Always save to local database so it is saved in the user's Inbox
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.notificationDao().insertNotification(
                    NotificationEntity(
                        userId = userId,
                        title = title,
                        message = message,
                        type = type,
                        tournamentId = tournamentId
                    )
                )
                Log.d(TAG, "Notification saved to local Room DB")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification to Room DB", e)
            }
        }

        // 2. Dispatch system status bar notification
        if (mockMode || serverKey.isNullOrBlank()) {
            // Local simulation: directly trigger system notification
            Log.i(TAG, "[FCM SIMULATION ALERT] Triggering high-fidelity local notification popup.")
            showLocalSystemNotification(context, title, message)
        } else {
            // Real FCM Gateway dispatch via HTTP Legacy API
            Log.i(TAG, "[FCM REAL GATEWAY ALERT] Dispatching to FCM servers via HTTP.")
            val recipient = if (!topic.isNullOrBlank()) "/topics/$topic" else targetToken
            if (recipient.isNullOrBlank()) {
                Log.w(TAG, "No recipient token or topic specified for real FCM delivery. Falling back to local simulation.")
                showLocalSystemNotification(context, title, message)
                return
            }

            val payload = JSONObject().apply {
                put("to", recipient)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", message)
                    put("sound", "default")
                })
                put("data", JSONObject().apply {
                    put("title", title)
                    put("message", message)
                    put("type", type)
                    if (tournamentId != null) {
                        put("tournamentId", tournamentId.toString())
                    }
                    put("userId", userId)
                })
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send")
                .post(body)
                .addHeader("Authorization", "key=$serverKey")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to deliver real FCM message", e)
                    // Fallback to local system notification so user still receives the visual alert
                    showLocalSystemNotification(context, title, message)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (response.isSuccessful) {
                            Log.d(TAG, "FCM real message successfully delivered. Response code: ${response.code}")
                        } else {
                            Log.e(TAG, "FCM real message returned error. Code: ${response.code}, Response: ${response.body?.string()}")
                            // Fallback to local notification
                            showLocalSystemNotification(context, title, message)
                        }
                    }
                }
            })
        }
    }

    private fun showLocalSystemNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "battlezone_fcm_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
