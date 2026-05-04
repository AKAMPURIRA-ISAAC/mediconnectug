package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.healthbridge.adapters.AppointmentAdapter
import com.healthbridge.adapters.AppointmentItem
import com.healthbridge.data.database.AppointmentEntity
import com.healthbridge.data.repository.RepositoryFactory
import kotlinx.coroutines.launch

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: AppointmentAdapter

    private var allAppointments: List<AppointmentItem> = emptyList()
    private val tabStatuses = listOf("upcoming", "past", "cancelled")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        recycler    = findViewById(R.id.recyclerAppointments)
        emptyState  = findViewById(R.id.emptyState)
        tabLayout   = findViewById(R.id.tabLayout)

        // Notifications
        findViewById<ImageView>(R.id.ivNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // RecyclerView
        adapter = AppointmentAdapter(
            onItemClick = { item ->
                if (item.status == "upcoming") {
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("doctor_name", item.doctorName)
                    }
                    startActivity(intent)
                }
            },
            onReschedule = { item -> openReschedule(item) },
            onCancel     = { item -> confirmCancel(item) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Tabs
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterByStatus(tabStatuses[tab?.position ?: 0])
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // FAB + empty state button → Find Doctors
        val goFind = View.OnClickListener { startActivity(Intent(this, FindDoctorsActivity::class.java)) }
        findViewById<FloatingActionButton>(R.id.fabBook).setOnClickListener(goFind)
        findViewById<Button>(R.id.btnBookNow).setOnClickListener(goFind)

        setupBottomNav()
        loadAppointments()
    }

    private fun loadAppointments() {
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.appointmentRepository.getAppointments(forceRefresh = true)
                result.onSuccess { entities ->
                    allAppointments = entities.map { a -> a.toAppointmentItem() }
                }
                result.onFailure {
                    allAppointments = emptyList()
                }
            } catch (_: Exception) {
                allAppointments = emptyList()
            }
            filterByStatus(tabStatuses[tabLayout.selectedTabPosition])
        }
    }

    private fun AppointmentEntity.toAppointmentItem() = AppointmentItem(
        id         = id.toString(),
        doctorName = doctorName,
        specialty  = specialty,
        date       = appointmentDate,
        time       = appointmentTime,
        type       = type,
        fee        = fee,
        status     = status
    )

    private fun filterByStatus(status: String) {
        val filtered = allAppointments.filter { it.status == status }
        adapter.updateData(filtered)
        val isEmpty = filtered.isEmpty()
        emptyState.visibility  = if (isEmpty) View.VISIBLE else View.GONE
        recycler.visibility    = if (isEmpty) View.GONE    else View.VISIBLE
    }

    private fun openReschedule(item: AppointmentItem) {
        val intent = Intent(this, RescheduleActivity::class.java).apply {
            putExtra("APPOINTMENT_ID",   item.id)
            putExtra("DOCTOR_NAME",      item.doctorName)
            putExtra("DOCTOR_SPECIALTY", item.specialty)
            putExtra("DOCTOR_FEE",       item.fee)
            putExtra("CURRENT_DATE",     item.date)
            putExtra("CURRENT_TIME",     item.time)
            putExtra("CURRENT_TYPE",     item.type)
        }
        startActivity(intent)
    }

    private fun confirmCancel(item: AppointmentItem) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel your appointment with ${item.doctorName} on ${item.date} at ${item.time}?")
            .setPositiveButton("Yes, Cancel") { _, _ -> cancelAppointment(item) }
            .setNegativeButton("Keep It", null)
            .show()
    }

    private fun cancelAppointment(item: AppointmentItem) {
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.appointmentRepository.cancelAppointment(item.id.toInt())
                result.onSuccess {
                    Toast.makeText(this@MyAppointmentsActivity, "Appointment cancelled", Toast.LENGTH_SHORT).show()
                    loadAppointments()
                }
                result.onFailure { err ->
                    // Offline — update in memory
                    allAppointments = allAppointments.map { a ->
                        if (a.id == item.id) a.copy(status = "cancelled") else a
                    }
                    filterByStatus(tabStatuses[tabLayout.selectedTabPosition])
                    Toast.makeText(this@MyAppointmentsActivity, "Appointment cancelled (offline)", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                allAppointments = allAppointments.map { a ->
                    if (a.id == item.id) a.copy(status = "cancelled") else a
                }
                filterByStatus(tabStatuses[tabLayout.selectedTabPosition])
                Toast.makeText(this@MyAppointmentsActivity, "Appointment cancelled (offline)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNav() {
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java)); finish()
        }
        findViewById<TextView>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }
        findViewById<TextView>(R.id.navChat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
