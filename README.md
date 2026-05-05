# 🏥 MediConnectUG - Healthcare App

**Version:** 2.1.3  
**Status:** ✅ Production Ready  
**Updated:** May 5, 2026

A modern Android healthcare application connecting patients with doctors in Uganda.

---

## 🚀 Quick Start

### For Development
```powershell
# 1. Start Backend
cd D:\HealthBridge\backend
npm start

# 2. Start Auto-Builder (in another terminal)
cd D:\HealthBridge\android_app
.\scripts\auto-build.ps1

# 3. Code! APK rebuilds automatically on save
```

### For Production Build
```powershell
# Switch to production backend
# Edit: app/src/main/java/com/healthbridge/network/ApiService.kt
# Set: USE_LOCAL_BACKEND = false

# Build release
.\gradlew assembleRelease

# APK: app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## ✨ Features

### Patient Features
- 🔍 Find & book doctors
- 💬 AI health assistant
- 📅 Appointment management
- 👨‍⚕️ Direct chat with doctors
- 🏥 Hospital & pharmacy locator
- 📱 Emergency services
- 📋 Medical records
- 💊 Prescription management
- 🔔 Notifications

### Doctor Features  
- 📊 Patient dashboard
- 💬 Direct patient messaging
- 📅 Appointment management
- 🩺 Patient consultations

---

## 🛠️ Development Tools

### Automatic Backend Switching
```kotlin
// In ApiService.kt
private const val USE_LOCAL_BACKEND = true  // Local development
private const val USE_LOCAL_BACKEND = false // Production
```

### Automatic Build Scripts
- `scripts\auto-build.ps1` - Continuous file watcher & rebuild
- `scripts\quick-build.ps1` - Fast single builds
- `scripts\build-install-run.ps1` - Complete workflow (build→install→launch)
- `scripts\push-to-github.ps1` - Git automation

### Session Management
- JWT tokens: 180-day expiry
- HTTP timeouts: 60 seconds
- Automatic token refresh
- Local storage with SharedPreferences

---

## 📱 App Architecture

### Frontend (Android)
- **Language:** Kotlin
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Architecture:** MVVM with Repository pattern
- **DI:** Manual dependency injection
- **Network:** Retrofit + OkHttp
- **Local DB:** Room Database
- **Async:** Kotlin Coroutines

### Backend (Node.js)
- **Runtime:** Node.js 18+
- **Framework:** Express.js
- **Database:** PostgreSQL (Supabase)
- **Auth:** JWT tokens
- **Hosting:** Render.com
- **API:** REST JSON

---

## 🔧 Configuration

### Backend URLs
```kotlin
// Local Development (Emulator)
LOCAL_URL = "http://10.0.2.2:3001/"

// Local Development (Physical Device)
LOCAL_URL = "http://YOUR_PC_IP:3001/"  // Get IP with: ipconfig

// Production
PRODUCTION_URL = "https://mediconnectug.onrender.com/"
```

### Environment Variables (Backend)
```env
DATABASE_URL=postgresql://user:pass@host:5432/db
JWT_SECRET=your_secret_key_here
PORT=3001
```

---

## 📦 Build & Deploy

### Development Build
```powershell
cd D:\HealthBridge\android_app
.\gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Production Build
```powershell
# 1. Set production backend
USE_LOCAL_BACKEND = false

# 2. Build release
.\gradlew assembleRelease

# 3. Sign APK (if needed)
# Use Android Studio or jarsigner

# 4. APK location
app/build/outputs/apk/release/app-release.apk
```

### Backend Deployment
```bash
# Auto-deploys from GitHub to Render
git push origin main

# Or manual deploy in Render dashboard
```

---

## 🧪 Testing

### Quick Test Workflow
```powershell
# Build, install, and launch
.\scripts\build-install-run.ps1
```

### Manual Testing
```powershell
# Build
.\gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat -s HealthBridge:* ApiClient:*
```

---

## 📚 Documentation

### 🚀 Quick Start
- **START_HERE.md** - 5-minute project overview ⭐
- **PROJECT_STRUCTURE.md** - Complete project organization guide
- **docs/QUICK_START_v2.0.0.md** - Setup instructions
- **docs/REPOSITORY_QUICK_REFERENCE.md** - Repository API reference

### 📖 Documentation Organization
All documentation is organized in the **`docs/`** folder:

```
docs/
├── guides/
│   ├── development/        # Development guides (backend switching, building, etc.)
│   ├── deployment/         # Deployment & production guides
│   └── features/           # Feature implementation docs (AI chat, doctor chat, etc.)
├── releases/               # Release notes & changelogs
├── troubleshooting/        # Bug fixes & solutions
└── archive/                # Historical documents
```

### 📂 Main Documentation Files
- **[docs/README.md](docs/README.md)** - Documentation index & navigation
- **[docs/guides/development/AUTOMATIC_BACKEND_SWITCHING.md](docs/guides/development/AUTOMATIC_BACKEND_SWITCHING.md)** - Backend configuration
- **[docs/guides/development/AUTOMATIC_BUILDING.md](docs/guides/development/AUTOMATIC_BUILDING.md)** - Build automation
- **[docs/guides/development/DATABASE_INTEGRATION_GUIDE.md](docs/guides/development/DATABASE_INTEGRATION_GUIDE.md)** - Database setup
- **[docs/guides/deployment/DEPLOYMENT_GUIDE.md](docs/guides/deployment/DEPLOYMENT_GUIDE.md)** - Production deployment
- **[docs/guides/features/API_ENDPOINTS_POSTGRESQL.md](docs/guides/features/API_ENDPOINTS_POSTGRESQL.md)** - API documentation
- **[docs/releases/PRODUCTION_RELEASE_v2.1.3.md](docs/releases/PRODUCTION_RELEASE_v2.1.3.md)** - Latest release notes

