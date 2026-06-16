const mongoose = require('mongoose');
const SupportTicket = require('../models/SupportTicket');
const User = require('../models/User');
const adminLogService = require('../services/adminLogService');

/**
 * Support Ticket Controller
 * Handles client-side support inquiry creation and full administrative ticket lifecycle resolution.
 */

/**
 * @desc    Create a new support ticket
 * @route   POST /api/tickets
 * @access  Private
 */
exports.createTicket = async (req, res) => {
  try {
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'No active session found. Authentication required.'
      });
    }

    const userId = req.user.id || req.user._id;
    const { subject, description, category, priority } = req.body;

    if (!subject || !subject.trim()) {
      return res.status(400).json({
        success: false,
        message: 'A brief ticket subject is required.'
      });
    }

    if (!description || !description.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Please provide detailed description of the inquiry.'
      });
    }

    // Default parameters validation defaults
    const validCategories = ['WALLET', 'TOURNAMENT', 'TECHNICAL', 'OTHER'];
    const ticketCategory = category && validCategories.includes(category.toUpperCase()) 
      ? category.toUpperCase() 
      : 'OTHER';

    const validPriorities = ['LOW', 'MEDIUM', 'HIGH'];
    const ticketPriority = priority && validPriorities.includes(priority.toUpperCase()) 
      ? priority.toUpperCase() 
      : 'MEDIUM';

    // Build the ticket
    const ticket = new SupportTicket({
      userId,
      subject: subject.trim(),
      description: description.trim(),
      category: ticketCategory,
      priority: ticketPriority,
      status: 'PENDING'
    });

    await ticket.save();

    return res.status(201).json({
      success: true,
      message: 'Support ticket registered successfully.',
      data: ticket
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to create support ticket.',
      error: error.message
    });
  }
};

/**
 * @desc    Fetch active user's tickets
 * @route   GET /api/tickets/my-tickets
 * @access  Private
 */
exports.getMyTickets = async (req, res) => {
  try {
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'No active session found. Authentication required.'
      });
    }

    const userId = req.user.id || req.user._id;

    const tickets = await SupportTicket.find({ userId })
      .sort({ createdAt: -1 });

    return res.status(200).json({
      success: true,
      count: tickets.length,
      data: tickets
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to retrieve support ticket history.',
      error: error.message
    });
  }
};

/**
 * @desc    Get support ticket detail by ID (User owned or Admin)
 * @route   GET /api/tickets/:id
 * @access  Private
 */
exports.getTicketById = async (req, res) => {
  try {
    if (!req.user || (!req.user.id && !req.user._id)) {
      return res.status(401).json({
        success: false,
        message: 'No active session found. Authentication required.'
      });
    }

    const currentUserId = req.user.id || req.user._id;
    const ticketId = req.params.id;

    if (!mongoose.Types.ObjectId.isValid(ticketId)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid support ticket identifier format.'
      });
    }

    const ticket = await SupportTicket.findById(ticketId)
      .populate('userId', 'inGameName email role')
      .populate('resolvedBy', 'inGameName email role');

    if (!ticket) {
      return res.status(404).json({
        success: false,
        message: 'Support ticket not found.'
      });
    }

    // Authorization verification: user must own the ticket or be an admin
    const isOwner = ticket.userId && ticket.userId._id.toString() === currentUserId.toString();
    const isAdmin = req.user.role === 'admin' || req.user.isAdmin;

    if (!isOwner && !isAdmin) {
      return res.status(403).json({
        success: false,
        message: 'Access denied. You do not have permissions to view this support ticket.'
      });
    }

    return res.status(200).json({
      success: true,
      data: ticket
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to retrieve ticket details.',
      error: error.message
    });
  }
};

/**
 * @desc    Fetch all support tickets with filtering (Admin only)
 * @route   GET /api/tickets
 * @access  Private/Admin
 */
exports.getAllTickets = async (req, res) => {
  try {
    // 1. Double check admin privilege
    const isAdmin = req.user && (req.user.role === 'admin' || req.user.isAdmin);
    if (!isAdmin) {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    const { status, category, priority, page, limit } = req.query;

    const filter = {};
    if (status) filter.status = status.toUpperCase();
    if (category) filter.category = category.toUpperCase();
    if (priority) filter.priority = priority.toUpperCase();

    const pageNum = parseInt(page, 10) || 1;
    const limitNum = parseInt(limit, 10) || 20;
    const skipNum = (pageNum - 1) * limitNum;

    const tickets = await SupportTicket.find(filter)
      .populate('userId', 'inGameName email freeFireUid')
      .sort({ createdAt: -1 })
      .skip(skipNum)
      .limit(limitNum);

    const totalCount = await SupportTicket.countDocuments(filter);

    return res.status(200).json({
      success: true,
      pagination: {
        totalItems: totalCount,
        currentPage: pageNum,
        totalPages: Math.ceil(totalCount / limitNum),
        size: tickets.length
      },
      data: tickets
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to retrieve administrative tickets catalog.',
      error: error.message
    });
  }
};

/**
 * @desc    Update support ticket status and resolution notes (Admin only)
 * @route   PUT /api/tickets/:id/status
 * @access  Private/Admin
 */
exports.updateTicketStatus = async (req, res) => {
  try {
    // 1. Confirm Administrative scale
    const isAdmin = req.user && (req.user.role === 'admin' || req.user.isAdmin);
    if (!isAdmin) {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    const ticketId = req.params.id;
    const adminId = req.user.id || req.user._id;
    const { status, adminNotes } = req.body;

    if (!mongoose.Types.ObjectId.isValid(ticketId)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid support ticket identifier format.'
      });
    }

    if (!status) {
      return res.status(400).json({
        success: false,
        message: 'New status is required to update support ticket.'
      });
    }

    const validStatuses = ['PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
    if (!validStatuses.includes(status.toUpperCase())) {
      return res.status(400).json({
        success: false,
        message: `Invalid status. Supported: ${validStatuses.join(', ')}`
      });
    }

    const ticket = await SupportTicket.findById(ticketId);
    if (!ticket) {
      return res.status(404).json({
        success: false,
        message: 'Support ticket not found.'
      });
    }

    const originalStatus = ticket.status;
    const newStatus = status.toUpperCase();

    ticket.status = newStatus;
    if (adminNotes !== undefined) {
      ticket.adminNotes = adminNotes.trim();
    }

    if (newStatus === 'RESOLVED' || newStatus === 'CLOSED') {
      ticket.resolvedBy = adminId;
      ticket.resolvedAt = new Date();
    } else {
      // Re-opened or dynamic tracking
      ticket.resolvedBy = undefined;
      ticket.resolvedAt = undefined;
    }

    await ticket.save();

    // Log the update action for accountability transparency
    await adminLogService.logAction({
      adminId,
      action: 'UPDATE_TICKET',
      targetType: 'SupportTicket',
      targetId: ticketId,
      details: `Support ticket status updated from ${originalStatus} to ${newStatus}. Notes added: "${adminNotes || 'None'}"`
    });

    return res.status(200).json({
      success: true,
      message: 'Support ticket status resolved and updated successfully.',
      data: ticket
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to update support ticket status.',
      error: error.message
    });
  }
};
