package com.example.db

import kotlinx.coroutines.flow.Flow

class BattleZoneRepository(private val db: AppDatabase) {
    val userDao = db.userDao()
    val tournamentDao = db.tournamentDao()
    val joinDao = db.joinDao()
    val transactionDao = db.transactionDao()
    val withdrawalDao = db.withdrawalDao()
    val supportDao = db.supportDao()

    // Users
    fun getUserFlow(uid: String): Flow<UserEntity?> = userDao.getUserFlow(uid)
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsersFlow()
    suspend fun getAllUsersSync(): List<UserEntity> = userDao.getAllUsersSync()
    suspend fun getUserSync(uid: String): UserEntity? = userDao.getUserSync(uid)
    suspend fun getUserByPhoneSync(phone: String): UserEntity? = userDao.getUserByPhoneSync(phone)
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun deleteUserById(uid: String) = userDao.deleteUserById(uid)

    // Tournaments
    val allTournaments: Flow<List<TournamentEntity>> = tournamentDao.getAllTournamentsFlow()
    suspend fun getTournamentsSync(): List<TournamentEntity> = tournamentDao.getAllTournamentsSync()
    fun getTournamentFlow(id: Int): Flow<TournamentEntity?> = tournamentDao.getTournamentFlow(id)
    suspend fun getTournamentSync(id: Int): TournamentEntity? = tournamentDao.getTournamentSync(id)
    suspend fun insertTournament(tournament: TournamentEntity): Long = tournamentDao.insertTournament(tournament)
    suspend fun updateTournament(tournament: TournamentEntity) = tournamentDao.updateTournament(tournament)
    suspend fun deleteTournament(tournament: TournamentEntity) = tournamentDao.deleteTournament(tournament)
    suspend fun insertTournaments(tournaments: List<TournamentEntity>) = tournamentDao.insertTournaments(tournaments)

    // Joins
    fun getJoinsForTournamentFlow(tournamentId: Int): Flow<List<TournamentJoinEntity>> = joinDao.getJoinsForTournamentFlow(tournamentId)
    suspend fun getJoinsForTournamentSync(tournamentId: Int): List<TournamentJoinEntity> = joinDao.getJoinsForTournamentSync(tournamentId)
    fun getJoinsForUserFlow(userId: String): Flow<List<TournamentJoinEntity>> = joinDao.getJoinsForUserFlow(userId)
    suspend fun getJoinSync(userId: String, tournamentId: Int): TournamentJoinEntity? = joinDao.getJoinSync(userId, tournamentId)
    suspend fun getJoinByIdSync(joinId: Int): TournamentJoinEntity? = joinDao.getJoinByIdSync(joinId)
    fun getAllSubmittedProofsFlow(): Flow<List<TournamentJoinEntity>> = joinDao.getAllSubmittedProofsFlow()
    fun getAllJoinsFlow(): Flow<List<TournamentJoinEntity>> = joinDao.getAllJoinsFlow()
    suspend fun insertJoin(join: TournamentJoinEntity) = joinDao.insertJoin(join)
    suspend fun deleteJoin(join: TournamentJoinEntity) = joinDao.deleteJoin(join)

    // Transactions
    fun getTransactionsForUserFlow(userId: String): Flow<List<TransactionEntity>> = transactionDao.getTransactionsForUserFlow(userId)
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)
    suspend fun getTransactionByIdSync(id: Int): TransactionEntity? = transactionDao.getTransactionByIdSync(id)
    suspend fun getTransactionByInvoiceIdSync(invoiceId: String): TransactionEntity? = transactionDao.getTransactionByInvoiceIdSync(invoiceId)
    suspend fun deleteTransactionByIdSync(id: Int) = transactionDao.deleteTransactionByIdSync(id)

    // Withdrawals
    fun getWithdrawalsForUserFlow(userId: String): Flow<List<WithdrawalRequestEntity>> = withdrawalDao.getWithdrawalsForUserFlow(userId)
    val allWithdrawals: Flow<List<WithdrawalRequestEntity>> = withdrawalDao.getAllWithdrawalsFlow()
    suspend fun insertWithdrawal(withdrawal: WithdrawalRequestEntity): Long = withdrawalDao.insertWithdrawal(withdrawal)
    suspend fun updateWithdrawal(withdrawal: WithdrawalRequestEntity) = withdrawalDao.updateWithdrawal(withdrawal)

    // Support Tickets
    fun getTicketsForUserFlow(userId: String): Flow<List<SupportTicketEntity>> = supportDao.getTicketsForUserFlow(userId)
    val allTickets: Flow<List<SupportTicketEntity>> = supportDao.getAllTicketsFlow()
    suspend fun insertTicket(ticket: SupportTicketEntity) = supportDao.insertTicket(ticket)
    suspend fun updateTicket(ticket: SupportTicketEntity) = supportDao.updateTicket(ticket)

    // Refunds
    val refundDao = db.refundDao()
    fun getRefundsForUserFlow(userId: String): Flow<List<RefundRequestEntity>> = refundDao.getRefundsForUserFlow(userId)
    val allRefunds: Flow<List<RefundRequestEntity>> = refundDao.getAllRefundsFlow()
    suspend fun insertRefund(refund: RefundRequestEntity): Long = refundDao.insertRefund(refund)
    suspend fun updateRefund(refund: RefundRequestEntity) = refundDao.updateRefund(refund)
    suspend fun getRefundByIdSync(id: Int): RefundRequestEntity? = refundDao.getRefundByIdSync(id)
    suspend fun getRefundByUserAndTournamentSync(userId: String, tournamentId: Int): RefundRequestEntity? = refundDao.getRefundByUserAndTournamentSync(userId, tournamentId)

    // Notifications
    val notificationDao = db.notificationDao()
    fun getNotificationsForUserFlow(userId: String): Flow<List<NotificationEntity>> = notificationDao.getNotificationsForUserFlow(userId)
    suspend fun insertNotification(notification: NotificationEntity) = notificationDao.insertNotification(notification)
    suspend fun markAllNotificationsAsRead(userId: String) = notificationDao.markAllAsRead(userId)
    suspend fun deleteNotification(id: Int) = notificationDao.deleteNotification(id)
    suspend fun clearAllNotificationsForUser(userId: String) = notificationDao.clearAllForUser(userId)

    // Prefill Database if empty
    suspend fun prefillIfEmpty() {
        // Disabled automatically generated dummy/fake users and tournaments as requested by the admin.
        // This ensures the database remains completely blank unless real users register or tournaments are manually created by the Admin.
    }
}
