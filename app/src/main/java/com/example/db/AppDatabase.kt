package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        TournamentEntity::class,
        TournamentJoinEntity::class,
        TransactionEntity::class,
        WithdrawalRequestEntity::class,
        SupportTicketEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun joinDao(): JoinDao
    abstract fun transactionDao(): TransactionDao
    abstract fun withdrawalDao(): WithdrawalDao
    abstract fun supportDao(): SupportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "battlezone_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
