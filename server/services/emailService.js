/**
 * Email Service Module (Secure Gmail SMTP OTP Dispatcher)
 * 
 * This service implements a completely free and fully secure OTP dispatch system via Gmail SMTP.
 * Instead of exposing sensitive Google App Passwords inside the Android client APK, the Android app
 * sends a payload to this secure backend endpoint. The backend uses the credentials stored in
 * environment variables (.env) to authenticate with Gmail and deliver the 6-digit OTP code.
 */

const nodemailer = require('nodemailer');

// Extract properties from environment variables
const GMAIL_USER = process.env.GMAIL_USER || '';
const GMAIL_APP_PASSWORD = process.env.GMAIL_APP_PASSWORD || ''; // 16-character Google App Password

/**
 * Creates and configures Nodemailer SMTP Transporter
 */
const createTransporter = () => {
  if (!GMAIL_USER || !GMAIL_APP_PASSWORD) {
    console.warn('[Email Service] Gmail credentials are not configured in your environment variables.');
  }

  return nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: GMAIL_USER,
      pass: GMAIL_APP_PASSWORD
    }
  });
};

/**
 * Sends a secure, professionally formatted OTP registration/login email via Gmail SMTP
 * 
 * @param {string} recipientEmail - User's destination email
 * @param {string} otpCode - The 6-digit numerical OTP code
 * @param {string} purpose - Purpose of verification (e.g. 'SignIn', 'Register', 'ResetPassword')
 * @returns {Promise<Object>} Status and details of SMTP transmission
 */
exports.sendOtpEmail = async (recipientEmail, otpCode, purpose = 'Verification') => {
  try {
    if (!recipientEmail || !recipientEmail.includes('@')) {
      throw new Error('Invalid receiver email address provided.');
    }

    if (!otpCode) {
      throw new Error('OTP verification code parameter cannot be empty.');
    }

    const transporter = createTransporter();

    // Elegant Material-style HTML markup for premium inbox appearance
    const htmlBody = `
      <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 550px; margin: 0 auto; padding: 25px; border-radius: 12px; background-color: #0f0a14; border: 1px solid #28252c; color: #eceff1;">
        <div style="text-align: center; padding-bottom: 20px; border-bottom: 2px solid #ff4d4d;">
          <h2 style="color: #ff4d4d; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 1px;">BATTLEZONE FF</h2>
          <span style="color: #b0bec5; font-size: 11px; font-weight: 600; letter-spacing: 0.5px;">E-SPORTS MULTIPLAYER ARENA</span>
        </div>
        
        <div style="padding: 25px 10px; text-align: center;">
          <h3 style="color: #ffffff; font-size: 18px; margin-top: 0;">One-Time Verification Request</h3>
          <p style="color: #b0bec5; font-size: 14px; line-height: 1.5; margin-bottom: 25px;">
            Here is your dynamic security credentials key for <strong>${purpose}</strong>. This verification code is single-use and valid for exactly 10 minutes.
          </p>
          
          <div style="display: inline-block; padding: 15px 40px; margin: 10px auto; border-radius: 8px; background-color: #1a1224; border: 1px dashed #ff4d4d; font-size: 32px; font-weight: 900; letter-spacing: 6px; color: #ff9100; text-shadow: 0 0 10px rgba(255,145,0,0.2);">
            ${otpCode}
          </div>
          
          <p style="color: #78909c; font-size: 11px; margin-top: 25px; font-style: italic;">
            If you did not issue this verification request, please disregard this email or contact support. Keep your credentials confidential.
          </p>
        </div>
        
        <div style="text-align: center; padding-top: 20px; border-top: 1px solid #201a27; font-size: 11px; color: #546e7a;">
          © 2026 BattleZone Esports Team. Powered by Secure SMTP Gateways.
        </div>
      </div>
    `;

    const mailOptions = {
      from: `"BattleZone Admin" <${GMAIL_USER}>`,
      to: recipientEmail.trim().toLowerCase(),
      subject: `[BattleZone] Secure Verification Code Key: ${otpCode}`,
      html: htmlBody,
      text: `BattleZone Esports Team verification credentials code: ${otpCode}. Valid for 10 minutes.`
    };

    const info = await transporter.sendMail(mailOptions);
    console.log(`[Email Dispatch Success] Message transaction ID: ${info.messageId} to ${recipientEmail}`);
    return {
      success: true,
      messageId: info.messageId,
      accepted: info.accepted
    };
  } catch (error) {
    console.error('[Email Dispatch Fail] Nodemailer system encounter:', error);
    throw error;
  }
};
