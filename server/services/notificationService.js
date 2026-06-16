const mongoose = require('mongoose');
const Tournament = require('../models/Tournament');
const TournamentJoin = require('../models/TournamentJoin');
const User = require('../models/User');

let admin = null;
let isFirebaseInitialized = false;

// Safe dynamic package load to handle environments without firebase-admin pre-installed
try {
  admin = require('firebase-admin');
} catch (e) {
  console.warn('[Notification Service] "firebase-admin" SDK is not installed. Operating in mock logging fallback mode.');
}

// 1. Initialize Firebase Admin App context securely
if (admin) {
  try {
    if (admin.apps.length === 0) {
      if (process.env.FIREBASE_SERVICE_ACCOUNT) {
        // Option A: Service account JSON string passed through process environment variables
        const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
        admin.initializeApp({
          credential: admin.credential.cert(serviceAccount)
        });
        isFirebaseInitialized = true;
        console.log('[Notification Service] Firebase Admin SDK successfully initialized via Env JSON certificate.');
      } else if (process.env.FIREBASE_SERVICE_ACCOUNT_KEY_PATH) {
        // Option B: Service account certificate file path configuration input
        admin.initializeApp({
          credential: admin.credential.cert(process.env.FIREBASE_SERVICE_ACCOUNT_KEY_PATH)
        });
        isFirebaseInitialized = true;
        console.log(`[Notification Service] Firebase Admin SDK successfully initialized via key path: ${process.env.FIREBASE_SERVICE_ACCOUNT_KEY_PATH}`);
      } else {
        // Option C: Graceful background setup or Default Application credentials
        admin.initializeApp();
        isFirebaseInitialized = true;
        console.log('[Notification Service] Firebase Admin SDK initialized using Default Application credentials.');
      }
    } else {
      isFirebaseInitialized = true;
    }
  } catch (error) {
    console.error('[Notification Service] SDK Initialization failed:', error.message);
  }
}

/**
 * Sends a multicast push notification to the list of user FCM registration tokens.
 * Supports elegant fallback testing mode if Firebase secret configurations are absent.
 * @param {Object} options - Message payloads { tokens: string[], title: string, body: string, data: Object }
 * @returns {Promise<Object>} Receipt diagnostic outputs
 */
exports.sendMulticastNotification = async ({ tokens, title, body, data = {} }) => {
  const filteredTokens = (tokens || []).filter(t => typeof t === 'string' && t.trim() !== '');

  if (filteredTokens.length === 0) {
    return { success: true, message: 'No valid registration tokens provided.', results: [] };
  }

  // Fallback Logging Mode
  if (!admin || !isFirebaseInitialized) {
    console.log(`[Notification Service] [MOCK PUSH] Triggered notification for ${filteredTokens.length} devices.`);
    console.log(`[Notification Service] [MOCK PUSH] Title: "${title}" | Body: "${body}"`);
    console.log('[Notification Service] [MOCK PUSH] Payload Data:', JSON.stringify(data, null, 2));
    
    return {
      success: true,
      mock: true,
      message: 'FCM disabled. Notification printed to server stdout console.',
      sentCount: filteredTokens.length
    };
  }

  try {
    // Construct the standard FCM payload format
    const message = {
      tokens: filteredTokens,
      notification: {
        title,
        body
      },
      data: Object.keys(data).reduce((acc, key) => {
        // FCM custom data properties must strictly hold string values
        acc[key] = String(data[key]);
        return acc;
      }, {}),
      android: {
        priority: 'high',
        notification: {
          sound: 'default',
          channelId: 'tournament_alerts',
          clickAction: 'OPEN_TOURNAMENT_SCREEN'
        }
      }
    };

    // sendEachForMulticast is the modern, thread-safe, non-deprecated FCM transmission API
    const response = await admin.messaging().sendEachForMulticast(message);
    
    console.log(`[Notification Service] FCM delivery batch complete. Successful payloads: ${response.successCount}, Failures: ${response.failureCount}`);

    // Log diagnostic warning on individual token rejections
    if (response.failureCount > 0) {
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          console.error(`[Notification Service] Failed delivery to token Index [${idx}]:`, resp.error ? resp.error.message : 'Unknown rejection');
        }
      });
    }

    return {
      success: true,
      sentCount: response.successCount,
      failureCount: response.failureCount,
      responses: response.responses
    };
  } catch (error) {
    console.error('[Notification Service] FCM sendEachForMulticast failure:', error.message);
    throw error;
  }
};

