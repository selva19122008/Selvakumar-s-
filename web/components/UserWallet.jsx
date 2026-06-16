import React, { useState, useMemo } from 'react';

/**
 * UserWallet Component
 * A high-fidelity, fully responsive, and polished React wallet interface.
 * Matches the "Obsidian & Crimson" gaming theme.
 * 
 * Major Capabilities:
 * - Real-time client-state balance tracker with separate registers for Deposit, Winnings, and Bonus balances.
 * - Interactive preset-based 'Add Money' workflow with validation.
 * - Robust 'Withdrawal' workflow with real-time winnings balance checking and safe payout fields.
 * - Dynamic Transaction Ledger showcasing realistic past matches/wallet adjustments.
 * 
 * @param {Object} props
 * @param {Object} [props.initialBalances] - Initial starting balances representing Indian Rupees (₹).
 * @param {number} [props.initialBalances.deposit=150.00]
 * @param {number} [props.initialBalances.winnings=320.00]
 * @param {number} [props.initialBalances.bonus=50.00]
 * @param {Function} [props.onAddMoney] - Triggered when money is successfully added.
 * @param {Function} [props.onWithdraw] - Triggered when a withdrawal is initiated.
 * @param {Function} [props.onBack] - Close or navigate back action callback.
 */
export default function UserWallet({
  initialBalances = { deposit: 150.00, winnings: 320.00, bonus: 50.00 },
  onAddMoney = (amount) => {},
  onWithdraw = (amount, upiId) => {},
  onBack = () => {}
}) {
  // 1. Maintain local reactive state for balances to enable seamless client side playground experience
  const [balances, setBalances] = useState(initialBalances);
  const [transactions, setTransactions] = useState([
    { id: 'tx-2', type: 'debit', label: 'Pro Duo - Free Fire Entry', date: 'Today, 2:15 PM', amount: 15.00, category: 'match_entry' },
    { id: 'tx-1', type: 'credit', label: 'Clash Squad - Elite Victory', date: 'Yesterday, 8:45 PM', amount: 350.00, category: 'winnings' },
    { id: 'tx-0', type: 'credit', label: 'Promotional Welcome Bonus', date: 'June 14, 2026', amount: 50.00, category: 'bonus' }
  ]);

  // 2. Active Operation States
  const [activeModal, setActiveModal] = useState(null); // 'add' | 'withdraw' | null
  const [addAmount, setAddAmount] = useState('');
  const [withdrawAmount, setWithdrawAmount] = useState('');
  const [upiId, setUpiId] = useState('');
  const [withdrawError, setWithdrawError] = useState('');
  const [addError, setAddError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // 3. Computed Balances
  const totalBalance = useMemo(() => {
    return balances.deposit + balances.winnings + balances.bonus;
  }, [balances]);

  // 4. Input Presets for Adding Money
  const addPresets = [100, 200, 500, 1000];

  // 5. Add Money Action
  const handleAddMoneySubmit = (e) => {
    e.preventDefault();
    const amount = parseFloat(addAmount);
    
    if (isNaN(amount) || amount <= 0) {
      setAddError('Please enter a valid amount greater than ₹0.');
      return;
    }

    if (amount < 10) {
      setAddError('Minimum deposit is ₹10.');
      return;
    }

    // Update balances and add to ledger
    setBalances(prev => ({
      ...prev,
      deposit: prev.deposit + amount
    }));

    const newTx = {
      id: `tx-${Date.now()}`,
      type: 'credit',
      label: 'Added Cash (Wallet Deposit)',
      date: 'Just Now',
      amount,
      category: 'deposit'
    };

    setTransactions(prev => [newTx, ...prev]);
    onAddMoney(amount);

    // Provide feedback and cleanup
    setAddAmount('');
    setAddError('');
    setActionSuccess(`Successfully added ₹${amount.toFixed(2)} to Deposit balance!`);
    
    setTimeout(() => {
      setActionSuccess('');
      setActiveModal(null);
    }, 2000);
  };

  // 6. Withdraw Action
  const handleWithdrawSubmit = (e) => {
    e.preventDefault();
    const amount = parseFloat(withdrawAmount);

    if (isNaN(amount) || amount <= 0) {
      setWithdrawError('Please enter an amount greater than ₹0.');
      return;
    }

    if (amount > balances.winnings) {
      setWithdrawError(`Insufficient funds. You can withdraw up to your total winnings (₹${balances.winnings.toFixed(2)}).`);
      return;
    }

    if (amount < 100) {
      setWithdrawError('Minimum withdrawal amount is ₹100.');
      return;
    }

    if (!upiId.trim() || !upiId.includes('@')) {
      setWithdrawError('Please enter a valid UPI ID (e.g., username@bank).');
      return;
    }

    // Deduct winnings and log transaction
    setBalances(prev => ({
      ...prev,
      winnings: prev.winnings - amount
    }));

    const newTx = {
      id: `tx-${Date.now()}`,
      type: 'debit',
      label: 'Withdrawal to UPI Requested',
      date: 'Just Now',
      amount,
      category: 'winnings_payout'
    };

    setTransactions(prev => [newTx, ...prev]);
    onWithdraw(amount, upiId);

    // Reset inputs
    setWithdrawAmount('');
    setUpiId('');
    setWithdrawError('');
    setActionSuccess(`₹${amount.toFixed(2)} withdrawal successfully scheduled to UPI!`);

    setTimeout(() => {
      setActionSuccess('');
      setActiveModal(null);
    }, 2000);
  };

  return (
    <div style={styles.walletContainer}>
      {/* Visual background ambient glow */}
      <div style={styles.radialGlow} />

      {/* Header layout */}
      <div style={styles.header}>
        <button onClick={onBack} style={styles.backButton}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
          BACK TO GAME
        </button>
        <div style={styles.lobbyTag}>
          <span style={styles.glowDot} /> SECURE VAULT
        </div>
      </div>

      <div style={styles.mainContent}>
        {/* Left Hand Card: Total combined balance and core transaction triggers */}
        <div style={styles.leftSection}>
          
          {/* TOTAL BALANCE CARD */}
          <div style={styles.totalBalanceCard}>
            <div style={styles.balanceHeader}>
              <span style={styles.walletLabel}>TOTAL NET ASSETS</span>
              <div style={styles.securitySeal}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#81C784" strokeWidth="2" style={{ marginRight: '4px' }}>
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
                128-BIT SECURE
              </div>
            </div>
            <div style={styles.balanceDisplay}>
              <span style={styles.currencySymbol}>₹</span> {totalBalance.toFixed(2)}
            </div>
            
            {/* Quick Action Buttons */}
            <div style={styles.actionButtonGroup}>
              <button 
                onClick={() => { setActiveModal('add'); setAddError(''); }} 
                style={styles.addMoneyBtn}
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ marginRight: '6px' }}>
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                ADD MONEY
              </button>
              <button 
                onClick={() => { setActiveModal('withdraw'); setWithdrawError(''); }} 
                style={styles.withdrawBtn}
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ marginRight: '6px' }}>
                  <line x1="7" y1="17" x2="17" y2="7"></line>
                  <polyline points="7 7 17 7 17 17"></polyline>
                </svg>
                WITHDRAWAL
              </button>
            </div>
          </div>

          {/* SPLIT BALANCES GRID */}
          <h3 style={styles.sectionHeaderTitle}>DETAILED ASSETS ALLOCATION</h3>
          <div style={styles.splitGrid}>
            
            {/* Deposit Balance */}
            <div style={styles.splitCard}>
              <div style={styles.splitCardHeader}>
                <div style={{ ...styles.colorTagCircle, backgroundColor: '#00B0FF' }} />
                <span style={styles.splitLabel}>DEPOSIT CASH</span>
              </div>
              <div style={styles.splitAmount}>
                ₹ {balances.deposit.toFixed(2)}
              </div>
              <p style={styles.splitDescription}>
                Funds added to join tournaments. Standard entry stakes deduct here first. Non-withdrawable.
              </p>
            </div>

            {/* Winning Balance */}
            <div style={{ ...styles.splitCard, border: '1px solid rgba(255, 179, 0, 0.2)' }}>
              <div style={styles.splitCardHeader}>
                <div style={{ ...styles.colorTagCircle, backgroundColor: '#FFB300' }} />
                <span style={{ ...styles.splitLabel, color: '#FFB300' }}>WINNINGS REGISTER</span>
                <span style={styles.payoutPermittedTag}>WITHDRAWABLE</span>
              </div>
              <div style={{ ...styles.splitAmount, color: '#FFFFFF' }}>
                ₹ {balances.winnings.toFixed(2)}
              </div>
              <p style={styles.splitDescription}>
                Net cash won from regional/championship level lobbies. Directly withdrawable to your bank via UPI instantly.
              </p>
            </div>

            {/* Bonus Balance */}
            <div style={styles.splitCard}>
              <div style={styles.splitCardHeader}>
                <div style={{ ...styles.colorTagCircle, backgroundColor: '#D81B60' }} />
                <span style={styles.splitLabel}>PROMO BONUS</span>
              </div>
              <div style={styles.splitAmount}>
                ₹ {balances.bonus.toFixed(2)}
              </div>
              <p style={styles.splitDescription}>
                Granted for referrals, promotions or registration loyalty. Offsets up to 15% of select entry stakes.
              </p>
            </div>

          </div>

        </div>

        {/* Right Hand Card: Interactive Action Area (Form or Transaction History fallback) */}
        <div style={styles.rightSection}>
          
          {/* DYNAMIC FORM DRAWER */}
          {activeModal === 'add' && (
            <div style={styles.interactiveAreaCard}>
              <div style={styles.cardHeaderWithClose}>
                <h3 style={styles.cardTitle}>
                  <span style={styles.crimsonMarker} /> ADD MONEY TO WALLET
                </h3>
                <button onClick={() => setActiveModal(null)} style={styles.closeCardBtn}>✕</button>
              </div>

              {actionSuccess ? (
                <div style={styles.successMessageBlock}>
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#81C784" strokeWidth="2.5" style={{ marginBottom: '8px' }}>
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                  <span>{actionSuccess}</span>
                </div>
              ) : (
                <form onSubmit={handleAddMoneySubmit} style={styles.formLayout}>
                  <p style={styles.formHint}>Select a preset payload or input custom entry fee stakes below:</p>
                  
                  {/* Preset Pills */}
                  <div style={styles.presetsGrid}>
                    {addPresets.map(preset => (
                      <button 
                        key={preset}
                        type="button"
                        onClick={() => setAddAmount(preset.toString())}
                        style={{
                          ...styles.presetPill,
                          borderColor: addAmount === preset.toString() ? '#FF4D4D' : 'rgba(255, 255, 255, 0.08)',
                          backgroundColor: addAmount === preset.toString() ? 'rgba(255, 77, 77, 0.1)' : 'rgba(255, 255, 255, 0.02)',
                          color: addAmount === preset.toString() ? '#FF4D4D' : '#ECEFF1'
                        }}
                      >
                        + ₹{preset}
                      </button>
                    ))}
                  </div>

                  <div style={styles.inputLabelBlock}>
                    <label style={styles.formFieldLabel}>ENTER DEPOSIT AMOUNT (₹)</label>
                    <input 
                      type="number"
                      placeholder="e.g. 250"
                      value={addAmount}
                      onChange={(e) => setAddAmount(e.target.value)}
                      style={styles.formInput}
                      min="10"
                    />
                  </div>

                  {addError && <p style={styles.errorText}>⚠️ {addError}</p>}

                  <button type="submit" style={styles.submitActionBtn}>
                    SECURE INSTANT PAY
                  </button>
                  <p style={styles.payoutDisclaimer}>
                    Transactions integrated via verified gateway architecture. Supports UPI, Netbanking & Cards.
                  </p>
                </form>
              )}
            </div>
          )}

          {activeModal === 'withdraw' && (
            <div style={styles.interactiveAreaCard}>
              <div style={styles.cardHeaderWithClose}>
                <h3 style={styles.cardTitle}>
                  <span style={{ ...styles.crimsonMarker, backgroundColor: '#FFB300' }} /> REDEEM WINNINGS TO BANK
                </h3>
                <button onClick={() => setActiveModal(null)} style={styles.closeCardBtn}>✕</button>
              </div>

              {actionSuccess ? (
                <div style={styles.successMessageBlock}>
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#81C784" strokeWidth="2.5" style={{ marginBottom: '8px' }}>
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                  <span>{actionSuccess}</span>
                </div>
              ) : (
                <form onSubmit={handleWithdrawSubmit} style={styles.formLayout}>
                  <p style={styles.formHint}>
                    Payout balances are securely transferred within 6-12 hours of auditing checks.
                  </p>

                  <div style={styles.inputLabelBlock}>
                    <label style={styles.formFieldLabel}>WITHDRAWAL AMOUNT (₹)</label>
                    <div style={styles.withdrawableMaxRow}>
                      <span style={{ fontSize: '11px', color: '#B0BEC5' }}>Max withdrawable: ₹{balances.winnings.toFixed(2)}</span>
                      <button 
                        type="button" 
                        onClick={() => setWithdrawAmount(balances.winnings.toString())} 
                        style={styles.maximizeBtn}
                      >
                        WITHDRAW ALL
                      </button>
                    </div>
                    <input 
                      type="number"
                      placeholder="Min ₹100"
                      value={withdrawAmount}
                      onChange={(e) => setWithdrawAmount(e.target.value)}
                      style={styles.formInput}
                      min="100"
                    />
                  </div>

                  <div style={styles.inputLabelBlock}>
                    <label style={styles.formFieldLabel}>VERIFIED PAYOUT UPI ID</label>
                    <input 
                      type="text"
                      placeholder="e.g. gameplayerr0r@ybl"
                      value={upiId}
                      onChange={(e) => setUpiId(e.target.value)}
                      style={styles.formInput}
                    />
                  </div>

                  {withdrawError && <p style={styles.errorText}>⚠️ {withdrawError}</p>}

                  <button type="submit" style={{ ...styles.submitActionBtn, backgroundColor: '#E65100', borderColor: '#EF6C00' }}>
                    CONFIRM PAYOUT REQUEST
                  </button>
                  <p style={styles.payoutDisclaimer}>
                    To prevent fraudulent patterns, self-withdrawing during active competitive locks is temporarily verified by tournament refs. 
                  </p>
                </form>
              )}
            </div>
          )}

          {/* HISTORICAL TRANSACTION STATEMENT */}
          <div style={styles.ledgerCard}>
            <div style={styles.ledgerHeader}>
              <h3 style={styles.ledgerTitle}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#FF4D4D" strokeWidth="2" style={{ marginRight: '8px', verticalAlign: 'middle' }}>
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                  <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
                TRANSACTION AUDITING HISTORY
              </h3>
              <span style={styles.ledgerSubBadge}>SECURE LOGS</span>
            </div>

            <div style={styles.ledgerScroller}>
              {transactions.length === 0 ? (
                <div style={styles.emptyLedgerContainer}>
                  <p style={{ margin: 0, color: '#757575' }}>No transactions recorded yet in your safe register.</p>
                </div>
              ) : (
                transactions.map((tx) => (
                  <div key={tx.id} style={styles.txItem}>
                    <div style={styles.txIconBox}>
                      {tx.type === 'credit' ? (
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#81C784" strokeWidth="3">
                          <polyline points="17 11 12 6 7 11"></polyline>
                          <line x1="12" y1="18" x2="12" y2="6"></line>
                        </svg>
                      ) : (
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#E57373" strokeWidth="3">
                          <polyline points="7 13 12 18 17 13"></polyline>
                          <line x1="12" y1="6" x2="12" y2="18"></line>
                        </svg>
                      )}
                    </div>
                    
                    <div style={styles.txDetails}>
                      <span style={styles.txTitle}>{tx.label}</span>
                      <span style={styles.txDate}>{tx.date}</span>
                    </div>

                    <div style={styles.txAmountContainer}>
                      <span style={{ 
                        ...styles.txAmount, 
                        color: tx.type === 'credit' ? '#81C784' : '#E57373' 
                      }}>
                        {tx.type === 'credit' ? '+' : '-'} ₹{tx.amount.toFixed(2)}
                      </span>
                      <span style={styles.txMetaCode}>{tx.category.toUpperCase()}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}

// Visual layout options aligned with the Crimson and Obsidian design standard
const styles = {
  walletContainer: {
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
    background: 'radial-gradient(circle, rgba(211, 47, 47, 0.06) 0%, rgba(0,0,0,0) 70%)',
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
    backgroundColor: 'rgba(255, 77, 77, 0.12)',
    border: '1px solid rgba(255, 77, 77, 0.3)',
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
    gridTemplateColumns: '1.15fr 0.85fr',
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
    flexDirection: 'column',
    gap: '20px'
  },
  totalBalanceCard: {
    backgroundColor: '#121216',
    border: '1px solid rgba(255, 255, 255, 0.05)',
    borderRadius: '12px',
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    position: 'relative'
  },
  balanceHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  walletLabel: {
    fontSize: '11px',
    color: '#90A4AE',
    fontWeight: '800',
    letterSpacing: '1.5px'
  },
  securitySeal: {
    fontSize: '9px',
    fontWeight: '900',
    color: '#81C784',
    letterSpacing: '0.5px',
    display: 'flex',
    alignItems: 'center',
    backgroundColor: 'rgba(129, 199, 132, 0.08)',
    padding: '4px 8px',
    borderRadius: '4px'
  },
  balanceDisplay: {
    fontSize: '40px',
    fontWeight: '900',
    color: '#FFFFFF',
    letterSpacing: '1px',
    margin: '4px 0',
    display: 'flex',
    alignItems: 'baseline',
    gap: '8px'
  },
  currencySymbol: {
    fontSize: '24px',
    color: '#FF4D4D',
    fontWeight: '700'
  },
  actionButtonGroup: {
    display: 'flex',
    gap: '12px',
    marginTop: '8px'
  },
  addMoneyBtn: {
    flex: 1,
    backgroundColor: '#FF4D4D',
    border: '1px solid #FF3333',
    color: '#FFFFFF',
    padding: '12px',
    borderRadius: '8px',
    fontSize: '13px',
    fontWeight: '800',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    letterSpacing: '0.75px',
    transition: 'all 0.15s ease'
  },
  withdrawBtn: {
    flex: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    color: '#FFFFFF',
    padding: '12px',
    borderRadius: '8px',
    fontSize: '13px',
    fontWeight: '800',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    letterSpacing: '0.75px',
    transition: 'all 0.15s ease'
  },
  sectionHeaderTitle: {
    fontSize: '12px',
    color: '#90A4AE',
    fontWeight: '800',
    letterSpacing: '1px',
    margin: '12px 0 4px 0'
  },
  splitGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(220dp, 1fr))',
    gap: '16px'
  },
  splitCard: {
    backgroundColor: '#121216',
    border: '1px solid rgba(255, 255, 255, 0.04)',
    borderRadius: '10px',
    padding: '16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px'
  },
  splitCardHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    position: 'relative',
    width: '100%'
  },
  colorTagCircle: {
    width: '6px',
    height: '6px',
    borderRadius: '50%'
  },
  splitLabel: {
    fontSize: '10px',
    fontWeight: '800',
    color: '#90A4AE',
    letterSpacing: '0.5px'
  },
  payoutPermittedTag: {
    position: 'absolute',
    right: 0,
    fontSize: '8px',
    fontWeight: '900',
    color: '#FFB300',
    backgroundColor: 'rgba(255, 179, 0, 0.08)',
    padding: '2px 6px',
    borderRadius: '3px'
  },
  splitAmount: {
    fontSize: '18px',
    fontWeight: '900',
    color: '#FFFFFF'
  },
  splitDescription: {
    margin: 0,
    fontSize: '11px',
    color: '#78909C',
    lineHeight: '1.4'
  },
  interactiveAreaCard: {
    backgroundColor: '#121216',
    border: '1px solid rgba(255, 77, 77, 0.15)',
    borderRadius: '12px',
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    animation: 'fadeIn 0.2s ease-out'
  },
  cardHeaderWithClose: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
    paddingBottom: '12px',
    marginBottom: '16px'
  },
  cardTitle: {
    fontSize: '14px',
    fontWeight: '800',
    margin: 0,
    color: '#FFFFFF',
    letterSpacing: '0.5px',
    display: 'flex',
    alignItems: 'center'
  },
  crimsonMarker: {
    width: '4px',
    height: '14px',
    backgroundColor: '#FF4D4D',
    display: 'inline-block',
    marginRight: '8px',
    borderRadius: '1px'
  },
  closeCardBtn: {
    backgroundColor: 'transparent',
    border: 'none',
    color: '#78909C',
    fontSize: '16px',
    cursor: 'pointer',
    padding: '4px'
  },
  successMessageBlock: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center',
    padding: '24px 0',
    color: '#81C784',
    fontSize: '13px',
    lineHeight: '1.5'
  },
  formLayout: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  formHint: {
    margin: 0,
    fontSize: '12px',
    color: '#CFD8DC',
    lineHeight: '1.4'
  },
  presetsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gap: '8px',
    marginBottom: '4px'
  },
  presetPill: {
    border: '1px solid',
    borderRadius: '6px',
    padding: '8px 2px',
    fontSize: '11px',
    fontWeight: '800',
    cursor: 'pointer',
    transition: 'all 0.15s ease',
    textAlign: 'center'
  },
  inputLabelBlock: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
  },
  formFieldLabel: {
    fontSize: '10px',
    fontWeight: '800',
    color: '#90A4AE',
    letterSpacing: '0.75px'
  },
  formInput: {
    backgroundColor: 'rgba(0,0,0,0.2)',
    border: '1px solid rgba(255, 255, 255, 0.08)',
    borderRadius: '6px',
    padding: '10px 12px',
    color: '#FFFFFF',
    fontSize: '14px',
    fontFamily: 'inherit',
    outline: 'none',
    transition: 'border-color 0.15s ease'
  },
  withdrawableMaxRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  maximizeBtn: {
    backgroundColor: 'transparent',
    border: 'none',
    color: '#FFB300',
    fontSize: '10px',
    fontWeight: '800',
    cursor: 'pointer',
    padding: '0'
  },
  errorText: {
    margin: 0,
    fontSize: '12px',
    color: '#EF5350'
  },
  submitActionBtn: {
    backgroundColor: '#FF4D4D',
    border: '1px solid #FF3333',
    color: '#FFFFFF',
    padding: '12px',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: '800',
    cursor: 'pointer',
    letterSpacing: '0.75px',
    transition: 'all 0.1s ease',
    marginTop: '6px'
  },
  payoutDisclaimer: {
    margin: 0,
    fontSize: '10px',
    color: '#607D8B',
    textAlign: 'center',
    lineHeight: '1.4'
  },
  ledgerCard: {
    backgroundColor: '#121216',
    border: '1px solid rgba(255, 255, 255, 0.05)',
    borderRadius: '12px',
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    flexGrow: 1
  },
  ledgerHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
    paddingBottom: '14px',
    marginBottom: '16px'
  },
  ledgerTitle: {
    fontSize: '13px',
    fontWeight: '800',
    margin: 0,
    color: '#FFFFFF',
    letterSpacing: '0.5px'
  },
  ledgerSubBadge: {
    fontSize: '9px',
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    color: '#90A4AE',
    padding: '3px 8px',
    borderRadius: '4px',
    fontWeight: '800',
    letterSpacing: '0.5px'
  },
  ledgerScroller: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    maxHeight: '320px',
    overflowY: 'auto'
  },
  emptyLedgerContainer: {
    textAlign: 'center',
    padding: '32px 0',
    fontSize: '13px'
  },
  txItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '10px 12px',
    backgroundColor: 'rgba(255, 255, 255, 0.01)',
    border: '1px solid rgba(255, 255, 255, 0.02)',
    borderRadius: '8px',
    gap: '12px'
  },
  txIconBox: {
    width: '24px',
    height: '24px',
    borderRadius: '50%',
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0
  },
  txDetails: {
    display: 'flex',
    flexDirection: 'column',
    flexGrow: 1
  },
  txTitle: {
    fontSize: '12px',
    fontWeight: '700',
    color: '#ECEFF1'
  },
  txDate: {
    fontSize: '10px',
    color: '#607D8B',
    marginTop: '2px'
  },
  txAmountContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-end',
    flexShrink: 0
  },
  txAmount: {
    fontSize: '13px',
    fontWeight: '900'
  },
  txMetaCode: {
    fontSize: '8px',
    fontWeight: '800',
    color: '#90A4AE',
    marginTop: '2px',
    letterSpacing: '0.5px'
  }
};
