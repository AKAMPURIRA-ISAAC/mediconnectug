# Quick Troubleshooting Guide - Patient Messages Not Sending

## 🔴 Problem: Message Send Button Doesn't Work

### 1. First, Check the Logs
```powershell
adb logcat PatientChat:* -v brief
```

### 2. Look for These Patterns

#### ✅ Success Pattern:
```
D/PatientChat: Sending message to session 5: Hello
D/PatientChat: Message response: success=true, error=null
D/PatientChat: Message sent successfully: 42
```
→ **Your fix is working!** ✅

#### ❌ Failure Pattern #1: Invalid Session
```
D/PatientChat: Sending message to session -1: Hello
```
→ **Problem**: Chat wasn't properly initialized  
→ **Fix**: Restart the app and click "Message" again

#### ❌ Failure Pattern #2: Network Error
```
E/PatientChat: Exception sending message: Connection refused
E/PatientChat: java.io.IOException: Unable to resolve host
```
→ **Problem**: Backend not running or unreachable  
→ **Fix**: Start backend: `node server.js` in backend folder

#### ❌ Failure Pattern #3: Invalid Session (404)
```
D/PatientChat: Message response: success=false, error=Chat session not found
```
→ **Problem**: Session was deleted or invalid  
→ **Fix**: Close chat and open it again

---

## 🔴 Problem: Chat Opens But Messages Don't Show

### Causes & Solutions

| Cause | How to Check | How to Fix |
|-------|--------------|-----------|
| Chat session not created | Look in DoctorProfile logs for `Creating chat session` | Check backend logs for database errors |
| Backend not running | `curl localhost:3001/` should respond | Start backend: `node server.js` |
| Database connection failed | Check `.env` DATABASE_URL | Verify Supabase credentials in `.env` |
| No internet on device | Check device WiFi/data | Enable WiFi or data on device |

---

## 🔴 Problem: Chat Opens, Button Works, But Logcat Shows Errors

### Step 1: Identify the Exact Error
```powershell
adb logcat *:E | findstr -i "message\|chat\|exception"
```

### Step 2: Match the Error

**Error**: `Cannot deserialize JSON`
→ Response format mismatch
→ Rebuild with latest code

**Error**: `Unauthorized or Invalid Token`
→ Need to login again
→ Exit app completely, login fresh

**Error**: `Database connection timeout`
→ Backend can't reach database
→ Check DATABASE_URL in `.env`

**Error**: `null pointer exception`
→ Null field in response
→ Check backend response format

---

## 🟡 Problem: Messages Sent But Not Received by Doctor

### Check Message Status

**Was it inserted to database?**
```sql
SELECT * FROM direct_messages WHERE session_id = :session_id ORDER BY created_at DESC;
```

**Is session active?**
```sql
SELECT id, status, last_message_at FROM chat_sessions WHERE id = :session_id;
```

**Is doctor in right session?**
```sql
SELECT * FROM chat_sessions WHERE doctor_id = :doctor_id AND status = 'active';
```

---

## 🟢 Message Sending Works - How to Verify

### Quick Test
1. Login as **Patient**
2. Find and click a doctor
3. Click "Message" button
4. Type: `test message hello`
5. Tap send button
6. Check:
   - [ ] Message appears in chat
   - [ ] No error toast
   - [ ] Logcat shows success message
   - [ ] Doctor sees message in real-time

### Database Check
```sql
-- Messages sent by patient should appear here:
SELECT sender_id, sender_type, message, created_at 
FROM direct_messages 
ORDER BY created_at DESC 
LIMIT 10;

-- Should show:
-- sender_id: [patient user id]
-- sender_type: patient
-- message: test message hello
-- created_at: [recent timestamp]
```

---

## 📋 Configuration Checklist

Before testing messages, verify:

### Backend (.env file)
```
DATABASE_URL=postgresql://[user]:[password]@[host]:[port]/[db]
JWT_SECRET=your_secret_key
NODE_ENV=development
PORT=3001
```

### Android (ApiService.kt)
```kotlin
const val USE_LOCAL_BACKEND = false  // or true if testing locally
const val BASE_URL = "https://mediconnectug.onrender.com/"
```

### Database
```sql
-- Chat tables exist?
\dt chat_sessions
\dt direct_messages

-- Sample data exists?
SELECT COUNT(*) FROM chat_sessions;
SELECT COUNT(*) FROM direct_messages;
```

---

## 🚀 Quick Fix Commands

### Rebuild Backend
```powershell
cd D:\HealthBridge\android_app\backend
npm install  # if needed
node server.js
```

### Rebuild Android  
```powershell
cd D:\HealthBridge\android_app
.\gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Watch Logs
```powershell
# Real-time message logs
adb logcat PatientChat:D DoctorProfile:D *:E -v brief

# Save to file
adb logcat -d > C:\logs.txt
```

---

## 📞 Still Not Working?

### Collect These Details:

**1. Logcat output**
```powershell
adb logcat -d | Out-File C:\logs.txt
# Share the contents around "PatientChat" and "DoctorProfile"
```

**2. Backend logs**
```powershell
# Terminal where server.js runs - copy the output
```

**3. Database info**
```
- Where is database hosted? (Supabase/local/cloud)
- Can you access it directly?
- Do tables exist? (SELECT COUNT(*) FROM chat_sessions;)
```

**4. Device info**
```
- Device: Android phone or emulator?
- Android version?
- Internet: WiFi or cellular data?
```

**5. Action sequence**
```
Step by step:
1. Clicked "Message" - what happened?
2. Chat opened - what did you see?
3. Typed message - did cursor move?
4. Tapped send - did input field clear?
5. Did message appear? Where? Top/bottom/nowhere?
```

---

## 💡 Pro Tips

### Enable All Debugging
Add this to PatientChatActivity onCreate():
```kotlin
Log.d("PatientChat", "Session ID: $sessionId")
Log.d("PatientChat", "User ID: $myPatientId")
Log.d("PatientChat", "Backend URL: ${ApiClient.BASE_URL}")
```

### Test Backend Directly
```powershell
# Create session
curl -X POST http://localhost:3001/api/chat-sessions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "doctor_id": 1,
    "chief_complaint": "Test",
    "urgency_level": "MODERATE"
  }'

# Send message
curl -X POST http://localhost:3001/api/chat-sessions/1/messages \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message_text": "Hello Doctor"}'
```

### Simulate No Backend
If backend unreachable, test fallback by modifying DoctorProfileActivity:
```kotlin
// Temporary test - remove after debugging
val session = ChatSession(
    id = 999,
    patientId = 1,
    patientName = "Test Patient",
    doctorId = 1,
    doctorName = "Test Doctor",
    chiefComplaint = "Test",
    urgency = "MODERATE",
    status = "active",
    createdAt = System.currentTimeMillis().toString(),
    lastMessageAt = null
)
```

---

## Version Info

- **Backend Code**: `app/src/main/java/com/healthbridge/backend/server.js`
- **Android Code**: `app/src/main/java/com/healthbridge/PatientChatActivity.kt`
-**Last Updated**: May 5, 2026
- **Status**: ✅ Tested and working

**Remember**: Check Logcat first! Logs tell you exactly what's happening. 📱

