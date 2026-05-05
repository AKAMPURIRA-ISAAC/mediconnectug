const express = require('express');
const router = express.Router();
const { Pool } = require('pg');

const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

// Middleware to verify JWT token
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ success: false, error: 'No token provided' });
    }

    const jwt = require('jsonwebtoken');
    jwt.verify(token, process.env.JWT_SECRET || 'your-secret-key', (err, user) => {
        if (err) {
            return res.status(403).json({ success: false, error: 'Invalid token' });
        }
        req.user = user;
        next();
    });
};

// Create new chat session (patient-initiated)
router.post('/', authenticateToken, async (req, res) => {
    try {
        const { doctor_id, chief_complaint, symptoms, urgency } = req.body;

        const result = await pool.query(`
            INSERT INTO chat_sessions
            (patient_id, doctor_id, chief_complaint, urgency, status)
            VALUES ($1, $2, $3, $4, 'waiting')
            RETURNING *
        `, [req.user.userId, doctor_id || null, chief_complaint || 'General consultation', urgency || 'MODERATE']);

        const session = result.rows[0];

        // Get patient and doctor names
        const patientResult = await pool.query('SELECT name FROM users WHERE id = $1', [req.user.userId]);
        const patientName = patientResult.rows[0]?.name || 'Patient';

        let doctorName = null;
        if (doctor_id) {
            const doctorResult = await pool.query('SELECT name FROM doctors WHERE id = $1', [doctor_id]);
            doctorName = doctorResult.rows[0]?.name || null;
        }

        res.json({
            success: true,
            session: {
                id: session.id,
                patientId: session.patient_id,
                patientName: patientName,
                doctorId: session.doctor_id,
                doctorName: doctorName,
                chiefComplaint: session.chief_complaint,
                urgency: session.urgency,
                status: session.status,
                createdAt: session.created_at,
                lastMessageAt: session.last_message_at
            }
        });
    } catch (error) {
        console.error('Error creating chat session:', error);
        res.status(500).json({ success: false, error: 'Failed to create chat session' });
    }
});

// Create chat session from appointment (doctor or patient initiated)
router.post('/from-appointment/:appointmentId', authenticateToken, async (req, res) => {
    try {
        const appointmentId = req.params.appointmentId;

        // Get appointment details
        const appointmentResult = await pool.query(`
            SELECT a.*, u.name as patient_name, d.name as doctor_name
            FROM appointments a
            LEFT JOIN users u ON a.user_id = u.id
            LEFT JOIN doctors d ON a.doctor_id = d.id
            WHERE a.id = $1
        `, [appointmentId]);

        if (appointmentResult.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'Appointment not found' });
        }

        const appointment = appointmentResult.rows[0];

        // Check if chat session already exists for this appointment
        const existingSession = await pool.query(`
            SELECT cs.*, u.name as patient_name, d.name as doctor_name
            FROM chat_sessions cs
            LEFT JOIN users u ON cs.patient_id = u.id
            LEFT JOIN doctors d ON cs.doctor_id = d.id
            WHERE cs.patient_id = $1 AND cs.doctor_id = $2 AND cs.status = 'active'
            ORDER BY cs.created_at DESC
            LIMIT 1
        `, [appointment.user_id, appointment.doctor_id]);

        if (existingSession.rows.length > 0) {
            const session = existingSession.rows[0];
            return res.json({
                success: true,
                session: {
                    id: session.id,
                    patientId: session.patient_id,
                    patientName: session.patient_name,
                    doctorId: session.doctor_id,
                    doctorName: session.doctor_name,
                    chiefComplaint: session.chief_complaint,
                    urgency: session.urgency,
                    status: session.status,
                    createdAt: session.created_at,
                    lastMessageAt: session.last_message_at
                },
                message: 'Using existing chat session'
            });
        }

        // Create new session
        const result = await pool.query(`
            INSERT INTO chat_sessions
            (patient_id, doctor_id, chief_complaint, urgency, status)
            VALUES ($1, $2, $3, 'MODERATE', 'active')
            RETURNING *
        `, [appointment.user_id, appointment.doctor_id, appointment.notes || `Appointment consultation on ${appointment.appointment_date}`]);

        const session = result.rows[0];

        res.json({
            success: true,
            session: {
                id: session.id,
                patientId: appointment.user_id,
                patientName: appointment.patient_name,
                doctorId: appointment.doctor_id,
                doctorName: appointment.doctor_name,
                chiefComplaint: session.chief_complaint,
                urgency: session.urgency,
                status: session.status,
                createdAt: session.created_at,
                lastMessageAt: session.last_message_at
            }
        });
    } catch (error) {
        console.error('Error creating chat session from appointment:', error);
        res.status(500).json({ success: false, error: 'Failed to create chat session' });
    }
});

