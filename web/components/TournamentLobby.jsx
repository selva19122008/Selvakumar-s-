import React, { useState, useEffect, useMemo } from 'react';

/**
 * TournamentLobby Component
 * A high-fidelity, responsive, and visually stunning React component representing
 * a gaming tournament lobby.
 * 
 * Major Capabilities:
 * - Real-time countdown timer ticking down to the exact second.
 * - Dynamic structural checking: room credentials unlock exactly 10 minutes prior to the start time.
 * - Robust interactive clipboard triggers with custom visual feedback.
 * - Beautiful Obsidian & Crimson gamer theme matching the mobile app look.
 * 
 * @param {Object} props
 * @param {Object} props.tournament - The tourney data object.
 * @param {string} props.tournament.title - Title of the battle tournament.
 * @param {string|Date} props.tournament.matchDate - ISO string or Date object of the match starting bell.
 * @param {number} props.tournament.prizePool - Total prize pool value.
 * @param {number} props.tournament.entryFee - Entry fee required to register.
 * @param {string} props.tournament.mapType - Map variant (e.g., Bermuda, Purgatory).
 * @param {string|string[]} props.tournament.rules - Rules listing.
 * @param {string} [props.tournament.roomId] - Room ID configured by match ref.
 * @param {string} [props.tournament.roomPassword] - Room configuration password.
 * @param {boolean} [props.isRegistered=true] - Registration status of the active viewer.
 * @param {Function} [props.onBack] - Parent navigation click back callback.
 */
