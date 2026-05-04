package com.healthbridge

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.healthbridge.network.ApiClient
import com.healthbridge.network.Doctor
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var tvStatus: TextView
    private lateinit var btnLoadDoctors: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tvStatus = findViewById(R.id.tvStatus)
        btnLoadDoctors = findViewById(R.id.btnLoadDoctors)
        
        btnLoadDoctors.setOnClickListener {
            loadDoctors()
        }
    }
    
    private fun loadDoctors() {
        tvStatus.text = "Loading doctors..."
        
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getDoctors()
                if (response.success && response.doctors != null) {
                    val doctors = response.doctors
                    val text = doctors.joinToString("\n\n") { d ->
                        "👨‍⚕️ ${d.name}\n   ${d.specialty}\n   ⭐ ${d.rating} (${d.reviewCount} reviews)\n   💰 UGX ${d.consultationFee}"
                    }
                    tvStatus.text = "✅ Found ${doctors.size} doctors:\n\n$text"
                } else {
                    tvStatus.text = "❌ Error: ${response.error ?: "Unknown"}"
                }
            } catch (e: Exception) {
                tvStatus.text = "❌ Connection failed: ${e.message}"
            }
        }
    }
}