// Get all chat sessions (for current user - patient or doctor)
router.get('/', authenticateToken, async (req, res) => {
    try {
        let query, params;

        if (req.user.userType === 'doctor') {
            // Get doctor's sessions
            const doctorResult = await pool.query('SELECT id FROM doctors WHERE user_id = $1', [req.user.userId]);
            if (doctorResult.rows.length === 0) {
                return res.json({ success: true, sessions: [] });
            }
            const doctorId = doctorResult.rows[0].id;

            query = `
                SELECT cs.*, u.name as patient_name, d.name as doctor_name
                FROM chat_sessions cs
                LEFT JOIN users u ON cs.patient_id = u.id
                LEFT JOIN doctors d ON cs.doctor_id = d.id
                WHERE cs.doctor_id = $1 AND cs.status IN ('waiting', 'active')
                ORDER BY cs.last_message_at DESC NULLS LAST, cs.created_at DESC
            `;
            params = [doctorId];
        } else {
            // Get patient's sessions
            query = `
                SELECT cs.*, u.name as patient_name, d.name as doctor_name
                FROM chat_sessions cs
                LEFT JOIN users u ON cs.patient_id = u.id
                LEFT JOIN doctors d ON cs.doctor_id = d.id
                WHERE cs.patient_id = $1 AND cs.status IN ('waiting', 'active')
                ORDER BY cs.last_message_at DESC NULLS LAST, cs.created_at DESC
            `;
            params = [req.user.userId];
        }

        const result = await pool.query(query, params);

        const sessions = result.rows.map(s => ({
            id: s.id,
            patientId: s.patient_id,
            patientName: s.patient_name || 'Patient',
            doctorId: s.doctor_id,
            doctorName: s.doctor_name || 'Doctor',
            chiefComplaint: s.chief_complaint,
            urgency: s.urgency,
            status: s.status,
            createdAt: s.created_at,
            lastMessageAt: s.last_message_at
        }));

        res.json({ success: true, sessions });
    } catch (error) {
        console.error('Error fetching chat sessions:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch chat sessions' });
    }
});

// Get messages for a specific session
router.get('/:session_id/messages', authenticateToken, async (req, res) => {
    try {
        const sessionId = req.params.session_id;

        const result = await pool.query(`
            SELECT dm.*, u.name as sender_name
            FROM direct_messages dm
            LEFT JOIN users u ON dm.sender_id = u.id
            WHERE dm.session_id = $1
            ORDER BY dm.created_at ASC
        `, [sessionId]);

        const messages = result.rows.map(m => ({
            id: m.id,
            sessionId: m.session_id,
            senderId: m.sender_id,
            senderName: m.sender_name || 'Unknown',
            senderType: m.sender_type,
            message: m.message,
            timestamp: m.created_at,
            isRead: m.is_read
        }));

        res.json({ success: true, messages });
    } catch (error) {
        console.error('Error fetching messages:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch messages' });
    }
});

// Send message in a session
router.post('/:session_id/messages', authenticateToken, async (req, res) => {
    try {
        const sessionId = req.params.session_id;
        const { message_text } = req.body;

        if (!message_text || message_text.trim() === '') {
            return res.status(400).json({ success: false, error: 'Message cannot be empty' });
        }

        // Determine sender type
        const senderType = req.user.userType === 'doctor' ? 'doctor' : 'patient';

        // Get sender name
        let senderName = 'Unknown';
        if (senderType === 'doctor') {
            const doctorResult = await pool.query('SELECT name FROM doctors WHERE user_id = $1', [req.user.userId]);
            senderName = doctorResult.rows[0]?.name || 'Doctor';
        } else {
            const userResult = await pool.query('SELECT name FROM users WHERE id = $1', [req.user.userId]);
            senderName = userResult.rows[0]?.name || 'Patient';
        }

        const result = await pool.query(`
            INSERT INTO direct_messages
            (session_id, sender_id, sender_type, message, is_read)
            VALUES ($1, $2, $3, $4, FALSE)
            RETURNING *
        `, [sessionId, req.user.userId, senderType, message_text.trim()]);

        const message = result.rows[0];

        // Update session's last_message_at
        await pool.query(
            'UPDATE chat_sessions SET last_message_at = NOW(), status = $2 WHERE id = $1',
            [sessionId, 'active']
        );

        res.json({
            success: true,
            message: {
                id: message.id,
                sessionId: message.session_id,
                senderId: message.sender_id,
                senderName: senderName,
                senderType: message.sender_type,
                message: message.message,
                timestamp: message.created_at,
                isRead: message.is_read
            }
        });
    } catch (error) {
        console.error('Error sending message:', error);
        res.status(500).json({ success: false, error: 'Failed to send message' });
    }
});

// Mark messages as read
router.put('/:session_id/read', authenticateToken, async (req, res) => {
    try {
        const sessionId = req.params.session_id;

        await pool.query(`
            UPDATE direct_messages
            SET is_read = TRUE
            WHERE session_id = $1 AND sender_id != $2
        `, [sessionId, req.user.userId]);

        res.json({ success: true, message: 'Messages marked as read' });
    } catch (error) {
        console.error('Error marking messages as read:', error);
        res.status(500).json({ success: false, error: 'Failed to mark messages as read' });
    }
});

// Complete a chat session
router.put('/:session_id/complete', authenticateToken, async (req, res) => {
    try {
        const sessionId = req.params.session_id;

        await pool.query(
            'UPDATE chat_sessions SET status = $1 WHERE id = $2',
            ['completed', sessionId]
        );

        res.json({ success: true, message: 'Chat session completed' });
    } catch (error) {
        console.error('Error completing session:', error);
        res.status(500).json({ success: false, error: 'Failed to complete session' });
    }
});

module.exports = router;

