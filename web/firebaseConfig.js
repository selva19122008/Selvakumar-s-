import { initializeApp, getApps, getApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';
import { getAuth } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyDr0LJeBUoQcBzrxuZTb0sUcy3SCKP-eEU",
  authDomain: "battle-zone-ff-3b23f.firebaseapp.com",
  projectId: "battle-zone-ff-3b23f",
  storageBucket: "battle-zone-ff-3b23f.firebasestorage.app",
  messagingSenderId: "178066056608",
  appId: "1:178066056608:web:3b52f63c046ff4382970c5"
};

// Initialize Firebase
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();
const db = getFirestore(app);
const auth = getAuth(app);

export { app, db, auth, firebaseConfig };
export default firebaseConfig;
