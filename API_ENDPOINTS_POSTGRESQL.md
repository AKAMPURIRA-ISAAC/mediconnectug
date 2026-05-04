# HealthBridge - PostgreSQL Backend API Endpoints
**Generated:** May 4, 2026  
**Backend Base URL:** `http://localhost:3001/`  
**Android Emulator URL:** `http://10.0.2.2:3001/`

---

## 🔐 Authentication
All authenticated endpoints require a Bearer token in the Authorization header:
```
Authorization: Bearer {token}
```

---

## ✅ **ALREADY IMPLEMENTED & TESTED**

### 1. **Authentication Endpoints**

#### POST `/api/auth/login`
**Purpose:** User login  
**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
**Response:**
```json
{
  "success": true,
  "token": "jwt_token_here",
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "user@example.com",
    "phone": "+256123456789"
  }
}
```

#### POST `/api/auth/register`
**Purpose:** User registration  
**Request Body:**
```json
{
  "name": "John Doe",
  "email": "user@example.com",
  "phone": "+256123456789",
  "password": "password123"
}
```
**Response:** Same as login

---

### 2. **Doctors Endpoints**

#### GET `/api/doctors`
**Purpose:** Get list of all doctors  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "count": 6,
  "doctors": [
    {
      "id": 1,
      "name": "Dr. Sarah Johnson",
      "specialty": "Cardiologist",
      "rating": 4.8,
      "review_count": 127,
      "consultation_fee": 50000,
      "experience_years": 12
    }
  ]
}
```

---

### 3. **Appointments Endpoints**

#### GET `/api/appointments`
**Purpose:** Get user's appointments  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "appointments": [
    {
      "id": 1,
      "doctor_name": "Dr. Sarah Johnson",
      "specialty": "Cardiologist",
      "appointment_date": "2026-05-10",
      "appointment_time": "10:00 AM",
      "type": "in_person",
      "status": "upcoming",
      "fee": 50000,
      "notes": "Follow-up checkup"
    }
  ]
}
```

#### POST `/api/appointments`
**Purpose:** Book a new appointment  
**Auth:** Required  
**Request Body:**
```json
{
  "doctor_id": "1",
  "doctor_name": "Dr. Sarah Johnson",
  "date": "2026-05-10",
  "time": "10:00 AM",
  "type": "in_person",
  "notes": "Annual checkup"
}
```
**Response:**
```json
{
  "success": true,
  "appointment": { /* appointment object */ },
  "message": "Appointment booked successfully"
}
```

#### DELETE `/api/appointments/{id}`
**Purpose:** Cancel an appointment  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "message": "Appointment cancelled successfully"
}
```

---

### 4. **User Profile Endpoints**

#### GET `/api/profile`
**Purpose:** Get user profile  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "user@example.com",
    "phone": "+256123456789",
    "blood_type": "O+",
    "date_of_birth": "1990-01-15",
    "address": "Kampala, Uganda",
    "allergies": "Penicillin"
  }
}
```

#### PUT `/api/profile`
**Purpose:** Update user profile  
**Auth:** Required  
**Request Body:**
```json
{
  "name": "John Doe",
  "phone": "+256123456789",
  "blood_type": "O+",
  "address": "Kampala, Uganda"
}
```
**Response:** Same as GET profile

---

## 🆕 **NEW ENDPOINTS TO IMPLEMENT**

### 5. **Chat / AI Assistant Endpoints**

#### POST `/api/chat/message`
**Purpose:** Send message to AI assistant & get response  
**Auth:** Required  
**Request Body:**
```json
{
  "message": "I have a headache and fever",
  "conversation_id": "conv_123abc",
  "symptoms": ["headache", "fever"]
}
```
**Response:**
```json
{
  "success": true,
  "conversation_id": "conv_123abc",
  "ai_response": "I understand you're experiencing headache and fever. Can you tell me when these symptoms started?",
  "suggested_actions": [
    "Rest and stay hydrated",
    "Monitor temperature",
    "Connect with a doctor"
  ]
}
```

#### GET `/api/chat/history`
**Purpose:** Get chat conversation history  
**Auth:** Required  
**Query Params:** `conversation_id` (optional)  
**Response:**
```json
{
  "success": true,
  "messages": [
    {
      "id": 1,
      "conversation_id": "conv_123abc",
      "message": "I have a headache",
      "is_user": true,
      "timestamp": "2026-05-04T10:30:00Z",
      "ai_context": null
    },
    {
      "id": 2,
      "conversation_id": "conv_123abc",
      "message": "I understand. When did it start?",
      "is_user": false,
      "timestamp": "2026-05-04T10:30:05Z",
      "ai_context": "symptom_collection"
    }
  ]
}
```

#### DELETE `/api/chat/history/{conversation_id}`
**Purpose:** Clear specific conversation history  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "message": "Chat history cleared"
}
```

---

### 6. **Medical Records Endpoints**

#### GET `/api/medical-records`
**Purpose:** Get user's medical records  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "count": 3,
  "records": [
    {
      "id": 1,
      "title": "Blood Test Results",
      "type": "lab_test",
      "date": "2026-04-20",
      "doctor_name": "Dr. Sarah Johnson",
      "description": "Complete Blood Count (CBC)",
      "file_url": "https://storage.example.com/records/blood_test_123.pdf",
      "created_at": "2026-04-20T14:30:00Z"
    },
    {
      "id": 2,
      "title": "Chest X-Ray",
      "type": "x_ray",
      "date": "2026-03-15",
      "doctor_name": "Dr. Michael Chen",
      "description": "Chest X-Ray - Clear",
      "file_url": "https://storage.example.com/records/xray_456.jpg",
      "created_at": "2026-03-15T11:00:00Z"
    }
  ]
}
```