export default function TournamentLobby({ 
  tournament = {
    title: "Championship Pro League - Elite Duo",
    matchDate: new Date(Date.now() + 15 * 60 * 1000).toISOString(), // 15 mins from now by default
    prizePool: 500,
    entryFee: 15,
    mapType: "Bermuda (Remastered)",
    rules: [
      "No usage of third-party graphics configuration software or emulator triggers.",
      "Team alignment and seats must strictly align with registered slot coordinates.",
      "Teaming up with rival participants will warrant immediate server ban and balance freeze.",
      "Submit match-end statistics screenshots within 15 minutes of completion for payouts."
    ],
    roomId: "9912085",
    roomPassword: "FF_EPRO_SECRET"
  },
  isRegistered = true,
  onBack = () => {}
}) {
  const matchTime = useMemo(() => new Date(tournament.matchDate), [tournament.matchDate]);
  const [timeLeft, setTimeLeft] = useState(matchTime.getTime() - Date.now());
  const [copiedField, setCopiedField] = useState(null); // 'roomId' | 'password' | null

  // 1. Live ticker state runner
  useEffect(() => {
    // Initial calculate
    setTimeLeft(matchTime.getTime() - Date.now());

    const timer = setInterval(() => {
      const diff = matchTime.getTime() - Date.now();
      setTimeLeft(diff);
    }, 1000);

    return () => clearInterval(timer);
  }, [matchTime]);

  // 2. Compute key locking triggers (10 minutes = 600,000 ms)
  const UNLOCK_THRESHOLD_MS = 10 * 60 * 1000;
  const isUnlocked = timeLeft <= UNLOCK_THRESHOLD_MS;
  const isLive = timeLeft <= 0;

  // 3. Format timer into an elegant visual indicator
  const formattedCountdown = useMemo(() => {
    if (timeLeft <= 0) return "MATCH IN PROGRESS";

    const totalSeconds = Math.floor(timeLeft / 1000);
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    const parts = [];
    if (days > 0) parts.push(`${days}d`);
    parts.push(`${String(hours).padStart(2, '0')}h`);
    parts.push(`${String(minutes).padStart(2, '0')}m`);
    parts.push(`${String(seconds).padStart(2, '0')}s`);

    return parts.join(' : ');
  }, [timeLeft]);

  // 4. Calculate exact release time for labels
  const releaseTimeFormatted = useMemo(() => {
    const releaseDate = new Date(matchTime.getTime() - UNLOCK_THRESHOLD_MS);
    return releaseDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }, [matchTime]);

  // 5. Handling Clipboard actions safely with temporary success state alerts
  const handleCopy = (text, fieldName) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedField(fieldName);
    setTimeout(() => {
      setCopiedField(null);
    }, 2000);
  };

  // Convert rules parameter gracefully if passed as plain string
  const rulesList = Array.isArray(tournament.rules) 
    ? tournament.rules 
    : (typeof tournament.rules === 'string' && tournament.rules.trim() ? tournament.rules.split('\n') : []);

  // Determine dynamic accent state based on time severity
  const timerColor = useMemo(() => {
    if (timeLeft <= 0) return '#4CAF50'; // Green: Match Live
    if (timeLeft <= UNLOCK_THRESHOLD_MS) return '#F44336'; // Red: Unlocked Credentials / Starts < 10 mins
    if (timeLeft <= 60 * 60 * 1000) return '#FF9800'; // Orange: Starts < 1 hour
    return '#E0E0E0'; // Silver fallback
  }, [timeLeft]);

  return (
    <div style={styles.lobbyContainer}>
      {/* Visual background atmospheric overlays */}
      <div style={styles.radialGlow} />

      {/* Header layout row */}
      <div style={styles.header}>
        <button onClick={onBack} style={styles.backButton}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
          LEAVE LOBBY
        </button>
        <div style={styles.lobbyTag}>
          <span style={styles.glowDot} /> {isLive ? "LIVE" : "GAMER ROOM"}
        </div>
      </div>

      <div style={styles.mainContent}>
        {/* Left Side: Match information and credentials lock */}
        <div style={styles.leftSection}>
          <div style={styles.cardHeader}>
            <span style={styles.gameBadge}>FREE FIRE BR</span>
            <h1 style={styles.title}>{tournament.title}</h1>
            <div style={styles.matchMetaRow}>
              <div style={styles.metaItem}>
                <span style={styles.metaLabel}>PRIZE POOL</span>
                <span style={styles.metaValueHighlight}>₹ {tournament.prizePool}</span>
              </div>
              <div style={styles.divider} />
              <div style={styles.metaItem}>
                <span style={styles.metaLabel}>ENTRY FEE</span>
                <span style={styles.metaValue}>₹ {tournament.entryFee}</span>
              </div>
              <div style={styles.divider} />
              <div style={styles.metaItem}>
                <span style={styles.metaLabel}>MAP TYPE</span>
                <span style={styles.metaValue}>{tournament.mapType}</span>
              </div>
            </div>
          </div>

          {/* TIMER CARD */}
          <div style={styles.timerCard}>
            <div style={styles.timerLabelRow}>
              <span style={styles.timerSubLabel}>METICULOUS START COUNTDOWN</span>
              <span style={{ ...styles.timeUrgencyLabel, color: timerColor }}>
                {timeLeft <= 0 ? "BATTLE ACTIVE" : timeLeft <= UNLOCK_THRESHOLD_MS ? "CRITICAL WINDOW" : "UPCOMING"}
              </span>
            </div>
            <div style={{ ...styles.countdownDisplay, color: timerColor }}>
              {formattedCountdown}
            </div>
            <div style={styles.matchDateFooter}>
              Scheduled Departure: {matchTime.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}
            </div>
          </div>

          {/* CREDENTIALS PROTECTION HUB */}
          <div style={styles.credentialsCard}>
            <div style={styles.credentialsCardHeader}>
              <h2 style={styles.cardTitle}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={isUnlocked ? "#4CAF50" : "#9E9E9E"} strokeWidth="2" style={{ marginRight: '8px', verticalAlign: 'middle' }}>
                  {isUnlocked ? (
                    <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
                  ) : (
                    <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
                  )}
                  {isUnlocked ? (
                    <path d="M7 11V7a5 5 0 0 1 9.9-1" />
                  ) : (
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  )}
                </svg>
                Room Credentials {isUnlocked ? 'Unlocked' : 'Secured'}
              </h2>
              {isRegistered && (
                <span style={{ ...styles.statusPill, backgroundColor: isUnlocked ? 'rgba(76, 175, 80, 0.15)' : 'rgba(244, 67, 54, 0.15)', color: isUnlocked ? '#81C784' : '#E57373' }}>
                  {isUnlocked ? 'ROOM DEPLOYED' : 'AWAITING LOCK RELEASE'}
                </span>
              )}
            </div>

            {!isRegistered ? (
              <div style={styles.unregisteredFallback}>
                <p style={styles.fallbackText}>You are currently a spectator. You must register to view active lobby credentials.</p>
              </div>
            ) : isUnlocked ? (
              // UNLOCKED STATE
              (!tournament.roomId && !tournament.roomPassword) ? (
                <div style={styles.credentialsPendingContainer}>
                  <div style={styles.spinner} />
                  <p style={{ margin: 0, fontSize: '13px', color: '#B0BEC5' }}>
                    Authenticating credentials allocation from server cluster. Please wait...
                  </p>
                </div>
              ) : (
                <div style={styles.unlockedFieldsContainer}>
                  <div style={styles.credentialRow}>
                    <div style={styles.fieldLabelContainer}>
                      <span style={styles.fieldLabel}>ROOM ID</span>
                      <span style={styles.fieldSub}>Enter this code in game search bar</span>
                    </div>
                    <div style={styles.fieldValueContainer}>
                      <code style={styles.codeText}>{tournament.roomId || 'NOT_ASSIGNED'}</code>
                      <button 
                        onClick={() => handleCopy(tournament.roomId, 'roomId')} 
                        disabled={!tournament.roomId}
                        style={styles.copyButton}
                      >
                        {copiedField === 'roomId' ? 'COPIED!' : 'COPY ID'}
                      </button>
                    </div>
                  </div>

                  <div style={styles.fieldDivider} />

                  <div style={styles.credentialRow}>
                    <div style={styles.fieldLabelContainer}>
                      <span style={styles.fieldLabel}>ROOM PASSWORD</span>
                      <span style={styles.fieldSub}>Password case-sensitive</span>
                    </div>
                    <div style={styles.fieldValueContainer}>
                      <code style={{ ...styles.codeText, color: '#FF4D4D' }}>{tournament.roomPassword || 'NOT_ASSIGNED'}</code>
                      <button 
                        onClick={() => handleCopy(tournament.roomPassword, 'password')} 
                        disabled={!tournament.roomPassword}
                        style={styles.copyButton}
                      >
                        {copiedField === 'password' ? 'COPIED!' : 'COPY PASS'}
                      </button>
                    </div>
                  </div>
                </div>
              )
            ) : (
              // LOCKED STATE
              <div style={styles.lockedContainer}>
                <div style={styles.lockOverlayIcon}>
                  <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#616161" strokeWidth="1.5">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <div style={styles.lockInstructionBlock}>
                  <p style={styles.lockHeadline}>Credentials locked for security</p>
                  <p style={styles.lockSubtext}>
                    Access keys are automatically broadcasted exactly 10 minutes prior to match launch to minimize malicious entry vectors.
                  </p>
                  <div style={styles.unlockCountdownAlert}>
                    Unfolds today at: <strong style={{ color: '#E0E0E0' }}>{releaseTimeFormatted}</strong>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Side: Gameplay Rules and Regulations checklist */}
        <div style={styles.rightSection}>
          <div style={styles.rulesCard}>
            <div style={styles.rulesCardHeader}>
              <h2 style={styles.cardTitle}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#D32F2F" strokeWidth="2" style={{ marginRight: '8px', verticalAlign: 'middle' }}>
                  <polyline points="9 11 12 14 22 4"></polyline>
                  <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
                </svg>
                Tournament Mandated Rules
              </h2>
              <span style={styles.rulesCountPill}>{rulesList.length} ITEMS</span>
            </div>
            
            <div style={styles.rulesScroller}>
              {rulesList.length === 0 ? (
                <div style={styles.noRulesContainer}>
                  <p style={{ margin: 0, color: '#757575', fontSize: '13px' }}>Standard regional Free Fire fairplay guidelines apply.</p>
                </div>
              ) : (
                rulesList.map((rule, idx) => (
                  <div key={idx} style={styles.ruleItem}>
                    <div style={styles.ruleMarkerBox}>
                      <span style={styles.ruleNumber}>{String(idx + 1).padStart(2, '0')}</span>
                    </div>
                    <div style={styles.ruleContentBlock}>
                      <p style={styles.ruleText}>{rule}</p>
                    </div>
                  </div>
                ))
              )}
            </div>

            <div style={styles.declarationFooter}>
              ⚠️ By launching matches on client software, you confirm compliance with anticheat validation audits. Failure results in immediate disqualification.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Visual style definitions
const styles = {
  lobbyContainer: {
    backgroundColor: '#0A0A0C',
    color: '#ECEFF1',
    fontFamily: '"Rajdhani", "Segoe UI", Roboto, sans-serif',
    minHeight: '100vh',
    padding: '24px',
    boxSizing: 'border-box',
    display: 'flex',
    flexDirection: 'column',
    position: 'relative',
    overflow: 'hidden'
  },
  radialGlow: {
    position: 'absolute',
    width: '600px',
    height: '600px',
    borderRadius: '50%',
    background: 'radial-gradient(circle, rgba(211, 47, 47, 0.08) 0%, rgba(0,0,0,0) 70%)',
    top: '-300px',
    right: '-100px',
    zIndex: 0,
    pointerEvents: 'none'
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
    zIndex: 1
  },
  backButton: {
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    color: '#ECEFF1',
    padding: '8px 16px',
    borderRadius: '6px',
    fontSize: '12px',
    fontWeight: '700',
    letterSpacing: '1px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    transition: 'all 0.2s ease',
    textTransform: 'uppercase'
  },
  lobbyTag: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    backgroundColor: 'rgba(211, 47, 47, 0.12)',
    border: '1px solid rgba(211, 47, 47, 0.3)',
    borderRadius: '20px',
    padding: '6px 14px',
    fontSize: '11px',
    fontWeight: '800',
    letterSpacing: '1px',
    color: '#FF4D4D'
  },
  glowDot: {
    width: '6px',
    height: '6px',
    backgroundColor: '#FF4D4D',
    borderRadius: '50%',
    boxShadow: '0 0 8px #FF4D4D'
  },
  mainContent: {
    display: 'grid',
    gridTemplateColumns: '1.2fr 0.8fr',
    gap: '24px',
    flexGrow: 1,
    zIndex: 1,
    maxWidth: '1200px',
    width: '100%',
    margin: '0 auto'
  },
  leftSection: {
    display: 'flex',
    flexDirection: 'column',
    gap: '20px'
  },
  rightSection: {
    display: 'flex',
    flexDirection: 'column'
  },
  cardHeader: {
    backgroundColor: '#121216',
    border: '1px solid rgba(255, 255, 255, 0.05)',
    borderRadius: '12px',
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start'
  },
  gameBadge: {
    backgroundColor: 'rgba(255, 235, 59, 0.12)',
    color: '#FFEE58',
    fontSize: '10px',
    fontWeight: '900',
    padding: '4px 8px',
    borderRadius: '4px',
    marginBottom: '8px',
    letterSpacing: '0.5px'
  },
  title: {
    fontSize: '24px',
    fontWeight: '800',
    margin: '0 0 16px 0',
    color: '#FFFFFF',
    letterSpacing: '0.5px'
  },
  matchMetaRow: {
    display: 'flex',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '16px',
    width: '100%'
  },
  metaItem: {
    display: 'flex',
    flexDirection: 'column'
  },
  metaLabel: {
    fontSize: '9px',
    color: '#90A4AE',
    fontWeight: '700',
    letterSpacing: '1px',
    marginBottom: '4px'
  },
  metaValue: {
    fontSize: '13px',
    fontWeight: '700',
    color: '#E0E0E0'
  },
  metaValueHighlight: {
    fontSize: '14px',
    fontWeight: '900',
    color: '#FFB300'
  },
  divider: {
    width: '1px',
    height: '24px',
    backgroundColor: 'rgba(255, 255, 255, 0.1)'
  },
  timerCard: {
    backgroundColor: '#121216',
    border: '1px solid rgba(255, 255, 255, 0.05)',
    borderRadius: '12px',
    padding: '24px',
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center'
  },
  timerLabelRow: {
    display: 'flex',
    justifyContent: 'space-between',
    width: '100%',
    marginBottom: '12px',
    alignItems: 'center'
  },
  timerSubLabel: {
    fontSize: '10px',
    color: '#90A4AE',
    fontWeight: '800',
    letterSpacing: '1.5px'
  },
  timeUrgencyLabel: {
    fontSize: '10px',
    fontWeight: '900',
    letterSpacing: '1px'
  },
  countdownDisplay: {
    fontSize: '44px',
    fontWeight: '900',
    fontFamily: '"Courier New", Monospace',
    letterSpacing: '2px',
    margin: '8px 0',
    textShadow: '0 0 10px rgba(0,0,0,0.5)'
  },
  matchDateFooter: {
    fontSize: '12px',
    color: '#90A4AE',
    marginTop: '8px'
  },
  credentialsCard: {
    backgroundColor: '#121216',
    border: '1px solid rgba(255, 255, 255, 0.05)',
    borderRadius: '12px',
    padding: '20px',
    display: 'flex',
    flexDirection: 'column'
  },
  credentialsCardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
    paddingBottom: '14px',
    marginBottom: '16px'
  },
  cardTitle: {
    fontSize: '15px',
    fontWeight: '700',
    margin: 0,
    color: '#FFFFFF'
  },
  statusPill: {
    fontSize: '9px',
    fontWeight: '800',
    padding: '4px 10px',
    borderRadius: '12px',
    letterSpacing: '0.5px'
  },
  unregisteredFallback: {
    padding: '24px',
    textAlign: 'center',
    backgroundColor: 'rgba(244, 67, 54, 0.04)',
    border: '1px dashed rgba(244, 67, 54, 0.2)',
    borderRadius: '8px'
  },
  fallbackText: {
    margin: 0,
    fontSize: '13px',
    color: '#E57373',
    lineHeight: 1.5
  },
  credentialsPendingContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '12px',
    padding: '24px 0'
  },
  spinner: {
    width: '16px',
    height: '16px',
    border: '2px solid rgba(255, 255, 255, 0.1)',
    borderTopColor: '#FF4D4D',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite'
  },
  unlockedFieldsContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  credentialRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '4px 0'
  },
  fieldLabelContainer: {
    display: 'flex',
    flexDirection: 'column'
  },
  fieldLabel: {
    fontSize: '11px',
    fontWeight: '800',
    letterSpacing: '0.5px',
    color: '#90A4AE'
  },
  fieldSub: {
    fontSize: '10px',
    color: '#607D8B',
    marginTop: '2px'
  },
  fieldValueContainer: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  },
  codeText: {
    fontFamily: 'Consolas, Monaco, monospace',
    fontSize: '16px',
    fontWeight: 'bold',
    backgroundColor: 'rgba(0,0,0,0.3)',
    border: '1px solid rgba(255, 255, 255, 0.08)',
    padding: '6px 12px',
    borderRadius: '4px',
    color: '#81C784',
    letterSpacing: '0.5px'
  },
  copyButton: {
    backgroundColor: '#263238',
    border: '1px solid #37474F',
    color: '#B0BEC5',
    padding: '6px 12px',
    borderRadius: '4px',
    fontSize: '10px',
    fontWeight: 'bold',
    cursor: 'pointer',
    transition: 'all 0.15s ease'
  },
  fieldDivider: {
    height: '1px',
    backgroundColor: 'rgba(255, 255, 255, 0.04)'
  },
  lockedContainer: {
    display: 'flex',
    alignItems: 'center',
    gap: '20px',
    padding: '16px',
    backgroundColor: 'rgba(0,0,0,0.15)',
    border: '1px solid rgba(255, 255, 255, 0.03)',
    borderRadius: '8px'
  },
  lockOverlayIcon: {
    backgroundColor: 'rgba(255, 255, 255, 0.02)',
    padding: '16px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    border: '1px solid rgba(255, 255, 255, 0.04)'
  },
  lockInstructionBlock: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start'
  },
  lockHeadline: {
    fontSize: '13px',
    fontWeight: '800',
    color: '#ECEFF1',
    margin: '0 0 4px 0'
  },
  lockSubtext: {
    fontSize: '11px',
    color: '#78909C',
    lineHeight: '1.4',
    margin: '0 0 10px 0'
  },
  unlockCountdownAlert: {
    backgroundColor: 'rgba(211, 47, 47, 0.04)',
    border: '1px solid rgba(211, 47, 47, 0.15)',
    borderRadius: '4px',
    padding: '4px 8px',
    fontSize: '10px',
    color: '#E57373',
    fontWeight: '600'
  },
  rulesCard: {
    backgroundColor: '#121216',
    border: '1px solid rgba(255, 255, 255, 0.05)',
    borderRadius: '12px',
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
    boxSizing: 'border-box'
  },
  rulesCardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
    paddingBottom: '14px',
    marginBottom: '16px'
  },
  rulesCountPill: {
    fontSize: '9px',
    backgroundColor: 'rgba(211, 47, 47, 0.1)',
    color: '#FF8A80',
    padding: '3px 8px',
    borderRadius: '4px',
    fontWeight: '800',
    letterSpacing: '0.5px'
  },
  rulesScroller: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    flexGrow: 1,
    overflowY: 'auto',
    maxHeight: '400px',
    paddingRight: '6px'
  },
  noRulesContainer: {
    padding: '24px 0',
    textAlign: 'center'
  },
  ruleItem: {
    display: 'flex',
    gap: '12px',
    alignItems: 'flex-start',
    backgroundColor: 'rgba(255, 255, 255, 0.02)',
    padding: '12px',
    borderRadius: '6px',
    border: '1px solid rgba(255, 255, 255, 0.02)',
    transition: 'all 0.2s ease'
  },
  ruleMarkerBox: {
    backgroundColor: 'rgba(211, 47, 47, 0.12)',
    border: '1px solid rgba(211, 47, 47, 0.25)',
    borderRadius: '4px',
    width: '24px',
    height: '24px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0
  },
  ruleNumber: {
    fontSize: '11px',
    fontWeight: '900',
    color: '#FF5252'
  },
  ruleContentBlock: {
    display: 'flex',
    flexDirection: 'column'
  },
  ruleText: {
    margin: 0,
    fontSize: '12px',
    color: '#CFD8DC',
    lineHeight: '1.5',
    fontWeight: '500'
  },
  declarationFooter: {
    fontSize: '10px',
    color: '#FF8A80',
    backgroundColor: 'rgba(211, 47, 47, 0.05)',
    border: '1px solid rgba(211, 47, 47, 0.15)',
    padding: '10px',
    borderRadius: '6px',
    marginTop: '16px',
    lineHeight: '1.4'
  }
};
