const mongoose = require('mongoose');

const FaqSchema = new mongoose.Schema({
  question: {
    type: String,
    required: [true, 'Question is required'],
    trim: true
  },
  answer: {
    type: String,
    required: [true, 'Answer is required'],
    trim: true
  },
  category: {
    type: String,
    required: [true, 'Category is required'],
    trim: true,
    default: 'General'
  },
  order: {
    type: Number,
    default: 0
  },
  isActive: {
    type: Boolean,
    default: true
  },
  createdBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'Creator identifier (Admin) is required']
  }
}, {
  timestamps: true
});

module.exports = mongoose.model('Faq', FaqSchema);
