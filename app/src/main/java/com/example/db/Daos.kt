package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :uid LIMIT 1")
    fun getUserFlow(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :uid LIMIT 1")
    suspend fun getUserSync(uid: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY timestamp ASC")
    fun getAllTournamentsFlow(): Flow<List<TournamentEntity>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoin(join: TournamentJoinEntity)

    @Delete
    suspend fun deleteJoin(join: TournamentJoinEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUserFlow(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawal_requests WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWithdrawalsForUserFlow(userId: String): Flow<List<WithdrawalRequestEntity>>

    @Query("SELECT * FROM withdrawal_requests ORDER BY timestamp DESC")
    fun getAllWithdrawalsFlow(): Flow<List<WithdrawalRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalRequestEntity)

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalRequestEntity)
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
