const razorpayService = require('../services/razorpayPaymentService');

/**
 * Razorpay Payment Gateway Controller
 * Manages production order creations and webhook signature ingestion.
 */

/**
 * @desc    Generate a dedicated authenticated checkout order on Razorpay for user deposit
 * @route   POST /api/razorpay/create-order
 * @access  Private
 */
exports.initiateCheckoutOrder = async (req, res) => {
  try {
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
        message: 'A valid numeric top-up deposit amount is required.'
      });
    }

    const numericAmount = parseFloat(amount);
    const checkoutResult = await razorpayService.createRazorpayOrder(userId, numericAmount);

    return res.status(200).json({
      success: true,
      message: 'Razorpay payment order initialized.',
      data: checkoutResult
    });

  } catch (error) {
    console.error('Checkout Initialization Error:', error);
    return res.status(500).json({
      success: false,
      message: 'Gateway controller failed to generate order token.',
      error: error.message
    });
  }
};

/**
 * @desc    Standard Webhook Receiver endpoint triggered asynchronously by Razorpay servers
 *          immediately upon final card/UPI capture.
 * @route   POST /api/razorpay/webhook
 * @access  Public
 */
exports.handleServerWebhook = async (req, res) => {
  try {
    const signature = req.headers['x-razorpay-signature'];
    
    // In a live express setup, body-parser must pass rawBody to preserve exact cryptographic integrity.
    // E.g., express.json({ verify: (req, res, buf) => { req.rawBody = buf.toString(); } })
    const rawPayloadString = req.rawBody || JSON.stringify(req.body);

    if (!signature) {
      return res.status(400).json({
        success: false,
        message: 'Webhooks require an authentic cryptographic x-razorpay-signature header.'
      });
    }

    const processingResult = await razorpayService.verifyAndProcessWebhook(rawPayloadString, signature);

    // Razorpay requires a flat 200 OK block to acknowledge successful ingestion and stop retries
    return res.status(200).json({
      success: true,
      data: processingResult
    });

  } catch (error) {
    console.error('Fatal Webhook processing error:', error);
    // Keep response 400 or 500 for failures to instruct gateway to retry delivery later
    return res.status(400).json({
      success: false,
      message: 'Signature mismatched or processing dropped.',
      error: error.message
    });
  }
};
