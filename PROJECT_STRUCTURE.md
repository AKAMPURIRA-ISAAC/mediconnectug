# 📂 MediConnectUG - Project Structure

**Version:** 2.1.3  
**Date:** May 5, 2026  
**Status:** ✅ Production Ready

---

## 📁 Root Directory Structure

```
android_app/
├── 📄 README.md                    # Main project documentation
├── 📄 START_HERE.md                # Quick start guide ⭐
├── 📄 PROJECT_STRUCTURE.md         # This file - Project organization
├── 📄 build.gradle.kts             # Project build configuration
├── 📄 settings.gradle.kts          # Project settings
├── 📄 gradle.properties            # Gradle properties
├── 📄 local.properties             # Local SDK paths
├── 📄 render.yaml                  # Backend deployment config
├── 📄 .gitignore                   # Git ignore rules
│
├── 📂 app/                         # Android application source
├── 📂 backend/                     # Node.js backend API
├── 📂 supabase/                    # Database schemas & migrations
├── 📂 docs/                        # 📚 All documentation files
├── 📂 scripts/                     # 🛠️ Build & automation scripts
├── 📂 gradle/                      # Gradle wrapper files
├── 📂 .gradle/                     # Gradle build cache
├── 📂 .idea/                       # Android Studio IDE files
└── 📂 .github/                     # GitHub workflows & templates
```

---

## 📂 App Directory (Android Application)

```
app/
├── 📄 build.gradle.kts             # App module build config
├── 📂 src/
│   └── main/
│       ├── 📂 java/com/healthbridge/
│       │   ├── 📂 activities/      # UI Activities
│       │   ├── 📂 adapters/        # RecyclerView adapters
│       │   ├── 📂 data/            # 💾 Database & Repositories
│       │   ├── 📂 models/          # Data models
│       │   ├── 📂 network/         # API service layer
│       │   ├── 📂 util/            # Utility classes
│       │   └── 📄 HealthBridgeApplication.kt
│       │
│       ├── 📂 res/                 # Android resources
│       │   ├── drawable/           # Images & icons
│       │   ├── layout/             # XML layouts
│       │   ├── values/             # Strings, colors, themes
│       │   └── mipmap/             # App icons
│       │
│       └── 📄 AndroidManifest.xml  # App manifest
│
├── 📂 build/                       # Build outputs
│   └── outputs/
│       └── apk/
│           ├── release/            # Release APK (production)
│           └── debug/              # Debug APK (testing)
│
└── 📂 keystore/                    # Signing keys (gitignored)
    └── README.md
```

---

## 📂 Backend Directory (Node.js API)

```
backend/
├── 📄 server.js                    # Main Express.js server
├── 📄 package.json                 # Node dependencies
└── 📄 package-lock.json            # Locked dependencies
```

**Backend URL (Production):** https://mediconnectug.onrender.com/

---

## 📂 Supabase Directory (Database)

```
supabase/
├── 📄 schema.sql                   # Main database schema
├── 📄 doctor_chat_schema.sql       # Doctor chat tables
└── 📄 rls_policies.sql             # Row-level security policies
```

---

## 📚 Documentation Directory

