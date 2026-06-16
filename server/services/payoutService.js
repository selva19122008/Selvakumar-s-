const mongoose = require('mongoose');
const Tournament = require('../models/Tournament');
const TournamentJoin = require('../models/TournamentJoin');
const User = require('../models/User');
const Transaction = require('../models/Transaction');
const adminLogService = require('./adminLogService');

/**
 * Payout Service
 * Handles calculations and automated distributions of tournament prize money
 * based on ranking performance.
 */

/**
 * Calculates rank distribution ratios dynamically based on participant counts.
 * Max top-3 payout structure is supported natively to comply with standard esports tournaments.
 * 
 * @param {number} participantCount 
 * @returns {Array<number>} An array where index represents rank-1 percentage (0 to 1).
 */
const getPayoutRatios = (participantCount) => {
  if (participantCount <= 0) return [];
  if (participantCount === 1) return [1.0]; // 100% to 1st place
  if (participantCount === 2) return [0.65, 0.35]; // 65% to 1st, 35% to 2nd
  return [0.50, 0.30, 0.20]; // Default Top-3: 50% to 1st, 30% to 2nd, 20% to 3rd
};

/**
 * Automates ranks calculation, computes payouts, credits winner wallets, and logs transactions.
 * Designed to execute within an atomic mongoose transition session.
 * 
 * @param {string} tournamentId - The completed tournament ID.
 * @param {mongoose.ClientSession} externalSession - Existing MongoDB session if any.
 * @returns {Promise<Object>} Summary distribution reports.
 */
exports.distributeTournamentPrizes = async (tournamentId, externalSession = null) => {
  const session = externalSession || await mongoose.startSession();
  const isInternalSession = !externalSession;

  if (isInternalSession) {
    session.startTransaction();
  }

  try {
    // 1. Fetch tournament details and check eligibility
    const tournament = await Tournament.findById(tournamentId).session(session);
    if (!tournament) {
      throw new Error(`Tournament matching ID: ${tournamentId} not found.`);
    }

    if (tournament.status === 'completed') {
      throw new Error(`Prizes for tournament "${tournament.title}" have already been fully distributed.`);
    }

    // 2. Query all registered participant records
    const participants = await TournamentJoin.find({ tournamentId }).session(session);

    if (participants.length === 0) {
      // Empty lobby: complete tournament without payouts to avoid freeze
      tournament.status = 'completed';
      await tournament.save({ session });

      if (isInternalSession) {
        await session.commitTransaction();
      }

      return {
        success: true,
        message: 'Tournament Completed. No payouts distributed since there were 0 registered participants.',
        distributed: []
      };
    }

    // 3. Compile rankings sorted by points (descending), then kills (descending)
    const rankedParticipants = [...participants].sort((a, b) => {
      // Primary: points
      if (b.points !== a.points) {
        return b.points - a.points;
      }
      // Secondary: kills
      if (b.kills !== a.kills) {
        return b.kills - a.kills;
      }
      // Tertiary: earlier sign-up gets the upper hand
      return new Date(a.joinedAt) - new Date(b.joinedAt);
    });

    const totalPrizePool = tournament.prizePool || 0;
    const payoutRatios = getPayoutRatios(rankedParticipants.length);
    const distributions = [];

    // 4. Distribute prizes to top ranks
    for (let index = 0; index < rankedParticipants.length; index++) {
      const participant = rankedParticipants[index];
      const rank = index + 1;
      
      // Determine if a prize allocation exists for this rank
      const ratio = payoutRatios[index] || 0;
      const prizeAmount = parseFloat((totalPrizePool * ratio).toFixed(2));

      // Capture participant performance
      distributions.push({
        rank,
        userId: participant.userId,
        freeFireUid: participant.freeFireUid,
        inGameName: participant.inGameName,
        kills: participant.kills,
        points: participant.points,
        ratio,
        prizeCredited: prizeAmount
      });

      // Update User Winning Balance only if positive prize exists
      if (prizeAmount > 0) {
        const user = await User.findById(participant.userId).session(session);
        if (user) {
          user.winningBalance = parseFloat((user.winningBalance + prizeAmount).toFixed(2));
          await user.save({ session });

          // Document Prize Winning in transaction auditing log
          const payoutInvoiceId = `WIN-RK${rank}-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
          const prizeTxn = new Transaction({
            userId: participant.userId,
            title: `Tournament Prize Rank #${rank}: "${tournament.title}"`,
            amount: prizeAmount,
            type: 'PRIZE_WINNING',
            category: 'WINNING',
            status: 'SUCCESS',
            invoiceId: payoutInvoiceId
          });
          await prizeTxn.save({ session });
        }
      }
    }

    // 5. Explicitly assign winner credentials in the tournament document
    const champion = distributions[0];
    if (champion) {
      tournament.winnerUid = champion.freeFireUid;
      tournament.winnerName = champion.inGameName;
    }

    // 6. Complete and save the tournament status
    tournament.status = 'completed';
    await tournament.save({ session });

    // 7. Record an Audit action detailing complete payout schema
    await adminLogService.logAction({
      adminId: new mongoose.Types.ObjectId(), // System payout routine event ID
      action: 'USER_WALLET_ADJUSTMENT',
      targetType: 'Tournament',
      targetId: tournamentId,
      details: `Automated prize payouts processed for "${tournament.title}". Distributed ₹${totalPrizePool.toFixed(2)} to top players. Champion: ${champion ? champion.inGameName : 'N/A'}`
    });

    if (isInternalSession) {
      await session.commitTransaction();
    }

    return {
      success: true,
      message: 'Automated tournament calculations and payout distributions finalized successfully.',
      tournamentTitle: tournament.title,
      totalPrizePool,
      payoutBreakdown: distributions
    };

  } catch (error) {
    if (isInternalSession) {
      await session.abortTransaction();
    }
    throw error;
  } finally {
    if (isInternalSession) {
      session.endSession();
    }
  }
};
