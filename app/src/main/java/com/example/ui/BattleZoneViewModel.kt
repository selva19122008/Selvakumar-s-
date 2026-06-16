package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.db.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class BattleZoneViewModel(
    application: Application,
    private val repository: BattleZoneRepository
) : AndroidViewModel(application) {

    // Active Current User
    val currentUserId = "default_user"
    val currentUser: StateFlow<UserEntity?> = repository.getUserFlow(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All registered users (for admin user management)
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tournaments List
    val allTournaments: StateFlow<List<TournamentEntity>> = repository.allTournaments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All active joins for current user
    val currentUserJoins: StateFlow<List<TournamentJoinEntity>> = repository.getJoinsForUserFlow(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions list
    val currentUserTransactions: StateFlow<List<TransactionEntity>> = repository.getTransactionsForUserFlow(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Withdrawal Requests list
    val currentUserWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = repository.getWithdrawalsForUserFlow(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Support Tickets list
    val currentUserTickets: StateFlow<List<SupportTicketEntity>> = repository.getTicketsForUserFlow(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -- ADMIN PANEL SOURCES --
    val adminAllWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = repository.allWithdrawals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAllTickets: StateFlow<List<SupportTicketEntity>> = repository.allTickets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAllTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAllSubmittedProofs: StateFlow<List<TournamentJoinEntity>> = repository.getAllSubmittedProofsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.prefillIfEmpty()
        }
    }

    // --- USER FLOW ACTIONS ---

    // Edit User Profile
    fun updateProfile(inGameName: String, ffUid: String, phone: String, email: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getUserSync(currentUserId) ?: UserEntity()
            val updated = existing.copy(
                inGameName = inGameName,
                freeFireUid = ffUid,
                phoneNumber = phone,
                email = email
            )
            repository.insertUser(updated)
            onFinished(true)
        }
    }

    // Join Tournament Function
    fun joinTournament(tournamentId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserSync(currentUserId) ?: return@launch onResult("User account not found")
            val tournament = repository.getTournamentSync(tournamentId) ?: return@launch onResult("Tournament not found")

            if (tournament.slotsRemaining <= 0) {
                return@launch onResult("This tournament is fully booked!")
            }

            // Check if user already joined
            val alreadyJoined = repository.getJoinSync(currentUserId, tournamentId)
            if (alreadyJoined != null) {
                return@launch onResult("You have already joined this tournament!")
            }

            val entryFee = tournament.entryFee

            // Deduct balance logic: Bonus balance first, then Deposit balance, then Winnings
            var remainingFee = entryFee
            var newBonus = user.bonusBalance
            var newDeposit = user.depositBalance
            var newWinning = user.winningBalance

            if (newBonus >= remainingFee) {
                newBonus -= remainingFee
                remainingFee = 0.0
            } else {
                remainingFee -= newBonus
                newBonus = 0.0
            }

            if (remainingFee > 0.0) {
                if (newDeposit >= remainingFee) {
                    newDeposit -= remainingFee
                    remainingFee = 0.0
                } else {
                    remainingFee -= newDeposit
                    newDeposit = 0.0
                }
            }

            if (remainingFee > 0.0) {
                if (newWinning >= remainingFee) {
                    newWinning -= remainingFee
                    remainingFee = 0.0
                } else {
                    return@launch onResult("Insufficient budget. Add money to your wallet!")
                }
            }

            // Save player join & update remaining slots
            val reservedSeat = tournament.slotsTotal - tournament.slotsRemaining + 1
            val joinEntry = TournamentJoinEntity(
                userId = currentUserId,
                tournamentId = tournamentId,
                freeFireUid = user.freeFireUid,
                inGameName = user.inGameName,
                seatNumber = reservedSeat
            )
            repository.insertJoin(joinEntry)

            // Update tournament slots
            repository.updateTournament(tournament.copy(slotsRemaining = tournament.slotsRemaining - 1))

            // Update user balance
            repository.insertUser(user.copy(
                bonusBalance = newBonus,
                depositBalance = newDeposit,
                winningBalance = newWinning
            ))

            // Insert Transaction log
            repository.insertTransaction(
                TransactionEntity(
                    userId = currentUserId,
                    title = "Joined match: ${tournament.title}",
                    amount = entryFee,
                    type = "ENTRY_FEE",
                    category = "WALLET_DEBITS",
                    status = "SUCCESS",
                    invoiceId = "TXN-JOIN-${UUID.randomUUID().toString().take(6).uppercase()}"
                )
            )

            onResult("SUCCESS")
        }
    }

    // Money Deposit Simulation
    fun addMoney(amount: Double, gateway: String, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserSync(currentUserId) ?: return@launch onFinished("Error")
            val updatedUser = user.copy(depositBalance = user.depositBalance + amount)
            repository.insertUser(updatedUser)

            val invoiceId = "TXN-DEP-${UUID.randomUUID().toString().take(8).uppercase()}"
            repository.insertTransaction(
                TransactionEntity(
                    userId = currentUserId,
                    title = "Deposited via $gateway",
                    amount = amount,
                    type = "DEPOSIT",
                    category = "DEPOSIT",
                    status = "SUCCESS",
                    invoiceId = invoiceId
                )
            )
            onFinished(invoiceId)
        }
    }

    // Money Withdrawal Request Submission
    fun requestWithdrawal(amount: Double, upiId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (amount < 50.0) {
                return@launch onResult("Minimum withdrawal amount is ₹50 INR")
            }

            val user = repository.getUserSync(currentUserId) ?: return@launch onResult("User not found")
            if (user.winningBalance < amount) {
                return@launch onResult("Insufficient winning balance! (Available: ₹${user.winningBalance})")
            }

            // Place withdrawal on a processing state in the database
            val request = WithdrawalRequestEntity(
                userId = currentUserId,
                amount = amount,
                upiId = upiId,
                status = "PENDING"
            )
            repository.insertWithdrawal(request)

            // Insert matching pending transaction log
            repository.insertTransaction(
                TransactionEntity(
                    userId = currentUserId,
                    title = "UPI Withdrawal Request (Pending)",
                    amount = amount,
                    type = "WITHDRAWAL",
                    category = "WINNING",
                    status = "PENDING",
                    invoiceId = "TXN-PEND-WD-${UUID.randomUUID().toString().take(6).uppercase()}"
                )
            )

            onResult("SUCCESS")
        }
    }

    // Support System Ticket Creator
    fun createSupportTicket(title: String, message: String, onFinished: () -> Unit) {
        viewModelScope.launch {
            val ticket = SupportTicketEntity(
                userId = currentUserId,
                title = title,
                message = message,
                status = "OPEN"
            )
            repository.insertTicket(ticket)
            onFinished()
        }
    }

    // Claim Referral Reward Code
    fun claimReferral(code: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val normalizedCode = code.trim().uppercase()
            if (normalizedCode == "BZONEFF77") {
                return@launch onResult("Cannot refer yourself!")
            }

            val user = repository.getUserSync(currentUserId) ?: return@launch onResult("User not found")
            if (user.referrerId != null) {
                return@launch onResult("You have already claimed a referral bonus!")
            }

            // Add referral bonus balance (₹15 Deposit and ₹10 Bonus)
            val updatedUser = user.copy(
                referrerId = "invited_by_bzone77",
                bonusBalance = user.bonusBalance + 15.0,
                depositBalance = user.depositBalance + 10.0
            )
            repository.insertUser(updatedUser)

            // Create transactions
            val txId = "TXN-REF-${UUID.randomUUID().toString().take(6).uppercase()}"
            repository.insertTransaction(
                TransactionEntity(
                    userId = currentUserId,
                    title = "Referral code applied",
                    amount = 25.0,
                    type = "REFERRAL_REWARD",
                    category = "BONUS",
                    status = "SUCCESS",
                    invoiceId = txId
                )
            )
            onResult("SUCCESS")
        }
    }


    // --- ADMIN PANEL CONTROL ACTIONS ---

    // Create Tournament (Admin)
    fun adminCreateTournament(
        title: String,
        dateTimeStr: String,
        entryFee: Double,
        prizePool: Double,
        map: String,
        type: String,
        slotsTotal: Int,
        rules: String,
        onFinished: () -> Unit
    ) {
        viewModelScope.launch {
            val newTournament = TournamentEntity(
                title = title,
                dateTimeStr = dateTimeStr,
                timestamp = System.currentTimeMillis() + 86400000 * 3, // Defaults to 3 days out
                entryFee = entryFee,
                prizePool = prizePool,
                map = map,
                type = type,
                slotsTotal = slotsTotal,
                slotsRemaining = slotsTotal,
                status = "UPCOMING",
                rules = rules
            )
            repository.insertTournament(newTournament)
            onFinished()
        }
    }

    // Cancel Tournament & Refund All Players (Admin)
    fun adminCancelTournament(tournamentId: Int) {
        viewModelScope.launch {
            val tournament = repository.getTournamentSync(tournamentId) ?: return@launch
            // Query players who joined this match
            val participants = repository.getJoinsForTournamentSync(tournamentId)

            // Refund players
            participants.forEach { join ->
                val player = repository.getUserSync(join.userId)
                if (player != null) {
                    // Refund to deposit balance
                    val updatedPlayer = player.copy(
                        depositBalance = player.depositBalance + tournament.entryFee
                    )
                    repository.insertUser(updatedPlayer)

                    // Insert refund receipt
                    repository.insertTransaction(
                        TransactionEntity(
                            userId = join.userId,
                            title = "Match Cancelled Refund: ${tournament.title}",
                            amount = tournament.entryFee,
                            type = "BONUS_ADD",
                            category = "DEPOSIT",
                            status = "SUCCESS",
                            invoiceId = "TXN-REFUND-${UUID.randomUUID().toString().take(6).uppercase()}"
                        )
                    )
                }
                // Delete active register
                repository.deleteJoin(join)
            }

            // Remove tournament
            repository.deleteTournament(tournament)
        }
    }

    // Update Room Details (Admin)
    fun adminUpdateRoomDetails(tournamentId: Int, roomId: String?, roomPass: String?) {
        viewModelScope.launch {
            val tournament = repository.getTournamentSync(tournamentId) ?: return@launch
            val updated = tournament.copy(
                roomId = roomId,
                roomPassword = roomPass
            )
            repository.updateTournament(updated)
        }
    }

    // Update Tournament Status / End Match / Distribute Prizes (Admin)
    fun adminEndTournamentAndDistributePrize(tournamentId: Int, winnerFFUid: String, winnerInGameName: String) {
        viewModelScope.launch {
            val tournament = repository.getTournamentSync(tournamentId) ?: return@launch
            
            // Set tournament closed
            val updated = tournament.copy(
                status = "COMPLETED",
                winnerUid = winnerFFUid,
                winnerName = winnerInGameName
            )
            repository.updateTournament(updated)

            // Find matching local user or fallback to default user
            var winnerUser = repository.allUsers.firstOrNull()?.find { it.freeFireUid == winnerFFUid || it.inGameName == winnerInGameName }
            if (winnerUser == null) {
                // If the winner matches primary default_user
                winnerUser = repository.getUserSync(currentUserId)
            }

            if (winnerUser != null) {
                // Add prize pool rewards directly to winner winnings balance!
                val prize = tournament.prizePool
                val updatedWinner = winnerUser.copy(
                    winningBalance = winnerUser.winningBalance + prize
                )
                repository.insertUser(updatedWinner)

                // Add victory transaction log
                repository.insertTransaction(
                    TransactionEntity(
                        userId = winnerUser.id,
                        title = "E-Pro Tournament Champion: ${tournament.title}",
                        amount = prize,
                        type = "PRIZE_WINNING",
                        category = "WINNING",
                        status = "SUCCESS",
                        invoiceId = "TXN-WINNER-${UUID.randomUUID().toString().take(8).uppercase()}"
                    )
                )
            }
        }
    }

    // Approve Withdrawal Request (Admin)
    fun adminApproveWithdrawal(withdrawalId: Int) {
        viewModelScope.launch {
            val list = repository.allWithdrawals.first().find { it.id == withdrawalId } ?: return@launch
            if (list.status != "PENDING") return@launch

            // Fetch user
            val user = repository.getUserSync(list.userId) ?: return@launch
            if (user.winningBalance >= list.amount) {
                // Deduct balance
                val updatedUser = user.copy(winningBalance = user.winningBalance - list.amount)
                repository.insertUser(updatedUser)

                // Set approved and update request status
                repository.updateWithdrawal(list.copy(status = "APPROVED"))

                // Create success transaction invoice
                repository.insertTransaction(
                    TransactionEntity(
                        userId = user.id,
                        title = "UPI Withdrawal Approved (Sent)",
                        amount = list.amount,
                        type = "WITHDRAWAL",
                        category = "WINNING",
                        status = "SUCCESS",
                        invoiceId = "TXN-WD-SUCCESS-${UUID.randomUUID().toString().take(6).uppercase()}"
                    )
                )
            }
        }
    }

    // Reject Withdrawal Request (Admin)
    fun adminRejectWithdrawal(withdrawalId: Int) {
        viewModelScope.launch {
            val req = repository.allWithdrawals.first().find { it.id == withdrawalId } ?: return@launch
            if (req.status != "PENDING") return@launch

            // Mark rejected
            repository.updateWithdrawal(req.copy(status = "REJECTED"))

            // Log rejection
            repository.insertTransaction(
                TransactionEntity(
                    userId = req.userId,
                    title = "UPI Withdrawal Rejected by Admin (Returned)",
                    amount = req.amount,
                    type = "BONUS_ADD",
                    category = "WINNING",
                    status = "FAILED",
                    invoiceId = "TXN-WD-REJ-${UUID.randomUUID().toString().take(6).uppercase()}"
                )
            )
        }
    }

    // Support Ticket Reply Action (Admin)
    fun adminReplySupportTicket(ticketId: Int, reply: String) {
        viewModelScope.launch {
            val ticketList = repository.allTickets.first()
            val ticket = ticketList.find { it.id == ticketId } ?: return@launch
            val updated = ticket.copy(
                adminReply = reply,
                status = "RESOLVED"
            )
            repository.updateTicket(updated)
        }
    }

    // Modify User Wallet Balance directly (Admin Wallet Control)
    fun adminModifyUserBalance(userId: String, depositMod: Double, winningMod: Double, bonusMod: Double) {
        viewModelScope.launch {
            val user = repository.getUserSync(userId) ?: return@launch
            val updatedUser = user.copy(
                depositBalance = (user.depositBalance + depositMod).coerceAtLeast(0.0),
                winningBalance = (user.winningBalance + winningMod).coerceAtLeast(0.0),
                bonusBalance = (user.bonusBalance + bonusMod).coerceAtLeast(0.0)
            )
            repository.insertUser(updatedUser)

            // Log the modification transaction
            repository.insertTransaction(
                TransactionEntity(
                    userId = userId,
                    title = "Wallet Adjustment by BattleZone Admin",
                    amount = Math.abs(depositMod + winningMod + bonusMod),
                    type = if (depositMod + winningMod + bonusMod >= 0) "BONUS_ADD" else "BONUS_DEDUCT",
                    category = "DEPOSIT",
                    status = "SUCCESS",
                    invoiceId = "TXN-ADMIN-ADJ-${UUID.randomUUID().toString().take(6).uppercase()}"
                )
            )
        }
    }

    // Submit Screenshot Proof (User function)
    fun submitScreenshotProof(
        tournamentId: Int,
        screenshotPath: String,
        kills: Int,
        rank: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val join = repository.getJoinSync(currentUserId, tournamentId)
            if (join == null) {
                onResult(false, "You are not registered in this tournament.")
                return@launch
            }
            val updatedJoin = join.copy(
                screenshotProofPath = screenshotPath,
                proofStatus = "PENDING",
                claimedKills = kills,
                claimedRank = rank
            )
            repository.insertJoin(updatedJoin)
            onResult(true, "Screenshot proof submitted successfully to the verification queue!")
        }
    }

    // Verify and Validate Screenshot Proof (Admin function)
    fun adminVerifyProof(
        joinId: Int,
        newStatus: String,
        notes: String,
        distributeReward: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val joinObj = repository.getJoinByIdSync(joinId)
            if (joinObj == null) {
                onResult(false)
                return@launch
            }
            val updatedJoin = joinObj.copy(
                proofStatus = newStatus,
                adminNotes = notes
            )
            repository.insertJoin(updatedJoin)

            // If approved and requested to reward
            if (newStatus == "APPROVED") {
                val tournament = repository.getTournamentSync(joinObj.tournamentId)
                val user = repository.getUserSync(joinObj.userId)
                if (tournament != null && user != null) {
                    if (distributeReward) {
                        val prize = tournament.prizePool
                        val updatedUser = user.copy(winningBalance = user.winningBalance + prize)
                        repository.insertUser(updatedUser)

                        // Complete tournament
                        repository.updateTournament(tournament.copy(
                            status = "COMPLETED",
                            winnerUid = joinObj.freeFireUid,
                            winnerName = joinObj.inGameName
                        ))

                        // Insert transaction log
                        repository.insertTransaction(
                            TransactionEntity(
                                userId = user.id,
                                title = "Prize Claim Approved: ${tournament.title}",
                                amount = prize,
                                type = "PRIZE_WINNING",
                                category = "WINNING",
                                status = "SUCCESS",
                                invoiceId = "TXN-PROOF-WIN-${UUID.randomUUID().toString().take(6).uppercase()}"
                            )
                        )
                    }
                }
            }
            onResult(true)
        }
    }
}

// ViewModel Factory Provider
class BattleZoneViewModelFactory(
    private val application: Application,
    private val repository: BattleZoneRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BattleZoneViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BattleZoneViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
