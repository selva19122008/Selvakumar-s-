const mongoose = require('mongoose');
const Tournament = require('../models/Tournament');
const User = require('../models/User');
const TournamentJoin = require('../models/TournamentJoin');
const Transaction = require('../models/Transaction');
const referralService = require('../services/referralService');
const adminLogService = require('../services/adminLogService');
const tournamentDiscoveryService = require('../services/tournamentDiscoveryService');
const payoutService = require('../services/payoutService');

/**
 * @desc    Create a new tournament (Admin only)
 * @route   POST /api/tournaments
 * @access  Private/Admin
 */
exports.createTournament = async (req, res) => {
  try {
    // 1. Confirm Administrative Role Action
    if (!req.user || req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    const { title, matchDate, entryFee, prizePool, maxSlots, mapType, format, rules, roomId, roomPassword } = req.body;

    // 2. Initial administrative level validations
    if (entryFee < 0) {
      return res.status(400).json({ success: false, message: 'Entry fee cannot be negative.' });
    }
    if (prizePool < 0) {
      return res.status(400).json({ success: false, message: 'Prize pool cannot be negative.' });
    }
    if (!Number.isInteger(maxSlots) || maxSlots <= 0) {
      return res.status(400).json({ success: false, message: 'Max slots must be a positive integer.' });
    }

    const parsedDate = new Date(matchDate);
    if (isNaN(parsedDate.getTime()) || parsedDate <= new Date()) {
      return res.status(400).json({ success: false, message: 'A valid future date and time is required for the match date.' });
    }

    // 3. Create instance and trigger Mongoose Schema validations
    const tournament = new Tournament({
      title,
      matchDate: parsedDate,
      entryFee,
      prizePool,
      maxSlots,
      mapType,
      format,
      rules,
      roomId,
      roomPassword,
      createdBy: req.user.id || req.user._id // Tracking administrative audit log
    });

    const savedTournament = await tournament.save();

    await adminLogService.logAction({
      adminId: req.user.id || req.user._id,
      action: 'CREATE_TOURNAMENT',
      targetType: 'Tournament',
      targetId: savedTournament._id,
      description: `Scheduled new tournament: "${savedTournament.title}" on date: ${savedTournament.matchDate}`,
      changes: savedTournament,
      req
    });

    return res.status(201).json({
      success: true,
      message: 'Tournament successfully scheduled.',
      data: savedTournament
    });
  } catch (error) {
    if (error.name === 'ValidationError') {
      const messages = Object.values(error.errors).map(val => val.message);
      return res.status(400).json({ success: false, errors: messages });
    }
    return res.status(500).json({
      success: false,
      message: 'Server error occurred during tournament preparation.',
      error: error.message
    });
  }
};

/**
 * @desc    Update tournament fields and status (Admin only)
 * @route   PUT /api/tournaments/:id
 * @access  Private/Admin
 */
exports.updateTournament = async (req, res) => {
  try {
    // 1. Confirm Administrative Role Action
    if (!req.user || req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    const { id } = req.params;
    const tournament = await Tournament.findById(id);

    if (!tournament) {
      return res.status(404).json({
        success: false,
        message: `Tournament with ID ${id} not found.`
      });
    }

    // 2. Enforce Lifecycle State Machine validation
    const currentStatus = tournament.status;
    const newStatus = req.body.status;

    if (newStatus && currentStatus !== newStatus) {
      // Completed tournaments cannot be modified or re-opened
      if (currentStatus === 'completed') {
        return res.status(400).json({
          success: false,
          message: 'Completed tournaments are locked and their statuses cannot be changed.'
        });
      }

      // Upcoming cannot skip directly to Completed
      if (currentStatus === 'upcoming' && newStatus === 'completed') {
        return res.status(400).json({
          success: false,
          message: 'An upcoming tournament must be set to "live" before being marked as "completed".'
        });
      }
    }

    // 3. Prevent reducing maxSlots below currently joined slots
    if (req.body.maxSlots !== undefined) {
      const requestedMaxSlots = Number(req.body.maxSlots);
      if (!Number.isInteger(requestedMaxSlots) || requestedMaxSlots <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Maximum slots must be a positive integer.'
        });
      }
      if (requestedMaxSlots < tournament.joinedSlots) {
        return res.status(400).json({
          success: false,
          message: `Cannot reduce max slots to ${requestedMaxSlots} because ${tournament.joinedSlots} players have already joined.`
        });
      }
    }

    // 4. Update allowed fields
    const checkAndAssign = (field) => {
      if (req.body[field] !== undefined) {
        tournament[field] = req.body[field];
      }
    };

    checkAndAssign('title');
    checkAndAssign('entryFee');
    checkAndAssign('prizePool');
    checkAndAssign('maxSlots');
    checkAndAssign('mapType');
    checkAndAssign('format');
    checkAndAssign('status');
    checkAndAssign('rules');
    checkAndAssign('roomId');
    checkAndAssign('roomPassword');

    if (req.body.matchDate) {
      const parsedDate = new Date(req.body.matchDate);
      if (!isNaN(parsedDate.getTime())) {
        tournament.matchDate = parsedDate;
      } else {
        return res.status(400).json({ success: false, message: 'Invalid match date format provided.' });
      }
    }

    // Check if transitioning to completed status, use the automated payout service
    if (newStatus === 'completed' && currentStatus !== 'completed') {
      const checkAndAssignExcluded = (field) => {
        if (req.body[field] !== undefined && field !== 'status') {
          tournament[field] = req.body[field];
        }
      };

      checkAndAssignExcluded('title');
      checkAndAssignExcluded('entryFee');
      checkAndAssignExcluded('prizePool');
      checkAndAssignExcluded('maxSlots');
      checkAndAssignExcluded('mapType');
      checkAndAssignExcluded('format');
      checkAndAssignExcluded('rules');
      checkAndAssignExcluded('roomId');
      checkAndAssignExcluded('roomPassword');

      await tournament.save();

      // Perform automated rank-based prize payout distribution
      const payoutResult = await payoutService.distributeTournamentPrizes(id);
      
      // Fetch the updated completed tournament to return to the admin
      const finalCompletedTournament = await Tournament.findById(id);

      await adminLogService.logAction({
        adminId: req.user.id || req.user._id,
        action: 'UPDATE_TOURNAMENT',
        targetType: 'Tournament',
        targetId: id,
        description: `Marked tournament "${tournament.title}" as COMPLETED. Rankings calculated and prize pool distributed.`,
        changes: {
          beforeStatus: currentStatus,
          afterStatus: 'completed',
          updatedFields: Object.keys(req.body)
        },
        req
      });

      return res.status(200).json({
        success: true,
        message: 'Tournament set to COMPLETED. Individual rankings calculated and prize pool distributed to winner wallets.',
        data: finalCompletedTournament,
        payouts: payoutResult
      });
    }

    // 5. Trigger validations and save
    const updatedTournament = await tournament.save();

    await adminLogService.logAction({
      adminId: req.user.id || req.user._id,
      action: 'UPDATE_TOURNAMENT',
      targetType: 'Tournament',
      targetId: updatedTournament._id,
      description: `Modified details or state on tournament: "${updatedTournament.title}"`,
      changes: {
        beforeStatus: currentStatus,
        afterStatus: updatedTournament.status,
        updatedFields: Object.keys(req.body)
      },
      req
    });

    return res.status(200).json({
      success: true,
      message: 'Tournament successfully updated.',
      data: updatedTournament
    });
  } catch (error) {
    if (error.name === 'ValidationError') {
      const messages = Object.values(error.errors).map(val => val.message);
      return res.status(400).json({ success: false, errors: messages });
    }
    return res.status(500).json({
      success: false,
      message: 'Server error encountered during tournament update.',
      error: error.message
    });
  }
};

