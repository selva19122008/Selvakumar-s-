const mongoose = require('mongoose');
const Transaction = require('../models/Transaction');
const User = require('../models/User');
const adminLogService = require('./adminLogService');

/**
 * Payment Gateway Service
 * Manages secure transaction ledgering, intent generation, and payment callbacks.
 */

/**
 * Generates an checkout payment request for UPI top-ups, registering a PENDING state transaction.
 * 
 * @param {string} userId - Target mongoose.Types.ObjectId.
 * @param {number} amount - Amount in INR (₹) to add.
 * @returns {Promise<Object>} Mock Payment Initiation Intent.
 */
exports.createPaymentIntent = async (userId, amount) => {
  if (!amount || amount < 10) {
    throw new Error('Minimum deposit of ₹10.00 is required.');
  }

  // Generate a mock unique reference ID (UPI transaction ID)
  const txRefNum = 'UPI' + Date.now() + Math.floor(1000 + Math.random() * 9000);
  const orderId = 'ORD-' + Math.random().toString(36).substr(2, 9).toUpperCase();

  // Construct a standard industry UPI Deep-Link URI format:
  // upi://pay?pa=merchant@bank&pn=BattleZone%20FF&tr=TxRef&am=Amount&cu=INR
  const upiPayloadUri = `upi://pay?pa=battlezoneff@yesbank&pn=BattleZone%20FF%20Gaming&tr=${txRefNum}&am=${amount.toFixed(2)}&cu=INR&tn=Wallet%20TopUp%20-${orderId}`;

  // Register the transaction with status PENDING to prevent phantom updates or double-spend
  const transaction = new Transaction({
    userId,
    title: `UPI Deposit Request: ₹${amount.toFixed(2)}`,
    amount,
    type: 'DEPOSIT',
    category: 'DEPOSIT',
    status: 'PENDING',
    invoiceId: txRefNum
  });

  await transaction.save();

  return {
    orderId,
    transactionId: transaction._id,
    invoiceId: txRefNum,
    amount,
    upiUri: upiPayloadUri,
    status: 'PENDING',
    paymentGateway: 'MOCK_UPI_PAY',
    createdAt: transaction.createdAt
  };
};

/**
 * Handles incoming UPI payment callbacks, updating Transaction and user balance atomically.
 * 
 * @param {Object} payload Callback payload parameters.
 * @param {string} payload.invoiceId - Unique reference code generated during intent creation.
 * @param {string} payload.upiTxnRef - UPI network reference returned by provider.
 * @param {string} payload.status - ('SUCCESS' | 'FAILED') payment status.
 * @param {string} [payload.gatewaySignature] - Verification key simulating security signature.
 * @returns {Promise<Object>} Processed transaction summary.
 */
exports.processUpiCallback = async ({ invoiceId, upiTxnRef, status, gatewaySignature }) => {
  const session = await mongoose.startSession();
  session.startTransaction();

  try {
    // 1. Verify signatures if configured (Simulating webhook security verification)
    const expectedSignature = Buffer.from(invoiceId + '_' + status).toString('base64');
    if (gatewaySignature && gatewaySignature !== expectedSignature) {
      throw new Error('Security signature validation failure. Suspicious callback rejected.');
    }

    // 2. Fetch the existing pending transaction of type DEPOSIT
    const transaction = await Transaction.findOne({ invoiceId })
      .session(session);

    if (!transaction) {
      throw new Error(`No matching transaction record found for invoice ID: ${invoiceId}`);
    }

    // 3. Prevent duplicate credit execution
    if (transaction.status !== 'PENDING') {
      throw new Error(`Transaction ${invoiceId} has already been resolved with status: ${transaction.status}`);
    }

    const userId = transaction.userId;
    const amount = transaction.amount;

    // 4. Update state depending on final UPI gateway status
    if (status === 'SUCCESS') {
      transaction.status = 'SUCCESS';
      transaction.title = `UPI Deposit Completed: ₹${amount.toFixed(2)}`;
      await transaction.save({ session });

      // Increment User's depositBalance atomically
      const user = await User.findByIdAndUpdate(
        userId,
        { $inc: { depositBalance: amount } },
        { session, new: true }
      );

      if (!user) {
        throw new Error('Linked user account could not be found to credit balance.');
      }

      // Log the credit action for administration transparency
      await adminLogService.logAction({
        adminId: new mongoose.Types.ObjectId(), // System-generated event
        action: 'USER_WALLET_ADJUSTMENT',
        targetType: 'User',
        targetId: userId,
        details: `Automated UPI credit: Added ₹${amount.toFixed(2)} to Deposit balance. New Balance: ₹${user.depositBalance.toFixed(2)}`
      });

      await session.commitTransaction();

      return {
        success: true,
        message: 'Wallet top-up processed and credited successfully.',
        transaction: {
          id: transaction._id,
          invoiceId,
          amount,
          status: 'SUCCESS',
          type: 'DEPOSIT'
        },
        balances: {
          deposit: user.depositBalance,
          winnings: user.winningBalance,
          bonus: user.bonusBalance
        }
      };

    } else {
      // Handle failed UPI transactions
      transaction.status = 'FAILED';
      transaction.title = `UPI Deposit Failed: ₹${amount.toFixed(2)}`;
      await transaction.save({ session });

      await session.commitTransaction();

      return {
        success: false,
        message: 'UPI payment indicated failed transaction status.',
        transaction: {
          id: transaction._id,
          invoiceId,
          amount,
          status: 'FAILED',
          type: 'DEPOSIT'
        }
      };
    }

  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    session.endSession();
  }
};
