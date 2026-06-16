const mongoose = require('mongoose');
const Faq = require('../models/Faq');

/**
 * @desc    Get all FAQs (users fetch only active ones sorted; admins can fetch all including inactive)
 * @route   GET /api/faqs
 * @access  Public / Private (supports both)
 */
exports.getFaqs = async (req, res) => {
  try {
    const { category, search, all } = req.query;

    // 1. Base query filter
    const query = {};

    // By default, general users can only see active FAQs.
    // If 'all=true' is passed, confirm the user has admin role prior to displaying draft/inactive FAQs.
    if (all === 'true') {
      if (!req.user || req.user.role !== 'admin') {
        return res.status(403).json({
          success: false,
          message: 'Access denied. Administrative privileges are required to view inactive or hidden FAQs.'
        });
      }
    } else {
      query.isActive = true;
    }

    // 2. Filter by category if requested
    if (category) {
      query.category = { $regex: new RegExp('^' + category.trim() + '$', 'i') };
    }

    // 3. Optional term search over questions/answers
    if (search) {
      const searchTerm = search.trim();
      query.$or = [
        { question: { $regex: searchTerm, $options: 'i' } },
        { answer: { $regex: searchTerm, $options: 'i' } }
      ];
    }

    // 4. Retrieve FAQs sorted by category in ascending alphabetical, and order index
    const faqs = await Faq.find(query)
      .populate('createdBy', 'inGameName email')
      .sort({ category: 1, order: 1, createdAt: -1 });

    return res.status(200).json({
      success: true,
      count: faqs.length,
      message: 'Common questions fetched successfully.',
      data: faqs
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to retrieve FAQs.',
      error: error.message
    });
  }
};

/**
 * @desc    Get FAQ by ID
 * @route   GET /api/faqs/:id
 * @access  Public
 */
exports.getFaqById = async (req, res) => {
  try {
    const faq = await Faq.findById(req.params.id).populate('createdBy', 'inGameName email');
    
    if (!faq) {
      return res.status(404).json({
        success: false,
        message: 'The requested FAQ does not exist.'
      });
    }

    // Protect hidden inactive FAQs from general users
    if (!faq.isActive && (!req.user || req.user.role !== 'admin')) {
      return res.status(403).json({
        success: false,
        message: 'This FAQ is currently offline or drafted'
      });
    }

    return res.status(200).json({
      success: true,
      data: faq
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to retrieve the FAQ item.',
      error: error.message
    });
  }
};

/**
 * @desc    Create a new FAQ (Admin only)
 * @route   POST /api/faqs
 * @access  Private/Admin
 */
exports.createFaq = async (req, res) => {
  try {
    // 1. Validate Administrative rights
    if (!req.user || req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    const { question, answer, category, order, isActive } = req.body;

    // 2. Validate essential fields
    if (!question || !question.trim()) {
      return res.status(400).json({
        success: false,
        message: 'A valid question text is required.'
      });
    }

    if (!answer || !answer.trim()) {
      return res.status(400).json({
        success: false,
        message: 'A valid answer text is required.'
      });
    }

    // 3. Construct and save the FAQ item
    const faq = new Faq({
      question: question.trim(),
      answer: answer.trim(),
      category: category ? category.trim() : 'General',
      order: parseInt(order) || 0,
      isActive: isActive !== undefined ? isActive : true,
      createdBy: req.user.id || req.user._id
    });

    await faq.save();

    return res.status(201).json({
      success: true,
      message: 'New FAQ successfully registered.',
      data: faq
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to register the FAQ item.',
      error: error.message
    });
  }
};

/**
 * @desc    Update an existing FAQ (Admin only)
 * @route   PUT /api/faqs/:id
 * @access  Private/Admin
 */
exports.updateFaq = async (req, res) => {
  try {
    // 1. Validate Administrative rights
    if (!req.user || req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    const { question, answer, category, order, isActive } = req.body;

    // 2. Locate target document
    const faq = await Faq.findById(req.params.id);
    if (!faq) {
      return res.status(404).json({
        success: false,
        message: 'The requested FAQ to update was not found.'
      });
    }

    // 3. Apply updates dynamically if specified
    if (question !== undefined) {
      if (!question.trim()) {
        return res.status(400).json({
          success: false,
          message: 'Question cannot be empty.'
        });
      }
      faq.question = question.trim();
    }

    if (answer !== undefined) {
      if (!answer.trim()) {
        return res.status(400).json({
          success: false,
          message: 'Answer cannot be empty.'
        });
      }
      faq.answer = answer.trim();
    }

    if (category !== undefined) {
      faq.category = category.trim();
    }

    if (order !== undefined) {
      faq.order = parseInt(order) || 0;
    }

    if (isActive !== undefined) {
      faq.isActive = isActive;
    }

    await faq.save();

    return res.status(200).json({
      success: true,
      message: 'FAQ item successfully updated.',
      data: faq
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to update the FAQ item.',
      error: error.message
    });
  }
};

/**
 * @desc    Delete an FAQ (Admin only)
 * @route   DELETE /api/faqs/:id
 * @access  Private/Admin
 */
exports.deleteFaq = async (req, res) => {
  try {
    // 1. Validate Administrative rights
    if (!req.user || req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Access denied. Administrative privileges are required.'
      });
    }

    // 2. Locate and remove FAQ
    const faq = await Faq.findByIdAndDelete(req.params.id);
    if (!faq) {
      return res.status(404).json({
        success: false,
        message: 'The requested FAQ to delete does not exist.'
      });
    }

    return res.status(200).json({
      success: true,
      message: 'FAQ item successfully deleted.'
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server failed to delete the FAQ item.',
      error: error.message
    });
  }
};
