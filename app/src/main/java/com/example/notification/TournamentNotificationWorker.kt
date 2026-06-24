package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class TournamentNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db by lazy { AppDatabase.getDatabase(appContext) }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val tournamentId = inputData.getInt("TOURNAMENT_ID", -1)
        if (tournamentId == -1) {
            return@withContext Result.failure()
        }

        val tournament = db.tournamentDao().getTournamentSync(tournamentId)
            ?: return@withContext Result.success()

        // Only send alerts for incomplete/upcoming matches
        if (tournament.status == "COMPLETED") {
            return@withContext Result.success()
        }

        val joins = db.joinDao().getJoinsForTournamentSync(tournamentId)
        if (joins.isEmpty()) {
            Log.d("NotificationWorker", "No participants registered in tournament $tournamentId to notify.")
            return@withContext Result.success()
        }

        val roomId = tournament.roomId ?: "RELEASING_SOON"
        val roomPass = tournament.roomPassword ?: "RELEASING_SOON"
        val messageBody = "BattleZone Alert: Custom Room Credentials for Match #${tournament.id} (${tournament.title}) are out!\nRoom ID: $roomId\nPassword: $roomPass\nDo not share these credentials. Open Free Fire now!"

        // 1. Dispatch a local status bar notification so developers/testers see the trigger in action!
        showSystemNotification(
            "BattleZone Automatic Broadcast Trigger",
            "Sent match notification alert and credentials to ${joins.size} registerees for match #${tournament.id} (${tournament.title})."
        )

        // 2. Fetch SMS integrations preferences
        val sharedPrefs = appContext.getSharedPreferences("payment_prefs", Context.MODE_PRIVATE)
        val mode = sharedPrefs.getString("sms_gateway_mode", "TEST_MODE") ?: "TEST_MODE"

        Log.d("NotificationWorker", "Processing background logic SMS alerts via $mode for Match #${tournament.id}")

        val client = OkHttpClient()

        joins.forEach { join ->
            // Insert in-app database notification alert
            try {
                db.notificationDao().insertNotification(
                    com.example.db.NotificationEntity(
                        userId = join.userId,
                        title = "⏰ Tournament Starting Soon!",
                        message = "Your registered tournament \"${tournament.title}\" is starting in 10 minutes! Custom room credentials (Room ID: ${tournament.roomId ?: "RELEASING_SOON"}, Password: ${tournament.roomPassword ?: "RELEASING_SOON"}) are ready.",
                        type = "MATCH_START",
                        tournamentId = tournament.id
                    )
                )
            } catch (e: Exception) {
                Log.e("NotificationWorker", "Failed to save start-alert notification to database for user ${join.userId}", e)
            }

            val participant = db.userDao().getUserSync(join.userId)
            val phone = participant?.phoneNumber ?: participant?.extraMobileNumber
            if (participant != null && !phone.isNullOrBlank()) {
                if (mode == "TEST_MODE" || mode.isBlank()) {
                    Log.i("NotificationWorker", "[SIMULATED BACKGROUND BROADCAST SMS] To: $phone, Message: $messageBody")
                } else {
                    try {
                        var request: Request? = null
                        when (mode) {
                            "FAST2SMS" -> {
                                val apiKey = sharedPrefs.getString("fast2sms_api_key", "") ?: ""
                                if (apiKey.isNotBlank()) {
                                    val cleanPhone = phone.replace("+91", "").replace("+", "").replace(" ", "").trim()
                                    val url = "https://www.fast2sms.com/dev/bulkV2?authorization=$apiKey&message=${URLEncoder.encode(messageBody, "UTF-8")}&language=english&route=q&numbers=$cleanPhone"
                                    request = Request.Builder().url(url).get().build()
                                }
                            }
                            "TWILIO" -> {
                                val sid = sharedPrefs.getString("twilio_sid", "") ?: ""
                                val token = sharedPrefs.getString("twilio_token", "") ?: ""
                                val twilioPhoneNum = sharedPrefs.getString("twilio_phone", "") ?: ""
                                if (sid.isNotBlank() && token.isNotBlank() && twilioPhoneNum.isNotBlank()) {
                                    val credentials = android.util.Base64.encodeToString("$sid:$token".toByteArray(), android.util.Base64.NO_WRAP)
                                    val formBody = FormBody.Builder()
                                        .add("To", phone.trim())
                                        .add("From", twilioPhoneNum.trim())
                                        .add("Body", messageBody)
                                        .build()
                                    val url = "https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json"
                                    request = Request.Builder()
                                        .url(url)
                                        .header("Authorization", "Basic $credentials")
                                        .post(formBody)
                                        .build()
                                }
                            }
                            "CUSTOM_HTTP_API" -> {
                                val customUrl = sharedPrefs.getString("custom_sms_url", "") ?: ""
                                if (customUrl.isNotBlank()) {
                                    val cleanPhone = phone.replace("+", "").replace(" ", "").trim()
                                    val builtUrl = customUrl
                                        .replace("{phone}", cleanPhone)
                                        .replace("{otp}", URLEncoder.encode(messageBody, "UTF-8"))
                                    request = Request.Builder().url(builtUrl).get().build()
                                }
                            }
                        }

                        if (request != null) {
                            val response = client.newCall(request).execute()
                            Log.d("NotificationWorker", "Background notification dispatched to $phone with response code ${response.code}")
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationWorker", "Failed to dispatch SMS through background logic flow", e)
                    }
                }
            }
        }

        Result.success()
    }

    private fun showSystemNotification(title: String, text: String) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "battlezone_match_reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Match reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Custom room credentials delivery and starting cues"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
