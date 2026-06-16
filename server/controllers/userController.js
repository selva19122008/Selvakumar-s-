const User = require('../models/User');

/**
 * @desc    Update active user's FCM token for push notifications
 * @route   PUT /api/users/fcm-token
 * @access  Private
 */
exports.updateFcmToken = async (req, res) => {
  try {
    // 1. Confirm user is authenticated
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'No active session found. Authentication required.'
      });
    }

    const userId = req.user.id || req.user._id;
    const { fcmToken } = req.body;

    if (fcmToken === undefined) {
      return res.status(400).json({
        success: false,
        message: 'FCM token parameter is required.'
      });
    }

    // 2. Update the token inside persistence layer
    const user = await User.findByIdAndUpdate(
      userId,
      { $set: { fcmToken: fcmToken ? fcmToken.trim() : null } },
      { new: true, runValidators: true }
    );

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User profile not resolved in system.'
      });
    }

    return res.status(200).json({
      success: true,
      message: 'FCM token successfully synchronized.',
      data: {
        userId: user._id,
        fcmToken: user.fcmToken
      }
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to synchronize notification token.',
      error: error.message
    });
  }
};

/**
 * @desc    Upload profile picture for authenticated user
 * @route   POST /api/users/profile-picture
 * @access  Private
 */
exports.uploadProfilePicture = async (req, res) => {
  try {
    // 1. Double check Request authentication
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'No active session found. Authentication required.'
      });
    }

    const userId = req.user.id || req.user._id;

    // 2. Double check file upload was completed by uploader middleware
    if (!req.uploadedFileUrl) {
      return res.status(400).json({
        success: false,
        message: 'System failed to fetch uploaded asset URL reference.'
      });
    }

    // 3. Update the profilePicture field in the database
    const user = await User.findByIdAndUpdate(
      userId,
      { $set: { profilePicture: req.uploadedFileUrl } },
      { new: true, runValidators: true }
    );

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'Target user profile not resolved in system.'
      });
    }

    return res.status(200).json({
      success: true,
      message: 'Profile picture successfully updated.',
      data: {
        userId: user._id,
        inGameName: user.inGameName,
        profilePicture: user.profilePicture
      }
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to update profile picture.',
      error: error.message
    });
  }
};

