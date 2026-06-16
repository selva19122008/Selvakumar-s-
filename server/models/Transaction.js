const mongoose = require('mongoose');

const TransactionSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'User reference is required']
  },
  title: {
    type: String,
    required: [true, 'Transaction title is required']
  },
  amount: {
    type: Number,
    required: [true, 'Transaction amount is required']
  },
  type: {
    type: String,
    required: [true, 'Transaction type is required'],
    enum: ['DEPOSIT', 'WITHDRAWAL', 'ENTRY_FEE', 'PRIZE_WINNING', 'BONUS_ADD', 'BONUS_DEDUCT', 'REFERRAL_REWARD']
  },
  category: {
    type: String,
    required: [true, 'Transaction category is required'],
    enum: ['DEPOSIT', 'WINNING', 'BONUS']
  },
  status: {
    type: String,
    required: [true, 'Transaction status is required'],
    enum: ['SUCCESS', 'PENDING', 'FAILED'],
    default: 'SUCCESS'
  },
  invoiceId: {
    type: String,
    required: [true, 'Invoice or reference ID is required']
  },
  timestamp: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true
});

module.exports = mongoose.model('Transaction', TransactionSchema);
