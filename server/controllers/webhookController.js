const crypto = require('crypto');
const mongoose = require('mongoose');
const Tournament = require('../models/Tournament');
const TournamentJoin = require('../models/TournamentJoin');
const User = require('../models/User');
const Transaction = require('../models/Transaction');

// Retrieve secure webhook verification key from environment configurations
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || 'fw_webhook_sec_key_2026';

/**
 * @desc    Automated game platform webhook callback processor
 * @route   POST /api/webhooks/game-platform
 * @access  Public (Secured with signature check)
 */
exports.handleGamePlatformWebhook = async (req, res) => {
  const session = await mongoose.startSession();
  try {
    // 1. Authenticate Request Signatures (Secured HMAC Verification)
    const signature = req.headers['x-webhook-signature'];
    
    if (process.env.NODE_ENV === 'production' || req.headers['x-webhook-signature']) {
      if (!signature) {
        return res.status(401).json({
          success: false,
          message: 'Security validation failure. Webhook authentication signature is required.'
        });
      }

      const hmac = crypto.createHmac('sha256', WEBHOOK_SECRET);
      const computedSignature = hmac.update(JSON.stringify(req.body)).digest('hex');

      if (!crypto.timingSafeEqual(Buffer.from(signature, 'utf8'), Buffer.from(computedSignature, 'utf8'))) {
        return res.status(401).json({
          success: false,
          message: 'Security signature mismatch. Unauthorized callback source detected.'
        });
      }
    }

    const { event, data } = req.body;

    if (!event || !data) {
      return res.status(400).json({
        success: false,
        message: 'Invalid payload. "event" and "data" parameters are required.'
      });
    }

    // 2. Route events to high-fidelity internal processors
    switch (event) {
      case 'match.status_changed':
        return await processMatchStatusChanged(data, res, session);

      case 'match.results_submitted':
        return await processMatchResultsSubmitted(data, res, session);

      default:
        return res.status(400).json({
          success: false,
          message: `Unrecognized webhook event type: "${event}".`
        });
    }
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Critical error parsed during webhook processing context.',
      error: error.message
    });
  } finally {
    session.endSession();
  }
};

/**
 * Internal processor to handle Match Status Changes (upcoming, live, completed, cancelled)
 */
