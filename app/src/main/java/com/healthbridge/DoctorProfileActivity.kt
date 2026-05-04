package com.healthbridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout

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
        val doctorId  = intent.getStringExtra("DOCTOR_ID")        ?: "0"
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
                putExtra("DOCTOR_ID",        doctorId)
                putExtra("DOCTOR_FEE",       fee)
                putExtra("DOCTOR_PHONE",     phone)
            }
            startActivity(editIntent)
        }

        // Action buttons: Call, Message, Direction
        findViewById<View>(R.id.btnCall).setOnClickListener {
            // Uganda doctors — use doctor's phone or Uganda medical helpline
            val dialPhone = phone.ifEmpty { "0800100066" }
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialPhone")))
        }

        findViewById<View>(R.id.btnMessage).setOnClickListener {
            val chatIntent = Intent(this, ChatActivity::class.java).apply {
                putExtra("DOCTOR_NAME", name)
                putExtra("DOCTOR_ID",   doctorId)
            }
            startActivity(chatIntent)
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
                putExtra("DOCTOR_ID",        doctorId)
                putExtra("DOCTOR_FEE",       fee)
            }
            startActivity(bookIntent)
        }
    }
}
