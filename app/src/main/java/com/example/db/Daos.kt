package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :uid LIMIT 1")
    fun getUserFlow(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersSync(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :uid LIMIT 1")
    suspend fun getUserSync(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getUserByPhoneSync(phone: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :uid")
    suspend fun deleteUserById(uid: String)
}

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY timestamp ASC")
    fun getAllTournamentsFlow(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments ORDER BY timestamp ASC")
    suspend fun getAllTournamentsSync(): List<TournamentEntity>

    @Query("SELECT * FROM tournaments WHERE id = :id LIMIT 1")
    fun getTournamentFlow(id: Int): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournaments WHERE id = :id LIMIT 1")
    suspend fun getTournamentSync(id: Int): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity): Long

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Delete
    suspend fun deleteTournament(tournament: TournamentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournaments(tournaments: List<TournamentEntity>)

    @Query("DELETE FROM tournaments")
    suspend fun clearTournaments()

    @Transaction
    suspend fun syncTournaments(remoteTourneys: List<TournamentEntity>) {
        val local = getAllTournamentsSync()
        val remoteIds = remoteTourneys.map { it.id }.toSet()
        
        val toInsert = remoteTourneys.filter { r ->
            val l = local.find { it.id == r.id }
            l == null || l != r
        }
        if (toInsert.isNotEmpty()) {
            insertTournaments(toInsert)
        }
        
        val toDelete = local.filter { l -> !remoteIds.contains(l.id) }
        for (d in toDelete) {
            deleteTournament(d)
        }
    }
}

@Dao
interface JoinDao {
    @Query("SELECT * FROM user_tournament_joins WHERE tournamentId = :tournamentId ORDER BY seatNumber ASC")
    fun getJoinsForTournamentFlow(tournamentId: Int): Flow<List<TournamentJoinEntity>>

    @Query("SELECT * FROM user_tournament_joins WHERE tournamentId = :tournamentId")
    suspend fun getJoinsForTournamentSync(tournamentId: Int): List<TournamentJoinEntity>

    @Query("SELECT * FROM user_tournament_joins WHERE userId = :userId ORDER BY joinedAt DESC")
    fun getJoinsForUserFlow(userId: String): Flow<List<TournamentJoinEntity>>

    @Query("SELECT * FROM user_tournament_joins WHERE userId = :userId AND tournamentId = :tournamentId LIMIT 1")
    suspend fun getJoinSync(userId: String, tournamentId: Int): TournamentJoinEntity?

    @Query("SELECT * FROM user_tournament_joins WHERE id = :joinId LIMIT 1")
    suspend fun getJoinByIdSync(joinId: Int): TournamentJoinEntity?

    @Query("SELECT * FROM user_tournament_joins WHERE proofStatus != 'NONE' ORDER BY joinedAt DESC")
    fun getAllSubmittedProofsFlow(): Flow<List<TournamentJoinEntity>>

    @Query("SELECT * FROM user_tournament_joins ORDER BY joinedAt DESC")
    fun getAllJoinsFlow(): Flow<List<TournamentJoinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoin(join: TournamentJoinEntity)

    @Delete
    suspend fun deleteJoin(join: TournamentJoinEntity)

    @Query("DELETE FROM user_tournament_joins")
    suspend fun clearJoins()

    @Query("SELECT * FROM user_tournament_joins")
    suspend fun getAllJoinsSync(): List<TournamentJoinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoins(joins: List<TournamentJoinEntity>)

    @Transaction
    suspend fun syncJoins(remoteJoins: List<TournamentJoinEntity>) {
        val local = getAllJoinsSync()
        val remoteKeys = remoteJoins.map { "${it.userId}_${it.tournamentId}" }.toSet()
        
        val toInsert = remoteJoins.filter { r ->
            val l = local.find { it.userId == r.userId && it.tournamentId == r.tournamentId }
            l == null || l != r
        }
        if (toInsert.isNotEmpty()) {
            insertJoins(toInsert)
        }
        
        val toDelete = local.filter { l -> !remoteKeys.contains("${l.userId}_${l.tournamentId}") }
        for (d in toDelete) {
            deleteJoin(d)
        }
    }
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUserFlow(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionByIdSync(id: Int): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE invoiceId = :invoiceId LIMIT 1")
    suspend fun getTransactionByInvoiceIdSync(invoiceId: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionByIdSync(id: Int)
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawal_requests WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWithdrawalsForUserFlow(userId: String): Flow<List<WithdrawalRequestEntity>>

    @Query("SELECT * FROM withdrawal_requests ORDER BY timestamp DESC")
    fun getAllWithdrawalsFlow(): Flow<List<WithdrawalRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalRequestEntity): Long

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalRequestEntity)

    @Query("DELETE FROM withdrawal_requests")
    suspend fun clearWithdrawals()
}

@Dao
interface SupportDao {
    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTicketsForUserFlow(userId: String): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets ORDER BY timestamp DESC")
    fun getAllTicketsFlow(): Flow<List<SupportTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicketEntity)

    @Update
    suspend fun updateTicket(ticket: SupportTicketEntity)
}

@Dao
interface RefundDao {
    @Query("SELECT * FROM refund_requests WHERE userId = :userId ORDER BY timestamp DESC")
    fun getRefundsForUserFlow(userId: String): Flow<List<RefundRequestEntity>>

    @Query("SELECT * FROM refund_requests ORDER BY timestamp DESC")
    fun getAllRefundsFlow(): Flow<List<RefundRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefund(refund: RefundRequestEntity): Long

    @Update
    suspend fun updateRefund(refund: RefundRequestEntity)

    @Query("SELECT * FROM refund_requests WHERE id = :id LIMIT 1")
    suspend fun getRefundByIdSync(id: Int): RefundRequestEntity?

    @Query("SELECT * FROM refund_requests WHERE userId = :userId AND tournamentId = :tournamentId LIMIT 1")
    suspend fun getRefundByUserAndTournamentSync(userId: String, tournamentId: Int): RefundRequestEntity?
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUserFlow(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId AND isRead = 0")
    suspend fun markAllAsRead(userId: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}

