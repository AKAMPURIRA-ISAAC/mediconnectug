# 🚀 MediConnectUG — Free Deployment Guide
## Supabase (Database) + Render (API) — NO Play Store Required

**Total monthly cost: $0** | Time to go live: ~30 minutes | Google Play: Optional

---

## ✅ GO LIVE WITHOUT PLAY STORE — 3 Methods

> You do **not** need Google Play to distribute your app to real users.
> All 3 methods below are completely free.

---

### METHOD A — GitHub Releases (Recommended ⭐)
**Users download APK directly from GitHub. Auto-builds on every version tag.**

```
You push  →  GitHub Actions builds APK  →  Creates Release  →  User downloads link
```

**Setup (one-time, ~15 minutes):**

1. Push your code to a GitHub repo (private or public)
2. Generate a keystore in Android Studio:
   - **Build → Generate Signed Bundle/APK → APK → Create new keystore**
   - Save the `.jks` file and remember the passwords
3. Convert keystore to base64:
   ```bash
   # On Windows PowerShell:
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\mediconnectug.jks")) | clip
   # (this copies it to your clipboard)
   ```
4. Go to your GitHub repo → **Settings → Secrets and variables → Actions → New secret**
   Add these 4 secrets:

   | Secret Name | Value |
   |-------------|-------|
   | `KEYSTORE_BASE64` | *(paste the base64 string from step 3)* |
   | `KEYSTORE_PASSWORD` | *(your keystore password)* |
   | `KEY_ALIAS` | `mediconnectug` |
   | `KEY_PASSWORD` | *(your key password)* |

5. To release, create a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
6. GitHub Actions automatically builds the APK and publishes it at:
   ```
   https://github.com/YOUR-USERNAME/mediconnectug/releases/latest
   ```
7. Share that link — users tap **Download APK** and install it!

The `.github/workflows/build-release.yml` file is already created for you.

---

### METHOD B — Firebase App Distribution (Zero setup)
**Share with specific people via email — they get a download link on their phone.**

1. Go to **https://console.firebase.google.com**
2. Create project `mediconnectug`
3. Go to **App Distribution** (left sidebar)
4. Click **Get started** → Add Android app (package: `com.healthbridge`)
5. Build a debug APK in Android Studio:
   - **Build → Build Bundle(s)/APK(s) → Build APK(s)**
   - APK is at `app/build/outputs/apk/debug/app-debug.apk`
6. Upload the APK to Firebase App Distribution
7. Enter the email addresses of everyone you want to test
8. Click **Distribute** — they get an email with a direct install link

✅ **No keystore needed for debug APK**
✅ Users tap the link on their phone and install immediately
✅ No "unknown sources" settings required (Firebase uses its own installer)

---

### METHOD C — Direct Sharing (Instant, no accounts needed)
**Share the APK file directly via WhatsApp, Telegram, Google Drive, or QR code.**

1. Build debug APK: **Build → Build APK(s)** → `app-debug.apk`
2. Share via any of these:

   | Method | How |
   |--------|-----|
   | **WhatsApp/Telegram** | Send the APK file as a document (not media) |
   | **Google Drive** | Upload → Share link → Anyone with link can download |
   | **WeTransfer** | Upload at wetransfer.com → Share link (free, 2GB) |
   | **QR Code** | Upload to Drive → use qr-code-generator.com to make QR |

3. Tell users: *"Go to Settings → Security → Install unknown apps → Allow your browser/WhatsApp"*

---

## OVERVIEW

```
📱 Android App  →  Render (Node.js API)  →  Supabase (PostgreSQL)
                         ↑
                    FREE forever
                   (spins up on request)
```

---

## STEP 1 — Set Up Supabase (Free Database)

### 1.1 Create Account
1. Go to **https://supabase.com**
2. Click **Start your project** → Sign up with GitHub (fastest)
3. Click **New Project**
4. Fill in:
   - **Name:** `mediconnectug`
   - **Database Password:** Choose a strong password — **save this**, you'll need it
   - **Region:** `East Africa (AWS)` or nearest available
5. Click **Create new project** — wait ~2 minutes for it to provision

### 1.2 Run the Database Schema
1. In your Supabase project, click **SQL Editor** (left sidebar)
2. Click **New query**
3. Open `supabase/schema.sql` from this project
4. **Copy the entire file** and paste it into the SQL editor
5. Click **Run** (green button)
6. You should see: _"Success. No rows returned"_
7. Click **Table Editor** to verify tables were created (doctors, users, appointments, etc.)

### 1.3 Get Your Connection String
1. Go to **Settings** (gear icon) → **Database**
2. Scroll to **Connection string** section
3. Select **URI** tab
4. Copy the connection string — looks like:
   ```
   postgresql://postgres:[YOUR-PASSWORD]@db.xxxxxxxxxxxx.supabase.co:5432/postgres
   ```
5. **Replace `[YOUR-PASSWORD]`** with the password you set in step 1.1
6. **Save this string** — you'll need it in Step 2

---

## STEP 2 — Deploy Backend to Render (Free Hosting)

### 2.1 Push to GitHub First
You need your code on GitHub for Render to deploy it.

```bash
# In the android_app folder
git init
git add .
git commit -m "Initial MediConnectUG commit"
```

