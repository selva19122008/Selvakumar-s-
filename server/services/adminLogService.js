const AdminLog = require('../models/AdminLog');

/**
 * Persists a new administrative action record to the audit trace logs
 * @param {Object} params
 * @param {string} params.adminId - The identifier of the administrator executing the command
 * @param {string} params.action - The administrative event code (e.g., 'CREATE_TOURNAMENT')
 * @param {string} params.targetType - Model class identifier representing the resource type
 * @param {string} [params.targetId=null] - The exact ID of the modified record
 * @param {string} params.description - Human-readable operational context
 * @param {Object} [params.changes=null] - Payload state modifications delta (before/after objects)
 * @param {Object} [params.req=null] - Express Request object to automatically harvest client trace vectors
 * @returns {Promise<Object>} Persisted audit log document reference
 */
exports.logAction = async ({
  adminId,
  action,
  targetType,
  targetId = null,
  description,
  changes = null,
  req = null
}) => {
  try {
    let ipAddress = null;
    let userAgent = null;

    if (req) {
      // Safely scrape standard network proxies & cloud gateway headers
      ipAddress = req.headers['x-forwarded-for'] || req.socket.remoteAddress || req.ip;
      userAgent = req.headers['user-agent'] || null;
    }

    const logEntry = new AdminLog({
      adminId,
      action: action.toUpperCase(),
      targetType,
      targetId: targetId ? targetId.toString() : null,
      description,
      changes,
      ipAddress,
      userAgent
    });

    await logEntry.save();
    console.log(`[Audit Service] Certified log entry registered: [${action}] by Admin: ${adminId}`);
    return logEntry;
  } catch (error) {
    console.error('[Audit Service] Critical failure writing audit trail:', error.message);
    // Never fail parent business transaction because of an audit write failure unless requested
    return null;
  }
};

/**
 * Returns a list of audit records matching optional query triggers
 * @param {Object} filters - Search filters
 * @param {number} [filters.page=1] - Active screen index for pagination bounds
 * @param {number} [filters.limit=25] - Item page count size
 */
exports.fetchAuditLogs = async ({
  adminId,
  action,
  targetType,
  search,
  page = 1,
  limit = 25
} = {}) => {
  try {
    const query = {};
    const pageNum = Math.max(1, parseInt(page) || 1);
    const limitNum = Math.max(1, parseInt(limit) || 25);
    const skipNum = (pageNum - 1) * limitNum;

    if (adminId) {
      query.adminId = adminId;
    }

    if (action) {
      query.action = action.toUpperCase();
    }

    if (targetType) {
      query.targetType = targetType;
    }

    if (search) {
      query.description = { $regex: search.trim(), $options: 'i' };
    }

    const totalLogs = await AdminLog.countDocuments(query);
    const logs = await AdminLog.find(query)
      .populate('adminId', 'inGameName email role')
      .sort({ createdAt: -1 })
      .skip(skipNum)
      .limit(limitNum);

    return {
      success: true,
      data: logs,
      pagination: {
        totalItems: totalLogs,
        currentPage: pageNum,
        totalPages: Math.ceil(totalLogs / limitNum),
        itemsPerPage: limitNum
      }
    };
  } catch (error) {
    throw new Error(`Failed to compile audit logs list: ${error.message}`);
  }
};
