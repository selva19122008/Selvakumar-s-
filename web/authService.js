/**
 * Firebase Authentication Phone Auth Module
 * 
 * Provides structural methods for Phone Database OTP Authentication using
 * Firebase PhoneAuthProvider, RecaptchaVerifier, and normalized E.164 formatting
 * designed to accurately validate and verify Indian mobile connections (+91).
 */

import { 
  RecaptchaVerifier, 
  signInWithPhoneNumber 
} from 'firebase/auth';
import { auth } from './firebaseConfig';

/**
 * Normalizes and validates raw Indian mobile inputs.
 * Ensures numbers match exact E.164 structure prefixed with +91.
 * 
 * @param {string} input - The raw mobile number entered by the user
 * @returns {object} { isValid: boolean, formatted: string, error: string|null }
 */
export function normalizeIndianPhoneNumber(input) {
  if (!input) {
    return { 
      isValid: false, 
      formatted: '', 
      error: 'Please enter a mobile phone number.' 
    };
  }

  // Remove whitespace, hyphens, and parentheses
  let cleaned = input.trim().replace(/[\s\-\(\)]/g, '');

  // Case 1: Already starts with E.164 +91
  if (cleaned.startsWith('+91')) {
    const remainingDigits = cleaned.substring(3);
    if (/^[6789]\d{9}$/.test(remainingDigits)) {
      return { isValid: true, formatted: cleaned, error: null };
    }
    return { 
      isValid: false, 
      formatted: cleaned, 
      error: 'Indian mobile numbers must have exactly 10 digits and start with 6, 7, 8, or 9.' 
    };
  }

  // Case 2: Starts with country code 91 but missing '+'
  if (cleaned.startsWith('91') && cleaned.length === 12) {
    const remainingDigits = cleaned.substring(2);
    if (/^[6789]\d{9}$/.test(remainingDigits)) {
      return { isValid: true, formatted: '+' + cleaned, error: null };
    }
  }

  // Case 3: Standard national trunk dial prefix '0' (11 digits total)
  if (cleaned.startsWith('0') && cleaned.length === 11) {
    const remainingDigits = cleaned.substring(1);
    if (/^[6789]\d{9}$/.test(remainingDigits)) {
      return { isValid: true, formatted: '+91' + remainingDigits, error: null };
    }
  }

  // Case 4: Exactly 10 digits entered directly
  if (cleaned.length === 10) {
    if (/^[6789]\d{9}$/.test(cleaned)) {
      return { isValid: true, formatted: `+91${cleaned}`, error: null };
    }
    return { 
      isValid: false, 
      formatted: cleaned, 
      error: 'Indian mobile numbers must start with 6, 7, 8, or 9.' 
    };
  }

  return { 
    isValid: false, 
    formatted: cleaned, 
    error: 'Please enter a valid 10-digit Indian mobile number.' 
  };
}

/**
 * Instantiates the Firebase reCAPTCHA Verifier anchor of the application.
 * Securely supports invisible, normal, or complex challenge sizes.
 * 
 * @param {string} containerId - The element ID where the recaptcha anchor mounts
 * @param {object} options - Recaptcha UI options (size, callback, expired-callback)
 */
export function setupRecaptchaVerifier(containerId = 'recaptcha-container', options = {}) {
  try {
    const container = document.getElementById(containerId);
    if (!container) {
      console.warn(`reCAPTCHA container (#${containerId}) was not found in the DOM hierarchy.`);
    }

    // Standard recaptcha initialization
    const verifier = new RecaptchaVerifier(auth, containerId, {
      size: options.size || 'invisible',
      callback: (response) => {
        if (options.onSuccess) {
          options.onSuccess(response);
        }
      },
      'expired-callback': () => {
        if (options.onExpired) {
          options.onExpired();
        }
      }
    });

    // Explicitly render to force load verification frames
    verifier.render().catch((err) => {
      console.warn('reCAPTCHA render postponed/failed. Will evaluate eagerly during authorization dispatch.', err);
    });

    return verifier;
  } catch (error) {
    console.error('Fatal initialization error in setupRecaptchaVerifier:', error);
    throw error;
  }
}

/**
 * Requests Firebase Authentication server infrastructure to dispatch an
 * SMS OTP code to the requested mobile destination.
 * 
 * @param {string} rawPhone - Mobile phone string entered in inputs
 * @param {object} appVerifier - The instantiated RecaptchaVerifier instance
 * @returns {Promise<object>} contains confirmationResult of the active session
 */
export async function sendFirebasePhoneOtp(rawPhone, appVerifier) {
  // Validate and normalize to ensure Indian region safety
  const phoneValidation = normalizeIndianPhoneNumber(rawPhone);
  if (!phoneValidation.isValid) {
    throw new Error(phoneValidation.error);
  }

  const targetPhoneNumber = phoneValidation.formatted;

  try {
    // Dispatch instant SMS validation query
    const confirmationResult = await signInWithPhoneNumber(auth, targetPhoneNumber, appVerifier);
    
    return {
      success: true,
      formattedPhone: targetPhoneNumber,
      confirmationResult
    };
  } catch (error) {
    console.error('Firebase Auth Phone OTP Dispatch Failed:', error);
    
    let descriptiveMessage = error.message || 'Failed to send OTP verification code.';
    
    // Provide user troubleshooting details for common failures
    switch (error.code) {
      case 'auth/invalid-phone-number':
        descriptiveMessage = 'The mobile number supplied has an invalid format. Please use standard format +91 XXXXXXXXXX.';
        break;
      case 'auth/quota-exceeded':
        descriptiveMessage = 'The daily SMS limit for code dispatch has been reached. Please use fallback test accounts or try again tomorrow.';
        break;
      case 'auth/too-many-requests':
        descriptiveMessage = 'Too many authentication attempts. Please wait a few minutes before trying again.';
        break;
      case 'auth/app-not-authorized':
        descriptiveMessage = 'This domain is not authorized in the Firebase Console under authorized domains. Please check app authentication domains.';
        break;
    }

    throw new Error(descriptiveMessage);
  }
}

/**
 * Submits the received 6-digit SMS verification response token to 
 * authorize, sign in, and establish real-time session links.
 * 
 * @param {object} confirmationResult - The confirmation object holding active verification context
 * @param {string} verificationOtpCode - 6-digit SMS code entered by the user
 * @returns {Promise<object>} containing authenticated user and credential info
 */
export async function verifyFirebasePhoneOtp(confirmationResult, verificationOtpCode) {
  if (!confirmationResult) {
    throw new Error('Empty verification credentials. Please trigger a new phone verification code first.');
  }

  const code = (verificationOtpCode || '').trim();
  if (code.length !== 6 || !/^\d{6}$/.test(code)) {
    throw new Error('Please input the complete 6-digit verification pin sent via SMS.');
  }

  try {
    // Confirm verification code
    const result = await confirmationResult.confirm(code);
    const user = result.user;
    
    // Retrieve fresh secure Firebase ID token for client synchronization/backend requests
    const idToken = await user.getIdToken();

    return {
      success: true,
      user,
      idToken
    };
  } catch (error) {
    console.error('Firebase Auth OTP Code Verification Failed:', error);
    
    let descriptiveMessage = error.message || 'Validation code failed or time-expired.';
    
    switch (error.code) {
      case 'auth/invalid-verification-code':
        descriptiveMessage = 'The OTP validation pin entered is incorrect. Double-check and re-enter the code.';
        break;
      case 'auth/code-expired':
        descriptiveMessage = 'The security OTP validation pin has expired. Please request a new verification code.';
        break;
    }

    throw new Error(descriptiveMessage);
  }
}
