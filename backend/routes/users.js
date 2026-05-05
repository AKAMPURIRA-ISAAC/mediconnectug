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

// Get user profile
router.get('/profile', authenticateToken, async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT id, name, email, phone, blood_type, date_of_birth, address, allergies FROM users WHERE id = $1',
            [req.user.userId]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'User not found' });
        }

        const user = result.rows[0];
        res.json({
            success: true,
            user: {
                id: user.id,
                name: user.name,
                email: user.email,
                phone: user.phone,
                bloodType: user.blood_type,
                dateOfBirth: user.date_of_birth,
                address: user.address,
                allergies: user.allergies
            }
        });
    } catch (error) {
        console.error('Error fetching profile:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch profile' });
    }
});

// Update user profile
router.put('/profile', authenticateToken, async (req, res) => {
    try {
        const { name, phone, blood_type, date_of_birth, address, allergies } = req.body;

        const result = await pool.query(`
            UPDATE users
            SET name = COALESCE($1, name),
                phone = COALESCE($2, phone),
                blood_type = COALESCE($3, blood_type),
                date_of_birth = COALESCE($4, date_of_birth),
                address = COALESCE($5, address),
                allergies = COALESCE($6, allergies)
            WHERE id = $7
            RETURNING id, name, email, phone, blood_type, date_of_birth, address, allergies
        `, [name, phone, blood_type, date_of_birth, address, allergies, req.user.userId]);

        const user = result.rows[0];
        res.json({
            success: true,
            user: {
                id: user.id,
                name: user.name,
                email: user.email,
                phone: user.phone,
                bloodType: user.blood_type,
                dateOfBirth: user.date_of_birth,
                address: user.address,
                allergies: user.allergies
            }
        });
    } catch (error) {
        console.error('Error updating profile:', error);
        res.status(500).json({ success: false, error: 'Failed to update profile' });
    }
});

module.exports = router;

