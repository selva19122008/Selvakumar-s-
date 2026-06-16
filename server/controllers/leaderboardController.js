const mongoose = require('mongoose');
const User = require('../models/User');
const Tournament = require('../models/Tournament');
const TournamentJoin = require('../models/TournamentJoin');

/**
 * @desc    Get top 10 players based on total tournament wins and points
 * @route   GET /api/leaderboard
 * @access  Public
 */
exports.getLeaderboard = async (req, res) => {
  try {
    // Dynamically retrieve exact collection names from Mongoose models to prevent pluralization discrepancies.
    const tournamentCollectionName = Tournament.collection.name;
    const tournamentJoinCollectionName = TournamentJoin.collection.name;

    /**
     * Mongoose Aggregation Pipeline description:
     * 1. $lookup (Tournaments Won): Left-outer-joins the Tournament collection where the completion
     *    status is "completed", and the tournament's "winnerUid" matches the user's "freeFireUid".
     * 2. $lookup (Tournament Performance Matches): Left-outer-joins the TournamentJoin collection
     *    matching on the user's unique ObjectId "_id" to retrieve their total historical registrations.
     * 3. $project (Fields & Aggregations): Projects the essential user identity fields while computing:
     *    - totalWins: By taking the size of the won tournaments array.
     *    - totalPoints: By calculating the sum of the "points" field across all historical registered tournaments.
     *    - totalKills: By calculating the sum of the "kills" field across all historical registered tournaments.
     *    - tournamentsPlayed: By taking the size of the joins array.
     * 4. $sort (Dual-criteria sorting): Sorts the resulting pipeline documents by "totalWins" in descending
     *    order as the primary ranking factor, then "totalPoints" descending on ties, and "inGameName" alphabetically.
     * 5. $limit (Top 10 restriction): Restricts the resulting documents to the top 10 players on the platform.
     */
    const leaderboardPipeline = [
      // Step 1: Resolve all tournaments won by this user matching winnerUid with freeFireUid
      {
        $lookup: {
          from: tournamentCollectionName,
          let: { userFFUid: "$freeFireUid" },
          pipeline: [
            {
              $match: {
                $expr: {
                  $and: [
                    { $eq: ["$status", "completed"] },
                    { $eq: ["$winnerUid", "$$userFFUid"] }
                  ]
                }
              }
            }
          ],
          as: "wonTournaments"
        }
      },
      // Step 2: Resolve all tournament join documents to trace match performance statistics
      {
        $lookup: {
          from: tournamentJoinCollectionName,
          localField: "_id",
          foreignField: "userId",
          as: "joins"
        }
      },
      // Step 3: Project identity info and perform sums & counts using Mongoose aggregators
      {
        $project: {
          _id: 1,
          inGameName: 1,
          freeFireUid: 1,
          email: 1,
          profilePicture: { $ifNull: ["$profilePicture", ""] },
          totalWins: { $size: "$wonTournaments" },
          totalPoints: { $sum: "$joins.points" },
          totalKills: { $sum: "$joins.kills" },
          tournamentsPlayed: { $size: "$joins" }
        }
      },
      // Step 4: Multi-variable sort ranking sorting by Wins then Points then Name
      {
        $sort: {
          totalWins: -1,
          totalPoints: -1,
          inGameName: 1
        }
      },
      // Step 5: Cap results at top 10 leaderboard entries
      {
        $limit: 10
      }
    ];

    const topPlayers = await User.aggregate(leaderboardPipeline);

    return res.status(200).json({
      success: true,
      count: topPlayers.length,
      message: 'Leaderboard aggregated successfully.',
      data: topPlayers
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to calculate the leaderboard statistics.',
      error: error.message
    });
  }
};