```
docs/
├── 📄 index.html                              # GitHub Pages download site
├── 📄 .nojekyll                               # GitHub Pages config
├── 📄 README.md                               # Documentation index ⭐
├── 📄 DOCUMENTATION_INDEX.md                  # Full documentation map
├── 📄 REPOSITORY_QUICK_REFERENCE.md          # API reference guide
├── 📄 QUICK_START_v2.0.0.md                  # Quick start guide
│
├── 📂 guides/                                 # Implementation guides
│   ├── 📂 development/                        # Development guides
│   │   ├── AUTOMATIC_BACKEND_SWITCHING.md
│   │   ├── AUTOMATIC_BUILDING.md
│   │   ├── HOW_NETWORKING_WORKS.md
│   │   └── DATABASE_INTEGRATION_GUIDE.md
│   │
│   ├── 📂 deployment/                         # Deployment guides
│   │   ├── DEPLOYMENT_GUIDE.md
│   │   └── BACKEND_DEPLOYMENT_REQUIRED.md
│   │
│   └── 📂 features/                           # Feature documentation
│       ├── AI_CHAT_IMPROVEMENTS.md
│       ├── DOCTOR_CHAT_IMPLEMENTATION_GUIDE.md
│       ├── DOCTOR_CHAT_QUICK_START.md
│       ├── DOCTOR_CHAT_TEST_GUIDE.md
│       ├── API_ENDPOINTS_POSTGRESQL.md
│       └── SESSION_TIMEOUT_UPDATE.md
│
├── 📂 releases/                               # Release notes
│   ├── PRODUCTION_RELEASE_v2.1.3.md
│   ├── v2.0.0_RELEASE_NOTES.md
│   └── FINAL_UPDATE_v2.1.2.md
│
├── 📂 troubleshooting/                        # Bug fixes & solutions
│   ├── APP_CRASH_FIXES.md
│   ├── CRASH_FIXES_AND_ICON_SUMMARY.md
│   ├── MESSAGE_BUTTON_FIX.md
│   ├── MESSAGE_BUTTON_FIX_COMPLETE.md
│   ├── MESSAGE_BUTTON_QUICK_FIX.md
│   ├── MESSAGE_BUTTON_TROUBLESHOOTING.md
│   ├── DEBUG_MESSAGE_BUTTON.md
│   └── README_MESSAGE_BUTTON.md
│
└── 📂 archive/                                # Historical documents
    ├── COMPLETION_CHECKLIST.md
    ├── COMPLETION_REPORT.md
    ├── COMPLETION_REPORT_v2.1.3.md
    ├── FINAL_COMPLETION_SUMMARY.md
    ├── PROJECT_COMPLETE.md
    ├── QUICK_SUMMARY_v2.1.3.md
    ├── DOCTOR_CHAT_FINAL_SUMMARY.md
    ├── DOCTOR_CHAT_FIX_SUMMARY.md
    ├── DOCTOR_FEATURES_COMPLETE_SUMMARY.md
    ├── DOCTOR_QUICK_GUIDE.md
    ├── FILE_STRUCTURE_CHANGES.md
    └── IMPLEMENTATION_SUMMARY.md
```

---

## 🛠️ Scripts Directory

```
scripts/
├── 📄 auto-build.ps1               # Auto-rebuild on file changes
├── 📄 build-install-run.ps1        # Complete build → install → run workflow
├── 📄 quick-build.ps1              # Fast incremental build
├── 📄 push-to-github.ps1           # Commit & push automation
└── 📄 test-message-button.ps1      # Test specific features
```

### Script Usage

```powershell
# From project root:
.\scripts\auto-build.ps1           # Watch mode - auto rebuilds
.\scripts\build-install-run.ps1    # Build, install, and run app
.\scripts\quick-build.ps1          # Quick debug build
.\scripts\push-to-github.ps1       # Stage, commit, push
```

---

## 📱 Key Source Files

### Core Application Files

```
app/src/main/java/com/healthbridge/
│
├── HealthBridgeApplication.kt      # Application class (initializes DB)
│
├── 📂 activities/
│   ├── MainActivity.kt             # Home screen
│   ├── LoginActivity.kt            # Authentication
│   ├── RegisterActivity.kt         # User registration
│   ├── ChatActivity.kt             # AI health assistant
│   ├── DoctorChatActivity.kt       # Doctor messaging
│   ├── FindDoctorsActivity.kt      # Doctor search & booking
│   ├── AppointmentsActivity.kt     # Appointment management
│   ├── ProfileActivity.kt          # User profile
│   └── EmergencyActivity.kt        # Emergency services
│
├── 📂 data/
│   ├── 📂 database/
│   │   ├── AppDatabase.kt          # Room database
│   │   ├── Entities.kt             # Database entities
│   │   └── 📂 dao/                 # Data access objects
│   │       ├── DoctorDao.kt
│   │       ├── AppointmentDao.kt
│   │       ├── ChatDao.kt
│   │       └── UserDao.kt
│   │
│   └── 📂 repository/
│       ├── RepositoryFactory.kt    # Singleton repository access
│       └── Repositories.kt         # Repository implementations
│           ├── DoctorRepository
│           ├── AppointmentRepository
│           ├── ProfileRepository
│           ├── ChatRepository
│           ├── MedicalRecordRepository
│           ├── PrescriptionRepository
│           └── NotificationRepository
│
├── 📂 network/
│   ├── ApiService.kt               # Retrofit API interface
│   ├── RetrofitClient.kt           # HTTP client setup
│   └── NetworkModels.kt            # API request/response models
│
├── 📂 models/
│   ├── User.kt
│   ├── Doctor.kt
│   ├── Appointment.kt
│   ├── ChatMessage.kt
│   └── Notification.kt
│
├── 📂 adapters/
│   ├── DoctorAdapter.kt
│   ├── AppointmentAdapter.kt
│   ├── ChatAdapter.kt
│   └── NotificationAdapter.kt
│
└── 📂 util/
    ├── SessionManager.kt           # JWT token management
    ├── UpdateChecker.kt            # App update notifications
    └── Constants.kt                # App constants
```

