require('dotenv').config();
const express = require('express');
const cors    = require('cors');
const { Pool } = require('pg');
const bcrypt  = require('bcryptjs');
const jwt     = require('jsonwebtoken');

const app  = express();
const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.DATABASE_URL?.includes('supabase.com')
        ? { rejectUnauthorized: false }
        : false
});
const JWT_SECRET = process.env.JWT_SECRET || 'mediconnectug_secret_key_change_in_prod';

app.use(cors());
app.use(express.json());

// ── Auth middleware ──────────────────────────────────────────────────────────
function auth(req, res, next) {
  const header = req.headers['authorization'];
  if (!header) return res.status(401).json({ success: false, error: 'No token provided' });
  try {
    req.user = jwt.verify(header.replace('Bearer ', ''), JWT_SECRET);
    next();
  } catch {
    res.status(401).json({ success: false, error: 'Invalid or expired token' });
  }
}

// ── Health check ─────────────────────────────────────────────────────────────
app.get('/', (_, res) => res.json({
    status: 'MediConnectUG API is live',
    version: '1.0.0',
    db: process.env.DATABASE_URL ? 'configured' : 'NOT SET - add DATABASE_URL env var'
}));

// ════════════════════════════════════════════════════════════════════════════
//  AUTH
// ════════════════════════════════════════════════════════════════════════════

