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
    val extraMobileNumber: String = "",
    val isOnline: Boolean = false,
    val balance: Double = depositBalance
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
    val rules: String = """
🚫 RESTRICTIONS:
• Throwables items (Grenades / gloo melter / flash freeze / Smoke grenade / flash bank) and Vector are NOT allowed. If any team member uses these, the whole team will be fined, winning amount will be deducted, and accounts can also be blocked.
• Trogon not allowed.
• M10 not allowed.
• Zone Pack not allowed.
• Your Free Fire account level must be 40+.

📋 CUSTOM ROOM SETTINGS:
1. While joining, fill your In-Game Username, NOT your UID (Stylish fonts are not allowed; use normal fonts. Failure to follow this can result in being kicked from the room).
2. ID Level Must Be 40+.
3. Headshot rate must be under 60%.
4. Unlimited ammo & Gloo Wall enabled.
5. Default coin: 1500.
6. Character skills: No (No CS).

⚠️ IMPORTANT NOTES:
• Record all matches to review suspicious activities.
• If you find someone hacking, report immediately with a screenshot or video. We will refund you and ban the hacker.
• If you fail to join the custom match by the start time, we are not responsible, and refunds will not be processed. Make sure to join on time.
• Do not use abusive language with admins, in-game chat, or customer support. Violations can lead to losing winnings and account termination.
• The squad team leader is responsible for the behavior of teammates. Bullying is not allowed and can lead to bans without refunds.

🌍 GENERAL RULES:
- Contact us on Telegram for any problems or doubts.
- Matches can be rescheduled if the number of registered players is insufficient. Check our notifications, Telegram channel, or app for updates.
- Room ID and password will be shared in the app 10 minutes before match start time. Match will start 10 minutes after sharing.
- Do not share the Room ID and password. Violations can lead to account termination and loss of winnings.
- If you fail to join the room by match start time, disconnect, or lose connection, we are not responsible and refunds will not be processed.
- This is a paid match. Pay the entry fee to participate. Spots are first come, first served.
- Each team member (squad or duo) must pay the entry fee and register individually.
- Griefing and teaming are against game rules. Violations lead to disqualification and loss of prizes.
- Do not change your position in the custom room after joining. Violations can result in being kicked.
- All players ranking between 1 and 4 will receive special prizes. All players will be rewarded for each kill. Check reward details.
- Do not use screencast while playing. Violations result in an instant ban without warnings.
- Use only mobile devices to join matches. Hacks and emulators are not allowed.
- Violating these rules will result in immediate action, including account bans and forfeiture of rewards.
    """.trimIndent(),
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

@Entity(tableName = "refund_requests")
data class RefundRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val tournamentId: Int,
    val tournamentTitle: String,
    val entryFee: Double,
    val reason: String, // "NOT_CONDUCTED" or "TIMING_ISSUE"
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val refundDestination: String = "WALLET", // "WALLET" or "BANK" (reversals)
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // "MATCH_START", "TIME_UPDATE", "ROOM_CREDS", "GENERAL"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val tournamentId: Int? = null
)

