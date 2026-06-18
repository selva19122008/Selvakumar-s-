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
    suspend fun getUserSync(uid: String): UserEntity? = userDao.getUserSync(uid)
    suspend fun getUserByPhoneSync(phone: String): UserEntity? = userDao.getUserByPhoneSync(phone)
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)

    // Tournaments
    val allTournaments: Flow<List<TournamentEntity>> = tournamentDao.getAllTournamentsFlow()
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
    suspend fun insertWithdrawal(withdrawal: WithdrawalRequestEntity) = withdrawalDao.insertWithdrawal(withdrawal)
    suspend fun updateWithdrawal(withdrawal: WithdrawalRequestEntity) = withdrawalDao.updateWithdrawal(withdrawal)

    // Support Tickets
    fun getTicketsForUserFlow(userId: String): Flow<List<SupportTicketEntity>> = supportDao.getTicketsForUserFlow(userId)
    val allTickets: Flow<List<SupportTicketEntity>> = supportDao.getAllTicketsFlow()
    suspend fun insertTicket(ticket: SupportTicketEntity) = supportDao.insertTicket(ticket)
    suspend fun updateTicket(ticket: SupportTicketEntity) = supportDao.updateTicket(ticket)

    // Prefill Database if empty
    suspend fun prefillIfEmpty() {
        val user = userDao.getUserSync("default_user")
        if (user == null) {
            userDao.insertUser(UserEntity())
            userDao.insertUser(
                UserEntity(
                    id = "gamer_boy",
                    inGameName = "Shadow_Hunter",
                    freeFireUid = "FF-902819230",
                    phoneNumber = "+91 88877 66554",
                    email = "shadow@gamer.com",
                    depositBalance = 20.0,
                    winningBalance = 340.0,
                    bonusBalance = 5.0,
                    referralCode = "SHAD77"
                )
            )
            userDao.insertUser(
                UserEntity(
                    id = "elite_clash",
                    inGameName = "Elite_Sniper",
                    freeFireUid = "FF-110294829",
                    phoneNumber = "+91 77766 55443",
                    email = "elite@clash.com",
                    depositBalance = 0.0,
                    winningBalance = 1500.0,
                    bonusBalance = 50.0,
                    referralCode = "SNIP11"
                )
            )
            userDao.insertUser(
                UserEntity(
                    id = "bot_killer",
                    inGameName = "Bot_Exterminator",
                    freeFireUid = "FF-228391048",
                    phoneNumber = "+91 91929 39495",
                    email = "bot@killer.com",
                    depositBalance = 100.0,
                    winningBalance = 0.0,
                    bonusBalance = 0.0,
                    referralCode = "KILBOT9"
                )
            )
        }
        
        val tournaments = tournamentDao.getTournamentSync(1)
        if (tournaments == null) {
            val list = listOf(
                TournamentEntity(
                    id = 1,
                    title = "Free Fire Pro Solo Championship",
                    dateTimeStr = "June 20, 06:00 PM",
                    timestamp = System.currentTimeMillis() + 345600000, // 4 days later
                    entryFee = 20.0,
                    prizePool = 1000.0,
                    map = "Bermuda",
                    type = "Solo",
                    slotsTotal = 48,
                    slotsRemaining = 45,
                    rules = "1. Emulator players are prohibited. Mobile-only play is required.\n2. Direct teaming up in Solo results in disqualification.\n3. Match starting room ID & password will populate. Live check 10 minutes early.\n4. Upload screen proofs of victory directly into the results panel."
                ),
                TournamentEntity(
                    id = 2,
                    title = "Bermuda Rush Duo Battle",
                    dateTimeStr = "June 18, 08:00 PM",
                    timestamp = System.currentTimeMillis() + 172800000, // 2 days later
                    entryFee = 40.0,
                    prizePool = 2500.0,
                    map = "Purgatory",
                    type = "Duo",
                    slotsTotal = 24,
                    slotsRemaining = 24,
                    rules = "1. Duplicate team tags must match registration.\n2. Must upload high-quality screenshot proof.\n3. In-game communications must remain respectful at all times."
                ),
                TournamentEntity(
                    id = 3,
                    title = "Kalahari Clash Squad Elite",
                    dateTimeStr = "June 17, 04:00 PM",
                    timestamp = System.currentTimeMillis() + 86400000, // 1 day later
                    entryFee = 80.0,
                    prizePool = 5000.0,
                    map = "Kalahari",
                    type = "Squad",
                    slotsTotal = 12,
                    slotsRemaining = 10,
                    rules = "1. Standard 4-man squad team tag validation. \n2. Top 3 performing squads are subject to reward distribution.\n3. Discord connectivity is available via support."
                ),
                TournamentEntity(
                    id = 4,
                    title = "Novice Free Entrance Solo Cup",
                    dateTimeStr = "June 16, 09:30 PM",
                    timestamp = System.currentTimeMillis() + 14400000, // 4 hours later
                    entryFee = 0.0,
                    prizePool = 200.0,
                    map = "Bermuda",
                    type = "Solo",
                    slotsTotal = 50,
                    slotsRemaining = 12,
                    rules = "1. Newcomer friendly Solo cup.\n2. Entry fee of 0 INR. Prize rewards and bonus pools shared.\n3. Clean, hacks-free gaming strictly audited."
                )
            )
            tournamentDao.insertTournaments(list)
            
            // prefill some records for aesthetic gaming feel
            transactionDao.insertTransaction(
                TransactionEntity(
                    userId = "default_user",
                    title = "Welcome Signup Bonus",
                    amount = 20.0,
                    type = "BONUS_ADD",
                    category = "BONUS",
                    status = "SUCCESS",
                    timestamp = System.currentTimeMillis() - 86400000,
                    invoiceId = "TXN-SIGNUP-77291"
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    userId = "default_user",
                    title = "Mock PayTM Wallet Deposit",
                    amount = 150.0,
                    type = "DEPOSIT",
                    category = "DEPOSIT",
                    status = "SUCCESS",
                    timestamp = System.currentTimeMillis() - 43200000,
                    invoiceId = "TXN-DEP-110291"
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    userId = "default_user",
                    title = "Classic Bermuda Solo Victory",
                    amount = 50.0,
                    type = "PRIZE_WINNING",
                    category = "WINNING",
                    status = "SUCCESS",
                    timestamp = System.currentTimeMillis() - 21600000,
                    invoiceId = "TXN-WIN-883719"
                )
            )
        }
    }
}
