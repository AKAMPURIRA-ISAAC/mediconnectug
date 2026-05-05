# 🏥 Doctor-Patient Communication Flow - Complete Guide

## Overview
This guide explains how the fixed system enables proper two-way communication between doctors and patients.

---

## 📱 Flow 1: Patient Sees Doctor and Books Appointment

```
PATIENT SIDE                           BACKEND                          DOCTOR SIDE
───────────────────────────────────────────────────────────────────────────────────

Browse Doctors
    ↓
Select Doctor
    ↓
Book Appointment ──["doctor_id: 5, date, time"]──→  /api/appointments
                                           ↓
                                 Save to DB:
                                 INSERT INTO appointments
                                 (user_id, doctor_id, ...)
```

**Result:** Appointment appears in doctor's profile

---

## 📋 Flow 2: Doctor Views Their Appointments (NEW)

```
DOCTOR LOGIN
    ↓
JWT Token includes:
  - id: 123 (user ID)
  - doctor_id: 5
  - user_type: 'doctor'
    ↓
Open DoctorHomeActivity
    ↓
Click Appointments Tab
    ↓
Call getDoctorAppointments()
    ↓
     
Backend Query:
    SELECT * FROM appointments
    WHERE doctor_id = 5  ← Uses doctor_id from token
    ↓
Return all patient appointments
    ↓
Display:
  • Patient names
  • Appointment dates/times
  • Patient locations
  • Status (upcoming/past/cancelled)
```

**Result:** ✅ Doctor sees all their patient appointments

---

## 💬 Flow 3: Patient Escalates to Doctor (via AI)

```
PATIENT SIDE                           AI SYSTEM                        DATABASE
────────────────────────────────────────────────────────────────────────────────

Patient describes symptoms
in chat
    ↓
AI analyzes & determines
needs doctor
    ↓
AI calls:
  createChatSession(...) 
  doctor_id=5
  chief_complaint="Fever"
    ↓                    Backend creates record:
                         INSERT INTO chat_sessions
                         (patient_id, doctor_id, 
                          chief_complaint, urgency)
                         VALUES (123, 5, "Fever", "MODERATE")
                         ↓
                         session_id = 42
                         
Return session_id: 42 ←────────────────────────────────
    ↓
Open PatientChatActivity(42)
    ↓
Patient types message: "I have a high fever since yesterday"
    ↓
Press Send ──["message_text": "I have..."]──→ 
                /api/chat-sessions/42/messages
                
                           ↓
                         Store message:
                         INSERT INTO direct_messages
                         (session_id: 42,
                          sender_id: 123,
                          sender_type: 'patient',
                          message: "I have...")
                         
                         UPDATE chat_sessions
                         SET last_message_at = NOW()
                         WHERE id = 42
                         ↓
                         Return success
                    ←─────────────
    ↓
Message appears on screen
```

**Result:** ✅ Chat session created, patient message saved

---

## 👨‍⚕️ Flow 4: Doctor Receives and Replies (NEW)

```
DOCTOR SIDE
────────────────────────────────────────────────

Open DoctorHomeActivity
    ↓
Click Chats Tab
    ↓
Call getChatSessions()
    ↓
    
Backend Query:
  SELECT * FROM chat_sessions
  WHERE doctor_id = 5
  ORDER BY last_message_at DESC
  ↓
  Return active chats with patient names
    ↓
Chat list shows:
  • Patient: "John Doe"
  • Chief complaint: "Fever"
  • Priority: 🟡 MODERATE
  • Last message: "2 min ago"
    ↓
Click on chat
    ↓
Call getChatSessionMessages(42)
    ↓
    
Backend Query:
  SELECT * FROM direct_messages
  WHERE session_id = 42
  ↓
  Return all messages (patient + doctor)
    ↓
Display conversation:
  
  John: "I have a high fever"
  John:     "since yesterday"
  
        ← Input field
    ↓
Doctor types: "Take paracetamol 2x daily"
    ↓
Press Send ──["message_text": "Take..."]──→
            /api/chat-sessions/42/messages
            
                       ↓
                     Store message:
                     INSERT INTO direct_messages
                     (session_id: 42,
                      sender_id: 123,
                      sender_type: 'doctor',
                      message: "Take...")
                     
                     UPDATE chat_sessions
                     SET last_message_at = NOW()
                     ↓
                    ← Return success
    ↓
Message appears on screen
```

**Result:** ✅ Doctor reply saved and visible to both

---

## ↔️ Flow 5: Ongoing Two-Way Conversation

```
PATIENT'S APP                        DATABASE                        DOCTOR'S APP
─────────────────────────────────────────────────────────────────────────────────

Polls every 3 seconds:
getChatSessionMessages(42)
    ↓                          
                         SELECT * FROM direct_messages
                         WHERE session_id=42
                                 ↓
                         (includes doctor's message now)
                                 ↓
Doctor's message appears     ←──────────────────
  "Take paracetamol 2x daily"

Patient reads & types:       
"OK, how many days?"
    ↓
Send message ──────────���───→ INSERT INTO direct_messages
                                 ↓
                            UPDATE last_message_at
                                 ↓
                                            Doctor's app polls:
                                            getChatSessionMessages(42)
                                                 ↓
                                            (includes patient's new message)
                                                 ↓
                                            Message appears
                                            "OK, how many days?"
                
                                            Doctor replies:
                                            "For 3-5 days"
                                                 ↓
                                            Send ──→ Insert message
                                                     Update timestamp
                                                         ↓
Patient's app polls ←───────���─ (picks up doctor's "3-5 days")
Message appears

CYCLE REPEATS...
```

**Result:** ✅ Real-time bi-directional conversation

---

## 🔑 Key Data Elements

