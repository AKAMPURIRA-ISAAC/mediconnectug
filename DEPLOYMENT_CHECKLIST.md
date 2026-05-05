# ✅ Doctor Chat Fixes - Deployment & Testing Checklist

## Pre-Deployment Checklist

### Code Review
- [ ] Read `DOCTOR_CHAT_FIXES.md` for complete details
- [ ] Review changes in:
  - `ApiService.kt` - new endpoint `getDoctorAppointments()`
  - `DoctorHomeActivity.kt` - uses new endpoint
  - `server.js` - fixed queries and new route
  - `schema.sql` - added three new columns

### Dependencies & Configuration
- [ ] Node.js/backend environment ready
- [ ] Android Studio with latest Gradle
- [ ] Supabase access with SQL editor
- [ ] Git access to push changes

### Team Notified
- [ ] DevOps informed about deployment
- [ ] QA team ready for testing
- [ ] Support team knows about changes
- [ ] Documentation available to team

---

## Deployment Checklist (Strict Order!)

### STEP 1: Database Migration (CRITICAL - FIRST!)

**Location:** Supabase SQL Editor
**Time:** ~5 seconds
**Reversible:** Yes

```
BEFORE: Run this in Supabase SQL Editor
[ ] SELECT * FROM doctors LIMIT 1;
    (Check current columns)

[ ] ALTER TABLE doctors 
    ADD COLUMN IF NOT EXISTS user_id INT REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS hospital VARCHAR(200),
    ADD COLUMN IF NOT EXISTS license_number VARCHAR(100);

AFTER: Verify it worked
[ ] SELECT column_name FROM information_schema.columns 
    WHERE table_name='doctors'
    (Should see: user_id, hospital, license_number)
```

### STEP 2: Backend Deployment

**Location:** Terminal
**Time:** ~2-3 minutes (auto-deploy on Render)
**Reversible:** Yes (revert commits)

```
[ ] cd backend
[ ] git status  (verify clean working directory)
[ ] git pull    (get latest changes)
[ ] npm install (if needed)
[ ] node -c server.js  (syntax check)
[ ] git add -A
[ ] git commit -m "Fix: Doctor appointments, chat messages, schema"
[ ] git push
    (Watch Render dashboard for deployment)
[ ] Backend URL reachable: https://mediconnectug.onrender.com/
```

**Verify Backend Deployed:**
```bash
curl https://mediconnectug.onrender.com/
(Should show API version and status)
```

### STEP 3: Android App Rebuild

**Location:** Android Studio / Terminal
**Time:** ~5-10 minutes
**Reversible:** Yes (rebuild from previous tag)

```
[ ] cd android_app
[ ] ./gradlew clean
[ ] ./gradlew build
[ ] Check for errors (should be none)
[ ] Generate APK: ./gradlew assembleDebug
[ ] APK location: app/build/outputs/apk/debug/app-debug.apk
[ ] Size reasonable: ~8-12 MB
```

### STEP 4: Manual Testing (5 Minutes Each)

#### Test A: Doctor Registration & Login
```
[ ] Open app
[ ] Register new doctor:
    - Name: "Dr. Test"
    - Email: "drtest@hospital.com"
    - Password: "Test123!"
    - Specialty: "General Practice"
    - Hospital: "Test Hospital"
    - License: "LIC123456"
[ ] Login with doctor credentials
[ ] Check: JWT token has doctor_id (use debugger)
[ ] No crashes
```

#### Test B: Doctor Views Appointments
```
[ ] (Book an appointment as patient first)
[ ] Login as doctor
[ ] Click Appointments tab
[ ] See list of patient appointments
[ ] Check fields: patient name, date, time, status
[ ] Filter by "upcoming", "past", "cancelled"
[ ] No errors
```

