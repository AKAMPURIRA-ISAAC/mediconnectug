package com.healthbridge

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.healthbridge.network.ApiClient
import com.healthbridge.network.CreateChatSessionRequest
import com.healthbridge.util.PermissionManager
import kotlinx.coroutines.launch

class DoctorProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_profile)

        val btnBack            = findViewById<ImageView>(R.id.btnBack)
        val btnFavorite        = findViewById<ImageView>(R.id.btnFavorite)
        val btnShare           = findViewById<ImageView>(R.id.btnShare)
        val btnEdit            = findViewById<ImageView>(R.id.btnEdit)
        val tvDoctorName       = findViewById<TextView>(R.id.tvDoctorName)
        val tabLayout          = findViewById<TabLayout>(R.id.tabLayout)
        val btnBookAppointment = findViewById<Button>(R.id.btnBookAppointment)

        // Handle incoming intent data
        val name      = intent.getStringExtra("DOCTOR_NAME")      ?: getString(R.string.dr_sarah_johnson)
        val specialty = intent.getStringExtra("DOCTOR_SPECIALTY") ?: "Specialist"
        val phone     = intent.getStringExtra("DOCTOR_PHONE")     ?: ""
        val doctorIdString = intent.getStringExtra("DOCTOR_ID")   ?: "0"
        val doctorId  = doctorIdString.toIntOrNull() ?: 0
        val fee       = intent.getIntExtra("DOCTOR_FEE", 50000)
        val clinic    = intent.getStringExtra("DOCTOR_CLINIC")    ?: "Kampala, Uganda"

        tvDoctorName.text = name

        // Show Edit button if the current user is the doctor (role check)
        val prefs    = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val userRole = prefs.getString("userRole", "patient") ?: "patient"
        if (userRole == "doctor") {
            btnEdit.visibility = View.VISIBLE
        }

        btnBack.setOnClickListener { finish() }

        btnFavorite.setOnClickListener {
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
        }

        btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out $name on HealthBridge!\nSpecialty: $specialty\nLocation: $clinic")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Doctor Profile"))
        }

        btnEdit.setOnClickListener {
            val editIntent = Intent(this, EditDoctorProfileActivity::class.java).apply {
                putExtra("DOCTOR_NAME",      name)
                putExtra("DOCTOR_SPECIALTY", specialty)
                putExtra("DOCTOR_ID",        doctorIdString)
                putExtra("DOCTOR_FEE",       fee)
                putExtra("DOCTOR_PHONE",     phone)
            }
            startActivity(editIntent)
        }

        // Action buttons: Call, Message, Direction
        findViewById<View>(R.id.btnCall).setOnClickListener {
            val dialPhone = phone.ifEmpty { "0800100066" }
            PermissionManager.withCallPermission(this) {
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$dialPhone")))
            }
        }

        findViewById<View>(R.id.btnMessage).setOnClickListener {
            // Create a direct chat session with this specific doctor
            Toast.makeText(this, "Starting chat with $name...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                try {
                    Log.d("DoctorProfile", "Creating chat session for doctor: $name (ID: $doctorId)")

                    val response = ApiClient.instance.createChatSession(
                        CreateChatSessionRequest(
                            doctorId = if (doctorId > 0) doctorId else null,
                            chiefComplaint = "Direct consultation request",
                            symptoms = "Patient requested direct consultation with Dr. $name",
                            urgency = "MODERATE"
                        )
                    )

                    Log.d("DoctorProfile", "API Response: success=${response.success}, session=${response.session?.id}")

                    if (response.success && response.session != null) {
                        val session = response.session
                        Log.d("DoctorProfile", "Opening PatientChatActivity with session_id=${session.id}")

                        // Open direct chat with this doctor
                        val chatIntent = Intent(this@DoctorProfileActivity, PatientChatActivity::class.java).apply {
                            putExtra("session_id", session.id)
                            putExtra("doctor_name", name)
                            putExtra("chief_complaint", "Direct consultation")
                            putExtra("urgency_level", "MODERATE")
                        }
                        startActivity(chatIntent)

                        Log.d("DoctorProfile", "PatientChatActivity started successfully")
                    } else {
                        Log.e("DoctorProfile", "Failed to create session: ${response.error}")
                        runOnUiThread {
                            Toast.makeText(
                                this@DoctorProfileActivity,
                                "Could not start chat: ${response.error ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DoctorProfile", "Exception creating chat: ${e.message}", e)
                    e.printStackTrace()

                    runOnUiThread {
                        Toast.makeText(
                            this@DoctorProfileActivity,
                            "Network error: ${e.message}. Please check your connection.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Alternative: Open booking as fallback
                        AlertDialog.Builder(this@DoctorProfileActivity)
                            .setTitle("Chat Unavailable")
                            .setMessage("Unable to start chat. Would you like to book an appointment instead?")
                            .setPositiveButton("Book Appointment") { _, _ ->
                                val bookIntent = Intent(this@DoctorProfileActivity, BookingActivity::class.java).apply {
                                    putExtra("DOCTOR_NAME", name)
                                    putExtra("DOCTOR_SPECIALTY", specialty)
                                    putExtra("DOCTOR_ID", doctorIdString)
                                    putExtra("DOCTOR_FEE", fee)
                                }
                                startActivity(bookIntent)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
        }

        findViewById<View>(R.id.btnDirection).setOnClickListener {
            // Uganda-specific map search centered on Kampala (0.3163, 32.5822)
            val searchQuery = "$name $clinic Uganda"
            val mapsUri = Uri.parse("geo:0.3163,32.5822?q=${Uri.encode(searchQuery)}")
            val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri)
            mapsIntent.setPackage("com.google.android.apps.maps")
            if (mapsIntent.resolveActivity(packageManager) != null) {
                startActivity(mapsIntent)
            } else {
                startActivity(Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=${Uri.encode(searchQuery)}")
                ))
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        btnBookAppointment.setOnClickListener {
            val bookIntent = Intent(this, BookingActivity::class.java).apply {
                putExtra("DOCTOR_NAME",      name)
                putExtra("DOCTOR_SPECIALTY", specialty)
                putExtra("DOCTOR_ID",        doctorIdString)
                putExtra("DOCTOR_FEE",       fee)
            }
            startActivity(bookIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.handleResult(requestCode, permissions, grantResults,
            onDenied = {
                if (it == PermissionManager.RC_CALL_PHONE)
                    PermissionManager.showSettingsDialog(this, "Phone Permission Denied",
                        "Enable Phone permission to call this doctor directly.")
            }
        )
    }
}
