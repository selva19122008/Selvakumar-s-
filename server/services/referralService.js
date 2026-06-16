const crypto = require('crypto');
const User = require('../models/User');
const TournamentJoin = require('../models/TournamentJoin');
const Transaction = require('../models/Transaction');

/**
 * Generates a unique, reader-friendly, and distinct referral code.
 * Format: [PREFIX]-[RANDOM_ALPHANUMERIC] (e.g., TEAM-7D3X)
 * @param {string} inGameName - The player's in-game alias for custom branding
 * @returns {Promise<string>} A unique referral code
 */
exports.generateUniqueReferralCode = async (inGameName) => {
  const prefix = (inGameName || 'FF')
    .replace(/[^a-zA-Z0-9]/g, '')
    .toUpperCase()
    .slice(0, 4);

  let attempts = 0;
  const maxAttempts = 10;

  while (attempts < maxAttempts) {
    // Generate a random 4-character suffix
    const suffix = crypto.randomBytes(2).toString('hex').toUpperCase();
    const candidateCode = `${prefix}-${suffix}`;

    // Verify uniqueness against existing database users
    const existingUser = await User.findOne({ referralCode: candidateCode });
    if (!existingUser) {
      return candidateCode;
    }
    attempts++;
  }

  // Fallback in case of extreme collision rate
  return `FF-${Date.now().toString(36).toUpperCase().slice(-5)}`;
};

/**
 * Links a newly registered user to a referring user via code validation.
 * @param {string} userId - The ID of the newly registered user
 * @param {string} referralCode - The referral code to validate and apply
 * @param {ClientSession} [session] - Optional Mongoose session for transaction lock
 */
exports.applyReferralCode = async (userId, referralCode, session = null) => {
  if (!referralCode) {
    throw new Error('Referral code must be provided.');
  }

  const queryOptions = session ? { session } : {};

  // 1. Locate the referring partner
  const referrer = await User.findOne({ referralCode: referralCode.trim() }).setOptions(queryOptions);
  if (!referrer) {
    throw new Error('The entered referral code is invalid or does not exist.');
  }

  // 2. Prevent self-referrals
  if (referrer._id.toString() === userId.toString()) {
    throw new Error('Self-referring is not permitted.');
  }

  // 3. Locate the target referee profile
  const referee = await User.findById(userId).setOptions(queryOptions);
  if (!referee) {
    throw new Error('Referee user profile not found.');
  }

  // 4. Ensure user hasn't already been referred
  if (referee.referredBy) {
    throw new Error('A referral code has already been applied to this account.');
  }

  // 5. Establish link association
  referee.referredBy = referrer._id;
  await referee.save(queryOptions);

  return {
    success: true,
    referrerId: referrer._id,
    referrerName: referrer.inGameName
  };
};

/**
 * Awards referral incentives when a referred user signs up for their first tournament.
 * @param {string} userId - The ID of the referred user checking out slots
 * @param {string} tournamentTitle - Title of the registered tournament for auditor tracing
 * @param {ClientSession} session - Active Mongoose session for ACID compliance
 */
exports.awardFirstRegistrationBonus = async (userId, tournamentTitle, session) => {
  if (!session) {
    throw new Error('An active database session is required to guarantee atomic referral payout processes.');
  }

  // 1. Fetch referee profile and get the referrer
  const referee = await User.findById(userId).session(session);
  if (!referee || !referee.referredBy) {
    // User was not referred; skip bonus distribution gracefully
    return { status: 'DEFERRED', reason: 'No referrer associated with user profile.' };
  }

  // 2. Verify if this is the referee's first-ever tournament registration
  const registrationCount = await TournamentJoin.countDocuments({ userId }).session(session);
  // Note: During the callback/transaction context, the active registration is already saved.
  // Hence, a count of exactly 1 confirms this is indeed their first tournament action.
  if (registrationCount !== 1) {
    return { status: 'DEFERRED', reason: 'This registration is not the user\'s first match participation.' };
  }

  const referrerId = referee.referredBy;
  const referrer = await User.findById(referrerId).session(session);
  if (!referrer) {
    return { status: 'FAILED', reason: 'The referring user account no longer exists.' };
  }

  // 3. Define incentive bounds
  const REFERRER_BONUS = 15.0; // Payout reward for bringing an active player
  const REFEREE_BONUS = 5.0;   // Player sign-up bonus reward

  // 4. Award bonus to the referrer
  referrer.bonusBalance += REFERRER_BONUS;
  await referrer.save({ session });

  const referrerInvoiceId = `REF-BY-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
  const referrerTransaction = new Transaction({
    userId: referrerId,
    title: `Referral award: ${referee.inGameName} entered first match ${tournamentTitle}`,
    amount: REFERRER_BONUS,
    type: 'REFERRAL_REWARD',
    category: 'BONUS',
    status: 'SUCCESS',
    invoiceId: referrerInvoiceId
  });
  await referrerTransaction.save({ session });

  // 5. Award bonus to the referee
  referee.bonusBalance += REFEREE_BONUS;
  await referee.save({ session });

  const refereeInvoiceId = `REF-TO-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
  const refereeTransaction = new Transaction({
    userId: userId,
    title: `First tournament bonus (Referred by ${referrer.inGameName})`,
    amount: REFEREE_BONUS,
    type: 'BONUS_ADD',
    category: 'BONUS',
    status: 'SUCCESS',
    invoiceId: refereeInvoiceId
  });
  await refereeTransaction.save({ session });

  return {
    status: 'SUCCESS',
    referrerBonusAwarded: REFERRER_BONUS,
    refereeBonusAwarded: REFEREE_BONUS,
    referrerId,
    refereeId: userId
  };
};