#### Test C: Doctor Sees Chat Sessions
```
[ ] (Patient starts chat session before this)
[ ] Login as doctor
[ ] Click Chats tab
[ ] See patient chat sessions
[ ] Chat shows: patient name, chief complaint, urgency
[ ] Order: newest first
[ ] No errors
```

#### Test D: Doctor-Patient Chat Exchange
```
[ ] Patient: Open chat with doctor
[ ] Patient: Send message "Testing chat"
[ ] Doctor: Refresh/reopen app
[ ] Doctor: See patient message
[ ] Doctor: Send reply "Chat working!"
[ ] Patient: Refresh/open chat
[ ] Patient: See doctor's reply
[ ] Message appears on correct side
[ ] No errors
```

#### Test E: Message Content Verification
```
[ ] In Supabase SQL Editor, run:
    SELECT sender_type, message, created_at 
    FROM direct_messages 
    ORDER BY created_at DESC LIMIT 5;
[ ] Verify sender_type is "patient" or "doctor"
[ ] Verify message content is correct
[ ] Verify timestamp is recent
[ ] No NULL values
```

---

## Post-Deployment Verification

### Automated Checks
```
[ ] Backend responding: curl -s https://mediconnectug.onrender.com/ | grep status
[ ] Database migration successful: Check Supabase
[ ] APK builds without errors: gradle build success
[ ] No sensitive data in logs
```

### Unit Test Results
```
[ ] All authentication endpoints working
[ ] Appointment queries return correct data  
[ ] Chat session queries return correct data
[ ] Message insertion succeeds
[ ] Timestamp updates work
```

### Integration Tests
```
[ ] Full doctor registration flow works
[ ] Full patient-doctor chat flow works
[ ] Appointment booking → visibility flow works
[ ] No race conditions
[ ] No data loss
```

---

## Production Rollout Checklist

### Day 1: Limited Release (2 hours)
- [ ] Deploy to 1 test doctor account
- [ ] Monitor: server logs, database, app crashes
- [ ] Verify: chats, appointments, messages
- [ ] Get feedback from test user

### Day 2: Expanded Release (1 hour)
- [ ] Deploy to 10% of doctor users
- [ ] Monitor: errors, performance, data
- [ ] Collect feedback
- [ ] No critical issues? Continue

### Day 3: Full Release
- [ ] Deploy to all doctor users
- [ ] Monitor: dashboard, logs, support tickets
- [ ] Performance metrics: response times, error rates
- [ ] Celebrate! 🎉

---

## Rollback Procedure (If Needed)

### Quick Rollback
```
DATABASE:
  [ ] Run Supabase backup restore (if available)
  OR
  [ ] ALTER TABLE doctors DROP COLUMN user_id;
  [ ] ALTER TABLE doctors DROP COLUMN hospital;
  [ ] ALTER TABLE doctors DROP COLUMN license_number;

BACKEND:
  [ ] git revert <commit-hash>
  [ ] git push
  (Auto-deploys revert)

APP:
  [ ] Rebuild from previous tag
  [ ] Distribute old APK
```

### Full Rollback Process
```
1. Database: Restore from backup (ask DevOps)
2. Backend: Revert commits and push
3. App: Send push notification to re-download old APK
4. Verify: All services operational with old data
5. Post-incident: Review what went wrong
```

---

## Performance Monitoring

### Before vs After Metrics

**To capture:**
- [ ] Doctor login time
- [ ] Appointments load time
- [ ] Chat list load time
- [ ] Message send time
- [ ] Message receive time (polling)
- [ ] Error rate

**Expected:** ~5-10% improvement or no regression

---

## Issue Tracking

### Track any bugs found:
```
Bug #1: [DESCRIPTION]
  [ ] Reproduced: Yes/No
  [ ] Severity: Low/Medium/High/Critical
  [ ] Root cause: [ANALYSIS]
  [ ] Fix: [SOLUTION or WORKAROUND]
  [ ] Status: Open/In Progress/Resolved

Bug #2: [DESCRIPTION]
  [ ] Reproduced: Yes/No
  [ ] Severity: Low/Medium/High/Critical
  [ ] Root cause: [ANALYSIS]
  [ ] Fix: [SOLUTION or WORKAROUND]
  [ ] Status: Open/In Progress/Resolved
```

