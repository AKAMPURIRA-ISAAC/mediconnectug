# Quick Test Guide: Doctor-Patient Chat

## How to Test the Doctor Chat Fix

### Prerequisites
- ✅ Backend server running at `https://mediconnectug.onrender.com/`
- ✅ At least 1 patient account created
- ✅ At least 1 doctor account created
- ✅ App installed on device or emulator

---

## Test Scenario 1: Patient Initiates Chat

### Step 1: Login as Patient
```
1. Open HealthBridge app
2. Login with patient credentials
3. Navigate to Home screen
```

### Step 2: Chat with HealthBridge AI
```
1. Tap on "HealthBridge AI" card
2. Type: "I have a fever and headache"
3. Answer AI's follow-up questions:
   - Duration: "2 days"
   - Severity: "7/10"
```

### Step 3: Accept Doctor Recommendation
```
1. AI will analyze and suggest a doctor
2. Read doctor's profile shown by AI
3. Tap "Start Chat" button
4. ✅ VERIFY: PatientChatActivity opens
5. ✅ VERIFY: Doctor's name appears in top bar
6. ✅ VERIFY: Your chief complaint is shown
```

### Step 4: Send Message to Doctor
```
1. Type a message: "Hello doctor, I need help"
2. Tap send button
3. ✅ VERIFY: Message appears in chat (right-aligned, blue background)
4. ✅ VERIFY: Timestamp shows below message
```

---

## Test Scenario 2: Doctor Receives and Responds

### Step 1: Login as Doctor
```
1. Logout from patient account
2. Login with doctor credentials
3. ✅ VERIFY: DoctorHomeActivity shows
```

### Step 2: View Patient Chat
```
1. ✅ VERIFY: Patient's chat appears in the list
2. ✅ VERIFY: Shows patient name, chief complaint, urgency icon
3. ✅ VERIFY: Shows time ago (e.g., "Just now" or "2 min ago")
4. Tap on the chat session
5. ✅ VERIFY: DoctorChatActivity opens
6. ✅ VERIFY: Patient's messages are visible
```

### Step 3: Reply to Patient
```
1. Type a message: "Hello! I'll review your symptoms."
2. Tap send button
3. ✅ VERIFY: Message appears (right-aligned, blue background)
4. ✅ VERIFY: Patient's messages are left-aligned, white background
```

### Step 4: Continue Conversation
```
1. Send 2-3 more messages
2. ✅ VERIFY: All messages appear correctly
3. ✅ VERIFY: Timestamps are correct
4. ✅ VERIFY: Messages scroll smoothly
```

---

## Test Scenario 3: Real-Time Polling

### Step 1: Open Chat on Both Sides
```
DEVICE 1 (Patient):
1. Login as patient
2. Open the chat with doctor
3. Keep chat screen open

DEVICE 2 (Doctor):
1. Login as doctor
2. Open the chat with patient
3. Keep chat screen open
```

### Step 2: Test Message Delivery
```
FROM DOCTOR:
1. Send message: "How are you feeling now?"
2. Wait 3-5 seconds
3. ✅ VERIFY: Message appears on patient's device

FROM PATIENT:
1. Send reply: "Still feeling unwell"
2. Wait 3-5 seconds
3. ✅ VERIFY: Reply appears on doctor's device
```

### Step 3: Test Multiple Messages
```
1. Send 5 rapid messages from doctor
2. ✅ VERIFY: All appear on patient side within 5 seconds
3. Send 5 rapid messages from patient
4. ✅ VERIFY: All appear on doctor side within 5 seconds
```

---

## Test Scenario 4: App State Changes

### Step 1: Test Background/Foreground
```
1. Patient sends message to doctor
2. Doctor minimizes app (go to home screen)
3. Doctor reopens app
4. ✅ VERIFY: Chat screen still shows
5. ✅ VERIFY: New message is visible
```

### Step 2: Test Screen Rotation
```
1. Open chat on patient side
2. Rotate device
3. ✅ VERIFY: Messages still visible
4. ✅ VERIFY: No crashes
5. Send a message
6. ✅ VERIFY: Message sends successfully
```

### Step 3: Test Navigation
```
1. Open chat on patient side
2. Tap back button
3. ✅ VERIFY: Returns to Home
4. Open chat again
5. ✅ VERIFY: All previous messages still there
6. ✅ VERIFY: Can send new messages
```

---

## Test Scenario 5: Error Handling

### Step 1: Test Offline Mode
```
1. Open chat on patient side
2. Turn off WiFi/Data
3. Try to send message
4. ✅ VERIFY: Shows error toast
5. Turn WiFi/Data back on
6. Try again
7. ✅ VERIFY: Message sends successfully
```

### Step 2: Test Empty Messages
```
1. Try to send empty message (just spaces)
2. ✅ VERIFY: Nothing happens (message not sent)
3. Type actual message
4. ✅ VERIFY: Sends successfully
```

