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

// Get patient's appointments
router.get('/', authenticateToken, async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT
                a.id,
                a.appointment_date,
                a.appointment_time,
                a.type,
                a.status,
                a.fee,
                a.notes,
                d.name as doctor_name,
                d.specialty,
                a.patient_id,
                a.doctor_id
            FROM appointments a
            LEFT JOIN doctors d ON a.doctor_id = d.id
            WHERE a.user_id = $1
            ORDER BY a.appointment_date DESC, a.appointment_time DESC
        `, [req.user.userId]);

        const appointments = result.rows.map(row => ({
            id: row.id,
            doctorName: row.doctor_name || 'Unknown Doctor',
            specialty: row.specialty || 'General',
            appointmentDate: row.appointment_date,
            appointmentTime: row.appointment_time,
            type: row.type,
            status: row.status,
            fee: row.fee,
            notes: row.notes,
            patientId: row.patient_id,
            doctorId: row.doctor_id
        }));

        res.json({ success: true, appointments });
    } catch (error) {
        console.error('Error fetching appointments:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch appointments' });
    }
});

// Get doctor's appointments
router.get('/doctor', authenticateToken, async (req, res) => {
    try {
        // Find doctor record linked to this user
        const doctorResult = await pool.query(
            'SELECT id FROM doctors WHERE user_id = $1',
            [req.user.userId]
        );

        if (doctorResult.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'Doctor profile not found' });
        }

        const doctorId = doctorResult.rows[0].id;

        const result = await pool.query(`
            SELECT
                a.id,
                a.appointment_date,
                a.appointment_time,
                a.type,
                a.status,
                a.fee,
                a.notes,
                u.name as patient_name,
                a.user_id as patient_id,
                a.doctor_id,
                d.specialty
            FROM appointments a
            LEFT JOIN users u ON a.user_id = u.id
            LEFT JOIN doctors d ON a.doctor_id = d.id
            WHERE a.doctor_id = $1
            ORDER BY a.appointment_date DESC, a.appointment_time DESC
        `, [doctorId]);

        const appointments = result.rows.map(row => ({
            id: row.id,
            doctorName: row.patient_name || 'Unknown Patient', // In doctor view, this shows patient name
            specialty: row.specialty || 'General',
            appointmentDate: row.appointment_date,
            appointmentTime: row.appointment_time,
            type: row.type,
            status: row.status,
            fee: row.fee,
            notes: row.notes,
            patientId: row.patient_id,
            doctorId: row.doctor_id
        }));

        res.json({ success: true, appointments });
    } catch (error) {
        console.error('Error fetching doctor appointments:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch appointments' });
    }
});

// Book new appointment (patient-initiated, starts as "pending")
router.post('/', authenticateToken, async (req, res) => {
    try {
        const { doctor_id, doctor_name, date, time, type, notes } = req.body;

        const result = await pool.query(`
            INSERT INTO appointments
            (user_id, doctor_id, appointment_date, appointment_time, type, status, notes, fee)
            VALUES ($1, $2, $3, $4, $5, 'pending', $6, (SELECT consultation_fee FROM doctors WHERE id = $2))
            RETURNING *
        `, [req.user.userId, doctor_id, date, time, type, notes || null]);

        const appointment = result.rows[0];

        res.json({
            success: true,
            message: 'Appointment request sent! Waiting for doctor confirmation.',
            appointment: {
                id: appointment.id,
                doctorName: doctor_name,
                specialty: 'General',
                appointmentDate: appointment.appointment_date,
                appointmentTime: appointment.appointment_time,
                type: appointment.type,
                status: appointment.status,
                fee: appointment.fee,
                notes: appointment.notes
            }
        });
    } catch (error) {
        console.error('Error booking appointment:', error);
        res.status(500).json({ success: false, error: 'Failed to book appointment' });
    }
});

// Confirm appointment (doctor only)
router.put('/:id/confirm', authenticateToken, async (req, res) => {
    try {
        const appointmentId = req.params.id;

        // Verify the doctor owns this appointment
        const doctorResult = await pool.query(
            'SELECT id FROM doctors WHERE user_id = $1',
            [req.user.userId]
        );

        if (doctorResult.rows.length === 0) {
            return res.status(403).json({ success: false, error: 'Not authorized' });
        }

        const doctorId = doctorResult.rows[0].id;

        const result = await pool.query(`
            UPDATE appointments
            SET status = 'upcoming'
            WHERE id = $1 AND doctor_id = $2 AND status = 'pending'
            RETURNING *
        `, [appointmentId, doctorId]);

        if (result.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'Appointment not found or already confirmed' });
        }

        res.json({
            success: true,
            message: 'Appointment confirmed successfully',
            appointment: result.rows[0]
        });
    } catch (error) {
        console.error('Error confirming appointment:', error);
        res.status(500).json({ success: false, error: 'Failed to confirm appointment' });
    }
});

// Reject appointment (doctor only)
router.put('/:id/reject', authenticateToken, async (req, res) => {
    try {
        const appointmentId = req.params.id;
        const { reason } = req.body;

        // Verify the doctor owns this appointment
        const doctorResult = await pool.query(
            'SELECT id FROM doctors WHERE user_id = $1',
            [req.user.userId]
        );

        if (doctorResult.rows.length === 0) {
            return res.status(403).json({ success: false, error: 'Not authorized' });
        }

        const doctorId = doctorResult.rows[0].id;

        const result = await pool.query(`
            UPDATE appointments
            SET status = 'cancelled', notes = COALESCE(notes || E'\\n', '') || $3
            WHERE id = $1 AND doctor_id = $2 AND status = 'pending'
            RETURNING *
        `, [appointmentId, doctorId, `Rejected by doctor: ${reason || 'Schedule conflict'}`]);

        if (result.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'Appointment not found' });
        }

        res.json({
            success: true,
            message: 'Appointment rejected',
            appointment: result.rows[0]
        });
    } catch (error) {
        console.error('Error rejecting appointment:', error);
        res.status(500).json({ success: false, error: 'Failed to reject appointment' });
    }
});

// Reschedule appointment
router.put('/:id/reschedule', authenticateToken, async (req, res) => {
    try {
        const { date, time, type, notes } = req.body;
        const appointmentId = req.params.id;

        const result = await pool.query(`
            UPDATE appointments
            SET appointment_date = $1, appointment_time = $2, type = $3, notes = $4
            WHERE id = $5 AND user_id = $6 AND status = 'upcoming'
            RETURNING *
        `, [date, time, type, notes, appointmentId, req.user.userId]);

        if (result.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'Appointment not found or cannot be rescheduled' });
        }

        res.json({ success: true, message: 'Appointment rescheduled', appointment: result.rows[0] });
    } catch (error) {
        console.error('Error rescheduling appointment:', error);
        res.status(500).json({ success: false, error: 'Failed to reschedule appointment' });
    }
});

// Cancel appointment
router.delete('/:id', authenticateToken, async (req, res) => {
    try {
        const appointmentId = req.params.id;

        const result = await pool.query(`
            UPDATE appointments
            SET status = 'cancelled'
            WHERE id = $1 AND user_id = $2
            RETURNING *
        `, [appointmentId, req.user.userId]);

        if (result.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'Appointment not found' });
        }

        res.json({ success: true, message: 'Appointment cancelled successfully' });
    } catch (error) {
        console.error('Error cancelling appointment:', error);
        res.status(500).json({ success: false, error: 'Failed to cancel appointment' });
    }
});

module.exports = router;