app.post('/api/auth/register', async (req, res) => {
  const { name, email, phone, password } = req.body;
  if (!name || !email || !password)
    return res.status(400).json({ success: false, error: 'name, email and password are required' });
  try {
    const exists = await pool.query('SELECT id FROM users WHERE email=$1', [email]);
    if (exists.rows.length) return res.status(409).json({ success: false, error: 'Email already registered' });
    const hash = await bcrypt.hash(password, 10);
    const r = await pool.query(
      'INSERT INTO users (name,email,phone,password_hash,user_type) VALUES($1,$2,$3,$4,$5) RETURNING id,name,email,phone,user_type',
      [name, email, phone || null, hash, 'patient']
    );
    const user = r.rows[0];
    const token = jwt.sign({ id: user.id, email: user.email, user_type: 'patient' }, JWT_SECRET, { expiresIn: '30d' });
    res.json({ success: true, token, user });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/api/auth/register-doctor', async (req, res) => {
  const { name, email, phone, password, specialty, hospital, license_number } = req.body;
  if (!name || !email || !password || !specialty || !hospital || !license_number)
    return res.status(400).json({ success: false, error: 'All fields are required' });
  try {
    // Check if email already exists
    const exists = await pool.query('SELECT id FROM users WHERE email=$1', [email]);
    if (exists.rows.length) return res.status(409).json({ success: false, error: 'Email already registered' });

    const hash = await bcrypt.hash(password, 10);

    // Create user account with doctor type
    const userResult = await pool.query(
      'INSERT INTO users (name,email,phone,password_hash,user_type) VALUES($1,$2,$3,$4,$5) RETURNING id,name,email,phone,user_type',
      [name, email, phone || null, hash, 'doctor']
    );
    const user = userResult.rows[0];

    // Create doctor profile
    const doctorResult = await pool.query(
      `INSERT INTO doctors (name, specialty, rating, review_count, consultation_fee, experience_years, hospital, license_number, user_id, is_online)
       VALUES($1, $2, 5.0, 0, 50000, 5, $3, $4, $5, false) RETURNING id`,
      [name, specialty, hospital, license_number, user.id]
    );
    const doctorId = doctorResult.rows[0].id;

    const token = jwt.sign({ id: user.id, email: user.email, user_type: 'doctor', doctor_id: doctorId }, JWT_SECRET, { expiresIn: '30d' });

    res.json({
      success: true,
      token,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        user_type: 'doctor',
        doctor_id: doctorId
      }
    });
  } catch (e) {
    console.error('Doctor registration error:', e);
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/api/auth/login', async (req, res) => {
  const { email, password, user_type } = req.body;
  if (!email || !password)
    return res.status(400).json({ success: false, error: 'email and password are required' });
  try {
    const r = await pool.query('SELECT * FROM users WHERE email=$1', [email]);
    if (!r.rows.length) return res.status(401).json({ success: false, error: 'Invalid credentials' });
    const user = r.rows[0];

    // Verify user type matches if specified
    if (user_type && user.user_type !== user_type) {
      return res.status(401).json({ success: false, error: `This account is not a ${user_type} account` });
    }

    const valid = await bcrypt.compare(password, user.password_hash);
    if (!valid) return res.status(401).json({ success: false, error: 'Invalid credentials' });

    let doctorId = null;
    if (user.user_type === 'doctor') {
      const doctorResult = await pool.query('SELECT id FROM doctors WHERE user_id=$1', [user.id]);
      if (doctorResult.rows.length) {
        doctorId = doctorResult.rows[0].id;
        // Update doctor online status
        await pool.query('UPDATE doctors SET is_online=true WHERE id=$1', [doctorId]);
      }
    }

    const token = jwt.sign({
      id: user.id,
      email: user.email,
      user_type: user.user_type || 'patient',
      doctor_id: doctorId
    }, JWT_SECRET, { expiresIn: '30d' });

    res.json({
      success: true,
      token,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        user_type: user.user_type || 'patient',
        doctor_id: doctorId
      }
    });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/api/auth/logout', auth, async (req, res) => {
  try {
    // If user is a doctor, mark them as offline
    if (req.user.doctor_id) {
      await pool.query('UPDATE doctors SET is_online=false WHERE id=$1', [req.user.doctor_id]);
    }
    res.json({ success: true, message: 'Logged out successfully' });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  PROFILE
// ════════════════════════════════════════════════════════════════════════════

app.get('/api/profile', auth, async (req, res) => {
  try {
    const r = await pool.query(
      'SELECT id,name,email,phone,blood_type,date_of_birth,address,allergies FROM users WHERE id=$1',
      [req.user.id]
    );
    if (!r.rows.length) return res.status(404).json({ success: false, error: 'User not found' });
    res.json({ success: true, user: r.rows[0] });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.put('/api/profile', auth, async (req, res) => {
  const { name, phone, blood_type, date_of_birth, address, allergies } = req.body;
  try {
    const r = await pool.query(
      `UPDATE users SET
        name=COALESCE($1,name), phone=COALESCE($2,phone),
        blood_type=COALESCE($3,blood_type), date_of_birth=COALESCE($4,date_of_birth),
        address=COALESCE($5,address), allergies=COALESCE($6,allergies)
       WHERE id=$7
       RETURNING id,name,email,phone,blood_type,date_of_birth,address,allergies`,
      [name, phone, blood_type, date_of_birth, address, allergies, req.user.id]
    );
    res.json({ success: true, user: r.rows[0] });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  DOCTORS
// ════════════════════════════════════════════════════════════════════════════

app.get('/api/doctors', async (req, res) => {
  try {
    const r = await pool.query(
      'SELECT id,name,specialty,rating,review_count,consultation_fee,experience_years,is_online FROM doctors ORDER BY is_online DESC, rating DESC'
    );
    res.json({ success: true, count: r.rowCount, doctors: r.rows });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  APPOINTMENTS
// ════════════════════════════════════════════════════════════════════════════

app.get('/api/appointments', auth, async (req, res) => {
  try {
    const r = await pool.query(
      `SELECT a.id, d.name as doctor_name, d.specialty,
              a.appointment_date, a.appointment_time, a.type, a.status, a.fee, a.notes
       FROM appointments a
       JOIN doctors d ON d.id = a.doctor_id
       WHERE a.user_id=$1
       ORDER BY a.appointment_date DESC`,
      [req.user.id]
    );
    res.json({ success: true, appointments: r.rows });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/api/appointments', auth, async (req, res) => {
  const { doctor_id, doctor_name, date, time, type, notes } = req.body;
  try {
    // Fetch doctor details to get fee and specialty
    const doc = await pool.query('SELECT consultation_fee, specialty FROM doctors WHERE id=$1', [doctor_id]);
    if (!doc.rows.length) {
      return res.status(404).json({ success: false, error: 'Doctor not found' });
    }
    const fee = doc.rows[0]?.consultation_fee || 0;
    const specialty = doc.rows[0]?.specialty || 'General';

    const r = await pool.query(
      `INSERT INTO appointments (user_id,doctor_id,appointment_date,appointment_time,type,status,fee,notes)
       VALUES($1,$2,$3,$4,$5,'upcoming',$6,$7)
       RETURNING id`,
      [req.user.id, doctor_id, date, time, type || 'in_person', fee, notes || null]
    );

    // Return fields matching Android app's Appointment model exactly
    res.json({
      success: true,
      message: 'Appointment booked successfully',
      appointment: {
        id: r.rows[0].id,
        doctor_name,
        specialty,
        appointment_date: date,
        appointment_time: time,
        type: type || 'in_person',
        status: 'upcoming',
        fee,
        notes: notes || null
      }
    });
  } catch (e) {
    console.error('Booking error:', e);
    res.status(500).json({ success: false, error: e.message });
  }
});

app.put('/api/appointments/:id/reschedule', auth, async (req, res) => {
  const { date, time, type, notes } = req.body;
  try {
    await pool.query(
      `UPDATE appointments SET appointment_date=$1,appointment_time=$2,type=COALESCE($3,type),notes=COALESCE($4,notes)
       WHERE id=$5 AND user_id=$6`,
      [date, time, type, notes, req.params.id, req.user.id]
    );

    // Fetch updated appointment with doctor details
    const r = await pool.query(
      `SELECT a.id, d.name as doctor_name, d.specialty,
              a.appointment_date, a.appointment_time, a.type, a.status, a.fee, a.notes
       FROM appointments a
       JOIN doctors d ON d.id = a.doctor_id
       WHERE a.id=$1 AND a.user_id=$2`,
      [req.params.id, req.user.id]
    );

    res.json({
      success: true,
      message: 'Appointment rescheduled',
      appointment: r.rows[0]
    });
  } catch (e) {
    console.error('Reschedule error:', e);
    res.status(500).json({ success: false, error: e.message });
  }
});

app.delete('/api/appointments/:id', auth, async (req, res) => {
  try {
    await pool.query(
      "UPDATE appointments SET status='cancelled' WHERE id=$1 AND user_id=$2",
      [req.params.id, req.user.id]
    );
    res.json({ success: true, message: 'Appointment cancelled' });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  MEDICAL RECORDS
// ════════════════════════════════════════════════════════════════════════════

app.get('/api/medical-records', auth, async (req, res) => {
  try {
    const r = await pool.query(
      'SELECT id,title,type,date,doctor_name,description,file_url,created_at FROM medical_records WHERE user_id=$1 ORDER BY date DESC',
      [req.user.id]
    );
    res.json({ success: true, count: r.rowCount, records: r.rows });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/api/medical-records', auth, async (req, res) => {
  const { title, type, date, doctor_name, description, file_url } = req.body;
  try {
    const r = await pool.query(
      'INSERT INTO medical_records (user_id,title,type,date,doctor_name,description,file_url) VALUES($1,$2,$3,$4,$5,$6,$7) RETURNING *',
      [req.user.id, title, type || 'report', date, doctor_name, description, file_url]
    );
    res.json({ success: true, records: [r.rows[0]] });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.delete('/api/medical-records/:id', auth, async (req, res) => {
  try {
    await pool.query('DELETE FROM medical_records WHERE id=$1 AND user_id=$2', [req.params.id, req.user.id]);
    res.json({ success: true, message: 'Record deleted' });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  PRESCRIPTIONS
// ════════════════════════════════════════════════════════════════════════════

app.get('/api/prescriptions', auth, async (req, res) => {
  try {
    const r = await pool.query(
      'SELECT * FROM prescriptions WHERE user_id=$1 ORDER BY issued_date DESC',
      [req.user.id]
    );
    res.json({ success: true, prescriptions: r.rows });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/api/prescriptions/:id/refill', auth, async (req, res) => {
  try {
    await pool.query(
      'UPDATE prescriptions SET refills_remaining = GREATEST(refills_remaining - 1, 0) WHERE id=$1 AND user_id=$2',
      [req.params.id, req.user.id]
    );
    res.json({ success: true, message: 'Refill requested' });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  NOTIFICATIONS
// ════════════════════════════════════════════════════════════════════════════

app.get('/api/notifications', auth, async (req, res) => {
  try {
    const r = await pool.query(
      'SELECT * FROM notifications WHERE user_id=$1 ORDER BY created_at DESC',
      [req.user.id]
    );
    const unread = r.rows.filter(n => !n.is_read).length;
    res.json({ success: true, notifications: r.rows, unread_count: unread });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.put('/api/notifications/:id/read', auth, async (req, res) => {
  try {
    await pool.query('UPDATE notifications SET is_read=true WHERE id=$1 AND user_id=$2', [req.params.id, req.user.id]);
    res.json({ success: true, message: 'Marked as read' });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.put('/api/notifications/read-all', auth, async (req, res) => {
  try {
    await pool.query('UPDATE notifications SET is_read=true WHERE user_id=$1', [req.user.id]);
    res.json({ success: true, message: 'All marked as read' });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  CHAT / AI (stores history; AI responses served from Android app logic)
// ════════════════════════════════════════════════════════════════════════════

app.post('/api/chat/message', auth, async (req, res) => {
  const { message, conversation_id } = req.body;
  const convId = conversation_id || require('crypto').randomUUID();
  try {
    await pool.query(
      'INSERT INTO chat_messages (user_id,conversation_id,message,is_user) VALUES($1,$2,$3,true)',
      [req.user.id, convId, message]
    );
    // Return minimal response — Android handles AI processing locally
    res.json({ success: true, conversation_id: convId, ai_response: null, suggested_actions: [] });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.get('/api/chat/history', auth, async (req, res) => {
  const convId = req.query.conversation_id;
  try {
    const r = await pool.query(
      `SELECT * FROM chat_messages WHERE user_id=$1 ${convId ? 'AND conversation_id=$2' : ''} ORDER BY id DESC LIMIT 50`,
      convId ? [req.user.id, convId] : [req.user.id]
    );
    res.json({ success: true, messages: r.rows });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.delete('/api/chat/history/:conversation_id', auth, async (req, res) => {
  try {
    await pool.query('DELETE FROM chat_messages WHERE conversation_id=$1 AND user_id=$2', [req.params.conversation_id, req.user.id]);
    res.json({ success: true, message: 'Chat history cleared' });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  PATIENT-DOCTOR CHAT
// ════════════════════════════════════════════════════════════════════════════

// Create new chat session (AI escalation to doctor)
app.post('/api/chat-sessions', auth, async (req, res) => {
  const { doctor_id, urgency_level, chief_complaint, symptoms, severity_score, duration_text, ai_assessment } = req.body;
  try {
    // Start a chat session
    const sessionResult = await pool.query(
      `INSERT INTO chat_sessions (patient_id, doctor_id, session_type, urgency_level, chief_complaint)
       VALUES($1, $2, 'doctor_chat', $3, $4) RETURNING id`,
      [req.user.id, doctor_id, urgency_level || 'normal', chief_complaint || 'General consultation']
    );
    const sessionId = sessionResult.rows[0].id;

    // Create escalation record
    await pool.query(
      `INSERT INTO ai_escalations (user_id, doctor_id, reason, urgency, symptoms, severity_score, duration_text, ai_assessment)
       VALUES($1, $2, $3, $4, $5, $6, $7, $8)`,
      [req.user.id, doctor_id, chief_complaint || 'AI referral', urgency_level || 'moderate',
       symptoms || [], severity_score, duration_text, ai_assessment]
    );

    // Get doctor details
    const docResult = await pool.query(
      'SELECT id, name, specialty, is_online FROM doctors WHERE id=$1',
      [doctor_id]
    );

    res.json({
      success: true,
      session_id: sessionId,
      doctor: docResult.rows[0],
      message: 'Chat session created'
    });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// Get patient's active chat sessions
app.get('/api/chat-sessions', auth, async (req, res) => {
  try {
    const isDoctor = req.user.user_type === 'doctor';
    let r;

    if (isDoctor) {
      // Get sessions for this doctor
      r = await pool.query(
        `SELECT cs.id, cs.patient_id, u.name as patient_name, cs.doctor_id, d.name as doctor_name,
           cs.chief_complaint, cs.urgency, cs.status, cs.created_at, cs.last_message_at
         FROM chat_sessions cs
         JOIN users u ON cs.patient_id = u.id
         LEFT JOIN doctors d ON cs.doctor_id = d.id
         WHERE cs.doctor_id=$1
         ORDER BY cs.last_message_at DESC NULLS LAST, cs.created_at DESC`,
        [req.user.doctor_id]
      );
    } else {
      // Get sessions for this patient
      r = await pool.query(
        `SELECT cs.id, cs.patient_id, u.name as patient_name, cs.doctor_id, d.name as doctor_name,
           cs.chief_complaint, cs.urgency, cs.status, cs.created_at, cs.last_message_at
         FROM chat_sessions cs
         JOIN users u ON cs.patient_id = u.id
         LEFT JOIN doctors d ON cs.doctor_id = d.id
         WHERE cs.patient_id=$1
         ORDER BY cs.last_message_at DESC NULLS LAST, cs.created_at DESC`,
        [req.user.id]
      );
    }

    res.json({ success: true, sessions: r.rows });
  } catch (e) {
    console.error('Get chat sessions error:', e);
    res.status(500).json({ success: false, error: e.message });
  }
});

// Get messages in a chat session
app.get('/api/chat-sessions/:session_id/messages', auth, async (req, res) => {
  try {
    const r = await pool.query(
      `SELECT dm.id, dm.session_id, dm.sender_id, dm.sender_type, dm.message, dm.is_read, dm.created_at,
         CASE
           WHEN dm.sender_type = 'patient' THEN u.name
           WHEN dm.sender_type = 'doctor' THEN d.name
         END as sender_name
       FROM direct_messages dm
       LEFT JOIN users u ON dm.sender_id = u.id AND dm.sender_type = 'patient'
       LEFT JOIN doctors d ON dm.sender_id = d.id AND dm.sender_type = 'doctor'
       WHERE dm.session_id=$1
       ORDER BY dm.created_at ASC`,
      [req.params.session_id]
    );

    const messages = r.rows.map(msg => ({
      id: msg.id,
      session_id: msg.session_id,
      sender_id: msg.sender_id,
      sender_name: msg.sender_name,
      sender_type: msg.sender_type,
      message: msg.message,
      timestamp: msg.created_at,
      is_read: msg.is_read
    }));

    res.json({ success: true, messages });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// Send message in chat session
app.post('/api/chat-sessions/:session_id/messages', auth, async (req, res) => {
  const { message_text, attachment_url } = req.body;
  const sender_type = req.user.user_type === 'doctor' ? 'doctor' : 'patient';

  try {
    const r = await pool.query(
      `INSERT INTO direct_messages (session_id, sender_id, sender_type, message_text, attachment_url)
       VALUES($1, $2, $3, $4, $5) RETURNING *`,
      [req.params.session_id, req.user.id, sender_type, message_text, attachment_url || null]
    );
    res.json({ success: true, message: r.rows[0] });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// Mark messages as read
app.put('/api/chat-sessions/:session_id/read', auth, async (req, res) => {
  try {
    await pool.query(
      `UPDATE direct_messages SET is_read=true
       WHERE session_id=$1 AND sender_id!=$2`,
      [req.params.session_id, req.user.id]
    );
    res.json({ success: true, message: 'Messages marked as read' });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// Get online doctors (for AI routing)
app.get('/api/doctors/online', async (req, res) => {
  try {
    const r = await pool.query(
      `SELECT id, name, specialty, is_online, rating
       FROM doctors
       WHERE is_online=true
       ORDER BY rating DESC, experience_years DESC
       LIMIT 10`
    );
    res.json({ success: true, doctors: r.rows });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// Get pending escalations (for doctor dashboard)
app.get('/api/escalations/pending', auth, async (req, res) => {
  try {
    if (req.user.user_type !== 'doctor') {
      return res.status(403).json({ success: false, error: 'Access denied' });
    }
    const r = await pool.query(
      `SELECT e.*, u.name as patient_name, u.phone as patient_phone
       FROM ai_escalations e
       JOIN users u ON e.user_id = u.id
       WHERE e.doctor_id=$1 AND e.status='pending'
       ORDER BY
         CASE e.urgency
           WHEN 'emergency' THEN 1
           WHEN 'urgent' THEN 2
           WHEN 'moderate' THEN 3
           ELSE 4
         END,
         e.created_at ASC`,
      [req.user.id]
    );
    res.json({ success: true, escalations: r.rows });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// ── Start server ─────────────────────────────────────────────────────────────
const PORT = process.env.PORT || 3001;
app.listen(PORT, () => console.log(`\n🚀 MediConnectUG API running on port ${PORT}\n`));