### Chat Session Record (database: `chat_sessions`)
```json
{
  "id": 42,
  "patient_id": 123,           ← John Doe (user ID)
  "doctor_id": 5,              ← Dr. Sarah (doctor ID)
  "chief_complaint": "Fever",
  "urgency": "MODERATE",       ← UPPERCASE!
  "status": "active",
  "created_at": "2026-05-05T10:30:00Z",
  "last_message_at": "2026-05-05T10:45:23Z"
}
```

### Message Record (database: `direct_messages`)
```json
{
  "id": 8,
  "session_id": 42,            ← Links to chat session
  "sender_id": 123,            ← John Doe's user ID
  "sender_type": "patient",    ← Determines who sent it
  "message": "I have a fever",
  "is_read": false,
  "created_at": "2026-05-05T10:45:23Z"
}
```

### API Response Format (Doctor's Chat List)
```json
{
  "success": true,
  "sessions": [
    {
      "id": 42,
      "patient_id": 123,
      "patient_name": "John Doe",       ← Shows patient name!
      "doctor_id": 5,
      "doctor_name": "Dr. Sarah",
      "chief_complaint": "Fever",
      "urgency": "MODERATE",
      "status": "active",
      "created_at": "2026-05-05T10:30:00Z",
      "last_message_at": "2026-05-05T10:45:23Z"
    }
  ]
}
```

---

## 🔄 Message Flow Diagram

```
PATIENT SENDS MESSAGE
    ↓
Android: SendMessageRequest(message_text="Hi doc")
    ↓
iOS/Web Client
    ↓
POST /api/chat-sessions/42/messages
    ↓
Backend Middleware:
  - Extract user from JWT
  - Determine sender_type from token (patient/doctor)
    ↓
INSERT INTO direct_messages
  (session_id=42, sender_id=123, sender_type='patient', message='Hi doc')
    ↓
UPDATE chat_sessions SET last_message_at=NOW()
    ↓
HTTP 200 Response: { success: true, message: {...} }
    ↓
DOCTOR RECEIVES MESSAGE
    ↓
Doctor's app polls getChatSessionMessages(42)
    ↓
Backend queries: SELECT * FROM direct_messages WHERE session_id=42
    ↓
Returns message with:
  - sender_id: 123
  - sender_type: 'patient'
  - sender_name: 'John Doe'
  - message: 'Hi doc'
    ↓
Android displays message on LEFT side (patient side)
    ↓
DOCTOR REPLIES
    ↓
Similar process...
    ↓
Android displays on RIGHT side (doctor side)
```

---

## 🚀 Critical Implementation Details

### Field Name Conversions (NOW FIXED!)

| What Android Sends | Database Column | What Backend Stores |
|---|---|---|
| `message_text` | `message` | ✅ Correct |
| `urgency` | `urgency` | ✅ Correct |
| N/A | `last_message_at` | ✅ Updated on every message |
| `sender_type` | `sender_type` | ✅ 'patient' or 'doctor' |

### Authentication Flow

```
Doctor Login:
  ↓
POST /api/auth/login
  Body: { email, password, user_type: 'doctor' }
  ↓
Backend verifies and returns:
  JWT Token with:
    - id: 123 (user ID)
    - email: "doc@hospital.com"
    - user_type: 'doctor'
    - doctor_id: 5  ← CRITICAL!
  ↓
Android stores token
  ↓
Every API call includes:
  Header: Authorization: Bearer <token>
  ↓
Backend uses token to:
  - Verify user is a doctor
  - Get doctor_id for queries
  - Set sender_type to 'doctor'
```

---

## ✅ What Works Now

| Feature | Status | Details |
|---------|---------|---------|
| Doctor registers | ✅ | Hospital & license stored properly |
| Doctor logs in | ✅ | doctor_id included in JWT |
| Doctor views appointments | ✅ | NEW: getDoctorAppointments() endpoint |
| Doctor sees chats | ✅ | getChatSessions filters by doctor_id |
| Doctor receives messages | ✅ | Fixed: message_text → message |
| Doctor sends messages | ✅ | Fixed: sender_type correctly set |
| Patient receives doctor's reply | ✅ | Polling refreshes message list |
| Chat history preserved | ✅ | last_message_at timestamp updates |

---

## 🎯 Testing Scenarios

### Scenario 1: New Doctor Registration & Setup
```
1. Doctor registers with all fields
2. Check database: user_id, hospital, license_number saved
3. Login as doctor
4. Verify JWT has doctor_id
5. Go to Appointments tab - should be empty or show existing
```

### Scenario 2: Existing Patient & New Doctor Chat
```
1. Patient searches doctors, finds one
2. Patient starts AI chat
3. AI escalates, creates chat session
4. Patient sends message
5. Doctor logs in
6. Doctor opens Chats tab - sees patient chat
7. Doctor reads conversation
8. Doctor sends response
9. Patient's app polls and receives response
10. Verify both see same conversation
```

### Scenario 3: Appointment Management
```
1. Patient books appointment with doctor
2. Doctor logs in, goes to Appointments
3. Doctor sees patient appointment
4. Doctor can cancel if needed
5. Patient sees cancellation
```

---

## 🐛 Debugging Checklist

If something doesn't work:

- [ ] Database migration ran successfully
- [ ] JWT token includes `doctor_id` after doctor login
- [ ] Backend logs show correct `sender_type` in messages
- [ ] `last_message_at` is updating in database
- [ ] Message field is `message` not `message_text` in database
- [ ] `urgency` field is stored (not `urgency_level`)
- [ ] Doctor can retrieve chats with `getChatSessions()`
- [ ] Patient can retrieve messages with `getChatSessionMessages()`

---

**Status:** ✅ Complete Doctor-Patient System Documented & Implemented

For implementation details, see: `DOCTOR_CHAT_FIXES.md`

