const jwt = require('jsonwebtoken');
const User = require('../models/User');

const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_esports_key';

/**
 * @desc    Middleware to authenticate any incoming request using standard JWT headers.
 */
exports.authenticateUser = async (req, res, next) => {
  try {
    let token;

    // Check Authorization header for Bearer token
    if (req.headers.authorization && req.headers.authorization.startsWith('Bearer ')) {
      token = req.headers.authorization.split(' ')[1];
    }

    if (!token) {
      return res.status(401).json({
        success: false,
        message: 'Access denied. Security authentication token is required.'
      });
    }

    // Verify JWT payload signatures
    const decoded = jwt.verify(token, JWT_SECRET);

    // Look up the active user to confirm their account still exists in Database
    const user = await User.findById(decoded.id);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'Account associated with this token no longer exists.'
      });
    }

    // Bind decoded user info and raw database model properties to Request context
    req.user = {
      id: user._id,
      _id: user._id,
      email: user.email,
      role: decoded.role || 'user',
      is2faVerified: decoded.is2faVerified || false
    };

    return next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({
        success: false,
        message: 'Security token has expired. Please sign in again.'
      });
    }
    return res.status(401).json({
      success: false,
      message: 'Access denied. Provided authentication signature is invalid.'
    });
  }
};

/**
 * @desc    Admin validation middleware ensuring administrative privilege levels 
 *          with an enforcement flag for Two-Factor Authentication (2FA).
 * @param   {Object} options Config option flag { require2FA: true/false }
 */
exports.requireAdmin = (options = { require2FA: true }) => {
  return (req, res, next) => {
    // 1. Confirm user is authenticated
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'Authentication is required before accessing administrative endpoints.'
      });
    }

    // 2. Validate administrative role identity
    if (req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Access denied. This action requires administrative scope and permission.'
      });
    }

    // 3. Optional Multi-Factor / 2FA check verification
    if (options.require2FA && !req.user.is2faVerified) {
      return res.status(403).json({
        success: false,
        message: 'Two-Factor Authentication is mandated for this admin route. Verification pending.',
        mfaRequired: true
      });
    }

    return next();
  };
};
