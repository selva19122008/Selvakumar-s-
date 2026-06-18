import React, { useState, useEffect } from 'react';
import { initializeApp, getApps, getApp } from 'firebase/app';
import { 
  getFirestore, 
  collection, 
  onSnapshot, 
  doc, 
  deleteDoc, 
  addDoc,
  serverTimestamp 
} from 'firebase/firestore';

/**
 * FirestoreUserList Component
 * 
 * Provides real-time synchronization of the registered esports gamers directory.
 * Fully interactive, supporting direct Firestore live stream synchronization 
 * alongside an high-fidelity offline sandbox simulator.
 * 
 * Styled specifically using the "Obsidian & Crimson" gaming interface design aesthetic.
 */
export default function FirestoreUserList({ 
  firebaseConfig = null, 
  onLogEvent = () => {} 
}) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isLiveConnection, setIsLiveConnection] = useState(false);
  const [errorStatus, setErrorStatus] = useState(null);
  const [notification, setNotification] = useState(null);
  
  // Create / Simulation modal state
  const [showAddGamerModal, setShowAddGamerModal] = useState(false);
  const [newGamerName, setNewGamerName] = useState('');
  const [newGamerEmail, setNewGamerEmail] = useState('');
  const [newGamerPhone, setNewGamerPhone] = useState('');
  const [newGamerUid, setNewGamerUid] = useState('');

  // Built-in sandbox users to display if not connected to live Firestore
  const mockGamers = [
    { id: 'mock-u-101', inGameName: 'Gamer_9122', email: 'gamer9122@gmail.com', phoneNumber: '+919999999991', freeFireUid: 'UID991208', depositBalance: 150.00, winningBalance: 320.00, role: 'user', createdAt: new Date().toISOString() },
    { id: 'mock-u-102', inGameName: 'Elite_Shooter', email: 'shooter_pro@gmail.com', phoneNumber: '+919999999992', freeFireUid: 'UID881245', depositBalance: 45.00, winningBalance: 610.00, role: 'user', createdAt: new Date().toISOString() },
    { id: 'mock-u-103', inGameName: 'Phoenix_FF', email: 'phoenix_gamer@gmail.com', phoneNumber: '+919999999993', freeFireUid: 'UID772418', depositBalance: 500.00, winningBalance: 0.00, role: 'user', createdAt: new Date().toISOString() },
    { id: 'mock-u-104', inGameName: 'Admin_Selva', email: 'selva19122008@gmail.com', phoneNumber: '+919999999999', freeFireUid: 'UID001912', depositBalance: 10.00, winningBalance: 1200.00, role: 'admin', createdAt: new Date().toISOString() }
  ];

  // Initialize and subscribe to Firestore
  useEffect(() => {
    let unsubscribe = null;
    setErrorStatus(null);

    // Fallback configurations if none provided
    const activeConfig = firebaseConfig || {
      apiKey: "AIzaSyFakeKey_BattleZone_19122008",
      authDomain: "battlezone-esports.firebaseapp.com",
      projectId: "battlezone-esports",
      storageBucket: "battlezone-esports.appspot.com",
      messagingSenderId: "101185877248",
      appId: "1:101185877248:web:abcdef123"
    };

    if (isLiveConnection) {
      setLoading(true);
      try {
        const app = getApps().length === 0 ? initializeApp(activeConfig) : getApp();
        const db = getFirestore(app);
        const usersRef = collection(db, 'users');

        // Establish live listener
        unsubscribe = onSnapshot(usersRef, (snapshot) => {
          const fetchedUsers = [];
          snapshot.forEach((docSnap) => {
            const data = docSnap.data();
            fetchedUsers.push({
              id: docSnap.id,
              ...data,
              // Format Firestore Timestamps gracefully
              createdAt: data.createdAt?.toDate?.()?.toISOString() || data.createdAt || new Date().toISOString()
            });
          });
          setUsers(fetchedUsers);
          setLoading(false);
          triggerToast('Connected to Firestore! Syncing user profiles in real-time.', 'success');
          onLogEvent('Established reactive Firestore snapshot channel for gamers database.');
        }, (error) => {
          console.error("Firestore subscription error: ", error);
          setErrorStatus(error.message);
          setLoading(false);
          triggerToast('Failed to sync. Please ensure Firestore Security Rules are configured correctly.', 'error');
          setIsLiveConnection(false); // Drop back to sandbox gracefully
        });
      } catch (err) {
        console.error("Firebase Initialization failure: ", err);
        setErrorStatus(err.message);
        setLoading(false);
        setIsLiveConnection(false);
        triggerToast('Database initialization error. Check console logs.', 'error');
      }
    } else {
      // Offline default interactive sandbox mode
      setLoading(true);
      const timer = setTimeout(() => {
        // Load initial sandbox list from localStorage or hardcoded list
        const cached = localStorage.getItem('battlezone_cached_gamers');
        if (cached) {
          try {
            setUsers(JSON.parse(cached));
          } catch {
            setUsers(mockGamers);
          }
        } else {
          setUsers(mockGamers);
          localStorage.setItem('battlezone_cached_gamers', JSON.stringify(mockGamers));
        }
        setLoading(false);
        onLogEvent('Loaded offline sandbox database cache.');
      }, 600);

      return () => clearTimeout(timer);
    }

    return () => {
      if (unsubscribe) {
        unsubscribe();
      }
    };
  }, [isLiveConnection, firebaseConfig]);

  const triggerToast = (text, type = 'info') => {
    setNotification({ text, type });
    setTimeout(() => setNotification(null), 4000);
  };

  // Delete User document
  const handleDeleteUser = async (userId, userIGN) => {
    onLogEvent(`Initiating deletion request for user ${userIGN} (ID: ${userId})`);
    
    if (isLiveConnection) {
      setLoading(true);
      try {
        const app = getApp();
        const db = getFirestore(app);
        const userDocRef = doc(db, 'users', userId);
        
        await deleteDoc(userDocRef);
        triggerToast(`Successfully purged profile '${userIGN}' from Firestore.`, 'success');
        onLogEvent(`Purged Firestore document path: /users/${userId}`);
      } catch (err) {
        console.error("Firestore document deletion failure: ", err);
        triggerToast(`Permission denied: ${err.message}`, 'error');
      } finally {
        setLoading(false);
      }
    } else {
      // Local sandbox state removal
      const updated = users.filter(u => u.id !== userId);
      setUsers(updated);
      localStorage.setItem('battlezone_cached_gamers', JSON.stringify(updated));
      triggerToast(`[Sandbox] Purged gamer '${userIGN}' successfully.`, 'success');
      onLogEvent(`Deleted local cache node for device wallet: ID=${userId}`);
    }
  };

  // Add User document
  const handleAddNewGamer = async (e) => {
    e.preventDefault();
    if (!newGamerName || !newGamerEmail || !newGamerPhone) {
      triggerToast('In-Game Name, Email, and Phone number are required.', 'error');
      return;
    }

    const payload = {
      inGameName: newGamerName.trim(),
      email: newGamerEmail.trim().toLowerCase(),
      phoneNumber: newGamerPhone.trim(),
      freeFireUid: newGamerUid.trim() || 'UID' + Math.floor(100000 + Math.random() * 900000),
      depositBalance: 100.00,
      winningBalance: 0.00,
      bonusBalance: 25.00,
      role: 'user'
    };

    if (isLiveConnection) {
      setLoading(true);
      try {
        const app = getApp();
        const db = getFirestore(app);
        
        await addDoc(collection(db, 'users'), {
          ...payload,
          createdAt: serverTimestamp()
        });
        
        triggerToast(`Esports gamer '${newGamerName}' written to Firestore securely!`, 'success');
        onLogEvent(`Inserted Firestore document under /users/ collection.`);
        setShowAddGamerModal(false);
        resetFormInputs();
      } catch (err) {
        console.error("Firestore document write failure: ", err);
        triggerToast(`Permission denied: ${err.message}`, 'error');
        setLoading(false);
      }
    } else {
      // Save local sandbox card
      const newId = 'mock-' + Date.now();
      const newEntry = {
        id: newId,
        ...payload,
        createdAt: new Date().toISOString()
      };
      
      const updated = [newEntry, ...users];
      setUsers(updated);
      localStorage.setItem('battlezone_cached_gamers', JSON.stringify(updated));
      triggerToast(`[Sandbox] Registered gamer '${newGamerName}' successfully!`, 'success');
      onLogEvent(`Registered sandbox user account: ID=${newId}`);
      setShowAddGamerModal(false);
      resetFormInputs();
    }
  };

  const resetFormInputs = () => {
    setNewGamerName('');
    setNewGamerEmail('');
    setNewGamerPhone('');
    setNewGamerUid('');
  };

  return (
    <div style={styles.cardContainer}>
      {/* HEADER CONTROLS */}
      <div style={styles.headerRow}>
        <div>
          <h3 style={styles.dashboardTitle}>
            🔥 FIRESTORE GAMERS REAL-TIME LIST 
            <span style={isLiveConnection ? styles.liveBadgePulse : styles.sandboxBadgePulse}>
              {isLiveConnection ? '● RECONNECTING' : '● SANDBOX'}
            </span>
          </h3>
          <p style={styles.subtext}>
            Instantly syncs, deletes, and monitors gamer profiles of Android app clients via live Firestore streams.
          </p>
        </div>

        <div style={styles.actionsPanel}>
          {/* Real-time sync connection toggle */}
          <button 
            onClick={() => setIsLiveConnection(!isLiveConnection)}
            style={isLiveConnection ? styles.liveConnectionActive : styles.liveConnectionInactive}
          >
            {isLiveConnection ? 'Disconnect Firestore' : 'Connect Firestore Stream'}
          </button>
          
          <button 
            onClick={() => setShowAddGamerModal(true)}
            style={styles.actionBtnPrimary}
          >
            + Register Gamer
          </button>
        </div>
      </div>

      {/* FEEDBACK BANNER */}
      {notification && (
        <div style={{
          ...styles.notificationBanner,
          backgroundColor: notification.type === 'success' ? 'rgba(76, 175, 80, 0.08)' : notification.type === 'error' ? 'rgba(239, 83, 80, 0.08)' : 'rgba(33, 150, 243, 0.08)',
          borderColor: notification.type === 'success' ? '#81C784' : notification.type === 'error' ? '#EF5350' : '#2196F3',
          color: notification.type === 'success' ? '#81C784' : notification.type === 'error' ? '#EF5350' : '#2196F3'
        }}>
          <b>{notification.type.toUpperCase()}:</b> {notification.text}
        </div>
      )}

      {errorStatus && (
        <div style={styles.errorBox}>
          <b>Firebase Status:</b> {errorStatus}
        </div>
      )}

      {/* REAL-TIME LEDGER STATISTICS */}
      <div style={styles.statsSummaryGrid}>
        <div style={styles.statsCard}>
          <div style={styles.statsLabel}>Total Nodes Loaded</div>
          <div style={styles.statsNumber}>{users.length}</div>
          <div style={styles.statsDetail}>Total records streamed</div>
        </div>
        <div style={styles.statsCard}>
          <div style={styles.statsLabel}>Sync Status</div>
          <div style={{ ...styles.statsNumber, color: isLiveConnection ? '#81C784' : '#FF9100' }}>
            {isLiveConnection ? 'ACTIVE' : 'OFFLINE'}
          </div>
          <div style={styles.statsDetail}>
            {isLiveConnection ? 'Listening to full database writes' : 'Using browser sandbox memory'}
          </div>
        </div>
        <div style={styles.statsCard}>
          <div style={styles.statsLabel}>Accumulated Obligation</div>
          <div style={{ ...styles.statsNumber, color: '#81C784' }}>
            ₹{users.reduce((sum, u) => sum + (u.depositBalance || 0) + (u.winningBalance || 0), 0).toFixed(2)}
          </div>
          <div style={styles.statsDetail}>Gamers dynamic wallets collective sum</div>
        </div>
      </div>

      {/* REACTIVE GAMER GRID VIEW */}
      <div style={styles.tableFrame}>
        {loading ? (
          <div style={styles.loaderArea}>
            <div style={styles.spinner} />
            <span style={{ fontSize: '13px', color: '#90A4AE' }}>Syncing with the database node...</span>
          </div>
        ) : (
          <table style={styles.gamersTable}>
            <thead>
              <tr>
                <th style={styles.thCell}>GAMER DETAIL</th>
                <th style={styles.thCell}>MOBILE IDENTITY</th>
                <th style={styles.thCell}>ACCOUNT BALANCES</th>
                <th style={styles.thCell}>REGISTERED DATE</th>
                <th style={{ ...styles.thCell, textAlign: 'right' }}>DESTRUCTOR</th>
              </tr>
            </thead>
            <tbody>
              {users.map((item) => (
                <tr key={item.id} style={styles.trRow}>
                  <td style={styles.tdCell}>
                    <div style={styles.identityFlex}>
                      <div style={{
                        ...styles.gamerAvatarBadge,
                        backgroundColor: item.role === 'admin' ? '#EF5350' : '#1F2025'
                      }}>
                        {item.inGameName.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <div style={styles.gamerIGN}>{item.inGameName}</div>
                        <div style={styles.gamerEmail}>{item.email}</div>
                      </div>
                    </div>
                  </td>
                  <td style={styles.tdCell}>
                    <div style={styles.monoCell}>Phone: {item.phoneNumber}</div>
                    <div style={styles.monoCell}>UID: {item.freeFireUid}</div>
                  </td>
                  <td style={styles.tdCell}>
                    <div style={styles.balanceGroup}>
                      <span style={styles.balanceBadgeDeposit}>Deposit: ₹{(item.depositBalance || 0).toFixed(0)}</span>
                      <span style={styles.balanceBadgeWinning}>Winning: ₹{(item.winningBalance || 0).toFixed(0)}</span>
                    </div>
                  </td>
                  <td style={styles.tdCell}>
                    <span style={styles.dateStamp}>
                      {new Date(item.createdAt).toLocaleDateString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </span>
                  </td>
                  <td style={{ ...styles.tdCell, textAlign: 'right' }}>
                    <button 
                      onClick={() => {
                        if (confirm(`⚠️ Are you sure you want to permanently delete gamer '${item.inGameName}'? This administrative write reflects instantly on all active client devices!`)) {
                          handleDeleteUser(item.id, item.inGameName);
                        }
                      }}
                      style={styles.deleteButton}
                      title="Purge Document on Firestore"
                    >
                      Delete Profile
                    </button>
                  </td>
                </tr>
              ))}

              {users.length === 0 && (
                <tr>
                  <td colSpan="5" style={styles.emptyNotice}>
                    No registered esport gamers currently exist. Click "+ Register Gamer" to populate the database stream.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* GAMER WRITE OVERLAY DIALOG MODAL */}
      {showAddGamerModal && (
        <div style={styles.dialogOverlay}>
          <div style={styles.dialogCard}>
            <div style={styles.dialogHeader}>
              <h3 style={styles.dialogTitle}>Register Esports Competitor</h3>
              <button onClick={() => { setShowAddGamerModal(false); resetFormInputs(); }} style={styles.closeBtn}>×</button>
            </div>

            <form onSubmit={handleAddNewGamer} style={styles.dialogForm}>
              <div style={styles.fieldUnit}>
                <label style={styles.fieldLabel}>Gamer In-Game Name (IGN) *</label>
                <input 
                  type="text"
                  required
                  placeholder="e.g. Elite_Shooter"
                  value={newGamerName}
                  onChange={(e) => setNewGamerName(e.target.value)}
                  style={styles.dialogInput}
                />
              </div>

              <div style={styles.fieldUnit}>
                <label style={styles.fieldLabel}>Email Address *</label>
                <input 
                  type="email"
                  required
                  placeholder="e.g. pro_shooter@gmail.com"
                  value={newGamerEmail}
                  onChange={(e) => setNewGamerEmail(e.target.value)}
                  style={styles.dialogInput}
                />
              </div>

              <div style={styles.fieldWrapperRow}>
                <div style={{ ...styles.fieldUnit, flex: 1 }}>
                  <label style={styles.fieldLabel}>Phone Identity *</label>
                  <input 
                    type="tel"
                    required
                    placeholder="e.g. +919999999991"
                    value={newGamerPhone}
                    onChange={(e) => setNewGamerPhone(e.target.value)}
                    style={styles.dialogInput}
                  />
                </div>

                <div style={{ ...styles.fieldUnit, flex: 1 }}>
                  <label style={styles.fieldLabel}>Free Fire UID (Optional)</label>
                  <input 
                    type="text"
                    placeholder="Auto-generated if left empty"
                    value={newGamerUid}
                    onChange={(e) => setNewGamerUid(e.target.value)}
                    style={styles.dialogInput}
                  />
                </div>
              </div>

              <p style={styles.formNote}>
                * Submitting this registration form issues a direct write transaction payload to the cloud Firestore database. It will sync dynamically to client nodes immediately.
              </p>

              <div style={styles.dialogActionsFlex}>
                <button 
                  type="button" 
                  onClick={() => { setShowAddGamerModal(false); resetFormInputs(); }}
                  style={styles.btnSecondary}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  disabled={loading}
                  style={styles.btnPrimary}
                >
                  {loading ? 'Submitting Write Payload...' : '✔ Write to Database'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

// Establishes inline CSS design systems preserving the unified Obsidian & Crimson palette
const styles = {
  cardContainer: {
    backgroundColor: '#111116',
    borderRadius: '12px',
    border: '1px solid rgba(255, 255, 255, 0.04)',
    padding: '24px',
    margin: '16px 0',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    color: '#ECEFF1'
  },
  headerRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '16px',
    borderBottom: '2px solid rgba(255,255,255,0.03)',
    paddingBottom: '20px',
    marginBottom: '20px'
  },
  dashboardTitle: {
    margin: '0 0 4px 0',
    fontSize: '16px',
    fontWeight: '900',
    letterSpacing: '0.8px',
    color: '#FFFFFF',
    display: 'flex',
    alignItems: 'center',
    gap: '10px'
  },
  liveBadgePulse: {
    fontSize: '9px',
    fontWeight: '900',
    backgroundColor: 'rgba(76, 175, 80, 0.1)',
    color: '#81C784',
    padding: '3px 8px',
    borderRadius: '4px',
    letterSpacing: '0.5px'
  },
  sandboxBadgePulse: {
    fontSize: '9px',
    fontWeight: '900',
    backgroundColor: 'rgba(255, 145, 0, 0.08)',
    color: '#FF9100',
    padding: '3px 8px',
    borderRadius: '4px',
    letterSpacing: '0.5px'
  },
  subtext: {
    margin: 0,
    fontSize: '12px',
    color: '#90A4AE',
    lineHeight: '1.5'
  },
  actionsPanel: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  },
  liveConnectionActive: {
    backgroundColor: 'rgba(76, 175, 80, 0.1)',
    border: '1px solid #81C784',
    color: '#81C784',
    padding: '8px 16px',
    borderRadius: '6px',
    fontSize: '12px',
    fontWeight: '800',
    cursor: 'pointer',
    transition: 'all 0.15s ease'
  },
  liveConnectionInactive: {
    backgroundColor: 'transparent',
    border: '1px solid rgba(255,255,255,0.08)',
    color: '#90A4AE',
    padding: '8px 16px',
    borderRadius: '6px',
    fontSize: '12px',
    fontWeight: '800',
    cursor: 'pointer',
    transition: 'all 0.15s ease'
  },
  actionBtnPrimary: {
    backgroundColor: '#EF5350',
    border: 'none',
    color: '#FFFFFF',
    padding: '8px 16px',
    borderRadius: '6px',
    fontSize: '12px',
    fontWeight: '900',
    cursor: 'pointer',
    boxShadow: '0 4px 10px rgba(239, 83, 80, 0.25)',
    transition: 'all 0.15s ease'
  },
  notificationBanner: {
    borderLeft: '4px solid',
    padding: '12px 18px',
    borderRadius: '4px',
    marginBottom: '20px',
    fontSize: '13px',
    lineHeight: '1.4'
  },
  errorBox: {
    backgroundColor: 'rgba(239, 83, 80, 0.08)',
    border: '1px solid rgba(239, 83, 80, 0.2)',
    color: '#EF5350',
    padding: '12px 18px',
    borderRadius: '6px',
    marginBottom: '20px',
    fontSize: '12.5px'
  },
  statsSummaryGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
    gap: '16px',
    marginBottom: '24px'
  },
  statsCard: {
    backgroundColor: '#0F0F14',
    border: '1px solid rgba(255,255,255,0.02)',
    borderRadius: '8px',
    padding: '16px',
    boxShadow: '0 2px 5px rgba(0,0,0,0.2)'
  },
  statsLabel: {
    fontSize: '10px',
    fontWeight: '800',
    color: '#607D8B',
    textTransform: 'uppercase',
    letterSpacing: '0.8px',
    marginBottom: '6px'
  },
  statsNumber: {
    fontSize: '20px',
    fontWeight: '900',
    color: '#FFFFFF',
    marginBottom: '4px'
  },
  statsDetail: {
    fontSize: '10.5px',
    color: '#90A4AE'
  },
  tableFrame: {
    width: '100%',
    overflowX: 'auto',
    backgroundColor: '#0F0F14',
    border: '1px solid rgba(255,255,255,0.02)',
    borderRadius: '8px'
  },
  loaderArea: {
    padding: '80px 0',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '16px'
  },
  spinner: {
    width: '32px',
    height: '32px',
    borderRadius: '50%',
    border: '3px solid rgba(239, 83, 80, 0.1)',
    borderTopColor: '#EF5350',
    animation: 'spin 1s linear infinite'
  },
  gamersTable: {
    width: '100%',
    borderCollapse: 'separate',
    borderSpacing: '0',
    fontSize: '12.5px',
    textAlign: 'left'
  },
  thCell: {
    padding: '14px 18px',
    color: '#607D8B',
    fontSize: '10px',
    fontWeight: '800',
    letterSpacing: '0.8px',
    background: '#14141A',
    borderBottom: '1px solid rgba(255,255,255,0.04)',
    textTransform: 'uppercase'
  },
  trRow: {
    transition: 'background-color 0.1s ease',
    '&:hover': {
      backgroundColor: 'rgba(255,255,255,0.01)'
    }
  },
  tdCell: {
    padding: '14px 18px',
    borderBottom: '1px solid rgba(255,255,255,0.02)',
    verticalAlignment: 'middle'
  },
  identityFlex: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  },
  gamerAvatarBadge: {
    width: '32px',
    height: '32px',
    borderRadius: '8px',
    color: '#FFFFFF',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '14px',
    fontWeight: '900',
    boxShadow: '0 2px 4px rgba(0,0,0,0.15)'
  },
  gamerIGN: {
    fontSize: '13px',
    fontWeight: '800',
    color: '#FFFFFF',
    marginBottom: '2px'
  },
  gamerEmail: {
    fontSize: '11px',
    color: '#607D8B'
  },
  monoCell: {
    fontFamily: 'Courier, monospace',
    fontSize: '11px',
    color: '#ECEFF1',
    lineHeight: '1.4'
  },
  balanceGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px'
  },
  balanceBadgeDeposit: {
    display: 'inline-block',
    fontSize: '10.5px',
    color: '#90A4AE',
    fontWeight: '600'
  },
  balanceBadgeWinning: {
    display: 'inline-block',
    fontSize: '10.5px',
    color: '#81C784',
    fontWeight: '800'
  },
  dateStamp: {
    fontSize: '11px',
    color: '#607D8B'
  },
  deleteButton: {
    backgroundColor: 'rgba(239, 83, 80, 0.08)',
    border: '1px solid rgba(239, 83, 80, 0.4)',
    color: '#EF5350',
    padding: '6px 12px',
    borderRadius: '4px',
    fontSize: '11px',
    fontWeight: '800',
    cursor: 'pointer',
    transition: 'all 0.15s ease'
  },
  emptyNotice: {
    textAlign: 'center',
    padding: '60px 24px',
    color: '#607D8B',
    fontSize: '13px'
  },
  dialogOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.7)',
    backdropFilter: 'blur(3px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 100,
    animation: 'fadeIn 0.2s ease'
  },
  dialogCard: {
    backgroundColor: '#111116',
    border: '1px solid rgba(255,255,255,0.06)',
    borderRadius: '12px',
    width: '100%',
    maxWidth: '500px',
    padding: '24px',
    boxShadow: '0 10px 30px rgba(0,0,0,0.5)'
  },
  dialogHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px'
  },
  dialogTitle: {
    margin: 0,
    fontSize: '15px',
    color: '#FFFFFF',
    fontWeight: '900',
    letterSpacing: '0.5px'
  },
  closeBtn: {
    background: 'none',
    border: 'none',
    color: '#90A4AE',
    fontSize: '24px',
    cursor: 'pointer',
    padding: '0'
  },
  dialogForm: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  fieldUnit: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
  },
  fieldLabel: {
    fontSize: '11px',
    fontWeight: '900',
    color: '#607D8B',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  dialogInput: {
    backgroundColor: '#0F0F14',
    border: '1px solid rgba(255,255,255,0.06)',
    color: '#FFFFFF',
    borderRadius: '6px',
    padding: '10px 12px',
    fontSize: '13px',
    outline: 'none',
    '&:focus': {
      borderColor: '#EF5350'
    }
  },
  fieldWrapperRow: {
    display: 'flex',
    gap: '16px'
  },
  formNote: {
    margin: '4px 0 0 0',
    fontSize: '10.5px',
    color: '#607D8B',
    lineHeight: '1.4'
  },
  dialogActionsFlex: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '12px',
    marginTop: '12px'
  },
  btnSecondary: {
    backgroundColor: 'transparent',
    border: '1px solid rgba(255,255,255,0.08)',
    color: '#90A4AE',
    padding: '10px 18px',
    borderRadius: '6px',
    fontSize: '12px',
    fontWeight: '800',
    cursor: 'pointer',
    transition: 'all 0.15s ease'
  },
  btnPrimary: {
    backgroundColor: '#EF5350',
    border: 'none',
    color: '#FFFFFF',
    padding: '10px 18px',
    borderRadius: '6px',
    fontSize: '12px',
    fontWeight: '900',
    cursor: 'pointer',
    boxShadow: '0 4px 10px rgba(239, 83, 80, 0.25)',
    transition: 'all 0.15s ease'
  }
};