/**
 * @desc    Fetch a list of tournaments with filtering, searching, and pagination
 * @route   GET /api/tournaments
 * @access  Public
 */
exports.fetchTournaments = async (req, res) => {
  try {
    const { 
      status, 
      mapType, 
      format, 
      search, 
      sort, 
      minEntryFee, 
      maxEntryFee, 
      sortBy: querySortBy, 
      sortOrder: querySortOrder, 
      page = 1, 
      limit = 10 
    } = req.query;

    // Preserve backwards compatibility with existing sorting keys ('prize_desc', 'prize_asc', 'date_desc')
    let resolvedSortBy = querySortBy || 'matchDate';
    let resolvedSortOrder = querySortOrder || 'asc';

    if (sort) {
      if (sort === 'prize_desc') {
        resolvedSortBy = 'prizePool';
        resolvedSortOrder = 'desc';
      } else if (sort === 'prize_asc') {
        resolvedSortBy = 'prizePool';
        resolvedSortOrder = 'asc';
      } else if (sort === 'date_desc') {
        resolvedSortBy = 'matchDate';
        resolvedSortOrder = 'desc';
      }
    }

    // Delegate tournament discovery to our clean service layer
    const discoveryResult = await tournamentDiscoveryService.discoverTournaments({
      mapType,
      format,
      minEntryFee,
      maxEntryFee,
      status,
      search,
      sortBy: resolvedSortBy,
      sortOrder: resolvedSortOrder,
      page,
      limit
    });

    return res.status(200).json({
      success: true,
      count: discoveryResult.data.length,
      pagination: discoveryResult.pagination,
      data: discoveryResult.data
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to query/discover tournaments.',
      error: error.message
    });
  }
};

