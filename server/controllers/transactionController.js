const mongoose = require('mongoose');
const Transaction = require('../models/Transaction');
const User = require('../models/User');

/**
 * @desc    Fetch and categorize active user's transaction history
 * @route   GET /api/transactions/history
 * @access  Private
 */
exports.getTransactionHistory = async (req, res) => {
  try {
    // 1. Confirm session authenticity
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'No active session found. Authentication required.'
      });
    }

    const userId = req.user.id || req.user._id;

    // Optional query params
    const page = parseInt(req.query.page, 10) || 1;
    const limit = parseInt(req.query.limit, 10) || 50;
    const skip = (page - 1) * limit;

    // 2. Fetch user details to provide contextual wallet summary
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'Requested user profile not found.'
      });
    }

    // 3. Query all transactions belonging to this user
    // We fetch a larger set (without limit) to compute accurate aggregate category registers,
    // or fetch the direct set and filter cleanly.
    const allUserTransactions = await Transaction.find({ userId })
      .sort({ timestamp: -1 });

    // 4. Categorize transactions into respective groups
    const deposits = [];
    const withdrawals = [];
    const entryFees = [];
    const otherTransactions = []; // To capture PRIZE_WINNING, BONUS_ADD, REFERRAL_REWARD, etc.

    // Compute sums for the requested buckets
    let totalDeposits = 0;
    let totalWithdrawals = 0;
    let totalEntryFees = 0;

    allUserTransactions.forEach(tx => {
      const amt = tx.amount || 0;
      
      if (tx.type === 'DEPOSIT') {
        deposits.push(tx);
        if (tx.status === 'SUCCESS') totalDeposits += amt;
      } else if (tx.type === 'WITHDRAWAL') {
        withdrawals.push(tx);
        if (tx.status === 'SUCCESS' || tx.status === 'PENDING') totalWithdrawals += amt;
      } else if (tx.type === 'ENTRY_FEE') {
        entryFees.push(tx);
        if (tx.status === 'SUCCESS') totalEntryFees += amt;
      } else {
        otherTransactions.push(tx);
      }
    });

    // 5. Build paginated listing for the main view
    const paginatedTransactions = allUserTransactions.slice(skip, skip + limit);
    const totalCount = allUserTransactions.length;

    return res.status(200).json({
      success: true,
      message: 'Transaction ledger retrieved and categorized successfully.',
      walletSummary: {
        balances: {
          deposit: user.depositBalance || 0,
          winnings: user.winningBalance || 0,
          bonus: user.bonusBalance || 0,
          total: (user.depositBalance || 0) + (user.winningBalance || 0) + (user.bonusBalance || 0)
        },
        aggregates: {
          totalDeposited: totalDeposits,
          totalWithdrawn: totalWithdrawals,
          totalEntryFeesInvested: totalEntryFees
        }
      },
      categories: {
        deposits: {
          count: deposits.length,
          totalAmount: totalDeposits,
          items: deposits.slice(0, 20) // send recent 20 in categories for quick view
        },
        withdrawals: {
          count: withdrawals.length,
          totalAmount: totalWithdrawals,
          items: withdrawals.slice(0, 20)
        },
        entryFees: {
          count: entryFees.length,
          totalAmount: totalEntryFees,
          items: entryFees.slice(0, 20)
        },
        others: {
          count: otherTransactions.length,
          items: otherTransactions.slice(0, 20)
        }
      },
      pagination: {
        totalItems: totalCount,
        currentPage: page,
        totalPages: Math.ceil(totalCount / limit),
        size: paginatedTransactions.length
      },
      transactions: paginatedTransactions
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to retrieve transaction auditing history.',
      error: error.message
    });
  }
};

/**
 * @desc    Simulate/Create a transaction for playground testing
 * @route   POST /api/transactions/mock
 * @access  Private
 */
exports.createTransactionMock = async (req, res) => {
  try {
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'Authentication required.'
      });
    }

    const userId = req.user.id || req.user._id;
    const { title, amount, type, category, status } = req.body;

    if (!title || !amount || !type || !category) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: title, amount, type, category.'
      });
    }

    const invoiceId = 'TXN-' + Math.random().toString(36).substr(2, 9).toUpperCase();

    const newTx = new Transaction({
      userId,
      title,
      amount,
      type,
      category,
      status: status || 'SUCCESS',
      invoiceId
    });

    await newTx.save();

    // Also update User balance if successful to keep state synced nicely
    const updateQuery = {};
    if (status !== 'FAILED') {
      const balanceField = category === 'DEPOSIT' ? 'depositBalance' : (category === 'WINNING' ? 'winningBalance' : 'bonusBalance');
      
      let balanceDelta = amount;
      if (['WITHDRAWAL', 'ENTRY_FEE', 'BONUS_DEDUCT'].includes(type)) {
        balanceDelta = -amount;
      }
      
      updateQuery['$inc'] = { [balanceField]: balanceDelta };
    }

    if (Object.keys(updateQuery).length > 0) {
      await User.findByIdAndUpdate(userId, updateQuery);
    }

    return res.status(211).json({
      success: true,
      message: 'Mock transaction documented successfully.',
      data: newTx
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to record mock transaction.',
      error: error.message
    });
  }
};
