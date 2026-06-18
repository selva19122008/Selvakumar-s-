package com.example.notification

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.db.TournamentEntity
import java.util.concurrent.TimeUnit

object TournamentNotificationScheduler {
    
    private const val TAG = "NotificationScheduler"

    /**
     * Schedules standard broadcast to run exactly 10 minutes (600,000 ms) before the game's start timestamp.
      If it falls within 10 minutes or past, it schedules immediately.
     */
    fun scheduleTournamentAlert(context: Context, tournament: TournamentEntity) {
        val workManager = WorkManager.getInstance(context)
        val workName = "tournament_notification_${tournament.id}"

        // If tournament is COMPLETED or has already started past threshold, drop scheduling
        if (tournament.status == "COMPLETED") {
            workManager.cancelUniqueWork(workName)
            Log.d(TAG, "Tournament #${tournament.id} is already completed. Cancelled notification queue.")
            return
        }

        // Alert target triggers exactly 10 minutes before the scheduled timestamp
        val targetTriggerThreshold = tournament.timestamp - (10 * 60 * 1000)
        val delayMillis = targetTriggerThreshold - System.currentTimeMillis()

        Log.d(TAG, "Tournament #${tournament.id} delay calculation: starting point timestamp = ${tournament.timestamp}, target = $targetTriggerThreshold, delayMillis = $delayMillis")

        // Standard delay constraint
        val initialDelay = if (delayMillis > 0) delayMillis else 0L

        val workRequest = OneTimeWorkRequestBuilder<TournamentNotificationWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("tournament") // Generic group tag for mass controls
            .setInputData(Data.Builder().putInt("TOURNAMENT_ID", tournament.id).build())
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        
        Log.d(TAG, "Enqueued unique WorkManager process for match #${tournament.id} with delay: ${initialDelay}ms")
    }

    fun cancelTournamentAlert(context: Context, tournamentId: Int) {
        val workManager = WorkManager.getInstance(context)
        val workName = "tournament_notification_$tournamentId"
        workManager.cancelUniqueWork(workName)
        Log.d(TAG, "Dropped unique background reminder job for tournament #$tournamentId")
    }
}
