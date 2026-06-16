const multer = require('multer');
const admin = require('firebase-admin');

// 1. Configure Multer with strict security limits
const storage = multer.memoryStorage();

// Validate mimetype to ensure only secure image files are processed
const fileFilter = (req, file, cb) => {
  const allowedMimeTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
  if (allowedMimeTypes.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error('Invalid image format. Only JPEG, JPG, PNG, and WebP files are allowed.'), false);
  }
};

const uploadConfig = multer({
  storage: storage,
  limits: {
    fileSize: 3 * 1024 * 1024, // Strict 3MB size limit
    files: 1 // Only 1 file upload allowed per query sequence
  },
  fileFilter: fileFilter
}).single('profilePicture');

/**
 * Robust middleware to securely handle multipart image uploads and transfer the payload directly
 * to Firebase Cloud Storage with elegant error handling and dry-run environment support.
 */
exports.secureProfileUpload = (req, res, next) => {
  uploadConfig(req, res, async (err) => {
    // A. Intercept Multer custom filter or size limit exceptions
    if (err) {
      return res.status(400).json({
        success: false,
        message: 'File upload validation rejected.',
        error: err.message
      });
    }

    // B. Check if a profile picture element was actually attached
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No file found. Please attach an image file mapped to the key "profilePicture".'
      });
    }

    // C. Securely authorize request context context
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'Unverified request scope. Session authentication is required prior to uploading assets.'
      });
    }

    const userId = req.user.id || req.user._id;

    try {
      let bucket = null;
      let storageAvailable = false;

      // Check if Firebase admin app is initialized with storage capability
      if (admin && admin.apps.length > 0) {
        try {
          const defaultBucketName = process.env.FIREBASE_STORAGE_BUCKET || `${admin.app().options.projectId}.appspot.com`;
          bucket = admin.storage().bucket(defaultBucketName);
          storageAvailable = !!bucket;
        } catch (e) {
          console.warn('[Upload Middleware] Firebase Storage resolution warning:', e.message);
        }
      }

      // Safe clean filename formation: sanitize file metadata to prevent path injection
      const fileExt = req.file.originalname.split('.').pop().toLowerCase();
      const sanitizedExt = ['jpg', 'jpeg', 'png', 'webp'].includes(fileExt) ? fileExt : 'jpg';
      const uniqueFilename = `profiles/${userId}/${Date.now()}.${sanitizedExt}`;

      if (storageAvailable && bucket) {
        // Option 1: True Firebase Storage Pipeline
        const fileUploadRef = bucket.file(uniqueFilename);

        console.log(`[Upload Middleware] Uploading asset [${req.file.mimetype}] to Cloud Storage destination: ${uniqueFilename}...`);

        // Save buffer stream directly to Cloud Storage
        await fileUploadRef.save(req.file.buffer, {
          metadata: {
            contentType: req.file.mimetype,
            metadata: {
              uploadedBy: userId.toString(),
              originalName: req.file.originalname
            }
          },
          public: true, // Make publically available
          resumable: false
        });

        // Make double-sure file access controls are marked as public
        try {
          await fileUploadRef.makePublic();
        } catch (aclError) {
          console.warn('[Upload Middleware] ACL public modification warning:', aclError.message);
        }

        // Construct direct public HTTPS visual URL reference
        const publicUrl = `https://storage.googleapis.com/${bucket.name}/${fileUploadRef.name}`;
        
        req.uploadedFileUrl = publicUrl;
        console.log(`[Upload Middleware] Asset successfully saved to Firebase. Accessible URL: ${publicUrl}`);

      } else {
        // Option 2: Full Simulated Fallback Offline mode for local development evaluation
        console.warn(`[Upload Middleware] Offline Firebase mode detected. Simulating secure storage upload for user: ${userId}`);

        // Construct high-quality base64 Data URI from buffered data so picture works with dynamic client widgets
        const base64Data = req.file.buffer.toString('base64');
        const simulatedDataUrl = `data:${req.file.mimetype};base64,${base64Data}`;

        req.uploadedFileUrl = simulatedDataUrl;
        console.log('[Upload Middleware] Asset serialized to Local Data Object successfully.');
      }

      return next();

    } catch (uploadError) {
      console.error('[Upload Middleware] Severe pipeline error uploading file:', uploadError);
      return res.status(500).json({
        success: false,
        message: 'System failed to write file to storage provider.',
        error: uploadError.message
      });
    }
  });
};
