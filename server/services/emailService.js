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

    // Elegant, highly polished, and branded HTML template for secure e-sports verification
    const htmlBody = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>BattleZone Esports Security Verification</title>
      </head>
      <body style="margin: 0; padding: 0; background-color: #07040b; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;">
        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #07040b; padding: 40px 10px;">
          <tr>
            <td align="center">
              <!-- Container Card -->
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #110d19; border-radius: 16px; border: 1px solid #ff4d4d; overflow: hidden; box-shadow: 0 12px 40px rgba(255, 77, 77, 0.15);">
                
                <!-- Red Esports Header Accent Bar -->
                <tr>
                  <td height="6" style="background: linear-gradient(90deg, #ff4d4d 0%, #ff8800 100%); font-size: 1px; line-height: 1px;">&nbsp;</td>
                </tr>

                <!-- Header / Logo Area -->
                <tr>
                  <td align="center" style="padding: 35px 20px 20px 20px; background-color: #161122;">
                    <!-- Visual Brand Badge -->
                    <table border="0" cellpadding="0" cellspacing="0" style="margin-bottom: 15px;">
                      <tr>
                        <td align="center" style="background-color: #ff4d4d; padding: 12px 18px; border-radius: 10px; box-shadow: 0 4px 15px rgba(255, 77, 77, 0.4);">
                          <span style="font-family: 'Impact', 'Arial Black', sans-serif; font-size: 26px; font-weight: bold; color: #ffffff; letter-spacing: 2px;">BZ</span>
                        </td>
                      </tr>
                    </table>
                    <h2 style="color: #ffffff; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: 2px; font-family: 'Impact', 'Arial Black', sans-serif;">BATTLEZONE FF</h2>
                    <span style="color: #ff4d4d; font-size: 11px; font-weight: 800; letter-spacing: 3px; text-transform: uppercase;">E-sports Tournament Arena</span>
                  </td>
                </tr>

                <!-- Content Area -->
                <tr>
                  <td style="padding: 40px 35px; background-color: #110d19;">
                    
                    <!-- Secure Handshake Shield Indicator -->
                    <table align="center" border="0" cellpadding="0" cellspacing="0" style="margin-bottom: 25px;">
                      <tr>
                        <td align="center" style="background-color: rgba(76, 175, 80, 0.12); border: 1px solid rgba(76, 175, 80, 0.3); padding: 8px 16px; border-radius: 20px;">
                          <span style="font-size: 13px; color: #4CAF50; font-weight: bold; display: inline-block; vertical-align: middle;">
                            🔒 SECURE END-TO-END SMTP TRANSMISSION
                          </span>
                        </td>
                      </tr>
                    </table>

                    <h3 style="color: #ffffff; font-size: 20px; font-weight: 700; text-align: center; margin-top: 0; margin-bottom: 12px; letter-spacing: 0.5px;">
                      One-Time Verification Request
                    </h3>
                    
                    <p style="color: #b3a9c2; font-size: 14px; line-height: 1.6; text-align: center; margin-bottom: 30px;">
                      You are receiving this dynamic verification key to complete your <strong>${purpose}</strong> process. This temporary security code is single-use and valid for exactly <strong>10 minutes</strong>.
                    </p>

                    <!-- Beautiful interactive OTP block -->
                    <table align="center" border="0" cellpadding="0" cellspacing="0" style="margin: 25px auto;">
                      <tr>
                        <td align="center" style="background-color: #1d1729; border: 2px dashed #ff4d4d; border-radius: 12px; padding: 20px 45px; box-shadow: inset 0 0 15px rgba(0,0,0,0.5);">
                          <span style="font-family: 'Courier New', Courier, monospace; font-size: 40px; font-weight: 900; letter-spacing: 8px; color: #ff9100; text-shadow: 0 0 12px rgba(255,145,0,0.35); text-align: center; display: block; margin: 0 auto; width: 100%;">
                            ${otpCode}
                          </span>
                        </td>
                      </tr>
                    </table>

                    <p style="color: #8c819c; font-size: 12px; line-height: 1.5; text-align: center; margin-top: 25px; margin-bottom: 35px; font-style: italic;">
                      Please enter this code immediately in the BattleZone app window to confirm your registration or session.
                    </p>

                    <!-- Trust and Safety Checklist Frame -->
                    <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #171123; border: 1px solid #231b31; border-radius: 10px; padding: 20px; margin-top: 25px;">
                      <tr>
                        <td>
                          <h4 style="color: #ffffff; margin-top: 0; margin-bottom: 12px; font-size: 13px; font-weight: bold; letter-spacing: 1px; text-transform: uppercase;">
                            🛡️ BattleZone Trust &amp; Safety Guidelines
                          </h4>
                          <table border="0" cellpadding="0" cellspacing="0" width="100%" style="font-size: 12px; line-height: 1.5; color: #b3a9c2;">
                            <tr>
                              <td style="padding-bottom: 8px; vertical-align: top; width: 20px;">✓</td>
                              <td style="padding-bottom: 8px;"><strong>Absolute Confidentiality:</strong> Never share this verification OTP code with anybody. BattleZone Referees and Administrators will never ask for your verification code.</td>
                            </tr>
                            <tr>
                              <td style="padding-bottom: 8px; vertical-align: top; width: 20px;">✓</td>
                              <td style="padding-bottom: 8px;"><strong>Intent Verification:</strong> If you did not initiate this e-sports sign-up request, you can safely ignore this automated system email.</td>
                            </tr>
                            <tr>
                              <td style="vertical-align: top; width: 20px;">✓</td>
                              <td><strong>Direct TLS Gateway:</strong> This message was generated via our automated SMTP delivery service using secure Google App SSL links.</td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>

                  </td>
                </tr>

                <!-- Footer Area -->
                <tr>
                  <td align="center" style="padding: 25px 20px; background-color: #0d0916; border-top: 1px solid #1c1626;">
                    <p style="color: #635a73; font-size: 11px; margin: 0 0 8px 0; font-weight: 500;">
                      © 2026 BattleZone Arena Systems. All rights reserved.
                    </p>
                    <p style="color: #4c4458; font-size: 10px; margin: 0; line-height: 1.4;">
                      You are receiving this operational email because you requested a security token for BattleZone Esports. This is a system-generated message. Please do not reply directly to this address.
                    </p>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
        </table>
      </body>
      </html>
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
