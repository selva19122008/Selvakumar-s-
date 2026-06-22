/**
 * Firestore Client Service Module
 * 
 * Provides structural methods for real-time listeners and database synchronization
 * on the 'tournaments' collection within Firestore, ensuring all connected user
 * devices and web clients fetch and display the exact same tournament list instantly.
 */

import { 
  collection, 
  doc, 
  addDoc, 
  setDoc, 
  updateDoc, 
  deleteDoc, 
  query, 
  orderBy, 
  onSnapshot,
  getDocs
} from 'firebase/firestore';
import { db } from './firebaseConfig';

/**
 * Subscribes to real-time updates of the 'tournaments' collection in Firestore.
 * This ensures that whenever an admin inserts, updates, or deletes an active tournament,
 * all connected player clients and dashboards receive the event immediately.
 * 
 * @param {Function} onUpdate - Callback function invoked with the updated list of tournaments.
 * @param {Function} [onError] - Optional subscription error callback handler.
 * @returns {Function} unsubscribe - Function to terminate the active real-time firestore listener.
 */
export function subscribeToTournaments(onUpdate, onError) {
  try {
    const tournamentsCollection = collection(db, 'tournaments');
    // Query tournaments ordered by match date/time
    const q = query(tournamentsCollection, orderBy('matchDate', 'asc'));

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const tournamentsList = [];
      snapshot.forEach((docSnap) => {
        const data = docSnap.data();
        tournamentsList.push({
          id: docSnap.id,
          _id: docSnap.id, // For high compatibility with various dashboard components
          ...data,
          // Support standard mapping parameters (e.g. converting Firestore Timestamp objects if needed)
          matchDate: data.matchDate || new Date().toISOString()
        });
      });
      onUpdate(tournamentsList);
    }, (error) => {
      console.error('Firestore tournaments real-time subscription failed:', error);
      if (onError) onError(error);
    });

    return unsubscribe;
  } catch (error) {
    console.error('Fatal initialization error in subscribeToTournaments:', error);
    throw error;
  }
}

/**
 * Synchronously or asynchronously fetches the complete list of tournaments once.
 * Useful for fast initial renders or offline fallback hydration.
 * 
 * @returns {Promise<Array>} List of tournaments
 */
export async function getTournamentsOnce() {
  try {
    const tournamentsCollection = collection(db, 'tournaments');
    const q = query(tournamentsCollection, orderBy('matchDate', 'asc'));
    const snapshot = await getDocs(q);
    
    const tournamentsList = [];
    snapshot.forEach((docSnap) => {
      tournamentsList.push({
        id: docSnap.id,
        _id: docSnap.id,
        ...docSnap.data()
      });
    });
    return tournamentsList;
  } catch (error) {
    console.error('Failed to manually fetch tournaments from Firestore:', error);
    throw error;
  }
}

/**
 * Creates/Adds a new tournament into Firestore.
 * Once resolved, it automatically triggers active real-time hooks on all connected clients.
 * 
 * @param {Object} tournamentData - The tournament metadata fields to store.
 * @returns {Promise<string>} The newly created Firestore document reference ID.
 */
export async function createTournament(tournamentData) {
  try {
    if (!tournamentData.title) {
      throw new Error('Tournament title is required to register a matchroom.');
    }

    const payload = {
      title: tournamentData.title,
      matchDate: tournamentData.matchDate || new Date().toISOString(),
      prizePool: Number(tournamentData.prizePool) || 0,
      entryFee: Number(tournamentData.entryFee) || 0,
      mapType: tournamentData.mapType || 'Bermuda',
      slotsTotal: Number(tournamentData.slotsTotal) || 48,
      slotsRemaining: Number(tournamentData.slotsRemaining) ?? (Number(tournamentData.slotsTotal) || 48),
      roomId: tournamentData.roomId || '',
      roomPassword: tournamentData.roomPassword || '',
      status: tournamentData.status || 'upcoming',
      rules: Array.isArray(tournamentData.rules) ? tournamentData.rules : [
        "Use mobile layout to play correctly.",
        "Emulators and cheat layers are strictly prohibited."
      ],
      createdAt: new Date().toISOString()
    };

    const docRef = await addDoc(collection(db, 'tournaments'), payload);
    return docRef.id;
  } catch (error) {
    console.error('Failed to create new tournament in Firestore:', error);
    throw error;
  }
}

/**
 * Updates an existing tournament's specifications.
 * Broadcasters will instantaneously spread changes like updated remaining slots, status, or credentials.
 * 
 * @param {string} tournamentId - The unique document identifier.
 * @param {Object} updateDetails - The modified fields.
 * @returns {Promise<void>}
 */
export async function updateTournament(tournamentId, updateDetails) {
  try {
    if (!tournamentId) {
      throw new Error('Target tournament key not provided for update transaction.');
    }

    const docRef = doc(db, 'tournaments', tournamentId);
    
    // Ensure data types are strictly sanitized
    const sanitized = { ...updateDetails };
    if (sanitized.prizePool !== undefined) sanitized.prizePool = Number(sanitized.prizePool) || 0;
    if (sanitized.entryFee !== undefined) sanitized.entryFee = Number(sanitized.entryFee) || 0;
    if (sanitized.slotsTotal !== undefined) sanitized.slotsTotal = Number(sanitized.slotsTotal) || 48;
    if (sanitized.slotsRemaining !== undefined) sanitized.slotsRemaining = Number(sanitized.slotsRemaining) || 0;

    await updateDoc(docRef, sanitized);
  } catch (error) {
    console.error(`Failed to update tournament ${tournamentId} in Firestore:`, error);
    throw error;
  }
}

/**
 * Removes a tournament from Firestore.
 * Automatically deletes tracking vectors on client views.
 * 
 * @param {string} tournamentId - The unique document identifier.
 * @returns {Promise<void>}
 */
export async function deleteTournament(tournamentId) {
  try {
    if (!tournamentId) {
      throw new Error('Target tournament key not provided for deletion transaction.');
    }

    const docRef = doc(db, 'tournaments', tournamentId);
    await deleteDoc(docRef);
  } catch (error) {
    console.error(`Failed to delete tournament ${tournamentId} in Firestore:`, error);
    throw error;
  }
}
