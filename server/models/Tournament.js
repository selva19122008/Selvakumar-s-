const mongoose = require('mongoose');

const TournamentSchema = new mongoose.Schema({
  title: {
    type: String,
    required: [true, 'Tournament title is required'],
    trim: true,
    maxlength: [100, 'Title cannot exceed 100 characters']
  },
  matchDate: {
    type: Date,
    required: [true, 'Match date and time is required'],
    validate: {
      validator: function(value) {
        // Ensure match date is in the future when creating
        if (this.isNew) {
          return value > new Date();
        }
        return true;
      },
      message: 'Match date must be in the future'
    }
  },
  entryFee: {
    type: Number,
    required: [true, 'Entry fee is required'],
    min: [0, 'Entry fee cannot be negative'],
    default: 0
  },
  prizePool: {
    type: Number,
    required: [true, 'Prize pool is required'],
    min: [0, 'Prize pool cannot be negative'],
    default: 0
  },
  maxSlots: {
    type: Number,
    required: [true, 'Maximum slots is required'],
    min: [1, 'Maximum slots must be at least 1'],
    validate: {
      validator: Number.isInteger,
      message: 'Maximum slots must be a whole integer'
    }
  },
  joinedSlots: {
    type: Number,
    default: 0,
    min: [0, 'Joined slots cannot be negative'],
    validate: {
      validator: function(val) {
        return Number.isInteger(val) && val <= this.maxSlots;
      },
      message: 'Joined slots must be a positive integer and cannot exceed maximum slots'
    }
  },
  mapType: {
    type: String,
    required: [true, 'Map type is required'],
    enum: {
      values: ['Bermuda', 'Purgatory', 'Kalahari', 'Alpine', 'Nexterra'],
      message: '{VALUE} is not a valid map type'
    }
  },
  format: {
    type: String,
    required: [true, 'Tournament format is required'],
    enum: {
      values: ['Solo', 'Duo', 'Squad'],
      message: '{VALUE} is not a valid format'
    },
    default: 'Solo'
  },
  status: {
    type: String,
    required: [true, 'Status is required'],
    enum: {
      values: ['upcoming', 'live', 'completed', 'cancelled'],
      message: 'Status must be upcoming, live, completed, or cancelled'
    },
    default: 'upcoming'
  },
  rules: {
    type: String,
    trim: true,
    default: 'Standard competitive rules apply.'
  },
  createdBy: {
    type: String,
    required: [true, 'Creator identifier (Admin) is required']
  },
  winnerUid: {
    type: String,
    default: null
  },
  winnerName: {
    type: String,
    default: null
  },
  roomId: {
    type: String,
    default: null,
    trim: true
  },
  roomPassword: {
    type: String,
    default: null,
    trim: true
  },
  roomCredentialsSent: {
    type: Boolean,
    default: false
  }
}, {
  timestamps: true
});

// Middleware index for performance optimizing sorting by date and filters
TournamentSchema.index({ status: 1, matchDate: 1 });

module.exports = mongoose.model('Tournament', TournamentSchema);
