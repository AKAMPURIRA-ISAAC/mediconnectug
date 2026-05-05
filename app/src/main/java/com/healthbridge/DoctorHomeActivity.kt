package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.healthbridge.adapters.AppointmentAdapter
import com.healthbridge.adapters.AppointmentItem
import com.healthbridge.adapters.DoctorChatSessionsAdapter
import com.healthbridge.network.ApiClient
import com.healthbridge.network.Appointment
import kotlinx.coroutines.launch

class DoctorHomeActivity : AppCompatActivity() {

    private lateinit var tvDoctorName: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var tabLayout: TabLayout

    // Chats tab
    private lateinit var layoutChatsTab: LinearLayout
    private lateinit var rvChatSessions: RecyclerView
    private lateinit var chatAdapter: DoctorChatSessionsAdapter

    // Appointments tab
    private lateinit var layoutAppointmentsTab: LinearLayout
    private lateinit var tabAppointmentStatus: TabLayout
    private lateinit var rvAppointments: RecyclerView
    private lateinit var emptyAppointments: LinearLayout
    private lateinit var appointmentAdapter: AppointmentAdapter

    private var allAppointments: List<AppointmentItem> = emptyList()
    private val appointmentStatuses = listOf("upcoming", "past", "cancelled")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_home)

        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val doctorName = prefs.getString("userName", "Doctor") ?: "Doctor"

        // Initialize views
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)
        tabLayout = findViewById(R.id.tabLayout)

        layoutChatsTab = findViewById(R.id.layoutChatsTab)
        rvChatSessions = findViewById(R.id.rvChatSessions)

        layoutAppointmentsTab = findViewById(R.id.layoutAppointmentsTab)
        tabAppointmentStatus = findViewById(R.id.tabAppointmentStatus)
        rvAppointments = findViewById(R.id.rvAppointments)
        emptyAppointments = findViewById(R.id.emptyAppointments)

        tvDoctorName.text = "Dr. $doctorName"
        tvOnlineStatus.text = "🟢 Online"

        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            showLogoutDialog()
        }

        setupMainTabs()
        setupChatSessions()
        setupAppointments()
        loadActiveChatSessions()
    }

    private fun setupMainTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutChatsTab.visibility = View.VISIBLE
                        layoutAppointmentsTab.visibility = View.GONE
                        loadActiveChatSessions()
                    }
                    1 -> {
                        layoutChatsTab.visibility = View.GONE
                        layoutAppointmentsTab.visibility = View.VISIBLE
                        loadAppointments()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupChatSessions() {
        chatAdapter = DoctorChatSessionsAdapter { session ->
            val intent = Intent(this, DoctorChatActivity::class.java)
            intent.putExtra("session_id", session.id)
            intent.putExtra("patient_name", session.patientName)
            intent.putExtra("chief_complaint", session.chiefComplaint)
            intent.putExtra("urgency_level", session.urgency)
            startActivity(intent)
        }
        rvChatSessions.layoutManager = LinearLayoutManager(this)
        rvChatSessions.adapter = chatAdapter
    }

    private fun setupAppointments() {
        appointmentAdapter = AppointmentAdapter(
            onItemClick = { item ->
                // Open appointment details
                val intent = Intent(this, DoctorAppointmentDetailActivity::class.java)
                intent.putExtra("APPOINTMENT_ITEM", item)
                startActivity(intent)
            },
            onReschedule = { _ ->
                Toast.makeText(this, "Reschedule not available for doctors", Toast.LENGTH_SHORT).show()
            },
            onCancel = { item ->
                confirmCancelAppointment(item)
            }
        )
        rvAppointments.layoutManager = LinearLayoutManager(this)
        rvAppointments.adapter = appointmentAdapter

        // Appointment status tabs
        tabAppointmentStatus.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterAppointmentsByStatus(appointmentStatuses[tab?.position ?: 0])
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadActiveChatSessions() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getChatSessions()
                if (response.success && response.sessions != null) {
                    chatAdapter.submitList(response.sessions)
                    if (response.sessions.isEmpty()) {
                        Toast.makeText(this@DoctorHomeActivity, "No active chats", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(this@DoctorHomeActivity, "Offline mode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAppointments() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getDoctorAppointments()
                if (response.success && response.appointments != null) {
                    allAppointments = response.appointments.map { it.toAppointmentItem() }
                    filterAppointmentsByStatus(appointmentStatuses[tabAppointmentStatus.selectedTabPosition])
                } else {
                    allAppointments = emptyList()
                    filterAppointmentsByStatus("upcoming")
                }
            } catch (_: Exception) {
                Toast.makeText(this@DoctorHomeActivity, "Could not load appointments", Toast.LENGTH_SHORT).show()
                allAppointments = emptyList()
                filterAppointmentsByStatus("upcoming")
            }
        }
    }

    private fun Appointment.toAppointmentItem() = AppointmentItem(
        id = id.toString(),
        doctorName = doctorName,
        specialty = specialty,
        date = appointmentDate,
        time = appointmentTime,
        type = type,
        fee = fee,
        status = status
    )

    private fun filterAppointmentsByStatus(status: String) {
        val filtered = allAppointments.filter { it.status == status }
        appointmentAdapter.updateData(filtered)

        val isEmpty = filtered.isEmpty()
        emptyAppointments.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvAppointments.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun confirmCancelAppointment(item: AppointmentItem) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel the appointment with ${item.doctorName}?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelAppointment(item)
            }
            .setNegativeButton("Keep It", null)
            .show()
    }

    private fun cancelAppointment(item: AppointmentItem) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.cancelAppointment(item.id.toInt())
                if (response.success) {
                    Toast.makeText(this@DoctorHomeActivity, "Appointment cancelled", Toast.LENGTH_SHORT).show()
                    loadAppointments()
                } else {
                    Toast.makeText(this@DoctorHomeActivity, "Failed to cancel", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@DoctorHomeActivity, "Failed to cancel", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? You'll be marked as offline.")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                ApiClient.instance.logout()
            } catch (_: Exception) { }

            val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
            prefs.edit().clear().apply()
            ApiClient.authToken = null

            startActivity(Intent(this@DoctorHomeActivity, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh current tab
        when (tabLayout.selectedTabPosition) {
            0 -> loadActiveChatSessions()
            1 -> loadAppointments()
        }
    }
}