async function processMatchStatusChanged(data, res, session) {
  const { tournamentId, status } = data;

  if (!tournamentId || !status) {
    return res.status(400).json({
      success: false,
      message: 'Both "tournamentId" and "status" are required in data block.'
    });
  }

  const validStatuses = ['upcoming', 'live', 'completed', 'cancelled'];
  if (!validStatuses.includes(status)) {
    return res.status(400).json({
      success: false,
      message: `Invalid target status. Must be one of: ${validStatuses.join(', ')}`
    });
  }

  session.startTransaction();

  const tournament = await Tournament.findById(tournamentId).session(session);
  if (!tournament) {
    await session.abortTransaction();
    return res.status(404).json({
      success: false,
      message: `Tournament matching ID ${tournamentId} does not exist.`
    });
  }

  // Prevent updates to already completed/closed tournaments to protect static histories
  if (tournament.status === 'completed') {
    await session.abortTransaction();
    return res.status(400).json({
      success: false,
      message: 'Modification forbidden. This tournament has already completed.'
    });
  }

  const matchesOldStatus = tournament.status;
  tournament.status = status;
  await tournament.save({ session });

  // Handle specialized Cancellation Flow (Automatic refund engine)
  if (status === 'cancelled') {
    const registrations = await TournamentJoin.find({ tournamentId }).session(session);

    if (registrations.length > 0) {
      const entryFee = tournament.entryFee;

      for (const reg of registrations) {
        // Refund back to user's deposit pool
        await User.findByIdAndUpdate(
          reg.userId,
          { $inc: { depositBalance: entryFee } },
          { session }
        );

        // Record a transaction log representing refund
        const refundInvoiceId = `RFD-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
        const refundTxn = new Transaction({
          userId: reg.userId,
          title: `Refund: Cancelled Tournament "${tournament.title}"`,
          amount: entryFee,
          type: 'DEPOSIT',
          category: 'DEPOSIT',
          status: 'SUCCESS',
          invoiceId: refundInvoiceId
        });
        await refundTxn.save({ session });
      }
    }
  }

  await session.commitTransaction();

  return res.status(200).json({
    success: true,
    message: `Match status successfully transit from "${matchesOldStatus}" to "${status}".`,
    data: {
      tournamentId,
      oldStatus: matchesOldStatus,
      newStatus: status,
      refundsProcessed: status === 'cancelled'
    }
  });
}

/**
 * Internal processor to compile Match Performance Statistics & Distribute Prize Money
 */
async function processMatchResultsSubmitted(data, res, session) {
  const { tournamentId, results } = data;

  if (!tournamentId || !Array.isArray(results)) {
    return res.status(400).json({
      success: false,
      message: 'Both "tournamentId" and a "results" array are required.'
    });
  }

  session.startTransaction();

  // Find target tournament
  const tournament = await Tournament.findById(tournamentId).session(session);
  if (!tournament) {
    await session.abortTransaction();
    return res.status(404).json({
      success: false,
      message: `Tournament ID ${tournamentId} not found.`
    });
  }

  // Ensure double-distribution is blocked
  if (tournament.status === 'completed') {
    await session.abortTransaction();
    return res.status(400).json({
      success: false,
      message: 'Match results have already been final and prizes distributed for this tournament.'
    });
  }

  let winnerRecord = null;

  // Process game results for each player
  for (const playerStats of results) {
    const { freeFireUid, kills = 0, points = 0, isWinner = false } = playerStats;

    if (!freeFireUid) {
      continue;
    }

    // Update individual registration's performance stats
    const joinRecord = await TournamentJoin.findOneAndUpdate(
      { tournamentId, freeFireUid },
      { $set: { kills: Math.max(0, kills), points: Math.max(0, points) } },
      { new: true, session }
    );

    // Track identified winner metadata
    if (isWinner && joinRecord) {
      winnerRecord = {
        userId: joinRecord.userId,
        freeFireUid: joinRecord.freeFireUid,
        inGameName: joinRecord.inGameName
      };
    }
  }

  // Fallback: If no explicit winner marked, compile winner as the player with the highest points
  if (!winnerRecord && results.length > 0) {
    const topPlayer = [...results].sort((a, b) => b.points - a.points)[0];
    if (topPlayer) {
      const joinRecord = await TournamentJoin.findOne({
        tournamentId,
        freeFireUid: topPlayer.freeFireUid
      }).session(session);
      
      if (joinRecord) {
        winnerRecord = {
          userId: joinRecord.userId,
          freeFireUid: joinRecord.freeFireUid,
          inGameName: joinRecord.inGameName
        };
      }
    }
  }

  // Award prize money to the tournament winner
  if (winnerRecord) {
    const prizeAmount = tournament.prizePool;

    // Credit winner's winningBalance wallet in User model
    const winningUser = await User.findById(winnerRecord.userId).session(session);
    if (winningUser) {
      winningUser.winningBalance += prizeAmount;
      await winningUser.save({ session });

      // Create Success Prize Distribution Transaction record
      const winningInvoiceId = `WIN-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
      const prizeTxn = new Transaction({
        userId: winnerRecord.userId,
        title: `Champion Prize: "${tournament.title}"`,
        amount: prizeAmount,
        type: 'PRIZE_WINNING',
        category: 'WINNING',
        status: 'SUCCESS',
        invoiceId: winningInvoiceId
      });
      await prizeTxn.save({ session });

      // Update tournament variables with winner credentials
      tournament.winnerUid = winnerRecord.freeFireUid;
      tournament.winnerName = winnerRecord.inGameName;
    }
  }

  // Formally mark tournament completed
  tournament.status = 'completed';
  await tournament.save({ session });

  await session.commitTransaction();

  return res.status(200).json({
    success: true,
    message: 'Match results successfully structured and compiled. Prize distribution complete.',
    data: {
      tournamentId,
      status: 'completed',
      winner: winnerRecord ? {
        freeFireUid: winnerRecord.freeFireUid,
        inGameName: winnerRecord.inGameName,
        prizeCredited: tournament.prizePool
      } : 'No players resolved'
    }
  });
}
