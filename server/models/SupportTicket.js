const mongoose = require('mongoose');

const SupportTicketSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'User identifier is required']
  },
  subject: {
    type: String,
    required: [true, 'Ticket subject is required'],
    trim: true,
    maxlength: [150, 'Subject cannot exceed 150 characters']
  },
  description: {
    type: String,
    required: [true, 'Ticket description details are required'],
    trim: true,
    maxlength: [1500, 'Description cannot exceed 1500 characters']
  },
  category: {
    type: String,
    required: [true, 'Category classification is required'],
    enum: {
      values: ['WALLET', 'TOURNAMENT', 'TECHNICAL', 'OTHER'],
      message: 'Invalid category. Supported: WALLET, TOURNAMENT, TECHNICAL, OTHER'
    },
    default: 'OTHER'
  },
  priority: {
    type: String,
    enum: {
      values: ['LOW', 'MEDIUM', 'HIGH'],
      message: 'Invalid priority level. Supported: LOW, MEDIUM, HIGH'
    },
    default: 'MEDIUM'
  },
  status: {
    type: String,
    enum: {
      values: ['PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'],
      message: 'Invalid status. Supported: PENDING, IN_PROGRESS, RESOLVED, CLOSED'
    },
    default: 'PENDING'
  },
  adminNotes: {
    type: String,
    trim: true,
    default: ''
  },
  resolvedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  },
  resolvedAt: {
    type: Date
  }
}, {
  timestamps: true
});

module.exports = mongoose.model('SupportTicket', SupportTicketSchema);
