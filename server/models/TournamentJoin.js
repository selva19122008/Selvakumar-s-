const mongoose = require('mongoose');

const TournamentJoinSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'User reference is required']
  },
  tournamentId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Tournament',
    required: [true, 'Tournament reference is required']
  },
  freeFireUid: {
    type: String,
    required: [true, 'Free Fire UID is required'],
    trim: true
  },
  inGameName: {
    type: String,
    required: [true, 'In-game name is required'],
    trim: true
  },
  seatNumber: {
    type: Number,
    required: [true, 'Seat number is required'],
    min: [1, 'Seat number must be at least 1']
  },
  kills: {
    type: Number,
    default: 0,
    min: [0, 'Kills cannot be negative']
  },
  points: {
    type: Number,
    default: 0,
    min: [0, 'Points cannot be negative']
  },
  joinedAt: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true
});

// Enforce unique join registration per user per tournament
TournamentJoinSchema.index({ userId: 1, tournamentId: 1 }, { unique: true });
// Enforce unique seat numbers per tournament
TournamentJoinSchema.index({ tournamentId: 1, seatNumber: 1 }, { unique: true });

module.exports = mongoose.model('TournamentJoin', TournamentJoinSchema);