#### POST `/api/medical-records`
**Purpose:** Upload a new medical record  
**Auth:** Required  
**Request Body:**
```json
{
  "title": "MRI Results",
  "type": "report",
  "date": "2026-05-04",
  "description": "Brain MRI",
  "file_url": "https://storage.example.com/records/mri_789.pdf"
}
```
**Response:**
```json
{
  "success": true,
  "records": [{ /* new record */ }],
  "message": "Medical record uploaded successfully"
}
```

#### DELETE `/api/medical-records/{id}`
**Purpose:** Delete a medical record  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "message": "Medical record deleted"
}
```

---

### 7. **Prescriptions Endpoints**

#### GET `/api/prescriptions`
**Purpose:** Get user's prescriptions  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "prescriptions": [
    {
      "id": 1,
      "medication_name": "Amoxicillin",
      "dosage": "500mg",
      "frequency": "3 times daily",
      "duration": "7 days",
      "doctor_name": "Dr. Sarah Johnson",
      "issued_date": "2026-05-01",
      "status": "active",
      "refills_remaining": 2,
      "instructions": "Take with food"
    },
    {
      "id": 2,
      "medication_name": "Ibuprofen",
      "dosage": "400mg",
      "frequency": "As needed",
      "duration": "14 days",
      "doctor_name": "Dr. Michael Chen",
      "issued_date": "2026-04-28",
      "status": "active",
      "refills_remaining": 1,
      "instructions": "For pain relief"
    }
  ]
}
```

#### POST `/api/prescriptions/{id}/refill`
**Purpose:** Request prescription refill  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "message": "Refill request sent to doctor",
  "prescriptions": [{ /* updated prescription */ }]
}
```

---

### 8. **Notifications Endpoints**

#### GET `/api/notifications`
**Purpose:** Get user's notifications  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "unread_count": 2,
  "notifications": [
    {
      "id": 1,
      "title": "Appointment Reminder",
      "message": "Your appointment with Dr. Sarah Johnson is tomorrow at 10:00 AM",
      "type": "appointment",
      "is_read": false,
      "created_at": "2026-05-03T09:00:00Z",
      "action_url": "/appointments/123"
    },
    {
      "id": 2,
      "title": "New Message",
      "message": "Dr. Michael Chen sent you a message",
      "type": "message",
      "is_read": false,
      "created_at": "2026-05-03T14:30:00Z",
      "action_url": "/chat/456"
    },
    {
      "id": 3,
      "title": "Prescription Ready",
      "message": "Your prescription is ready for pickup",
      "type": "prescription",
      "is_read": true,
      "created_at": "2026-05-02T16:00:00Z",
      "action_url": "/prescriptions/789"
    }
  ]
}
```

#### PUT `/api/notifications/{id}/read`
**Purpose:** Mark notification as read  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "message": "Notification marked as read"
}
```

#### PUT `/api/notifications/read-all`
**Purpose:** Mark all notifications as read  
**Auth:** Required  
**Response:**
```json
{
  "success": true,
  "message": "All notifications marked as read"
}
```

---

## 📊 **PostgreSQL Database Schema Recommendations**

### Tables to Create:

1. **users** - User accounts
2. **doctors** - Doctor profiles
3. **appointments** - Appointment bookings
4. **chat_conversations** - Chat session tracking
5. **chat_messages** - Individual messages
6. **medical_records** - User medical documents
7. **prescriptions** - Active prescriptions
8. **notifications** - User notifications

---

## 🔥 **Android App Features Utilizing Database:**

✅ **Authentication** - Login/Register with JWT tokens  
✅ **Doctor Discovery** - Browse and search doctors  
✅ **Appointment Booking** - Full CRUD operations  
✅ **User Profile** - Complete profile management  
✅ **AI Chat Assistant** - Intelligent symptom analysis with history  
✅ **Medical Records** - Upload, view, delete records  
✅ **Prescriptions** - View active prescriptions & request refills  
✅ **Notifications** - Real-time alerts and reminders  

---

## 🚀 **Offline Capability:**

The Android app includes **offline fallback** for all features:
- If backend is down, app uses local sample data
- Authentication credentials cached in SharedPreferences
- Works fully without network (limited data)
- Seamless transition when backend comes online

---

## 📝 **Notes for Backend Developers:**

1. All endpoints should accept **JSON** and return **JSON**
2. Use **Bearer token authentication** for protected routes
3. Return consistent error format:
   ```json
   {
     "success": false,
     "error": "Error message here"
   }
   ```
4. User ID should be extracted from JWT token
5. Implement **pagination** for large datasets (appointments, records, messages)
6. Add **rate limiting** to prevent abuse
7. Validate all inputs on backend
8. Use HTTPS in production (not http)

---

## 🎯 **Priority Implementation Order:**

1. ✅ Auth (login/register) - **DONE**
2. ✅ Doctors - **DONE**
3. ✅ Appointments - **DONE**
4. ✅ Profile - **DONE**
5. 🆕 Chat/AI Assistant - **NEW** (High Priority)
6. 🆕 Notifications - **NEW** (High Priority)
7. 🆕 Prescriptions - **NEW** (Medium Priority)
8. 🆕 Medical Records - **NEW** (Medium Priority)

---

**End of API Documentation**

