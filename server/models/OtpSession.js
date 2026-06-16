const mongoose = require('mongoose');

const OtpSessionSchema = new mongoose.Schema({
  phoneNumber: {
    type: String,
    required: [true, 'Phone number is required'],
    trim: true
  },
  otpCode: {
    type: String,
    required: [true, 'OTP verification code is required']
  },
  expiresAt: {
    type: Date,
    required: [true, 'Expiration timestamp is required'],
    index: { expires: '10m' } // Automatically expire and purge the document after 10 minutes
  },
  isVerified: {
    type: Boolean,
    default: false
  }
}, {
  timestamps: true
});

module.exports = mongoose.model('OtpSession', OtpSessionSchema);
