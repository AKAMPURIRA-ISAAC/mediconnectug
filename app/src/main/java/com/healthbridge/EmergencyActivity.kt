package com.healthbridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class EmergencyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)

        val btnBack           = findViewById<ImageView>(R.id.btnBack)
        val btnSOS            = findViewById<Button>(R.id.btnSOS)
        val btnAmbulance      = findViewById<CardView>(R.id.btnAmbulance)
        val btnPolice         = findViewById<CardView>(R.id.btnPolice)
        val btnFire           = findViewById<CardView>(R.id.btnFire)
        val btnNearbyHospitals = findViewById<Button>(R.id.btnNearbyHospitals)
        val tvBloodType       = findViewById<TextView>(R.id.tvBloodType)
        val tvAllergies       = findViewById<TextView>(R.id.tvAllergies)
        val tvConditions      = findViewById<TextView>(R.id.tvConditions)

        // Load medical info from prefs
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        tvBloodType.text  = prefs.getString("bloodType", "Unknown").takeIf { !it.isNullOrBlank() } ?: "Unknown"
        tvAllergies.text  = prefs.getString("allergies", "None recorded").takeIf { !it.isNullOrBlank() } ?: "None recorded"
        tvConditions.text = prefs.getString("conditions", "None recorded").takeIf { !it.isNullOrBlank() } ?: "None recorded"

        btnBack.setOnClickListener { finish() }

        // SOS — confirm dialog before triggering
        btnSOS.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🚨 Send SOS Alert?")
                .setMessage("This will notify emergency contacts and dispatch help to your location.")
                .setPositiveButton("SEND SOS") { _, _ ->
                    Toast.makeText(this, "🚨 SOS ALERT SENT! Help is on the way.", Toast.LENGTH_LONG).show()
                    // In production: send location via API
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnAmbulance.setOnClickListener {
            // Uganda National Ambulance Service
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0800100066")))
        }

        btnPolice.setOnClickListener {
            // Uganda Police Force Emergency
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:999")))
        }

        btnFire.setOnClickListener {
            // Uganda Fire Brigade
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0800199700")))
        }

        btnNearbyHospitals.setOnClickListener {
            val mapsUri = Uri.parse("geo:0.3163,32.5822?q=hospital+Kampala+Uganda")
            val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri)
            mapsIntent.setPackage("com.google.android.apps.maps")
            if (mapsIntent.resolveActivity(packageManager) != null) {
                startActivity(mapsIntent)
            } else {
                startActivity(Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=hospital+Kampala+Uganda")
                ))
            }
        }
    }
}
