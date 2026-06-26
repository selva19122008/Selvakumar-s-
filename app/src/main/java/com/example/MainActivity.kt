package com.example

import android.os.Bundle
import android.content.Context
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.db.AppDatabase
import com.example.db.BattleZoneRepository
import com.example.ui.BattleZoneMainApp
import com.example.ui.BattleZoneViewModel
import com.example.ui.BattleZoneViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notifications permission on Android 13+ (API 33)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        // Query the current Firebase Cloud Messaging Token on startup
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    android.util.Log.d("MainActivity", "Successfully retrieved current FCM token: $token")
                    val sharedPrefs = getSharedPreferences("payment_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().putString("fcm_token", token).apply()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to retrieve FCM token during launch initialization", e)
        }

        // 1. Initialize DB and Repository instance
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = BattleZoneRepository(db)
        
        // 2. Build our main ViewModel
        val factory = BattleZoneViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[BattleZoneViewModel::class.java]
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BattleZoneMainApp(viewModel = viewModel)
            }
        }
    }
}