---

## 🔧 Configuration Files

### Build Configuration

| File | Purpose |
|------|---------|
| `build.gradle.kts` (project) | Project-level Gradle config |
| `app/build.gradle.kts` | App module dependencies & version |
| `settings.gradle.kts` | Project structure definition |
| `gradle.properties` | Gradle JVM options |
| `local.properties` | Local SDK paths (gitignored) |

### App Configuration

| File | Purpose |
|------|---------|
| `AndroidManifest.xml` | App permissions & components |
| `res/values/strings.xml` | Text strings |
| `res/values/colors.xml` | Color palette |
| `res/values/themes.xml` | Material Design themes |

### Backend Configuration

| File | Purpose |
|------|---------|
| `backend/server.js` | Express.js server |
| `backend/package.json` | Node.js dependencies |
| `render.yaml` | Render.com deployment |

---

## 📖 Documentation Quick Reference

### 🚀 Getting Started
- **START_HERE.md** - Begin here (5 min read)
- **README.md** - Full project overview
- **docs/QUICK_START_v2.0.0.md** - Setup instructions

### 👨‍💻 Development
- **docs/guides/development/AUTOMATIC_BUILDING.md** - Build automation
- **docs/guides/development/AUTOMATIC_BACKEND_SWITCHING.md** - Switch backends
- **docs/guides/development/HOW_NETWORKING_WORKS.md** - Network layer
- **docs/guides/development/DATABASE_INTEGRATION_GUIDE.md** - Database usage
- **docs/REPOSITORY_QUICK_REFERENCE.md** - Repository API reference

### 🚀 Deployment
- **docs/guides/deployment/DEPLOYMENT_GUIDE.md** - Full deployment guide
- **docs/guides/deployment/BACKEND_DEPLOYMENT_REQUIRED.md** - Backend setup
- **render.yaml** - Deployment configuration

### 📱 Features
- **docs/guides/features/AI_CHAT_IMPROVEMENTS.md** - AI assistant
- **docs/guides/features/DOCTOR_CHAT_IMPLEMENTATION_GUIDE.md** - Doctor chat
- **docs/guides/features/API_ENDPOINTS_POSTGRESQL.md** - API reference
- **docs/guides/features/SESSION_TIMEOUT_UPDATE.md** - Session management

### 📝 Release Notes
- **docs/releases/PRODUCTION_RELEASE_v2.1.3.md** - Latest release
- **docs/releases/v2.0.0_RELEASE_NOTES.md** - v2.0.0 release
- **docs/releases/FINAL_UPDATE_v2.1.2.md** - v2.1.2 release

