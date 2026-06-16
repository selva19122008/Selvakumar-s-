const mongoose = require('mongoose');

const AdminLogSchema = new mongoose.Schema({
  adminId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'Administrative agent identifier is required']
  },
  action: {
    type: String,
    required: [true, 'Administrative action identifier is required'],
    trim: true,
    index: true
  },
  targetType: {
    type: String,
    required: [true, 'Target resource type is required'],
    enum: ['User', 'Tournament', 'Faq', 'Transaction', 'System'],
    index: true
  },
  targetId: {
    type: String,
    default: null,
    index: true
  },
  description: {
    type: String,
    required: [true, 'Detailed action log description is required'],
    trim: true
  },
  changes: {
    type: mongoose.Schema.Types.Mixed,
    default: null
  },
  ipAddress: {
    type: String,
    default: null,
    trim: true
  },
  userAgent: {
    type: String,
    default: null,
    trim: true
  }
}, {
  timestamps: { createdAt: true, updatedAt: false } // Only createdAt is needed for immutable logs
});

module.exports = mongoose.model('AdminLog', AdminLogSchema);
