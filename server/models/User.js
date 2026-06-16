const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema({
  inGameName: {
    type: String,
    required: [true, 'In-game name is required'],
    trim: true
  },
  freeFireUid: {
    type: String,
    required: [true, 'Free Fire UID is required'],
    trim: true
  },
  phoneNumber: {
    type: String,
    trim: true
  },
  email: {
    type: String,
    required: [true, 'Email is required'],
    unique: true,
    trim: true,
    lowercase: true
  },
  depositBalance: {
    type: Number,
    default: 0.0,
    min: [0, 'Deposit balance cannot be negative']
  },
  winningBalance: {
    type: Number,
    default: 0.0,
    min: [0, 'Winning balance cannot be negative']
  },
  bonusBalance: {
    type: Number,
    default: 0.0,
    min: [0, 'Bonus balance cannot be negative']
  },
  referralCode: {
    type: String,
    unique: true,
    sparse: true,
    trim: true
  },
  referredBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    default: null
  },
  fcmToken: {
    type: String,
    trim: true,
    default: null
  },
  profilePicture: {
    type: String,
    default: null
  },
  isBlocked: {
    type: Boolean,
    default: false
  },
  role: {
    type: String,
    enum: ['user', 'admin'],
    default: 'user'
  }
}, {
  timestamps: true
});


module.exports = mongoose.model('User', UserSchema);
