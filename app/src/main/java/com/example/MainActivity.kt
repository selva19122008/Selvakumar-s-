package com.example

import android.os.Bundle
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