### 🐛 Troubleshooting
- **docs/troubleshooting/** - All bug fixes & solutions

### 📦 Archive
- **docs/archive/** - Historical completion reports

---

## 🎯 Common Tasks

### Build & Run

```powershell
# Quick debug build
.\gradlew assembleDebug

# Production release build
.\gradlew assembleRelease

# Install on connected device
adb install app\build\outputs\apk\debug\app-debug.apk

# Complete workflow (build → install → run)
.\scripts\build-install-run.ps1

# Watch mode (auto-rebuild)
.\scripts\auto-build.ps1
```

### Switch Backend

**For local development:**
```kotlin
// In: app/src/main/java/com/healthbridge/network/ApiService.kt
private const val USE_LOCAL_BACKEND = true  // Use local backend
```

**For production:**
```kotlin
private const val USE_LOCAL_BACKEND = false  // Use production backend
```

### Git Workflow

```powershell
# Quick commit & push
.\scripts\push-to-github.ps1

# Or manually:
git add .
git commit -m "Your message"
git push origin main
```

---

## 🔐 Security Notes

### Sensitive Files (Gitignored)

- `local.properties` - SDK paths
- `app/keystore/` - Signing keys
- `*.jks` - Keystore files
- `.env` files - Environment variables
- `backend/node_modules/` - Node packages

### Credentials Management

- JWT tokens stored in encrypted SharedPreferences
- API keys configured in backend environment variables
- Database passwords managed by hosting provider

---

## 📊 Project Statistics

### Codebase
- **Total Files:** 150+
- **Lines of Code:** 15,000+
- **Activities:** 15+
- **Repositories:** 7
- **API Endpoints:** 25+

### Documentation
- **Documentation Files:** 40+
- **Total Documentation:** 30,000+ words
- **Guides:** 15+
- **Release Notes:** 3

### Build Output
- **Release APK Size:** ~8 MB
- **Debug APK Size:** ~8 MB
- **Min SDK:** Android 7.0 (API 24)
- **Target SDK:** Android 14 (API 34)

---

## 🔄 Development Workflow

### 1. Make Code Changes
Edit files in `app/src/main/java/com/healthbridge/`

### 2. Test Locally
```powershell
# Use local backend for testing
# Set USE_LOCAL_BACKEND = true in ApiService.kt
cd D:\HealthBridge\backend
npm start

# Auto-rebuild app on changes
cd D:\HealthBridge\android_app
.\scripts\auto-build.ps1
```

### 3. Build & Test
```powershell
.\scripts\build-install-run.ps1
```

### 4. Switch to Production
```kotlin
// In ApiService.kt:
private const val USE_LOCAL_BACKEND = false
```

### 5. Build Release
```powershell
.\gradlew assembleRelease
```

### 6. Commit & Push
```powershell
.\scripts\push-to-github.ps1
```

### 7. Backend Auto-Deploys
Render.com automatically deploys backend from GitHub

---

## 🌐 URLs & Resources

### Production
- **Backend API:** https://mediconnectug.onrender.com/
- **Health Check:** https://mediconnectug.onrender.com/api/health
- **Download Page:** https://akampurira-isaac.github.io/mediconnectug/

### Development
- **Local Backend:** http://localhost:3001
- **Local Health:** http://localhost:3001/api/health

### Repository
- **GitHub:** https://github.com/AKAMPURIRA-ISAAC/mediconnectug
- **Releases:** https://github.com/AKAMPURIRA-ISAAC/mediconnectug/releases

### Hosting
- **Backend:** Render.com
- **Database:** PostgreSQL (Render.com)
- **Download Site:** GitHub Pages

---

## 📞 Developer Resources

### Documentation Locations
- **Main Docs:** `docs/` folder
- **Scripts:** `scripts/` folder
- **Release Notes:** `docs/releases/`
- **Guides:** `docs/guides/`

### Key Documentation Files
1. **START_HERE.md** - Start here!
2. **README.md** - Project overview
3. **PROJECT_STRUCTURE.md** - This file
4. **docs/REPOSITORY_QUICK_REFERENCE.md** - API reference
5. **docs/DOCUMENTATION_INDEX.md** - Full doc index

---

## ✅ Project Organization Benefits

### ✨ Before (Messy)
- 40+ files in root directory
- Hard to find documentation
- Mixed scripts with docs
- No clear organization

### ✅ After (Clean)
- **Organized folders** by purpose
- **docs/** - All documentation together
- **scripts/** - All automation scripts
- **Clean root** - Only essential files
- **Easy navigation** - Clear structure
- **Professional** - Production-ready organization

---

## 🎯 Next Steps

1. **Read START_HERE.md** - Get project overview
2. **Explore docs/** - Find relevant documentation
3. **Use scripts/** - Automate your workflow
4. **Check docs/releases/** - See latest changes
5. **Reference docs/guides/** - Learn features

---

**Project Structure Version:** 1.0  
**Last Updated:** May 5, 2026  
**Maintained By:** Isaac Akampurira

**🎉 Well-organized and production-ready!**