/**
 * @desc    Get details of a single tournament by ID
 * @route   GET /api/tournaments/:id
 * @access  Public
 */
exports.getTournamentById = async (req, res) => {
  try {
    const { id } = req.params;
    const tournament = await Tournament.findById(id);

    if (!tournament) {
      return res.status(404).json({
        success: false,
        message: `Tournament with ID ${id} not found.`
      });
    }

    return res.status(200).json({
      success: true,
      data: tournament
    });
  } catch (error) {
    if (error.name === 'CastError') {
      return res.status(400).json({
        success: false,
        message: 'Invalid tournament ID format.'
      });
    }
    return res.status(500).json({
      success: false,
      message: 'Server failed to locate the specified tournament.',
      error: error.message
    });
  }
};

/**
 * @desc    Register a user for a tournament
 * @route   POST /api/tournaments/:id/register
 * @access  Private
 */
exports.registerTournament = async (req, res) => {
  const session = await mongoose.startSession();
  try {
    session.startTransaction();

    // 1. Confirm client is logged in
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'Authentication required to register for tournaments.'
      });
    }

    const userId = req.user.id || req.user._id;
    const tournamentId = req.params.id;
    const { freeFireUid, inGameName } = req.body;

    if (!freeFireUid || !inGameName) {
      return res.status(400).json({
        success: false,
        message: 'Both Free Fire UID and In-Game Name are required for registration.'
      });
    }

    // 2. Query tournament within transaction
    const tournament = await Tournament.findById(tournamentId).session(session);
    if (!tournament) {
      return res.status(404).json({
        success: false,
        message: `Tournament with ID ${tournamentId} not found.`
      });
    }

    // 3. Status Check (upcoming only)
    if (tournament.status !== 'upcoming') {
      return res.status(400).json({
        success: false,
        message: `Registration is closed. This tournament is currently ${tournament.status}.`
      });
    }

    // 4. Slots Check
    if (tournament.joinedSlots >= tournament.maxSlots) {
      return res.status(400).json({
        success: false,
        message: 'This tournament is already full. No available slots.'
      });
    }

    // 5. Existing Registration Check
    const existingRegistration = await TournamentJoin.findOne({ userId, tournamentId }).session(session);
    if (existingRegistration) {
      return res.status(400).json({
        success: false,
        message: 'You have already registered for this tournament.'
      });
    }

    // 6. User and Wallet Balance Check
    const user = await User.findById(userId).session(session);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User profile not found.'
      });
    }

    const entryFee = tournament.entryFee;
    const totalRealBalance = user.depositBalance + user.winningBalance;

    if (totalRealBalance < entryFee) {
      return res.status(400).json({
        success: false,
        message: `Sufficient real cash balance unavailable. Entry fee: ₹${entryFee}, your real cash balance: ₹${totalRealBalance}`
      });
    }

    // 7. Deduct wallet balances using priority: Deposit -> Winnings (excluding bonus balance from tournament entries)
    let remainingToDeduct = entryFee;
    let deductedBonus = 0;
    let deductedDeposit = 0;
    let deductedWinning = 0;

    if (remainingToDeduct > 0 && user.depositBalance > 0) {
      const depositDeduct = Math.min(user.depositBalance, remainingToDeduct);
      user.depositBalance -= depositDeduct;
      deductedDeposit = depositDeduct;
      remainingToDeduct -= depositDeduct;
    }

    if (remainingToDeduct > 0 && user.winningBalance > 0) {
      const winningDeduct = Math.min(user.winningBalance, remainingToDeduct);
      user.winningBalance -= winningDeduct;
      deductedWinning = winningDeduct;
      remainingToDeduct -= winningDeduct;
    }

    await user.save({ session });

    // 8. Find Lowest Available Seat Number
    const existingJoins = await TournamentJoin.find({ tournamentId }).select('seatNumber').session(session);
    const occupiedSeats = new Set(existingJoins.map(join => join.seatNumber));
    let seatNumber = 1;
    while (occupiedSeats.has(seatNumber)) {
      seatNumber++;
    }

    // Double check seat bounds
    if (seatNumber > tournament.maxSlots) {
      return res.status(400).json({
        success: false,
        message: 'Seat assigner exceeded max tournament capacity.'
      });
    }

    // 9. Increment joined slots
    tournament.joinedSlots += 1;
    await tournament.save({ session });

    // 10. Record Tournament Registration Entry
    const join = new TournamentJoin({
      userId,
      tournamentId,
      freeFireUid,
      inGameName,
      seatNumber
    });
    await join.save({ session });

    // 11. Create Transaction Logs
    const invoiceId = `TXN-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
    
    const transaction = new Transaction({
      userId,
      title: `Joined Free Fire tournament: ${tournament.title}`,
      amount: entryFee,
      type: 'ENTRY_FEE',
      category: deductedWinning > 0 ? 'WINNING' : (deductedDeposit > 0 ? 'DEPOSIT' : 'BONUS'),
      status: 'SUCCESS',
      invoiceId
    });
    await transaction.save({ session });

    // 12. Distribute Referral bonus credits if user was referred and this is their first tournament join
    let referralAwardResult = null;
    try {
      referralAwardResult = await referralService.awardFirstRegistrationBonus(userId, tournament.title, session);
    } catch (refError) {
      // In a transaction, if we want strict enforcement, we throw the error so that the registration is rolled back, 
      // preventing abuse if database is inconsistent.
      throw new Error(`Referral payout failed: ${refError.message}`);
    }

    await session.commitTransaction();
    session.endSession();

    // Dynamically retrieve the freshly updated profile balance from database
    const finalUser = await User.findById(userId);

    return res.status(201).json({
      success: true,
      message: 'Successfully registered for the tournament.',
      data: {
        registration: join,
        seatNumber,
        newBalances: {
          depositBalance: finalUser ? finalUser.depositBalance : user.depositBalance,
          winningBalance: finalUser ? finalUser.winningBalance : user.winningBalance,
          bonusBalance: finalUser ? finalUser.bonusBalance : user.bonusBalance
        },
        transactionId: transaction._id,
        referralStatus: referralAwardResult || { status: 'DEFERRED' }
      }
    });

  } catch (error) {
    await session.abortTransaction();
    session.endSession();
    return res.status(500).json({
      success: false,
      message: 'Registration transaction failed and aborted.',
      error: error.message
    });
  }
};

/**
 * @desc    Fetch tournament participation history for the logged-in user, filtered by status (live, completed)
 * @route   GET /api/tournaments/my-participation
 * @access  Private
 */
exports.getMyParticipationHistory = async (req, res) => {
  try {
    // 1. Authenticate Request context
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'No active session found. Authentication is required.'
      });
    }

    const userId = req.user.id || req.user._id;
    const mongooseUserId = new mongoose.Types.ObjectId(userId);

    const { status, page = 1, limit = 10 } = req.query;
    const pageNum = Math.max(1, parseInt(page) || 1);
    const limitNum = Math.max(1, parseInt(limit) || 10);
    const skipNum = (pageNum - 1) * limitNum;

    // 2. Build multi-variable match query stages
    const matchStage = {};
    if (status) {
      // Support comma-separated status values (e.g. live,completed)
      const statusArray = status.split(',').map(s => s.trim().toLowerCase());
      
      // Validate requested status triggers
      const allowedStatuses = ['upcoming', 'live', 'completed', 'cancelled'];
      const validStatuses = statusArray.filter(s => allowedStatuses.includes(s));
      
      if (validStatuses.length > 0) {
        matchStage['tournament.status'] = { $in: validStatuses };
      }
    }

    // 3. Assemble dynamic aggregation pipeline
    const pipeline = [
      // Step A: Target registrations matching the authenticated user
      {
        $match: {
          userId: mongooseUserId
        }
      },
      // Step B: Left-outer-join target Tournament document
      {
        $lookup: {
          from: Tournament.collection.name,
          localField: 'tournamentId',
          foreignField: '_id',
          as: 'tournament'
        }
      },
      // Step C: Decompose array reference to single plain object
      {
        $unwind: {
          path: '$tournament',
          preserveNullAndEmptyArrays: false // Only keep registrations where the tournament exists
        }
      }
    ];

    // Step D: Apply optional tournament-level status filters if requested
    if (Object.keys(matchStage).length > 0) {
      pipeline.push({ $match: matchStage });
    }

    // Capture count metrics before applying pagination limits
    const countPipeline = [...pipeline, { $count: 'total' }];
    const countResult = await TournamentJoin.aggregate(countPipeline);
    const totalItems = countResult.length > 0 ? countResult[0].total : 0;

    // Step E: Sort with most recent matchDate descending
    pipeline.push({
      $sort: {
        'tournament.matchDate': -1
      }
    });

    // Step F: Apply structural pagination variables
    pipeline.push({ $skip: skipNum });
    pipeline.push({ $limit: limitNum });

    // Step G: Project beautiful, high-fidelity response payload
    pipeline.push({
      $project: {
        _id: 1,
        userId: 1,
        tournamentId: 1,
        freeFireUid: 1,
        inGameName: 1,
        seatNumber: 1,
        kills: 1,
        points: 1,
        joinedAt: 1,
        tournament: {
          _id: '$tournament._id',
          id: '$tournament._id',
          title: '$tournament.title',
          matchDate: '$tournament.matchDate',
          entryFee: '$tournament.entryFee',
          prizePool: '$tournament.prizePool',
          maxSlots: '$tournament.maxSlots',
          mapType: '$tournament.mapType',
          status: '$tournament.status',
          rules: '$tournament.rules',
          roomId: '$tournament.roomId',
          roomPassword: '$tournament.roomPassword',
          winnerUid: '$tournament.winnerUid',
          winnerName: '$tournament.winnerName'
        }
      }
    });

    const participationHistory = await TournamentJoin.aggregate(pipeline);

    return res.status(200).json({
      success: true,
      count: participationHistory.length,
      pagination: {
        totalItems,
        currentPage: pageNum,
        totalPages: Math.ceil(totalItems / limitNum),
        itemsPerPage: limitNum
      },
      message: 'User tournament participation history retrieved successfully.',
      data: participationHistory
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to compile tournament participation history.',
      error: error.message
    });
  }
};