### Step 3: Test Long Messages
```
1. Type a very long message (200+ characters)
2. ✅ VERIFY: EditText expands (up to 4 lines)
3. Send the message
4. ✅ VERIFY: Message sends and displays correctly
5. ✅ VERIFY: Message wraps properly in chat bubble
```

---

## Expected UI Behavior

### PatientChatActivity (Patient View):
```
TOP BAR:
- [←] Dr. John Doe
- 📋 Fever and headache 🟡

MESSAGES:
- Left (white): Doctor's messages
- Right (blue): Patient's messages

BOTTOM:
- [Text input field] [Send button]
```

### DoctorChatActivity (Doctor View):
```
TOP BAR:
- [←] Jane Smith
- 📋 Fever and headache 🟡

MESSAGES:
- Left (white): Patient's messages
- Right (blue): Doctor's messages

BOTTOM:
- [Text input field] [Send button]
```

---

## Common Issues & Solutions

### Issue: Chat doesn't open after AI recommendation
**Solution:** 
- Check backend is running
- Verify API endpoint in ApiClient.kt
- Check Logcat for errors
- Verify session was created in backend

### Issue: Doctor doesn't see patient's chat
**Solution:**
- Refresh DoctorHomeActivity (go back and reopen)
- Check doctor is logged in correctly
- Verify backend assigns session to doctor
- Check getChatSessions API response

### Issue: Messages don't appear in real-time
**Solution:**
- Wait up to 5 seconds (polling interval is 3s)
- Check both devices have internet
- Verify polling is running (check Logcat)
- Check API endpoints are responding

### Issue: App crashes on opening chat
**Solution:**
- Check session_id is valid
- Verify all required data is passed via Intent
- Run `./gradlew clean build`
- Check for null pointer exceptions in Logcat

---

## Success Criteria

✅ **All tests pass without crashes**  
✅ **Messages deliver within 5 seconds**  
✅ **Chat UI displays correctly on both sides**  
✅ **Navigation works smoothly**  
✅ **Errors are handled gracefully**  

---

## Automated Test Script (Optional)

If you want to automate testing, here's pseudo-code:

```kotlin
// Test 1: Create chat session
@Test
fun testCreateChatSession() {
    // Login as patient
    loginAsPatient()
    
    // Navigate to AI chat
    openAIChat()
    
    // Create session
    val sessionId = createChatSession(
        chiefComplaint = "Fever",
        symptoms = "Headache, fatigue",
        urgency = "MODERATE"
    )
    
    // Verify session created
    assertNotNull(sessionId)
    assertTrue(sessionId > 0)
}

// Test 2: Send message
@Test
fun testSendMessage() {
    val message = "Hello doctor"
    val response = sendMessage(sessionId, message)
    
    assertTrue(response.success)
    assertNotNull(response.message)
    assertEquals(message, response.message.message)
}

// Test 3: Doctor sees chat
@Test
fun testDoctorSeesChat() {
    // Login as doctor
    loginAsDoctor()
    
    // Get chat sessions
    val sessions = getChatSessions()
    
    // Verify patient session exists
    assertTrue(sessions.any { it.id == sessionId })
}
```

---

## Performance Benchmarks

Expected performance metrics:

| Metric | Target | Acceptable |
|--------|--------|------------|
| Chat open time | < 1s | < 2s |
| Message send time | < 500ms | < 1s |
| Message receive time | < 3s | < 5s |
| Scroll smoothness | 60 FPS | 30 FPS |
| Memory usage | < 100MB | < 150MB |

---

## Test Report Template

```
# Test Report: Doctor-Patient Chat

Date: ___________
Tester: ___________
Device: ___________
Android Version: ___________

## Test Results

Scenario 1: Patient Initiates Chat
- [ ] PASS  [ ] FAIL  Notes: ___________

Scenario 2: Doctor Receives Chat
- [ ] PASS  [ ] FAIL  Notes: ___________

Scenario 3: Real-Time Polling
- [ ] PASS  [ ] FAIL  Notes: ___________

Scenario 4: App State Changes
- [ ] PASS  [ ] FAIL  Notes: ___________

Scenario 5: Error Handling
- [ ] PASS  [ ] FAIL  Notes: ___________

## Issues Found
1. ___________
2. ___________
3. ___________

## Overall Status
- [ ] Ready for Production
- [ ] Needs Minor Fixes
- [ ] Needs Major Fixes

Signature: ___________
```

---

**Happy Testing! 🎉**

If you encounter any issues, refer to:
- `DOCTOR_CHAT_FIX_SUMMARY.md` for technical details
- `DOCTOR_CHAT_IMPLEMENTATION_GUIDE.md` for implementation reference
- Logcat output for debugging information

