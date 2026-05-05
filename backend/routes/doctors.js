const express = require('express');
const router = express.Router();
const { Pool } = require('pg');

const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

// Get all doctors
router.get('/', async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT
                id,
                name,
                specialty,
                rating,
                review_count,
                consultation_fee,
                experience_years,
                is_online,
                hospital
            FROM doctors
            ORDER BY rating DESC, review_count DESC
        `);

        res.json({
            success: true,
            count: result.rows.length,
            doctors: result.rows.map(d => ({
                id: d.id,
                name: d.name,
                specialty: d.specialty,
                rating: parseFloat(d.rating),
                reviewCount: d.review_count,
                consultationFee: d.consultation_fee,
                experienceYears: d.experience_years,
                isOnline: d.is_online,
                hospital: d.hospital
            }))
        });
    } catch (error) {
        console.error('Error fetching doctors:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch doctors' });
    }
});

// Get online doctors only
router.get('/online', async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT
                id,
                name,
                specialty,
                rating,
                review_count,
                consultation_fee,
                experience_years,
                is_online,
                hospital
            FROM doctors
            WHERE is_online = TRUE
            ORDER BY rating DESC, review_count DESC
        `);

        res.json({
            success: true,
            count: result.rows.length,
            doctors: result.rows.map(d => ({
                id: d.id,
                name: d.name,
                specialty: d.specialty,
                rating: parseFloat(d.rating),
                reviewCount: d.review_count,
                consultationFee: d.consultation_fee,
                experienceYears: d.experience_years,
                isOnline: d.is_online,
                hospital: d.hospital
            }))
        });
    } catch (error) {
        console.error('Error fetching online doctors:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch online doctors' });
    }
});

// Get doctor by ID
router.get('/:id', async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT
                id,
                name,
                specialty,
                rating,
                review_count,
                consultation_fee,
                experience_years,
                is_online,
                hospital,
                bio,
                license_number
            FROM doctors
            WHERE id = $1
        `, [req.params.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'Doctor not found' });
        }

        const doctor = result.rows[0];
        res.json({
            success: true,
            doctor: {
                id: doctor.id,
                name: doctor.name,
                specialty: doctor.specialty,
                rating: parseFloat(doctor.rating),
                reviewCount: doctor.review_count,
                consultationFee: doctor.consultation_fee,
                experienceYears: doctor.experience_years,
                isOnline: doctor.is_online,
                hospital: doctor.hospital,
                bio: doctor.bio,
                licenseNumber: doctor.license_number
            }
        });
    } catch (error) {
        console.error('Error fetching doctor:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch doctor' });
    }
});

module.exports = router;