/**
 * Scan for upcoming match sessions starting in less than or equal to 10 minutes (600,000 ms)
 * which have room configurations assigned but have not yet completed their credentials broadcast.
 * @returns {Promise<Object>} Processing execution statistics
 */
exports.checkAndDispatchUpcomingNotifications = async () => {
  try {
    const now = new Date();
    // 10 minutes from now (600,000 milliseconds window threshold)
    const tenMinutesAhead = new Date(now.getTime() + 10 * 60 * 1000);

    // Query active upcoming tournaments starting within the 10-minutes threshold
    const tournaments = await Tournament.find({
      status: 'upcoming',
      matchDate: { $lte: tenMinutesAhead },
      roomId: { $ne: null, $exists: true },
      roomCredentialsSent: { $ne: true }
    });

    if (tournaments.length === 0) {
      return { success: true, processedCount: 0 };
    }

    console.log(`[Notification Scheduler] Found ${tournaments.length} tournament(s) due for push alerts.`);

    for (const tournament of tournaments) {
      // 1. Fetch matching registrations
      const registrations = await TournamentJoin.find({ tournamentId: tournament._id });
      if (registrations.length === 0) {
        // No users joined. Complete credentials status flag directly
        tournament.roomCredentialsSent = true;
        await tournament.save();
        console.log(`[Notification Scheduler] No registered players found for "${tournament.title}". Auto-marked as sent.`);
        continue;
      }

      // 2. Fetch associated player accounts to harvest FCM tokens
      const userIds = registrations.map(reg => reg.userId);
      const players = await User.find({
        _id: { $in: userIds },
        fcmToken: { $ne: null, $exists: true }
      });

      const tokens = [...new Set(players.map(p => p.fcmToken).filter(t => t && t.trim() !== ''))];

      if (tokens.length === 0) {
        console.log(`[Notification Scheduler] Match "${tournament.title}" has players but no FCM tokens are registered under profiles.`);
        tournament.roomCredentialsSent = true;
        await tournament.save();
        continue;
      }

      // 3. Dispatch the push alerts containing Room ID and password
      const alertTitle = `🎮 Battle Zone: Room Ready! - ${tournament.title}`;
      const alertBody = `Match starting in 10 minutes. Room ID: ${tournament.roomId} | Password: ${tournament.roomPassword}`;
      
      const payloadData = {
        tournamentId: tournament._id.toString(),
        roomId: tournament.roomId,
        roomPassword: tournament.roomPassword,
        matchTime: tournament.matchDate.toISOString(),
        eventType: 'ROOM_READY'
      };

      await exports.sendMulticastNotification({
        tokens,
        title: alertTitle,
        body: alertBody,
        data: payloadData
      });

      // 4. Update persistence state to protect players from repeat notifications
      tournament.roomCredentialsSent = true;
      await tournament.save();

      console.log(`[Notification Scheduler] Successfully notified ${tokens.length} players for tournament "${tournament.title}".`);
    }

    return { success: true, processedCount: tournaments.length };
  } catch (error) {
    console.error('[Notification Scheduler] Execution failure inside dispatcher cycle:', error.message);
    throw error;
  }
};

// Scheduler Interval handle state tracking
let notificationSchedulerIntervalId = null;

/**
 * Boots the autonomous background monitoring service to interval-poll upcoming games.
 * @param {number} [intervalMs=60000] Interval polling rate (defaults to 1 minute)
 */
exports.startNotificationScheduler = (intervalMs = 60 * 1000) => {
  if (notificationSchedulerIntervalId) {
    console.log('[Notification Scheduler] Background scan loop is already active.');
    return;
  }

  console.log(`[Notification Scheduler] Activated autonomous scan loop. Scan frequency: every ${intervalMs / 1000}s.`);
  
  notificationSchedulerIntervalId = setInterval(async () => {
    try {
      await exports.checkAndDispatchUpcomingNotifications();
    } catch (err) {
      console.error('[Notification Scheduler] Poll routine encountered error:', err.message);
    }
  }, intervalMs);
};

/**
 * Terminates the autonomous polling worker thread.
 */
exports.stopNotificationScheduler = () => {
  if (notificationSchedulerIntervalId) {
    clearInterval(notificationSchedulerIntervalId);
    notificationSchedulerIntervalId = null;
    console.log('[Notification Scheduler] Background scan loop deactivated successfully.');
  }
};