1. Go to **https://github.com/new**
2. Create a new repository called `mediconnectug`
3. Make it **Private** (recommended for medical app)
4. Run:
   ```bash
   git remote add origin https://github.com/YOUR-USERNAME/mediconnectug.git
   git push -u origin main
   ```

### 2.2 Create Render Web Service
1. Go to **https://render.com**
2. Sign up with GitHub
3. Click **New** → **Web Service**
4. Connect your GitHub repository `mediconnectug`
5. Configure:
   - **Name:** `mediconnectug-api`
   - **Root Directory:** `backend`
   - **Runtime:** `Node`
   - **Build Command:** `npm install`
   - **Start Command:** `node server.js`
   - **Instance Type:** `Free`
6. Click **Advanced** → **Add Environment Variable**:

   | Key | Value |
   |-----|-------|
   | `DATABASE_URL` | *(paste your Supabase connection string from Step 1.3)* |
   | `JWT_SECRET` | *(any long random string, e.g. `mediconnectug_prod_xk9z2p7q_2026`)* |

7. Click **Create Web Service**
8. Wait 3–5 minutes for first deployment
9. Your API URL will be: `https://mediconnectug-api.onrender.com`

### 2.3 Test Your API
Open this URL in your browser:
```
https://mediconnectug-api.onrender.com/
```
You should see:
```json
{"status": "MediConnectUG API is live 🚀", "version": "1.0.0"}
```

---

## STEP 3 — Update the Android App

`ApiService.kt` is already updated to use the Render URL:
```kotlin
private const val BASE_URL = "https://mediconnectug-api.onrender.com/"
```

If your Render URL is different, update it there.

### 3.1 Allow HTTPS in AndroidManifest
Make sure your `AndroidManifest.xml` does NOT have `android:usesCleartextTraffic="true"`
since you're now using HTTPS. Remove it or set it to `false`.

---

## STEP 4 — Build Signed APK

### 4.1 Generate a Keystore (one-time)
In Android Studio:
1. **Build** → **Generate Signed Bundle / APK**
2. Select **APK** → Next
3. Click **Create new...** keystore
4. Fill in all fields — **save the keystore file and passwords securely**
5. Select **release** build variant
6. Click **Finish**

APK will be at:
```
app/release/app-release.apk
```

### 4.2 Test the signed APK
Install it on a real Android device:
```bash
adb install app/release/app-release.apk
```

---

## STEP 5 — Publish to Google Play Store ($25 one-time)

### 5.1 Create Developer Account
1. Go to **https://play.google.com/console**
2. Pay the **$25 one-time** registration fee
3. Complete account setup (takes 1–2 days for review)

### 5.2 Create App Listing
1. Click **Create app**
2. Fill in:
   - **App name:** MediConnectUG
   - **Default language:** English (Uganda)
   - **App/game:** App
   - **Free/paid:** Free
3. Complete all required sections:
   - **Store listing** (description, screenshots, feature graphic)
   - **Content rating** (medical apps = PEGI 3 or Everyone)
   - **Privacy policy URL** (required — create a simple one)
   - **Data safety** form

### 5.3 Upload APK / AAB
1. Go to **Production** → **Create new release**
2. Upload your `app-release.apk` (or `.aab` for better compression)
3. Add release notes
4. Click **Review release** → **Start rollout to production**

**Review time:** Google typically reviews in 1–7 days for new apps.

---

## STEP 6 — Optional: Firebase App Distribution (Beta Testing First)

Before Google Play, test with real users for free:

1. Go to **https://console.firebase.google.com**
2. Add your app (use your package name `com.healthbridge`)
3. Go to **App Distribution**
4. Upload your APK
5. Add tester emails
6. Click **Distribute**

Testers get a link to download your app on their phones immediately.

---

## IMPORTANT NOTES

### ⚠️ Render Free Tier — Cold Starts
The free Render tier **spins down after 15 minutes of inactivity**.
- First request after sleep takes ~30 seconds to wake up
- Subsequent requests are instant

**Solution:** The app already handles API failures gracefully by falling back to local data.
For production at scale, upgrade to Render Starter ($7/month) for always-on.

### 🔒 Security Checklist Before Launch
- [ ] Change `JWT_SECRET` to a strong random string in Render
- [ ] Enable Row Level Security in Supabase (optional but recommended)
- [ ] Set up a Privacy Policy page (required by Google Play)
- [ ] Remove any test credentials from the code
- [ ] Test login/register/appointments with real data

### 📊 Free Tier Limits Summary

| Service | Limit | When you'll hit it |
|---------|-------|-------------------|
| Supabase DB | 500 MB | ~50,000 users with full data |
| Supabase bandwidth | 2 GB/month | ~10,000 daily active users |
| Render (API) | 750 hrs/month | Always-on for ~31 days |
| Firebase Auth | Unlimited | Never |
| Firebase FCM | Unlimited | Never |
| Google Play | $25 once | Never again |

---

## QUICK REFERENCE — Your URLs

| Service | URL |
|---------|-----|
| API (Render) | `https://mediconnectug-api.onrender.com` |
| Database (Supabase) | `https://app.supabase.com/project/YOUR-REF` |
| Google Play Console | `https://play.google.com/console` |
| Firebase Console | `https://console.firebase.google.com` |

---

*Generated for MediConnectUG — May 2026*

