# 📡 How MediConnectUG Networking Works

## YES — It is fully networked over the internet

The APK works the same way regardless of how it's installed (Play Store or direct download).
Once installed, it connects to the internet exactly like WhatsApp or your banking app.

---

## Network Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        USER'S PHONE                             │
│                                                                 │
│   MediConnectUG App                                             │
│   - Sends HTTPS requests                                        │
│   - Stores data in local Room DB (offline cache)                │
└──────────────┬──────────────────────────────────────────────────┘
               │
               │  HTTPS (encrypted, same as banking apps)
               │  WiFi or 4G/mobile data
               │
               ▼
┌─────────────────────────────────────────────────────────────────┐
│              RENDER (Free backend server)                       │
│   https://mediconnectug-api.onrender.com                        │
│                                                                 │
│   Node.js/Express API handles:                                  │
│   • Login / Register (/api/auth/*)                              │
│   • Doctors list (/api/doctors)                                 │
│   • Appointments (/api/appointments)                            │
│   • Medical records, prescriptions, notifications              │
│   • Chat history                                                │
└──────────────┬──────────────────────────────────────────────────┘
               │
               │  SSL/TLS connection
               │
               ▼
┌─────────────────────────────────────────────────────────────────┐
│              SUPABASE (Free PostgreSQL database)                │
│   db.[your-project].supabase.co                                 │
│                                                                 │
│   Tables: users, doctors, appointments,                         │
│           medical_records, prescriptions,                       │
│           notifications, chat_messages, health_tips             │
└─────────────────────────────────────────────────────────────────┘
```

---

## What Happens in Each Scenario

### ✅ User has internet (WiFi or 4G)
- App fetches live data from Render → Supabase
- All changes (bookings, profile edits) saved to real database
- Multiple users on different phones see the same data
- Doctor list updates in real-time

### 📵 User has NO internet
- App automatically falls back to **locally cached data** (Room database)
- Doctors, tips, and previous data still visible
- Read-only mode — changes queue until back online
- Toast message: "No internet — showing cached data"

### ⏳ First time opening (Render cold start)
- Render free tier sleeps after 15 min of no activity
- First request wakes it up — takes ~15 seconds
- App shows: "Connecting to server… first load may take ~15 seconds"
- All subsequent requests that session are instant

---

## Security
- All traffic is **HTTPS encrypted** (TLS 1.3)
- Passwords stored as **bcrypt hashes** — never in plain text
- Every API request requires a **JWT token** (expires in 30 days)
- Network security config: cleartext HTTP **blocked** except localhost

---

## What Users Need
- Android 7.0 or higher
- Any internet connection (WiFi, MTN, Airtel, etc.)
- ~15MB storage for the app + cached data

---

## Sharing the App (No Play Store)

| Method | What user does |
|--------|---------------|
| **GitHub link** | Tap link → Download APK → Allow install → Done |
| **WhatsApp** | Receive APK file → Open → Install |
| **QR Code** | Scan code → Browser downloads APK → Install |
| **Google Drive link** | Open link → Download → Install |

**One-time setup on user's phone:**
Settings → Security → Install unknown apps → Allow browser/WhatsApp → Done
(Only needed once, same as installing apps on feature phones)