**See [docs/README.md](docs/README.md) for complete documentation index.**

---

## 🎯 Key Project Structure

```
android_app/
├── 📄 README.md                       # This file
├── 📄 START_HERE.md                   # Quick start guide ⭐
├── 📄 PROJECT_STRUCTURE.md            # Complete structure reference
│
├── 📂 app/                            # Android application
│   ├── src/main/
│   │   ├── java/com/healthbridge/
│   │   │   ├── network/
│   │   │   │   └── ApiService.kt     # API & backend config ⚙️
│   │   │   ├── activities/
│   │   │   │   ├── HomeActivity.kt   # Main dashboard
│   │   │   │   ├── LoginActivity.kt  # Authentication
│   │   │   │   └── ChatActivity.kt   # AI assistant
│   │   │   ├── data/
│   │   │   │   ├── database/         # Room database
│   │   │   │   └── repository/       # Repository pattern
│   │   │   └── util/
│   │   │       └── UpdateChecker.kt  # Auto-update system
│   │   ├── res/                       # UI resources
│   │   └── AndroidManifest.xml       # App config
│   └── build.gradle.kts               # App dependencies
│
├── 📂 backend/                        # Node.js API server
│   ├── server.js                      # Express API server
│   └── package.json                   # Node dependencies
│
├── 📂 supabase/                       # Database schemas
│   ├── schema.sql                     # Main schema
│   └── doctor_chat_schema.sql         # Chat tables
│
├── 📂 docs/                           # 📚 All documentation
│   ├── guides/                        # Implementation guides
│   ├── releases/                      # Release notes
│   ├── troubleshooting/               # Bug fixes
│   └── archive/                       # Historical docs
│
├── 📂 scripts/                        # 🛠️ Build & automation
│   ├── auto-build.ps1                 # File watcher
│   ├── quick-build.ps1                # Quick builds
│   ├── build-install-run.ps1          # Full workflow
│   └── push-to-github.ps1             # Git automation
│
└── 📄 render.yaml                     # Backend deployment config
```

**See [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) for complete details.**

---

## 🔐 Security

- ✅ JWT authentication with 180-day expiry
- ✅ HTTPS for all API calls in production
- ✅ Password hashing with bcrypt (10 rounds)
- ✅ Token stored in private SharedPreferences
- ✅ SQL injection prevention (parameterized queries)
- ✅ Input validation on frontend and backend

---

## 🐛 Troubleshooting

### App won't connect to backend
```kotlin
// Check ApiClient logs
adb logcat -s Apiclient:*

// Verify backend URL is correct
// Check USE_LOCAL_BACKEND flag
```

### Build fails
```powershell
# Clean and rebuild
.\gradlew clean
.\gradlew assembleDebug
```

### Update checker shows on every login
- ✅ FIXED in v2.1.3
- Only shows when new version is available
- Tracks shown versions to avoid repetition

---

## 📊 Performance

- **App size:** ~8MB
- **Cold start:** <2 seconds
- **Backend response:** ~200-500ms (production)
- **Build time:** ~10 seconds (incremental)
- **Auto-rebuild:** ~10 seconds after save

---

## 🎨 UI/UX

- Material Design 3
- Custom color scheme (Medical blue #1565C0)
- Smooth animations
- Responsive layouts
- Dark mode support (system-based)
- Offline-first for cached data

---

## 🚦 CI/CD

### Automated
- Linting on commit
- Build on push
- Backend auto-deploy (GitHub → Render)

### Manual
- APK releases via GitHub Releases
- Play Store upload (manual)

---

## 📈 Version History

- **v2.1.3** (May 5, 2026) - Update checker fix, automatic builds
- **v2.1.2** - Session timeout extension (180 days)
- **v2.1.0** - Doctor chat features
- **v2.0.0** - Major UI overhaul
- **v1.0.0** - Initial release

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

---

## 📄 License

This project is licensed under the MIT License.

---

## 👥 Team

- **Developer:** Isaac Akampurira
- **GitHub:** [@AKAMPURIRA-ISAAC](https://github.com/AKAMPURIRA-ISAAC)
- **Repository:** [mediconnectug](https://github.com/AKAMPURIRA-ISAAC/mediconnectug)

---

## 🔗 Links

- **Download Page:** https://akampurira-isaac.github.io/mediconnectug/
- **API:** https://mediconnectug.onrender.com/
- **GitHub:** https://github.com/AKAMPURIRA-ISAAC/mediconnectug

---

## ✅ Production Checklist

Before deploying to production:

- [ ] Set `USE_LOCAL_BACKEND = false`
- [ ] Update version in `build.gradle.kts`
- [ ] Build release APK
- [ ] Test on physical device
- [ ] Verify all API endpoints work
- [ ] Check update checker works
- [ ] Create GitHub release
- [ ] Update download page
- [ ] Deploy backend to Render
- [ ] Monitor logs for errors

---

**Ready for Production** ✅  
**Build Status:** SUCCESS  
**Last Updated:** May 5, 2026

