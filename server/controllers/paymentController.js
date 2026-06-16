const paymentService = require('../services/paymentService');
const Transaction = require('../models/Transaction');

/**
 * @desc    Initiate a UPI payment top-up request
 * @route   POST /api/payments/initiate
 * @access  Private
 */
exports.initiatePayment = async (req, res) => {
  try {
    // 1. Confirm session authenticity
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'No active session found. Authentication required.'
      });
    }

    const userId = req.user.id || req.user._id;
    const { amount } = req.body;

    if (!amount || isNaN(amount)) {
      return res.status(400).json({
        success: false,
        message: 'A valid numeric top-up amount is required.'
      });
    }

    const amountNum = parseFloat(amount);
    if (amountNum < 10) {
      return res.status(400).json({
        success: false,
        message: 'Minimum top-up amount is ₹10.00.'
      });
    }

    // 2. Delegate to payment service layer
    const paymentIntent = await paymentService.createPaymentIntent(userId, amountNum);

    return res.status(200).json({
      success: true,
      message: 'Payment intent created successfully. Scan UPI URI to authorize.',
      data: paymentIntent
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to initiate payment gateway intent.',
      error: error.message
    });
  }
};

/**
 * @desc    Mock UPI webhook callback handler (External Payment Gateway Webhook Receiver)
 * @route   POST /api/webhooks/upi-callback
 * @access  Public
 */
exports.handleUpiCallback = async (req, res) => {
  try {
    const { invoiceId, upiTxnRef, status, gatewaySignature } = req.body;

    if (!invoiceId || !status) {
      return res.status(400).json({
        success: false,
        message: 'Required callback parameters are missing (invoiceId, status).'
      });
    }

    const validStatuses = ['SUCCESS', 'FAILED'];
    if (!validStatuses.includes(status.toUpperCase())) {
      return res.status(400).json({
        success: false,
        message: `Invalid transaction status in callback. Supported: ${validStatuses.join(', ')}`
      });
    }

    // Process callback
    const result = await paymentService.processUpiCallback({
      invoiceId,
      upiTxnRef: upiTxnRef || `REF-${Date.now()}`,
      status: status.toUpperCase(),
      gatewaySignature
    });

    return res.status(200).json({
      success: true,
      message: 'UPI callback parsed and verified successfully.',
      data: result
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Payment gateway callback error.',
      error: error.message
    });
  }
};

/**
 * @desc    Admin / Sandbox sandbox API to instantly mock bypass complete a pending payment
 * @route   POST /api/payments/simulate-success
 * @access  Private
 */
exports.simulateGatewaySuccess = async (req, res) => {
  try {
    const { invoiceId } = req.body;

    if (!invoiceId) {
      return res.status(400).json({
        success: false,
        message: 'Required input missing: invoiceId'
      });
    }

    const transaction = await Transaction.findOne({ invoiceId });
    if (!transaction) {
      return res.status(404).json({
        success: false,
        message: `No active transaction matches invoice ID matching "${invoiceId}"`
      });
    }

    if (transaction.status !== 'PENDING') {
      return res.status(400).json({
        success: false,
        message: `Transaction has already been finalized with status "${transaction.status}".`
      });
    }

    // Construct mock gateway verification signature
    const status = 'SUCCESS';
    const upiTxnRef = 'REF-SIM-' + Math.floor(100000 + Math.random() * 900000);
    const signature = Buffer.from(invoiceId + '_' + status).toString('base64');

    // Trigger processUpiCallback
    const result = await paymentService.processUpiCallback({
      invoiceId,
      upiTxnRef,
      status,
      gatewaySignature: signature
    });

    return res.status(200).json({
      success: true,
      message: 'Instantly bypassed & marked payment as SUCCESS. Wallet credited.',
      simulationResult: result
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Sandbox payment simulation failed.',
      error: error.message
    });
  }
};
