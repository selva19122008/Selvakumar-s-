package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "default_user",
    val inGameName: String = "Alpha_Gamer",
    val freeFireUid: String = "FF-837492047",
    val phoneNumber: String = "+91 98765 43210",
    val email: String = "gamer@battlezone.com",
    val profilePicture: String = "",
    val referralCode: String = "BZONEFF77",
    val depositBalance: Double = 0.0,
    val winningBalance: Double = 0.0,
    val bonusBalance: Double = 5.0,
    val referrerId: String? = null,
    val extraMobileNumber: String = ""
)

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateTimeStr: String,
    val timestamp: Long,
    val entryFee: Double,
    val prizePool: Double,
    val map: String,
    val type: String,
    val slotsTotal: Int = 48,
    val slotsRemaining: Int = 48,
    val status: String = "UPCOMING", // "UPCOMING", "LIVE", "COMPLETED"
    val rules: String = "1. Hacks/modded clients prohibited.\n2. Standard emulator restrictions apply.\n3. Send screenshot proof of kills or win if requested.\n4. Admin decisions are final.",
    val roomId: String? = null,
    val roomPassword: String? = null,
    val winnerName: String? = null,
    val winnerUid: String? = null
)

@Entity(tableName = "user_tournament_joins")
data class TournamentJoinEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val tournamentId: Int,
    val freeFireUid: String,
    val inGameName: String,
    val seatNumber: Int,
    val joinedAt: Long = System.currentTimeMillis(),
    val screenshotProofPath: String? = null,
    val proofStatus: String = "NONE", // "NONE", "PENDING", "APPROVED", "REJECTED"
    val claimedKills: Int = 0,
    val claimedRank: Int = 0,
    val adminNotes: String? = null
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val title: String,
    val amount: Double,
    val type: String, // "DEPOSIT", "WITHDRAWAL", "ENTRY_FEE", "PRIZE_WINNING", "BONUS_ADD", "BONUS_DEDUCT", "REFERRAL_REWARD"
    val category: String, // "DEPOSIT", "WINNING", "BONUS"
    val status: String, // "SUCCESS", "PENDING", "FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val invoiceId: String
)

@Entity(tableName = "withdrawal_requests")
data class WithdrawalRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val amount: Double,
    val upiId: String,
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val title: String,
    val message: String,
    val status: String = "OPEN", // "OPEN", "RESOLVED"
    val adminReply: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
