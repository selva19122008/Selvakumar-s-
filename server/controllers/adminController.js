const mongoose = require('mongoose');
const User = require('../models/User');
const Transaction = require('../models/Transaction');
const Tournament = require('../models/Tournament');
const adminLogService = require('../services/adminLogService');

/**
 * @desc    Get Admin Dashboard Analytics for the current month
 * @route   GET /api/admin/dashboard-stats
 * @access  Private/Admin
 */
exports.getDashboardStats = async (req, res) => {
  try {
    // 1. Confirm Administrative Role Action
    if (!req.user || req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    // 2. Define timezone-adjusted boundaries for the current month
    const startOfMonth = new Date();
    startOfMonth.setDate(1);
    startOfMonth.setHours(0, 0, 0, 0);

    const endOfMonth = new Date();
    endOfMonth.setMonth(endOfMonth.getMonth() + 1);
    endOfMonth.setDate(0);
    endOfMonth.setHours(23, 59, 59, 999);

    // 3. Aggregate Daily Active Users (DAU) grouped by day
    const dailyActiveUsers = await User.aggregate([
      {
        $match: {
          updatedAt: { $gte: startOfMonth, $lte: endOfMonth }
        }
      },
      {
        $group: {
          _id: { $dateToString: { format: "%Y-%m-%d", date: "$updatedAt" } },
          uniqueUsers: { $addToSet: "$_id" }
        }
      },
      {
        $project: {
          date: "$_id",
          count: { $size: "$uniqueUsers" },
          _id: 0
        }
      },
      { $sort: { date: 1 } }
    ]);

    // Calculate Average DAU and Monthly Active Users (MAU)
    const mActiveUsers = await User.countDocuments({
      updatedAt: { $gte: startOfMonth, $lte: endOfMonth }
    });

    const totalDauSum = dailyActiveUsers.reduce((sum, item) => sum + item.count, 0);
    const avgDau = dailyActiveUsers.length > 0 
      ? parseFloat((totalDauSum / dailyActiveUsers.length).toFixed(2)) 
      : 0;

    // 4. Aggregate Revenue (Entry fees) and transaction frequency
    const revenueStats = await Transaction.aggregate([
      {
        $match: {
          type: 'ENTRY_FEE',
          status: 'SUCCESS',
          timestamp: { $gte: startOfMonth, $lte: endOfMonth }
        }
      },
      {
        $group: {
          _id: null,
          totalRevenue: { $sum: "$amount" },
          transactionCount: { $sum: 1 }
        }
      }
    ]);

    const revenueResult = revenueStats[0] || { totalRevenue: 0, transactionCount: 0 };

    // Daily revenue decomposition for dynamic chart rendering
    const dailyRevenueBreakdown = await Transaction.aggregate([
      {
        $match: {
          type: 'ENTRY_FEE',
          status: 'SUCCESS',
          timestamp: { $gte: startOfMonth, $lte: endOfMonth }
        }
      },
      {
        $group: {
          _id: { $dateToString: { format: "%Y-%m-%d", date: "$timestamp" } },
          revenue: { $sum: "$amount" }
        }
      },
      {
        $project: {
          date: "$_id",
          revenue: 1,
          _id: 0
        }
      },
      { $sort: { date: 1 } }
    ]);

    // 5. Aggregate Pending Tournament Payouts
    // Trace 1: Pending processing player withdrawal requests
    const pendingWithdrawalStats = await Transaction.aggregate([
      {
        $match: {
          type: 'WITHDRAWAL',
          status: 'PENDING',
          timestamp: { $gte: startOfMonth, $lte: endOfMonth }
        }
      },
      {
        $group: {
          _id: null,
          totalPendingAmount: { $sum: "$amount" },
          pendingCount: { $sum: 1 }
        }
      }
    ]);

    const pendingWithdrawalResult = pendingWithdrawalStats[0] || { totalPendingAmount: 0, pendingCount: 0 };

    // Trace 2: Scheduled prize pools of completed tournaments awaiting prize distribution
    const undistributedPrizeStats = await Tournament.aggregate([
      {
        $match: {
          status: 'completed',
          winnerUid: null,
          matchDate: { $gte: startOfMonth, $lte: endOfMonth }
        }
      },
      {
        $group: {
          _id: null,
          totalUndistributedPrizes: { $sum: "$prizePool" },
          completedAwaitingWinnerCount: { $sum: 1 }
        }
      }
    ]);

    const undistributedPrizeResult = undistributedPrizeStats[0] || { totalUndistributedPrizes: 0, completedAwaitingWinnerCount: 0 };

    // 6. Combine and dispatch structured diagnostic response
    return res.status(200).json({
      success: true,
      message: 'Admin metrics aggregated successfully for the current billing cycle.',
      data: {
        billingPeriod: {
          start: startOfMonth,
          end: endOfMonth
        },
        users: {
          monthlyActiveUsers: mActiveUsers,
          averageDailyActiveUsers: avgDau,
          dailyActiveUsersRecord: dailyActiveUsers
        },
        revenue: {
          totalAmount: revenueResult.totalRevenue,
          transactionCount: revenueResult.transactionCount,
          dailyBreakdown: dailyRevenueBreakdown
        },
        payouts: {
          pendingWithdrawalAmount: pendingWithdrawalResult.totalPendingAmount,
          pendingWithdrawalCount: pendingWithdrawalResult.pendingCount,
          undistributedTournamentPrizeAmount: undistributedPrizeResult.totalUndistributedPrizes,
          undistributedTournamentCount: undistributedPrizeResult.completedAwaitingWinnerCount,
          totalCombinedPendingObligations: pendingWithdrawalResult.totalPendingAmount + undistributedPrizeResult.totalUndistributedPrizes
        }
      }
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to calculate executive dashboard statistics.',
      error: error.message
    });
  }
};

/**
 * @desc    Fetch administrative audit log entries (Admin only)
 * @route   GET /api/admin/audit-logs
 * @access  Private/Admin
 */
exports.getAuditLogs = async (req, res) => {
  try {
    // 1. Confirm Administrative Role Action
    if (!req.user || req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    const { adminId, action, targetType, search, page, limit } = req.query;

    const result = await adminLogService.fetchAuditLogs({
      adminId,
      action,
      targetType,
      search,
      page,
      limit
    });

    return res.status(200).json({
      success: true,
      message: 'Administrative audit logs compiled successfully.',
      ...result
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to retrieve system audit logs.',
      error: error.message
    });
  }
};

/**
 * @desc    Adjust user wallet balances manually (Admin only)
 * @route   POST /api/admin/users/:id/adjust-wallet
 * @access  Private/Admin
 */
exports.adjustUserWallet = async (req, res) => {
  const session = await mongoose.startSession();
  session.startTransaction();

  try {
    // 1. Confirm Administrative Role Action
    if (!req.user || req.user.role !== 'admin') {
      await session.abortTransaction();
      session.endSession();
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    const { balanceType, amount, description } = req.body;
    const targetUserId = req.params.id;

    // Validate inputs
    const allowedBalanceTypes = ['depositBalance', 'winningBalance', 'bonusBalance'];
    if (!allowedBalanceTypes.includes(balanceType)) {
      await session.abortTransaction();
      session.endSession();
      return res.status(400).json({
        success: false,
        message: `Invalid balanceType. Must be one of: ${allowedBalanceTypes.join(', ')}`
      });
    }

    if (amount === undefined || typeof amount !== 'number' || amount === 0) {
      await session.abortTransaction();
      session.endSession();
      return res.status(400).json({
        success: false,
        message: 'A valid non-zero numeric adjustment amount is required.'
      });
    }

    if (!description || !description.trim()) {
      await session.abortTransaction();
      session.endSession();
      return res.status(400).json({
        success: false,
        message: 'A descriptive reason or comment is required for ledger accountability.'
      });
    }

    // 2. Fetch User Profile within Transaction session
    const user = await User.findById(targetUserId).session(session);
    if (!user) {
      await session.abortTransaction();
      session.endSession();
      return res.status(404).json({
        success: false,
        message: 'The target user profile was not found.'
      });
    }

    const originalValue = user[balanceType];
    const targetValue = originalValue + amount;

    if (targetValue < 0) {
      await session.abortTransaction();
      session.endSession();
      return res.status(400).json({
        success: false,
        message: `Adjustment rejected. Target balance would drop below zero. Current: ${originalValue}, Intended delta: ${amount}`
      });
    }

    // 3. Set properties and persist
    user[balanceType] = targetValue;
    await user.save({ session });

    // 4. Create secondary transaction ledger trace
    let txCategory = 'DEPOSIT';
    if (balanceType === 'winningBalance') txCategory = 'WINNING';
    if (balanceType === 'bonusBalance') txCategory = 'BONUS';

    const txType = amount > 0 
      ? (balanceType === 'bonusBalance' ? 'BONUS_ADD' : 'DEPOSIT') 
      : (balanceType === 'bonusBalance' ? 'BONUS_DEDUCT' : 'WITHDRAWAL');

    const invoiceId = `ADJ-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
    const systemTransaction = new Transaction({
      userId: targetUserId,
      title: `Admin adjustment: ${description.trim()}`,
      amount: Math.abs(amount),
      type: txType,
      category: txCategory,
      status: 'SUCCESS',
      invoiceId: invoiceId
    });
    await systemTransaction.save({ session });

    // 5. Commit database operations
    await session.commitTransaction();
    session.endSession();

    // 6. Asynchronously append audit traces
    await adminLogService.logAction({
      adminId: req.user.id || req.user._id,
      action: 'ADJUST_WALLET',
      targetType: 'User',
      targetId: targetUserId,
      description: `Adjusted user ${user.inGameName}'s ${balanceType} by ${amount > 0 ? '+' : ''}${amount}. Reason: ${description.trim()}`,
      changes: {
        field: balanceType,
        before: originalValue,
        after: targetValue,
        transactionId: systemTransaction._id
      },
      req
    });

    return res.status(200).json({
      success: true,
      message: 'User wallet balance successfully modified.',
      data: {
        userId: user._id,
        inGameName: user.inGameName,
        balanceType,
        previousBalance: originalValue,
        currentBalance: targetValue,
        ledgerInvoice: invoiceId
      }
    });

  } catch (error) {
    await session.abortTransaction();
    session.endSession();
    return res.status(500).json({
      success: false,
      message: 'Administrative transaction failed to apply balance adjustments.',
      error: error.message
    });
  }
};

/**
 * @desc    Block or suspend user accounts (Admin only)
 * @route   PUT /api/admin/users/:id/block-status
 * @access  Private/Admin
 */
exports.blockOrUnblockUser = async (req, res) => {
  try {
    // 1. Confirm Administrative Role Action
    if (!req.user || req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    const targetUserId = req.params.id;
    const { isBlocked, reason } = req.body;

    if (isBlocked === undefined) {
      return res.status(400).json({
        success: false,
        message: 'The parameter "isBlocked" (true/false) is required.'
      });
    }

    if (isBlocked && (!reason || !reason.trim())) {
      return res.status(400).json({
        success: false,
        message: 'A valid reason is required before suspending an account.'
      });
    }

    // 2. Fetch target user
    const user = await User.findById(targetUserId);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'Target user account not found.'
      });
    }

    const previousStatus = user.isBlocked || false;
    if (previousStatus === isBlocked) {
      return res.status(400).json({
        success: false,
        message: `Account state is already configured as isBlocked: ${isBlocked}`
      });
    }

    // 3. Persist modifications
    user.isBlocked = isBlocked;
    await user.save();

    // 4. Record audit trace log
    const eventLabel = isBlocked ? 'BLOCK_USER' : 'UNBLOCK_USER';
    const logDesc = isBlocked 
      ? `Blocked player ${user.inGameName} (${user.email}). Reason: ${reason.trim()}`
      : `Restored player ${user.inGameName} (${user.email}).`;

    await adminLogService.logAction({
      adminId: req.user.id || req.user._id,
      action: eventLabel,
      targetType: 'User',
      targetId: targetUserId,
      description: logDesc,
      changes: {
        before: { isBlocked: previousStatus },
        after: { isBlocked },
        reason: reason ? reason.trim() : 'N/A'
      },
      req
    });

    return res.status(200).json({
      success: true,
      message: `User account has been successfully ${isBlocked ? 'suspended' : 'unlocked'}.`,
      data: {
        userId: user._id,
        inGameName: user.inGameName,
        email: user.email,
        isBlocked: user.isBlocked
      }
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to modify account suspension state.',
      error: error.message
    });
  }
};


