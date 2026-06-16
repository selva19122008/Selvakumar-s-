const Tournament = require('../models/Tournament');

/**
 * Filters and queries tournaments based on strict parameters to improve discovery for users
 * Supports precise constraints over map type, entry fee ranges, active/specific status, 
 * smart sorting criteria, pagination, and search patterns.
 * 
 * @param {Object} params - Search and filter parameters
 * @param {string|string[]} [params.mapType] - Filter by map name(s) (Bermuda, Purgatory, Kalahari, Alpine, Nexterra)
 * @param {string|string[]} [params.format] - Filter by format(s) (Solo, Duo, Squad)
 * @param {number} [params.minEntryFee] - Minimum boundary for registration fee (non-negative)
 * @param {number} [params.maxEntryFee] - Maximum boundary for registration fee (non-negative)
 * @param {string|string[]} [params.status] - Filter by specific status(es) (e.g., 'upcoming', 'live', 'completed', 'cancelled')
 * @param {string} [params.search] - Case-insensitive text search match for tournament title
 * @param {string} [params.sortBy='matchDate'] - Sort pivot (e.g., 'matchDate', 'entryFee', 'prizePool', 'joinedSlots', 'availableSlots')
 * @param {string} [params.sortOrder='asc'] - Sorting order ('asc' or 'desc')
 * @param {number} [params.page=1] - Pagination page number index
 * @param {number} [params.limit=10] - Max elements contained per page
 * @returns {Promise<Object>} Paginated discovery response payload
 */
exports.discoverTournaments = async ({
  mapType,
  format,
  minEntryFee,
  maxEntryFee,
  status,
  search,
  sortBy = 'matchDate',
  sortOrder = 'asc',
  page = 1,
  limit = 10
} = {}) => {
  try {
    const query = {};

    // 1. Map type parsing & validation
    if (mapType) {
      const allowedMaps = ['Bermuda', 'Purgatory', 'Kalahari', 'Alpine', 'Nexterra'];
      const mapList = Array.isArray(mapType) ? mapType : [mapType];
      
      const validMaps = mapList.filter(map => allowedMaps.includes(map));
      if (validMaps.length > 0) {
        query.mapType = { $in: validMaps };
      }
    }

    // 1b. Format parsing & validation
    if (format) {
      const allowedFormats = ['Solo', 'Duo', 'Squad'];
      const formatList = Array.isArray(format) ? format : [format];
      
      const validFormats = formatList.filter(f => allowedFormats.includes(f));
      if (validFormats.length > 0) {
        query.format = { $in: validFormats };
      }
    }

    // 2. Entry Fee Range construction
    const feeFilter = {};
    let hasFeeFilter = false;

    if (minEntryFee !== undefined && minEntryFee !== null) {
      const parsedMin = parseFloat(minEntryFee);
      if (!isNaN(parsedMin) && parsedMin >= 0) {
        feeFilter.$gte = parsedMin;
        hasFeeFilter = true;
      }
    }

    if (maxEntryFee !== undefined && maxEntryFee !== null) {
      const parsedMax = parseFloat(maxEntryFee);
      if (!isNaN(parsedMax) && parsedMax >= 0) {
        feeFilter.$lte = parsedMax;
        hasFeeFilter = true;
      }
    }

    if (hasFeeFilter) {
      query.entryFee = feeFilter;
    }

    // 3. Status filter with modern active-first approach
    const allowedStatuses = ['upcoming', 'live', 'completed', 'cancelled'];
    if (status) {
      const statusList = Array.isArray(status) 
        ? status.map(s => s.toLowerCase().trim()) 
        : [status.toLowerCase().trim()];
      
      const validStatuses = statusList.filter(s => allowedStatuses.includes(s));
      if (validStatuses.length > 0) {
        query.status = { $in: validStatuses };
      } else {
        // If query status is supplied but completely invalid, default to non-cancelled active states
        query.status = { $in: ['upcoming', 'live'] };
      }
    } else {
      // By default, filter purely the active state tournaments (upcoming & live) for optimal discovery
      query.status = { $in: ['upcoming', 'live'] };
    }

    // 4. Text-search title filtering
    if (search && search.trim()) {
      query.title = { $regex: search.trim(), $options: 'i' };
    }

    // 5. Advanced Dynamic Sorting
    const sortParams = {};
    const orderSign = sortOrder.toLowerCase() === 'desc' ? -1 : 1;

    // Support flexible sort fields
    if (sortBy === 'entryFee') {
      sortParams.entryFee = orderSign;
    } else if (sortBy === 'prizePool') {
      sortParams.prizePool = orderSign;
    } else if (sortBy === 'joinedSlots') {
      sortParams.joinedSlots = orderSign;
    } else {
      // Default to match date
      sortParams.matchDate = orderSign;
    }
    // Always append _id as tiebreaker to guarantee stable pagination results
    sortParams._id = 1;

    // 6. Pagination Bounds
    const pageNum = Math.max(1, parseInt(page) || 1);
    const limitNum = Math.max(1, parseInt(limit) || 10);
    const skipNum = (pageNum - 1) * limitNum;

    // 7. Perform DB aggregations & queries
    const totalItems = await Tournament.countDocuments(query);
    const tournaments = await Tournament.find(query)
      .sort(sortParams)
      .skip(skipNum)
      .limit(limitNum);

    // Provide friendly discovery tags or statistics (e.g., number of seats open/occupied)
    const decoratedTournaments = tournaments.map(t => {
      const doc = t.toObject();
      doc.availableSlots = Math.max(0, doc.maxSlots - doc.joinedSlots);
      doc.isFull = doc.joinedSlots >= doc.maxSlots;
      return doc;
    });

    return {
      success: true,
      data: decoratedTournaments,
      pagination: {
        totalItems,
        currentPage: pageNum,
        totalPages: Math.ceil(totalItems / limitNum),
        itemsPerPage: limitNum
      }
    };
  } catch (error) {
    console.error('[Discovery Service] Error tracking and sorting tournaments:', error.message);
    throw new Error(`Failed to discover active tournaments: ${error.message}`);
  }
};
