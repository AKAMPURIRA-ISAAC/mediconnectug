const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Pool } = require('pg');

const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';
const JWT_EXPIRY = '180d'; // 180 days as per your SESSION_TIMEOUT_UPDATE

// Patient/User Login
router.post('/login', async (req, res) => {
    try {
        const { email, password, user_type } = req.body;

        if (!email || !password) {
            return res.status(400).json({ success: false, error: 'Email and password required' });
        }

        if (user_type === 'doctor') {
            // Doctor login
            const result = await pool.query(
                'SELECT * FROM doctors WHERE email = $1',
                [email]
            );

            if (result.rows.length === 0) {
                return res.status(401).json({ success: false, error: 'Invalid credentials' });
            }

            const doctor = result.rows[0];
            const validPassword = await bcrypt.compare(password, doctor.password_hash);

            if (!validPassword) {
                return res.status(401).json({ success: false, error: 'Invalid credentials' });
            }

            const token = jwt.sign(
                { userId: doctor.user_id || doctor.id, userType: 'doctor', doctorId: doctor.id },
                JWT_SECRET,
                { expiresIn: JWT_EXPIRY }
            );

            // Update last seen
            await pool.query('UPDATE doctors SET last_seen = NOW(), is_online = TRUE WHERE id = $1', [doctor.id]);

            res.json({
                success: true,
                token,
                user: {
                    id: doctor.user_id || doctor.id,
                    name: doctor.name,
                    email: doctor.email,
                    phone: doctor.phone,
                    userType: 'doctor',
                    doctorId: doctor.id
                }
            });
        } else {
            // Patient login
            const result = await pool.query(
                'SELECT * FROM users WHERE email = $1',
                [email]
            );

            if (result.rows.length === 0) {
                return res.status(401).json({ success: false, error: 'Invalid credentials' });
            }

            const user = result.rows[0];
            const validPassword = await bcrypt.compare(password, user.password_hash);

            if (!validPassword) {
                return res.status(401).json({ success: false, error: 'Invalid credentials' });
            }

            const token = jwt.sign(
                { userId: user.id, userType: 'patient' },
                JWT_SECRET,
                { expiresIn: JWT_EXPIRY }
            );

            res.json({
                success: true,
                token,
                user: {
                    id: user.id,
                    name: user.name,
                    email: user.email,
                    phone: user.phone,
                    userType: 'patient'
                }
            });
        }
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ success: false, error: 'Login failed' });
    }
});

// Patient Registration
router.post('/register', async (req, res) => {
    try {
        const { name, email, phone, password } = req.body;

        if (!name || !email || !password) {
            return res.status(400).json({ success: false, error: 'Name, email and password required' });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        const result = await pool.query(`
            INSERT INTO users (name, email, phone, password_hash)
            VALUES ($1, $2, $3, $4)
            RETURNING id, name, email, phone
        `, [name, email, phone, hashedPassword]);

        const user = result.rows[0];

        const token = jwt.sign(
            { userId: user.id, userType: 'patient' },
            JWT_SECRET,
            { expiresIn: JWT_EXPIRY }
        );

        res.json({
            success: true,
            token,
            user: {
                id: user.id,
                name: user.name,
                email: user.email,
                phone: user.phone,
                userType: 'patient'
            },
            message: 'Registration successful'
        });
    } catch (error) {
        if (error.code === '23505') { // Unique violation
            res.status(409).json({ success: false, error: 'Email already exists' });
        } else {
            console.error('Registration error:', error);
            res.status(500).json({ success: false, error: 'Registration failed' });
        }
    }
});

// Doctor Registration
router.post('/register-doctor', async (req, res) => {
    try {
        const { name, email, phone, password, specialty, hospital, license_number } = req.body;

        if (!name || !email || !password || !specialty || !license_number) {
            return res.status(400).json({ success: false, error: 'All fields required' });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        // Create user account first
        const userResult = await pool.query(`
            INSERT INTO users (name, email, phone, password_hash)
            VALUES ($1, $2, $3, $4)
            RETURNING id
        `, [name, email, phone, hashedPassword]);

        const userId = userResult.rows[0].id;

        // Create doctor profile
        const doctorResult = await pool.query(`
            INSERT INTO doctors (user_id, name, email, phone, password_hash, specialty, hospital, license_number, is_online)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, FALSE)
            RETURNING *
        `, [userId, name, email, phone, hashedPassword, specialty, hospital, license_number]);

        const doctor = doctorResult.rows[0];

        const token = jwt.sign(
            { userId: userId, userType: 'doctor', doctorId: doctor.id },
            JWT_SECRET,
            { expiresIn: JWT_EXPIRY }
        );

        res.json({
            success: true,
            token,
            user: {
                id: userId,
                name: doctor.name,
                email: doctor.email,
                phone: doctor.phone,
                userType: 'doctor',
                doctorId: doctor.id
            },
            message: 'Doctor registration successful'
        });
    } catch (error) {
        if (error.code === '23505') {
            res.status(409).json({ success: false, error: 'Email already exists' });
        } else {
            console.error('Doctor registration error:', error);
            res.status(500).json({ success: false, error: 'Registration failed' });
        }
    }
});

// Logout
router.post('/logout', async (req, res) => {
    try {
        // Mark doctor as offline if doctor logout
        const authHeader = req.headers['authorization'];
        if (authHeader) {
            const token = authHeader.split(' ')[1];
            const decoded = jwt.verify(token, JWT_SECRET);

            if (decoded.userType === 'doctor' && decoded.doctorId) {
                await pool.query('UPDATE doctors SET is_online = FALSE WHERE id = $1', [decoded.doctorId]);
            }
        }

        res.json({ success: true, message: 'Logged out successfully' });
    } catch {
        res.json({ success: true, message: 'Logged out' });
    }
});

module.exports = router;