---

## Communication Checklist

### Before Deployment
- [ ] Team meeting: explain changes
- [ ] Documentation: send to team
- [ ] Changelog: update release notes
- [ ] Support: briefing on new features

### During Deployment
- [ ] Update team: "Deploying now"
- [ ] Monitor: active watchdog
- [ ] Slack: real-time updates
- [ ] Be ready: quick fix if needed

### After Deployment
- [ ] Announcement: "Deployed successfully"
- [ ] Review: what went well
- [ ] Retrospect: what could improve
- [ ] Release notes: published

---

## Success Criteria Validation

**All Must Pass:**
- [ ] Doctor can register ✅ / ❌
- [ ] Doctor can login ✅ / ❌
- [ ] Doctor sees appointments ✅ / ❌
- [ ] Doctor sees chats ✅ / ❌
- [ ] Chat messages work both ways ✅ / ❌
- [ ] No major errors/crashes ✅ / ❌
- [ ] Performance acceptable ✅ / ❌
- [ ] Data integrity maintained ✅ / ❌

**Pass Rate:** __%  (must be 100% for production)

---

## Sign-Off

### Code Review
```
Reviewed by: ________________  Date: ________
Approved by: ________________  Date: ________
```

### QA Testing
```
Tested by:   ________________  Date: ________
Status:      ✅ PASS / ⚠️ CONDITIONAL / ❌ FAIL
Comments:    _______________________________
```

### Deployment
```
Deployed by: ________________  Date: ________
Time:        Start: ________  End: ________
Status:      ✅ Success / ⚠️ Issues / ❌ Rollback
```

### Business Approval
```
Approved by: ________________  Date: ________
Released to: Full Users / Beta / Internal Test
Notes:       _______________________________
```

---

## Post-Deployment Support

### First Week Monitoring
- [ ] Daily check: error logs
- [ ] Daily check: user feedback
- [ ] Daily check: performance metrics
- [ ] Daily check: data integrity

### Issue Escalation
- **Low:** Fix in next release
- **Medium:** Fix within 24 hours
- **High:** Fix immediately
- **Critical:** Rollback if cannot fix in 1 hour

### Knowledge Transfer
- [ ] Document any workarounds
- [ ] Update runbooks
- [ ] Train support team
- [ ] Create FAQ for users

---

## Final Checklist

Before marking as COMPLETE:

- [ ] All steps completed
- [ ] All tests passed
- [ ] No rollback needed
- [ ] Users informed
- [ ] Documentation updated
- [ ] Team trained
- [ ] Monitoring active
- [ ] Support ready
- [ ] Metrics captured
- [ ] Sign-offs obtained

---

## DEPLOYMENT STATUS

| Phase | Status | Date | Notes |
|-------|--------|------|-------|
| Code Review | ⏳ Pending | _____ | _________ |
| Database Migration | ⏳ Pending | _____ | _________ |
| Backend Deploy | ⏳ Pending | _____ | _________ |
| App Rebuild | ⏳ Pending | _____ | _________ |
| Testing | ⏳ Pending | _____ | _________ |
| Production Release | ⏳ Pending | _____ | _________ |

**Overall Status:** READY FOR DEPLOYMENT ✅

**Estimated Total Time:** 30-45 minutes

---

**Questions?** Refer to:
- Technical details: `DOCTOR_CHAT_FIXES.md`
- Flow diagrams: `DOCTOR_PATIENT_FLOW.md`
- Database info: `DATABASE_MIGRATION.md`

**Last Updated:** May 5, 2026  
**Version:** 1.0  
**Status:** Ready for Production

