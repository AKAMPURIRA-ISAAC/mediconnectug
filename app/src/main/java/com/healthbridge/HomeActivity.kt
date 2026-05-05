package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.healthbridge.adapters.DoctorItem
import com.healthbridge.data.database.HealthTipEntity
import com.healthbridge.data.repository.RepositoryFactory
import com.healthbridge.util.NetworkUtils
import com.healthbridge.util.PermissionManager
import com.healthbridge.util.UpdateChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : BaseActivity() {

    private val avatarColors = listOf(
        "#1565C0", "#7B1FA2", "#00796B", "#E64A19", "#1976D2", "#388E3C"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val userName = prefs.getString("userName", null)
            ?: prefs.getString("userEmail", "User")?.substringBefore('@')
                ?.replaceFirstChar { it.uppercase() }
            ?: "User"

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvDate    = findViewById<TextView>(R.id.tvDate)
        val tvAvatar  = findViewById<TextView>(R.id.tvAvatar)

        tvWelcome.text = "Hello, $userName!"
        tvDate.text = SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date())
        val initials = userName.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
        tvAvatar.text = initials.ifEmpty { "U" }

        // ── Request permissions on first launch ───────────────────────────────
        // Ask for phone, location, notifications in one go so users grant them
        // before they need them (better UX than surprising them mid-flow).
        val permPrefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        if (permPrefs.getBoolean("permissions_requested", false).not()) {
            permPrefs.edit().putBoolean("permissions_requested", true).apply()
            PermissionManager.requestOnboardingPermissions(this)
        }

        // Show server warm-up notice on first use (Render free tier cold start)
        val isFirstLoad = prefs.getBoolean("firstLoad", true)
        NetworkUtils.warnIfFirstLoad(this, isFirstLoad)
        if (isFirstLoad) prefs.edit().putBoolean("firstLoad", false).apply()

        // ── Auto-update check ──────────────────────────────────────────────────
        // Runs silently in the background; shows a dialog only when a newer
        // GitHub release is available. Checks at most once per day.
        lifecycleScope.launch {
            delay(3000) // wait 3 s so the home screen fully loads first
            UpdateChecker.checkForUpdate(this@HomeActivity)
        }

        tvAvatar.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }

        // Quick action buttons
        findViewById<LinearLayout>(R.id.btnFindDoctors).setOnClickListener {
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnAppointments).setOnClickListener {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnEmergency).setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnChat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnPharmacy).setOnClickListener {
            startActivity(Intent(this, PharmacyActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnHospital).setOnClickListener {
            startActivity(Intent(this, HospitalActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        // Header icons
        findViewById<ImageView>(R.id.ivNotification).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Search bar click
        val tvSearchHint = findViewById<TextView>(R.id.tvSearchHint)
        tvSearchHint.setOnClickListener {
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }

        // View All links
        findViewById<TextView>(R.id.tvViewAllDoctors).setOnClickListener {
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }
        findViewById<TextView>(R.id.tvViewAllAppts).setOnClickListener {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }

        // Bottom navigation
        findViewById<TextView>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }
        findViewById<TextView>(R.id.navSchedule).setOnClickListener {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }
        findViewById<TextView>(R.id.navChat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        loadRecommendedDoctors()
        loadUpcomingAppointments()
        loadHealthTips()

        // View All tips → Chat (for health tip interaction)
        findViewById<TextView>(R.id.tvViewAllTips).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
    }

    private fun loadRecommendedDoctors() {
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.doctorRepository.getDoctors()
                result.onSuccess { doctorEntities ->
                    val doctors = doctorEntities.take(5).map { d ->
                        DoctorItem(
                            id = d.id.toString(),
                            name = d.name,
                            specialty = d.specialty,
                            rating = d.rating,
                            reviewCount = d.reviewCount,
                            distance = "Kampala, Uganda",
                            fee = d.consultationFee,
                            isOnline = true
                        )
                    }
                    populateDoctorCards(doctors)
                }
                result.onFailure { populateDoctorCards(getSampleDoctors()) }
            } catch (_: Exception) {
                populateDoctorCards(getSampleDoctors())
            }
        }
    }

    private fun populateDoctorCards(doctors: List<DoctorItem>) {
        val container = findViewById<LinearLayout>(R.id.llRecommendedDoctors)
        container.removeAllViews()
        doctors.forEach { doctor ->
            val card = createDoctorCard(doctor)
            container.addView(card)
        }
    }

    private fun createDoctorCard(doctor: DoctorItem): CardView {
        val ctx = this
        val card = CardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(220.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 12.dpToPx()
            }
            radius = 16.dpToPx().toFloat()
            cardElevation = 4.dpToPx().toFloat()
            setCardBackgroundColor(0xFFFFFFFF.toInt())
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        // Avatar circle
        val avatarText = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(60.dpToPx(), 60.dpToPx())
            val nameWords = doctor.name.removePrefix("Dr. ").split(" ")
            text = nameWords.filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            val colorIdx = doctor.name.length % avatarColors.size
            val color = android.graphics.Color.parseColor(avatarColors[colorIdx])
            val bg = getDrawable(R.drawable.avatar_circle)?.mutate()
            bg?.setTint(color)
            background = bg
        }
        inner.addView(avatarText)

        // Doctor name
        val nameView = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 10.dpToPx() }
            text = doctor.name
            textSize = 14f
            setTextColor(0xFF1A1A2E.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        inner.addView(nameView)

        // Specialty
        val specView = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 2.dpToPx() }
            text = doctor.specialty
            textSize = 12f
            setTextColor(0xFF888888.toInt())
        }
        inner.addView(specView)

        // Rating row
        val ratingRow = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 6.dpToPx() }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val starText = TextView(ctx).apply {
            text = "⭐ ${doctor.rating}"
            textSize = 12f
            setTextColor(0xFF333333.toInt())
        }
        val reviewText = TextView(ctx).apply {
            text = "  (${doctor.reviewCount})"
            textSize = 11f
            setTextColor(0xFF888888.toInt())
        }
        ratingRow.addView(starText)
        ratingRow.addView(reviewText)
        inner.addView(ratingRow)

        // Distance + Fee row
        val infoRow = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 4.dpToPx() }
            orientation = LinearLayout.HORIZONTAL
        }
        val distText = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "📍 ${doctor.distance}"
            textSize = 11f
            setTextColor(0xFF888888.toInt())
        }
        val feeText = TextView(ctx).apply {
            text = "UGX ${String.format("%,d", doctor.fee)}"
            textSize = 11f
            setTextColor(0xFF1565C0.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        infoRow.addView(distText)
        infoRow.addView(feeText)
        inner.addView(infoRow)

        // Book Now button
        val bookBtn = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 36.dpToPx()).apply { topMargin = 12.dpToPx() }
            text = "Book Now"
            textSize = 13f
            setTextColor(0xFF1565C0.toInt())
            gravity = android.view.Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            val bg = getDrawable(R.drawable.search_background)?.mutate()
            bg?.setTint(0xFFE3F2FD.toInt())
            background = bg
        }
        bookBtn.setOnClickListener {
            val intent = Intent(ctx, DoctorProfileActivity::class.java).apply {
                putExtra("DOCTOR_NAME", doctor.name)
                putExtra("DOCTOR_SPECIALTY", doctor.specialty)
                putExtra("DOCTOR_RATING", doctor.rating)
                putExtra("DOCTOR_FEE", doctor.fee)
            }
            startActivity(intent)
        }
        inner.addView(bookBtn)

        card.addView(inner)
        card.setOnClickListener {
            val intent = Intent(ctx, DoctorProfileActivity::class.java).apply {
                putExtra("DOCTOR_NAME", doctor.name)
                putExtra("DOCTOR_SPECIALTY", doctor.specialty)
                putExtra("DOCTOR_FEE", doctor.fee)
            }
            startActivity(intent)
        }
        return card
    }

    private fun loadUpcomingAppointments() {
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.appointmentRepository.getAppointments()
                result.onSuccess { appts ->
                    populateAppointmentCards(appts.take(2).map { a ->
                        Triple(a.doctorName, "${a.appointmentDate} • ${a.appointmentTime}", a.status)
                    })
                }
                result.onFailure { showSampleAppointments() }
            } catch (_: Exception) {
                showSampleAppointments()
            }
        }
    }

    private fun showSampleAppointments() {
        populateAppointmentCards(emptyList()) // show empty state instead of fake data
    }

    private fun loadHealthTips() {
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.healthTipsRepository.getHealthTips()
                result.onSuccess { tips -> populateHealthTipCards(tips.take(6)) }
                result.onFailure { /* tips are always available locally */ }
            } catch (_: Exception) { /* ignore */ }
        }
    }

    private fun populateHealthTipCards(tips: List<HealthTipEntity>) {
        val container = findViewById<LinearLayout>(R.id.llHealthTips)
        container.removeAllViews()
        val tipBgColors = listOf(
            Pair(0xFFE3F2FD.toInt(), 0xFF1565C0.toInt()),  // blue
            Pair(0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt()),  // green
            Pair(0xFFFFF8E1.toInt(), 0xFFF57F17.toInt()),  // amber
            Pair(0xFFF3E5F5.toInt(), 0xFF6A1B9A.toInt()),  // purple
            Pair(0xFFE0F2F1.toInt(), 0xFF00796B.toInt()),  // teal
            Pair(0xFFFFEBEE.toInt(), 0xFFC62828.toInt())   // red
        )
        tips.forEachIndexed { idx, tip ->
            val (bgColor, accentColor) = tipBgColors[idx % tipBgColors.size]
            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(160.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = 12.dpToPx()
                }
                radius = 16.dpToPx().toFloat()
                cardElevation = 3.dpToPx().toFloat()
                setCardBackgroundColor(bgColor)
            }
            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14.dpToPx(), 14.dpToPx(), 14.dpToPx(), 14.dpToPx())
            }
            // Icon circle
            val iconView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(44.dpToPx(), 44.dpToPx()).apply { bottomMargin = 10.dpToPx() }
                text = tip.icon
                textSize = 22f
                gravity = android.view.Gravity.CENTER
                background = getDrawable(R.drawable.avatar_circle)?.mutate()?.also { it.setTint(accentColor) }
            }
            inner.addView(iconView)
            // Title
            inner.addView(TextView(this).apply {
                text = tip.title.removeSuffix(" ${tip.icon}").trim()
                textSize = 13f
                setTextColor(0xFF1A1A2E.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                maxLines = 2
            })
            // Category tag
            inner.addView(TextView(this).apply {
                text = tip.category.replaceFirstChar { it.uppercase() }
                textSize = 10f
                setTextColor(accentColor)
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 4.dpToPx()
                }
            })
            card.addView(inner)
            card.setOnClickListener {
                startActivity(Intent(this, ChatActivity::class.java))
            }
            container.addView(card)
        }
    }


    private fun populateAppointmentCards(appointments: List<Triple<String, String, String>>) {
        val container = findViewById<LinearLayout>(R.id.llAppointments)
        container.removeAllViews()
        if (appointments.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No upcoming appointments.\nTap 'Book' to schedule one."
                textSize = 13f
                setTextColor(0xFF888888.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16.dpToPx(), 0, 16.dpToPx())
            }
            container.addView(emptyView)
            return
        }
        appointments.forEach { (doctor, dateTime, status) ->
            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 12.dpToPx()
                }
                radius = 16.dpToPx().toFloat()
                cardElevation = 2.dpToPx().toFloat()
                setCardBackgroundColor(0xFFFFFFFF.toInt())
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(16.dpToPx(), 14.dpToPx(), 16.dpToPx(), 14.dpToPx())
            }
            val iconCard = CardView(this).apply {
                val size = 44.dpToPx()
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = 14.dpToPx() }
                radius = (size / 2).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(0xFF7B1FA2.toInt())
            }
            val iconText = TextView(this).apply {
                text = "📅"
                textSize = 20f
                gravity = android.view.Gravity.CENTER
            }
            iconCard.addView(iconText)
            row.addView(iconCard)
            val info = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                orientation = LinearLayout.VERTICAL
            }
            info.addView(TextView(this).apply {
                text = doctor
                textSize = 14f
                setTextColor(0xFF1A1A2E.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
            })
            info.addView(TextView(this).apply {
                text = dateTime
                textSize = 12f
                setTextColor(0xFF888888.toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 2.dpToPx() }
            })
            row.addView(info)
            val statusColor = if (status.lowercase() == "confirmed") 0xFF2E7D32.toInt() else 0xFFFF8F00.toInt()
            val statusBg = if (status.lowercase() == "confirmed") 0xFFE8F5E9.toInt() else 0xFFFFF8E1.toInt()
            val badge = TextView(this).apply {
                text = status
                textSize = 11f
                setTextColor(statusColor)
                setPadding(10.dpToPx(), 4.dpToPx(), 10.dpToPx(), 4.dpToPx())
                val bg = getDrawable(R.drawable.badge_green)?.mutate()
                bg?.setTint(statusBg)
                background = bg
            }
            row.addView(badge)
            card.addView(row)
            card.setOnClickListener { startActivity(Intent(this, MyAppointmentsActivity::class.java)) }
            container.addView(card)
        }
    }

    private fun getSampleDoctors(): List<DoctorItem> = listOf(
        DoctorItem("1", "Dr. Sarah Nakigozi",  "Pediatrician",    4.9, 120, "Mulago, Kampala",    45000, true),
        DoctorItem("2", "Dr. Moses Ssali",     "Cardiologist",    4.8, 203, "Nakasero, Kampala",  60000, true),
        DoctorItem("3", "Dr. Grace Namukwaya", "Neurologist",     4.7, 156, "Mulago, Kampala",    70000, false),
        DoctorItem("4", "Dr. Patrick Odeke",   "General Practitioner", 4.6, 89, "IHK, Kampala", 35000, true),
        DoctorItem("5", "Dr. Amina Nakato",    "Dermatologist",   4.8, 112, "Aga Khan, Kampala", 55000, true)
    )


    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.handleResult(requestCode, permissions, grantResults,
            onGranted = { /* permissions granted — nothing extra to do here */ },
            onDenied = { /* user denied — they'll be prompted again just-in-time when feature is used */ }
        )
    }
}
