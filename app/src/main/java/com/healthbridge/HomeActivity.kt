package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

class HomeActivity : AppCompatActivity() {

    private val avatarColors = listOf(
        "#1565C0", "#7B1FA2", "#00796B", "#E64A19", "#1976D2", "#388E3C"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Session timeout check
        checkSessionTimeout()

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

        // Request permissions
        val permPrefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        if (permPrefs.getBoolean("permissions_requested", false).not()) {
            permPrefs.edit().putBoolean("permissions_requested", true).apply()
            PermissionManager.requestOnboardingPermissions(this)
        }

        NetworkUtils.warnIfFirstLoad(this, prefs.getBoolean("firstLoad", true))
        if (prefs.getBoolean("firstLoad", true)) prefs.edit().putBoolean("firstLoad", false).apply()

        lifecycleScope.launch {
            delay(3000)
            UpdateChecker.checkForUpdate(this@HomeActivity)
        }

        tvAvatar.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }

        // Quick actions
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
        
        // NEW: Direct Messages from Doctors
        findViewById<LinearLayout>(R.id.btnInbox)?.setOnClickListener {
            startActivity(Intent(this, PatientChatListActivity::class.java))
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
            logout()
        }

        // Search and View All
        findViewById<ImageView>(R.id.ivNotification).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<TextView>(R.id.tvSearchHint).setOnClickListener {
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }
        findViewById<TextView>(R.id.tvViewAllDoctors).setOnClickListener {
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }
        findViewById<TextView>(R.id.tvViewAllAppts).setOnClickListener {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }

        // Bottom nav
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
    }

    private fun checkSessionTimeout() {
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val lastActive = prefs.getLong("last_active_time", 0L)
        val currentTime = System.currentTimeMillis()
        
        // 15 minutes = 15 * 60 * 1000 milliseconds
        if (lastActive != 0L && (currentTime - lastActive) > 15 * 60 * 1000) {
            // Session expired, require login
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("session_expired", true)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        // Update last active time on any user interaction
        getSharedPreferences("HealthBridge", MODE_PRIVATE).edit()
            .putLong("last_active_time", System.currentTimeMillis())
            .apply()
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
                            distance = "Uganda",
                            fee = d.consultationFee,
                            isOnline = d.isOnline
                        )
                    }
                    populateDoctorCards(doctors)
                }
                result.onFailure { populateDoctorCards(emptyList()) } // Strict: No sample data
            } catch (_: Exception) {
                populateDoctorCards(emptyList())
            }
        }
    }

    private fun populateDoctorCards(doctors: List<DoctorItem>) {
        val container = findViewById<LinearLayout>(R.id.llRecommendedDoctors)
        container.removeAllViews()
        if (doctors.isEmpty()) {
            val emptyMsg = TextView(this).apply {
                text = "Looking for available doctors..."
                setPadding(16, 16, 16, 16)
            }
            container.addView(emptyMsg)
            return
        }
        doctors.forEach { doctor -> container.addView(createDoctorCard(doctor)) }
    }

    private fun createDoctorCard(doctor: DoctorItem): CardView {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(220.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 12.dpToPx()
            }
            radius = 16.dpToPx().toFloat()
            cardElevation = 4.dpToPx().toFloat()
            setCardBackgroundColor(0xFFFFFFFF.toInt())
        }

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        val avatarText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(60.dpToPx(), 60.dpToPx())
            val nameWords = doctor.name.removePrefix("Dr. ").split(" ")
            text = nameWords.filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            val colorIdx = doctor.name.length % avatarColors.size
            background = getDrawable(R.drawable.avatar_circle)?.mutate()?.apply {
                setTint(android.graphics.Color.parseColor(avatarColors[colorIdx]))
            }
        }
        inner.addView(avatarText)

        inner.addView(TextView(this).apply {
            text = doctor.name
            textSize = 14f
            setTextColor(0xFF1A1A2E.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 10.dpToPx() }
        })

        inner.addView(TextView(this).apply {
            text = doctor.specialty
            textSize = 12f
            setTextColor(0xFF888888.toInt())
        })

        val bookBtn = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 36.dpToPx()).apply { topMargin = 12.dpToPx() }
            text = "Consult"
            textSize = 13f
            setTextColor(0xFF1565C0.toInt())
            gravity = android.view.Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = getDrawable(R.drawable.search_background)?.mutate()?.apply { setTint(0xFFE3F2FD.toInt()) }
        }
        bookBtn.setOnClickListener {
            startActivity(Intent(this, DoctorProfileActivity::class.java).apply {
                putExtra("DOCTOR_NAME", doctor.name)
                putExtra("DOCTOR_ID", doctor.id)
            })
        }
        inner.addView(bookBtn)

        card.addView(inner)
        card.setOnClickListener {
            startActivity(Intent(this, DoctorProfileActivity::class.java).apply {
                putExtra("DOCTOR_NAME", doctor.name)
                putExtra("DOCTOR_ID", doctor.id)
            })
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
                result.onFailure { populateAppointmentCards(emptyList()) }
            } catch (_: Exception) {
                populateAppointmentCards(emptyList())
            }
        }
    }

    private fun loadHealthTips() {
        lifecycleScope.launch {
            RepositoryFactory.healthTipsRepository.getHealthTips().onSuccess { tips ->
                populateHealthTipCards(tips.take(6))
            }
        }
    }

    private fun populateHealthTipCards(tips: List<HealthTipEntity>) {
        val container = findViewById<LinearLayout>(R.id.llHealthTips)
        container.removeAllViews()
        tips.forEach { tip ->
            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(160.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 12.dpToPx() }
                radius = 16.dpToPx().toFloat()
                setCardBackgroundColor(0xFFE3F2FD.toInt())
                val inner = LinearLayout(this@HomeActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(14.dpToPx(), 14.dpToPx(), 14.dpToPx(), 14.dpToPx())
                    addView(TextView(this@HomeActivity).apply { text = tip.icon; textSize = 22f })
                    addView(TextView(this@HomeActivity).apply { text = tip.title; textSize = 13f; setTypeface(null, android.graphics.Typeface.BOLD) })
                }
                addView(inner)
                setOnClickListener { startActivity(Intent(this@HomeActivity, ChatActivity::class.java)) }
            }
            container.addView(card)
        }
    }

    private fun populateAppointmentCards(appointments: List<Triple<String, String, String>>) {
        val container = findViewById<LinearLayout>(R.id.llAppointments)
        container.removeAllViews()
        if (appointments.isEmpty()) {
            container.addView(TextView(this).apply { text = "No upcoming appointments." })
            return
        }
        appointments.forEach { (doctor, dateTime, status) ->
            // Simpler layout for brief view
            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8.dpToPx() }
                radius = 12.dpToPx().toFloat()
                val inner = LinearLayout(this@HomeActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(16, 16, 16, 16)
                    addView(TextView(this@HomeActivity).apply { text = "👨‍⚕️ $doctor\n$dateTime"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                    addView(TextView(this@HomeActivity).apply { text = status.uppercase(); textSize = 10f; setTextColor(0xFF1565C0.toInt()) })
                }
                addView(inner)
                setOnClickListener { startActivity(Intent(this@HomeActivity, MyAppointmentsActivity::class.java)) }
            }
            container.addView(card)
        }
    }

    private fun logout() {
        getSharedPreferences("HealthBridge", MODE_PRIVATE).edit().clear().apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
